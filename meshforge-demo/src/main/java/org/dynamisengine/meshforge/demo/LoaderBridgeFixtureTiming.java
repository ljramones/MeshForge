package org.dynamisengine.meshforge.demo;

import org.dynamisengine.meshforge.api.Packers;
import org.dynamisengine.meshforge.gpu.GpuGeometryUploadPlan;
import org.dynamisengine.meshforge.gpu.MeshForgeGpuBridge;
import org.dynamisengine.meshforge.gpu.RuntimeGeometryPayload;
import org.dynamisengine.meshforge.gpu.cache.RuntimeGeometryCachePolicy;
import org.dynamisengine.meshforge.gpu.cache.RuntimeGeometryLoader;
import org.dynamisengine.meshforge.loader.MeshLoaders;
import org.dynamisengine.meshforge.pack.spec.PackSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * G3-2 integration timing for canonical engine path:
 * RuntimeGeometryLoader -> MeshForgeGpuBridge -> GpuGeometryUploadPlan.
 */
public final class LoaderBridgeFixtureTiming {
    private static final int WARMUP = 2;
    private static final int ROUNDS = 7;

    private LoaderBridgeFixtureTiming() {
    }

    public static void main(String[] args) throws Exception {
        String fixtureFilter = null;
        boolean forceRebuild = false;
        Path cacheDir = null;
        for (String arg : args) {
            if (arg.startsWith("--fixture=")) {
                fixtureFilter = arg.substring("--fixture=".length()).trim().toLowerCase(Locale.ROOT);
            } else if ("--force-rebuild".equals(arg)) {
                forceRebuild = true;
            } else if (arg.startsWith("--cache-dir=")) {
                cacheDir = Path.of(arg.substring("--cache-dir=".length()).trim());
            }
        }

        Path fixtureDir = Path.of("fixtures", "baseline");
        if (!Files.isDirectory(fixtureDir)) {
            System.out.println("Missing fixture directory: " + fixtureDir.toAbsolutePath());
            return;
        }
        final String filter = fixtureFilter;
        List<Path> fixtures = Files.list(fixtureDir)
            .filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj"))
            .filter(p -> filter == null || p.getFileName().toString().toLowerCase(Locale.ROOT).contains(filter))
            .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
            .toList();
        if (fixtures.isEmpty()) {
            System.out.println("No fixtures matched.");
            return;
        }

        MeshLoaders loaders = MeshLoaders.defaultsFast();
        PackSpec spec = Packers.realtime();
        RuntimeGeometryLoader runtimeLoader = new RuntimeGeometryLoader(loaders, spec);
        if (cacheDir != null) {
            Files.createDirectories(cacheDir);
        }

        System.out.println("| Fixture | Cache-hit Load ms | Upload Plan ms | Loader->Bridge ms | Triangles |");
        System.out.println("|---|---:|---:|---:|---:|");
        for (Path fixture : fixtures) {
            Row row = measureFixture(runtimeLoader, cacheDir, fixture, forceRebuild);
            System.out.printf(
                Locale.ROOT,
                "| `%s` | %.3f | %.3f | %.3f | %d |%n",
                row.name, row.cacheLoadMs, row.uploadPlanMs, row.totalMs, row.triangles
            );
        }
    }

    private static Row measureFixture(
        RuntimeGeometryLoader runtimeLoader,
        Path cacheDir,
        Path fixture,
        boolean forceRebuild
    ) throws Exception {
        Path cacheFile = cacheDir == null
            ? RuntimeGeometryCachePolicy.sidecarPathFor(fixture)
            : cacheDir.resolve(fixture.getFileName().toString() + ".mfgc");

        RuntimeGeometryLoader.Result seeded = runtimeLoader.load(fixture, cacheFile, forceRebuild);
        RuntimeGeometryPayload seededPayload = seeded.payload();
        int triangles = seededPayload.indexCount() / 3;

        double[] loadPass = new double[3];
        double[] uploadPass = new double[3];
        double[] totalPass = new double[3];

        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < WARMUP; i++) {
                RuntimeGeometryPayload warmPayload = runtimeLoader.load(fixture, cacheFile, false).payload();
                GpuGeometryUploadPlan warmPlan = MeshForgeGpuBridge.buildUploadPlan(warmPayload);
                assertUploadPlanConsistency(warmPayload, warmPlan);
            }

            double loadNs = 0.0;
            double uploadNs = 0.0;
            double totalNs = 0.0;
            for (int i = 0; i < ROUNDS; i++) {
                long t0 = System.nanoTime();
                RuntimeGeometryLoader.Result loaded = runtimeLoader.load(fixture, cacheFile, false);
                long t1 = System.nanoTime();
                GpuGeometryUploadPlan uploadPlan = MeshForgeGpuBridge.buildUploadPlan(loaded.payload());
                long t2 = System.nanoTime();

                if (loaded.source() != RuntimeGeometryLoader.Source.CACHE) {
                    throw new IllegalStateException("Expected cache hit during timing for " + fixture);
                }
                assertUploadPlanConsistency(loaded.payload(), uploadPlan);

                loadNs += (t1 - t0);
                uploadNs += (t2 - t1);
                totalNs += (t2 - t0);
            }

            loadPass[pass] = loadNs / ROUNDS / 1_000_000.0;
            uploadPass[pass] = uploadNs / ROUNDS / 1_000_000.0;
            totalPass[pass] = totalNs / ROUNDS / 1_000_000.0;
        }

        return new Row(
            fixture.getFileName().toString(),
            median(loadPass),
            median(uploadPass),
            median(totalPass),
            triangles
        );
    }

    private static void assertUploadPlanConsistency(RuntimeGeometryPayload payload, GpuGeometryUploadPlan uploadPlan) {
        GpuGeometryUploadPlan.VertexBinding vb = uploadPlan.vertexBinding();
        if (vb.byteSize() != payload.vertexBytes().remaining()) {
            throw new IllegalStateException("Vertex byte mismatch");
        }
        if (vb.vertexCount() != payload.vertexCount()) {
            throw new IllegalStateException("Vertex count mismatch");
        }
        if (vb.layout().strideBytes() != payload.layout().strideBytes()) {
            throw new IllegalStateException("Vertex layout mismatch");
        }
        if (uploadPlan.submeshes().size() != payload.submeshes().size()) {
            throw new IllegalStateException("Submesh count mismatch");
        }

        if (payload.indexType() == null || payload.indexCount() == 0 || payload.indexBytes() == null) {
            if (uploadPlan.indexBinding() != null) {
                throw new IllegalStateException("Unexpected index binding for non-indexed payload");
            }
            return;
        }
        GpuGeometryUploadPlan.IndexBinding ib = uploadPlan.indexBinding();
        if (ib == null) {
            throw new IllegalStateException("Missing index binding");
        }
        if (ib.type() != payload.indexType()) {
            throw new IllegalStateException("Index type mismatch");
        }
        if (ib.indexCount() != payload.indexCount()) {
            throw new IllegalStateException("Index count mismatch");
        }
        if (ib.byteSize() != payload.indexBytes().remaining()) {
            throw new IllegalStateException("Index byte mismatch");
        }
    }

    private static double median(double[] values) {
        List<Double> sorted = new ArrayList<>(values.length);
        for (double value : values) {
            sorted.add(value);
        }
        sorted.sort(Double::compareTo);
        return sorted.get(sorted.size() / 2);
    }

    private record Row(
        String name,
        double cacheLoadMs,
        double uploadPlanMs,
        double totalMs,
        int triangles
    ) {
    }
}
