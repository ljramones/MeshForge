package org.dynamisengine.meshforge.demo;

import org.dynamisengine.meshforge.api.Pipelines;
import org.dynamisengine.meshforge.core.bounds.Aabbf;
import org.dynamisengine.meshforge.core.mesh.MeshData;
import org.dynamisengine.meshforge.loader.MeshLoaders;
import org.dynamisengine.meshforge.mgi.MgiMeshDataCodec;
import org.dynamisengine.meshforge.mgi.MgiMeshletBounds;
import org.dynamisengine.meshforge.mgi.MgiMeshletData;
import org.dynamisengine.meshforge.ops.optimize.MeshletClusters;
import org.dynamisengine.meshforge.pack.buffer.Meshlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Compares runtime meshlet generation against loading prebaked meshlet metadata from MGI.
 */
public final class MeshletMgiLoadVsRuntimeTiming {
    private static final int DEFAULT_WARMUP = 2;
    private static final int DEFAULT_RUNS = 9;
    private static final int DEFAULT_MAX_VERTS = 64;
    private static final int DEFAULT_MAX_TRIS = 64;

    private MeshletMgiLoadVsRuntimeTiming() {
    }

    public static void main(String[] args) throws Exception {
        String fixtureFilter = null;
        int warmup = DEFAULT_WARMUP;
        int runs = DEFAULT_RUNS;
        int maxVerts = DEFAULT_MAX_VERTS;
        int maxTris = DEFAULT_MAX_TRIS;

        for (String arg : args) {
            if (arg.startsWith("--fixture=")) {
                fixtureFilter = arg.substring("--fixture=".length()).trim().toLowerCase(Locale.ROOT);
            } else if (arg.startsWith("--warmup=")) {
                warmup = parsePositive(arg.substring("--warmup=".length()), "warmup");
            } else if (arg.startsWith("--runs=")) {
                runs = parsePositive(arg.substring("--runs=".length()), "runs");
            } else if (arg.startsWith("--max-verts=")) {
                maxVerts = parsePositive(arg.substring("--max-verts=".length()), "max-verts");
            } else if (arg.startsWith("--max-tris=")) {
                maxTris = parsePositive(arg.substring("--max-tris=".length()), "max-tris");
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
            .filter(p -> {
                String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                return name.contains("revithouse") || name.contains("dragon");
            })
            .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
            .toList();

        if (fixtures.isEmpty()) {
            System.out.println("No fixtures matched (expected RevitHouse/dragon unless --fixture is used).");
            return;
        }

        MeshLoaders loaders = MeshLoaders.defaultsFast();
        MgiMeshDataCodec codec = new MgiMeshDataCodec();
        List<Row> rows = new ArrayList<>();

        for (Path fixture : fixtures) {
            rows.addAll(measureFixture(loaders, codec, fixture, warmup, runs, maxVerts, maxTris));
        }

        System.out.println("meshlet metadata: runtime generation vs prebaked MGI load");
        System.out.printf(Locale.ROOT, "warmup=%d runs=%d maxVerts=%d maxTris=%d%n", warmup, runs, maxVerts, maxTris);
        System.out.println();
        System.out.println(
            "| Fixture | Mode | Meshlets | Time ms (median) | Time ms (p95) | Whole Visible Tris | Meshlet Visible Tris | Triangle Reduction |");
        System.out.println("|---|---|---:|---:|---:|---:|---:|---:|");
        for (Row row : rows) {
            System.out.printf(
                Locale.ROOT,
                "| `%s` | `%s` | %d | %.3f | %.3f | %d | %d | %.2f%% |%n",
                row.fixture,
                row.mode,
                row.meshletCount,
                row.medianMs,
                row.p95Ms,
                row.wholeVisibleTriangles,
                row.meshletVisibleTriangles,
                row.triangleReductionPct
            );
        }
    }

    private static List<Row> measureFixture(
        MeshLoaders loaders,
        MgiMeshDataCodec codec,
        Path fixture,
        int warmup,
        int runs,
        int maxVerts,
        int maxTris
    ) throws Exception {
        int total = warmup + runs;
        List<Long> generationNs = new ArrayList<>(runs);
        Summary generatedSummary = null;

        for (int i = 0; i < total; i++) {
            MeshData loaded = loaders.load(fixture);
            MeshData processed = Pipelines.realtimeFast(loaded);
            int[] indices = processed.indicesOrNull();
            if (indices == null || indices.length == 0) {
                throw new IllegalStateException("Fixture has no indices: " + fixture);
            }

            long start = System.nanoTime();
            List<Meshlet> meshlets = MeshletClusters.buildMeshlets(processed, indices, maxVerts, maxTris);
            long end = System.nanoTime();

            if (i >= warmup) {
                generationNs.add(end - start);
            }
            generatedSummary = summarizeGenerated(meshlets, indices.length / 3);
        }

        Path prebaked = ensureMeshletSidecar(fixture, maxVerts, maxTris);
        byte[] sidecarBytes = Files.readAllBytes(prebaked);
        List<Long> loadNs = new ArrayList<>(runs);
        Summary loadedSummary = null;

        for (int i = 0; i < total; i++) {
            long start = System.nanoTime();
            MgiMeshDataCodec.RuntimeDecodeResult decoded = codec.readForRuntime(sidecarBytes);
            long end = System.nanoTime();
            if (!decoded.meshletDataPresent()) {
                throw new IllegalStateException("Prebaked sidecar missing meshlet metadata: " + prebaked);
            }

            if (i >= warmup) {
                loadNs.add(end - start);
            }
            int totalTriangles = decoded.meshData().indicesOrNull().length / 3;
            loadedSummary = summarizeLoaded(decoded.meshletDataOrNull(), totalTriangles);
        }

        String fixtureName = fixture.getFileName().toString();
        return List.of(
            new Row(
                fixtureName,
                "runtime-generate",
                generatedSummary.meshletCount,
                toMs(median(generationNs)),
                toMs(p95(generationNs)),
                generatedSummary.wholeVisibleTriangles,
                generatedSummary.meshletVisibleTriangles,
                generatedSummary.triangleReductionPct
            ),
            new Row(
                fixtureName,
                "mgi-prebaked-load",
                loadedSummary.meshletCount,
                toMs(median(loadNs)),
                toMs(p95(loadNs)),
                loadedSummary.wholeVisibleTriangles,
                loadedSummary.meshletVisibleTriangles,
                loadedSummary.triangleReductionPct
            )
        );
    }

    private static Path ensureMeshletSidecar(Path sourceObj, int maxVerts, int maxTris) throws Exception {
        Path sidecar = meshletSidecarPathFor(sourceObj);
        if (Files.isRegularFile(sidecar)) {
            return sidecar;
        }
        ObjToMgiMain.main(new String[] {
            "--input=" + sourceObj,
            "--with-meshlets",
            "--overwrite",
            "--meshlet-max-verts=" + maxVerts,
            "--meshlet-max-tris=" + maxTris
        });
        if (!Files.isRegularFile(sidecar)) {
            throw new IllegalStateException("Failed to create meshlet sidecar: " + sidecar);
        }
        return sidecar;
    }

    private static Path meshletSidecarPathFor(Path sourceObj) {
        String file = sourceObj.getFileName().toString();
        int dot = file.lastIndexOf('.');
        String base = dot <= 0 ? file : file.substring(0, dot);
        return sourceObj.resolveSibling(base + ".meshlets.mgi");
    }

    private static Summary summarizeGenerated(List<Meshlet> meshlets, int totalTriangles) {
        if (meshlets.isEmpty()) {
            return new Summary(0, totalTriangles, 0, 0.0);
        }
        Aabbf union = unionGenerated(meshlets);
        Aabbf view = centeredWindow(union, 0.5f);
        int wholeVisible = intersects(union, view) ? totalTriangles : 0;
        int meshletVisible = 0;
        for (Meshlet meshlet : meshlets) {
            if (intersects(meshlet.bounds(), view)) {
                meshletVisible += meshlet.triangleCount();
            }
        }
        double reduction = wholeVisible == 0
            ? 0.0
            : (1.0 - (meshletVisible / (double) wholeVisible)) * 100.0;
        return new Summary(meshlets.size(), wholeVisible, meshletVisible, reduction);
    }

    private static Summary summarizeLoaded(MgiMeshletData meshlets, int totalTriangles) {
        List<MgiMeshletBounds> bounds = meshlets.bounds();
        if (bounds.isEmpty()) {
            return new Summary(0, totalTriangles, 0, 0.0);
        }
        Box union = unionLoaded(bounds);
        Box view = centeredWindow(union, 0.5f);
        int wholeVisible = intersects(union, view) ? totalTriangles : 0;
        int meshletVisible = 0;
        for (var descriptor : meshlets.descriptors()) {
            if (intersects(bounds.get(descriptor.boundsIndex()), view)) {
                meshletVisible += descriptor.triangleCount();
            }
        }
        double reduction = wholeVisible == 0
            ? 0.0
            : (1.0 - (meshletVisible / (double) wholeVisible)) * 100.0;
        return new Summary(meshlets.descriptors().size(), wholeVisible, meshletVisible, reduction);
    }

    private static Aabbf unionGenerated(List<Meshlet> meshlets) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (Meshlet meshlet : meshlets) {
            Aabbf b = meshlet.bounds();
            if (b.minX() < minX) minX = b.minX();
            if (b.minY() < minY) minY = b.minY();
            if (b.minZ() < minZ) minZ = b.minZ();
            if (b.maxX() > maxX) maxX = b.maxX();
            if (b.maxY() > maxY) maxY = b.maxY();
            if (b.maxZ() > maxZ) maxZ = b.maxZ();
        }

        return new Aabbf(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Box unionLoaded(List<MgiMeshletBounds> bounds) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (MgiMeshletBounds b : bounds) {
            if (b.minX() < minX) minX = b.minX();
            if (b.minY() < minY) minY = b.minY();
            if (b.minZ() < minZ) minZ = b.minZ();
            if (b.maxX() > maxX) maxX = b.maxX();
            if (b.maxY() > maxY) maxY = b.maxY();
            if (b.maxZ() > maxZ) maxZ = b.maxZ();
        }

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static Aabbf centeredWindow(Aabbf global, float ratio) {
        float cx = (global.minX() + global.maxX()) * 0.5f;
        float cy = (global.minY() + global.maxY()) * 0.5f;
        float cz = (global.minZ() + global.maxZ()) * 0.5f;
        float hx = (global.maxX() - global.minX()) * 0.5f * ratio;
        float hy = (global.maxY() - global.minY()) * 0.5f * ratio;
        float hz = (global.maxZ() - global.minZ()) * 0.5f * ratio;
        return new Aabbf(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
    }

    private static Box centeredWindow(Box global, float ratio) {
        float cx = (global.minX + global.maxX) * 0.5f;
        float cy = (global.minY + global.maxY) * 0.5f;
        float cz = (global.minZ + global.maxZ) * 0.5f;
        float hx = (global.maxX - global.minX) * 0.5f * ratio;
        float hy = (global.maxY - global.minY) * 0.5f * ratio;
        float hz = (global.maxZ - global.minZ) * 0.5f * ratio;
        return new Box(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz);
    }

    private static boolean intersects(Aabbf a, Aabbf b) {
        return a.maxX() >= b.minX() && a.minX() <= b.maxX()
            && a.maxY() >= b.minY() && a.minY() <= b.maxY()
            && a.maxZ() >= b.minZ() && a.minZ() <= b.maxZ();
    }

    private static boolean intersects(Box a, Box b) {
        return a.maxX >= b.minX && a.minX <= b.maxX
            && a.maxY >= b.minY && a.minY <= b.maxY
            && a.maxZ >= b.minZ && a.minZ <= b.maxZ;
    }

    private static boolean intersects(MgiMeshletBounds a, Box b) {
        return a.maxX() >= b.minX && a.minX() <= b.maxX
            && a.maxY() >= b.minY && a.minY() <= b.maxY
            && a.maxZ() >= b.minZ && a.minZ() <= b.maxZ;
    }

    private static long median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        return sorted.get(sorted.size() / 2);
    }

    private static long p95(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int idx = (int) Math.ceil(sorted.size() * 0.95) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx);
    }

    private static double toMs(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static int parsePositive(String raw, String label) {
        int parsed = Integer.parseInt(raw);
        if (parsed <= 0) {
            throw new IllegalArgumentException(label + " must be > 0");
        }
        return parsed;
    }

    private record Summary(
        int meshletCount,
        int wholeVisibleTriangles,
        int meshletVisibleTriangles,
        double triangleReductionPct
    ) {
    }

    private record Box(
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ
    ) {
    }

    private record Row(
        String fixture,
        String mode,
        int meshletCount,
        double medianMs,
        double p95Ms,
        int wholeVisibleTriangles,
        int meshletVisibleTriangles,
        double triangleReductionPct
    ) {
    }
}
