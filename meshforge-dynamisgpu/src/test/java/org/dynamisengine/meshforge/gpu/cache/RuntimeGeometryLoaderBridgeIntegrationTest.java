package org.dynamisengine.meshforge.gpu.cache;

import org.dynamisengine.meshforge.api.Packers;
import org.dynamisengine.meshforge.gpu.GpuGeometryUploadPlan;
import org.dynamisengine.meshforge.gpu.MeshForgeGpuBridge;
import org.dynamisengine.meshforge.gpu.RuntimeGeometryPayload;
import org.dynamisengine.meshforge.loader.MeshLoaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RuntimeGeometryLoaderBridgeIntegrationTest {

    @Test
    void cacheHitPayloadBuildsConsistentUploadPlan(@TempDir Path dir) throws Exception {
        Path source = writeMinimalObj(dir.resolve("triangle.obj"));
        RuntimeGeometryLoader loader = new RuntimeGeometryLoader(MeshLoaders.defaultsFast(), Packers.realtimeFast());

        RuntimeGeometryLoader.Result first = loader.load(source);
        RuntimeGeometryLoader.Result second = loader.load(source);

        assertEquals(RuntimeGeometryLoader.Source.REBUILT, first.source());
        assertEquals(RuntimeGeometryLoader.Source.CACHE, second.source());

        RuntimeGeometryPayload payload = second.payload();
        GpuGeometryUploadPlan plan = MeshForgeGpuBridge.buildUploadPlan(payload);

        assertEquals(payload.layout(), plan.vertexBinding().layout());
        assertEquals(payload.vertexCount(), plan.vertexBinding().vertexCount());
        assertEquals(payload.layout().strideBytes(), plan.vertexBinding().strideBytes());
        assertEquals(payload.vertexBytes().remaining(), plan.vertexBinding().byteSize());
        assertEquals(payload.submeshes().size(), plan.submeshes().size());
        assertTrue(payload.vertexCount() > 0);

        if (payload.indexType() != null && payload.indexCount() > 0 && payload.indexBytes() != null) {
            assertNotNull(plan.indexBinding());
            assertEquals(payload.indexType(), plan.indexBinding().type());
            assertEquals(payload.indexCount(), plan.indexBinding().indexCount());
            assertEquals(payload.indexBytes().remaining(), plan.indexBinding().byteSize());
        }
    }

    private static Path writeMinimalObj(Path path) throws IOException {
        String data = """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            f 1 2 3
            """;
        Files.writeString(path, data);
        return path;
    }
}
