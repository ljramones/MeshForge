package org.dynamisengine.meshforge.demo;

import org.dynamisengine.meshforge.api.Packers;
import org.dynamisengine.meshforge.api.Pipelines;
import org.dynamisengine.meshforge.core.mesh.MeshData;
import org.dynamisengine.meshforge.gpu.GpuGeometryUploadPlan;
import org.dynamisengine.meshforge.gpu.MeshForgeGpuBridge;
import org.dynamisengine.meshforge.gpu.RuntimeGeometryPayload;
import org.dynamisengine.meshforge.loader.MeshLoaders;
import org.dynamisengine.meshforge.pack.packer.MeshPacker;
import org.dynamisengine.meshforge.pack.spec.PackSpec;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Measures runtime geometry timing split with explicit four-point timestamps:
 * T0 prep start, T1 packed payload ready, T2 submit accepted, T3 transfer complete.
 */
public final class PrepQueueTransferTtfuFixtureTiming {
    private static final int DEFAULT_WARMUP = 2;
    private static final int DEFAULT_RUNS = 9;
    private static final int DEFAULT_INFLIGHT = 2;

    private PrepQueueTransferTtfuFixtureTiming() {
    }

    public static void main(String[] args) throws Exception {
        String fixtureFilter = null;
        int warmup = DEFAULT_WARMUP;
        int runs = DEFAULT_RUNS;
        int maxInflight = DEFAULT_INFLIGHT;

        for (String arg : args) {
            if (arg.startsWith("--fixture=")) {
                fixtureFilter = arg.substring("--fixture=".length()).trim().toLowerCase(Locale.ROOT);
            } else if (arg.startsWith("--warmup=")) {
                warmup = parsePositive(arg.substring("--warmup=".length()), "warmup");
            } else if (arg.startsWith("--runs=")) {
                runs = parsePositive(arg.substring("--runs=".length()), "runs");
            } else if (arg.startsWith("--max-inflight=")) {
                maxInflight = parsePositive(arg.substring("--max-inflight=".length()), "max-inflight");
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

        System.out.println("prep+queue+transfer timing (median + p95)");
        System.out.printf(Locale.ROOT, "warmup=%d runs=%d maxInflight=%d%n", warmup, runs, maxInflight);
        System.out.println();
        System.out.println("| Fixture | Prep ms | Queue ms | Transfer ms | Total TTFU ms | Triangles | Upload Bytes | ");
        System.out.println("|---|---:|---:|---:|---:|---:|---:|");

        for (Path fixture : fixtures) {
            Row row = measureFixture(loaders, spec, fixture, warmup, runs, maxInflight);
            System.out.printf(
                Locale.ROOT,
                "| `%s` | %.3f (p95 %.3f) | %.3f (p95 %.3f) | %.3f (p95 %.3f) | %.3f (p95 %.3f) | %d | %d |%n",
                row.name,
                row.prepMedianMs,
                row.prepP95Ms,
                row.queueMedianMs,
                row.queueP95Ms,
                row.transferMedianMs,
                row.transferP95Ms,
                row.totalMedianMs,
                row.totalP95Ms,
                row.triangles,
                row.uploadBytes
            );
        }
    }

    private static Row measureFixture(
        MeshLoaders loaders,
        PackSpec spec,
        Path fixture,
        int warmup,
        int runs,
        int maxInflight
    ) throws Exception {
        int total = warmup + runs;
        List<Long> prepNs = new ArrayList<>(runs);
        List<Long> queueNs = new ArrayList<>(runs);
        List<Long> transferNs = new ArrayList<>(runs);
        List<Long> totalNs = new ArrayList<>(runs);

        int triangles = 0;
        int uploadBytes = 0;

        try (AsyncUploadSimulator uploader = new AsyncUploadSimulator(maxInflight)) {
            for (int i = 0; i < total; i++) {
                long t0 = System.nanoTime();
                MeshData loaded = loaders.load(fixture);
                MeshData processed = Pipelines.realtimeFast(loaded);
                MeshPacker.RuntimePackPlan runtimePlan = MeshPacker.buildRuntimePlan(processed, spec);
                MeshPacker.RuntimePackWorkspace workspace = new MeshPacker.RuntimePackWorkspace();
                MeshPacker.packPlannedInto(runtimePlan, workspace);
                RuntimeGeometryPayload payload =
                    MeshForgeGpuBridge.payloadFromRuntimeWorkspace(runtimePlan.layout(), workspace);
                GpuGeometryUploadPlan uploadPlan = MeshForgeGpuBridge.buildUploadPlan(payload);
                long t1 = System.nanoTime();

                PendingTransfer pending = uploader.submit(payload, uploadPlan, t0, t1);
                TransferTiming timing = pending.await();

                if (i >= warmup) {
                    prepNs.add(timing.prepNanos());
                    queueNs.add(timing.queueWaitNanos());
                    transferNs.add(timing.transferNanos());
                    totalNs.add(timing.totalTtfuNanos());
                }

                if (triangles == 0) {
                    triangles = payload.indexCount() / 3;
                }
                if (uploadBytes == 0) {
                    uploadBytes = uploadPlan.vertexBinding().byteSize()
                        + (uploadPlan.indexBinding() == null ? 0 : uploadPlan.indexBinding().byteSize());
                }
            }
        }

        return new Row(
            fixture.getFileName().toString(),
            toMs(median(prepNs)),
            toMs(p95(prepNs)),
            toMs(median(queueNs)),
            toMs(p95(queueNs)),
            toMs(median(transferNs)),
            toMs(p95(transferNs)),
            toMs(median(totalNs)),
            toMs(p95(totalNs)),
            triangles,
            uploadBytes
        );
    }

    private static int simulateTransfer(RuntimeGeometryPayload payload, GpuGeometryUploadPlan plan) {
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

    private record PendingTransfer(long t0, long t1, long t2, CompletableFuture<Integer> transferFuture) {
        private TransferTiming await() throws Exception {
            transferFuture.get();
            long t3 = System.nanoTime();
            return new TransferTiming(t1 - t0, t2 - t1, t3 - t2, t3 - t0);
        }
    }

    private record TransferTiming(long prepNanos, long queueWaitNanos, long transferNanos, long totalTtfuNanos) {
    }

    private record Row(
        String name,
        double prepMedianMs,
        double prepP95Ms,
        double queueMedianMs,
        double queueP95Ms,
        double transferMedianMs,
        double transferP95Ms,
        double totalMedianMs,
        double totalP95Ms,
        int triangles,
        int uploadBytes
    ) {
    }

    private static final class AsyncUploadSimulator implements AutoCloseable {
        private final Semaphore inflight;
        private final ExecutorService workers;

        private AsyncUploadSimulator(int maxInflight) {
            this.inflight = new Semaphore(maxInflight, true);
            this.workers = Executors.newFixedThreadPool(maxInflight, new UploadThreadFactory());
        }

        private PendingTransfer submit(
            RuntimeGeometryPayload payload,
            GpuGeometryUploadPlan plan,
            long t0,
            long t1
        ) throws InterruptedException {
            inflight.acquire();
            long t2 = System.nanoTime();
            CompletableFuture<Integer> future =
                CompletableFuture.supplyAsync(() -> simulateTransfer(payload, plan), workers)
                    .whenComplete((ignored, error) -> inflight.release());
            return new PendingTransfer(t0, t1, t2, future);
        }

        @Override
        public void close() {
            workers.shutdownNow();
        }
    }

    private static final class UploadThreadFactory implements ThreadFactory {
        private final AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread t = new Thread(runnable, "meshforge-upload-sim-" + nextId.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
