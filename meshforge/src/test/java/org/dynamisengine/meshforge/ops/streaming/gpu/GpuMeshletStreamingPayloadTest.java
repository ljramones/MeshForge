package org.dynamisengine.meshforge.ops.streaming.gpu;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GpuMeshletStreamingPayloadTest {

    @Test
    void rejectsMismatchedPayloadLength() {
        assertThrows(IllegalArgumentException.class,
            () -> new GpuMeshletStreamingPayload(2, 0, 5, new int[5]));
    }

    @Test
    void rejectsStrideBelowRequiredComponents() {
        assertThrows(IllegalArgumentException.class,
            () -> new GpuMeshletStreamingPayload(1, 0, 4, new int[4]));
    }

    @Test
    void reportsExpectedMetadataAndByteContract() {
        GpuMeshletStreamingPayload payload = new GpuMeshletStreamingPayload(
            2,
            0,
            5,
            new int[] {0, 0, 32, 0, 4096, 1, 32, 16, 4096, 2048}
        );

        assertEquals(10, payload.unitsIntCount());
        assertEquals(10, payload.expectedUnitsPayloadLengthInts());
        assertEquals(40, payload.unitsByteSize());
        assertEquals(20, payload.unitsStrideBytes());
        assertEquals(10, payload.toUnitsIntBuffer().remaining());
    }

    @Test
    void unitsPayloadAccessorReturnsDefensiveCopy() {
        GpuMeshletStreamingPayload payload = new GpuMeshletStreamingPayload(
            1,
            0,
            5,
            new int[] {0, 0, 32, 0, 4096}
        );

        int[] copy = payload.unitsPayload();
        copy[0] = 999;

        assertArrayEquals(new int[] {0, 0, 32, 0, 4096}, payload.unitsPayload());
    }

    @Test
    void byteBufferExportMatchesPayloadOrder() {
        GpuMeshletStreamingPayload payload = new GpuMeshletStreamingPayload(
            1,
            0,
            5,
            new int[] {2, 96, 8, 8192, 1024}
        );

        ByteBuffer bytes = payload.toUnitsByteBuffer().order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(20, bytes.remaining());
        assertEquals(2, bytes.getInt());
        assertEquals(96, bytes.getInt());
        assertEquals(8, bytes.getInt());
        assertEquals(8192, bytes.getInt());
        assertEquals(1024, bytes.getInt());
    }
}
