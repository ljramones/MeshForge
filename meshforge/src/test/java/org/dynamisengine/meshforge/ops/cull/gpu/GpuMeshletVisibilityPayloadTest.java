package org.dynamisengine.meshforge.ops.cull.gpu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GpuMeshletVisibilityPayloadTest {

    @Test
    void rejectsMismatchedPayloadLength() {
        assertThrows(IllegalArgumentException.class,
            () -> new GpuMeshletVisibilityPayload(2, 0, 6, new float[6]));
    }

    @Test
    void rejectsStrideBelowRequiredComponents() {
        assertThrows(IllegalArgumentException.class,
            () -> new GpuMeshletVisibilityPayload(1, 0, 5, new float[5]));
    }
}
