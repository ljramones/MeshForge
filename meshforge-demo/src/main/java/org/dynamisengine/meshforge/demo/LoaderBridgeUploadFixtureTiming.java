package org.dynamisengine.meshforge.demo;

import org.dynamisengine.meshforge.api.Packers;
import org.dynamisengine.meshforge.gpu.GpuGeometryUploadPlan;
import org.dynamisengine.meshforge.gpu.MeshForgeGpuBridge;
import org.dynamisengine.meshforge.gpu.RuntimeGeometryPayload;
import org.dynamisengine.meshforge.gpu.cache.RuntimeGeometryCachePolicy;
import org.dynamisengine.meshforge.gpu.cache.RuntimeGeometryLoader;
import org.dynamisengine.meshforge.loader.MeshLoaders;
import org.dynamisengine.meshforge.pack.spec.PackSpec;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * End-to-end runtime path timing:
 * cache-hit RuntimeGeometryLoader -> MeshForgeGpuBridge -> simulated GPU upload copy.
 */
public final class LoaderBridgeUploadFixtureTiming {
    private static final int WARMUP = 2;
    private static final int ROUNDS = 7;

    private LoaderBridgeUploadFixtureTiming() {
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

        System.out.println("| Fixture | Cache-hit Load ms | Bridge ms | Sim Upload ms | Total ms | Triangles |");
        System.out.println("|---|---:|---:|---:|---:|---:|");
        for (Path fixture : fixtures) {
            Row row = measureFixture(runtimeLoader, cacheDir, fixture, forceRebuild);
            System.out.printf(
                Locale.ROOT,
                "| `%s` | %.3f | %.3f | %.3f | %.3f | %d |%n",
                row.name, row.cacheLoadMs, row.bridgeMs, row.uploadMs, row.totalMs, row.triangles
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
        int triangles = seeded.payload().indexCount() / 3;

        double[] loadPass = new double[3];
        double[] bridgePass = new double[3];
        double[] uploadPass = new double[3];
        double[] totalPass = new double[3];

        for (int pass = 0; pass < 3; pass++) {
            for (int i = 0; i < WARMUP; i++) {
                RuntimeGeometryPayload warmPayload = runtimeLoader.load(fixture, cacheFile, false).payload();
                GpuGeometryUploadPlan warmPlan = MeshForgeGpuBridge.buildUploadPlan(warmPayload);
                simulateUpload(warmPayload, warmPlan);
            }

            double loadNs = 0.0;
            double bridgeNs = 0.0;
            double uploadNs = 0.0;
            double totalNs = 0.0;

            for (int i = 0; i < ROUNDS; i++) {
                long t0 = System.nanoTime();
                RuntimeGeometryLoader.Result loaded = runtimeLoader.load(fixture, cacheFile, false);
                long t1 = System.nanoTime();
                GpuGeometryUploadPlan plan = MeshForgeGpuBridge.buildUploadPlan(loaded.payload());
                long t2 = System.nanoTime();
                int checksum = simulateUpload(loaded.payload(), plan);
                long t3 = System.nanoTime();

                if (loaded.source() != RuntimeGeometryLoader.Source.CACHE) {
                    throw new IllegalStateException("Expected cache-hit load during timing for " + fixture);
                }
                if (checksum == Integer.MIN_VALUE) {
                    throw new IllegalStateException("Unreachable checksum guard");
                }

                loadNs += (t1 - t0);
                bridgeNs += (t2 - t1);
                uploadNs += (t3 - t2);
                totalNs += (t3 - t0);
            }

            loadPass[pass] = loadNs / ROUNDS / 1_000_000.0;
            bridgePass[pass] = bridgeNs / ROUNDS / 1_000_000.0;
            uploadPass[pass] = uploadNs / ROUNDS / 1_000_000.0;
            totalPass[pass] = totalNs / ROUNDS / 1_000_000.0;
        }

        return new Row(
            fixture.getFileName().toString(),
            median(loadPass),
            median(bridgePass),
            median(uploadPass),
            median(totalPass),
            triangles
        );
    }

    private static int simulateUpload(RuntimeGeometryPayload payload, GpuGeometryUploadPlan plan) {
        ByteBuffer vertexSrc = payload.vertexBytes().asReadOnlyBuffer();
        vertexSrc.position(0);
        ByteBuffer vertexDst = ByteBuffer.allocateDirect(plan.vertexBinding().byteSize());
        vertexDst.put(vertexSrc);

        int checksum = vertexDst.capacity();
        if (plan.indexBinding() != null && payload.indexBytes() != null) {
            ByteBuffer indexSrc = payload.indexBytes().asReadOnlyBuffer();
            indexSrc.position(0);
            ByteBuffer indexDst = ByteBuffer.allocateDirect(plan.indexBinding().byteSize());
            indexDst.put(indexSrc);
            checksum += indexDst.capacity();
        }
        return checksum;
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
        double bridgeMs,
        double uploadMs,
        double totalMs,
        int triangles
    ) {
    }
}
