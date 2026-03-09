package org.dynamisengine.meshforge.ops.streaming.gpu;

import org.dynamisengine.meshforge.ops.streaming.MeshletStreamUnitMetadata;
import org.dynamisengine.meshforge.ops.streaming.MeshletStreamingMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MeshletStreamingUploadPrepTest {

    @Test
    void packsUnitsInExpectedOrder() {
        MeshletStreamingMetadata metadata = new MeshletStreamingMetadata(List.of(
            new MeshletStreamUnitMetadata(0, 0, 32, 0, 4096),
            new MeshletStreamUnitMetadata(1, 32, 16, 4096, 2048)
        ));

        GpuMeshletStreamingPayload payload = MeshletStreamingUploadPrep.fromMetadata(metadata);

        assertEquals(2, payload.unitCount());
        assertEquals(0, payload.unitsOffsetInts());
        assertEquals(5, payload.unitsStrideInts());
        assertEquals(40, payload.unitsByteSize());
        assertArrayEquals(
            new int[] {
                0, 0, 32, 0, 4096,
                1, 32, 16, 4096, 2048
            },
            payload.unitsPayload()
        );
    }
}
