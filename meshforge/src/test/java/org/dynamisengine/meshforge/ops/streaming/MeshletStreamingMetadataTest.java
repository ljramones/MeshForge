package org.dynamisengine.meshforge.ops.streaming;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MeshletStreamingMetadataTest {

    @Test
    void acceptsOrderedNonOverlappingUnits() {
        MeshletStreamingMetadata metadata = new MeshletStreamingMetadata(List.of(
            new MeshletStreamUnitMetadata(0, 0, 32, 0, 4096),
            new MeshletStreamUnitMetadata(1, 32, 16, 4096, 2048)
        ));

        assertEquals(2, metadata.unitCount());
    }

    @Test
    void rejectsNonIncreasingUnitIds() {
        assertThrows(IllegalArgumentException.class, () -> new MeshletStreamingMetadata(List.of(
            new MeshletStreamUnitMetadata(0, 0, 32, 0, 4096),
            new MeshletStreamUnitMetadata(0, 32, 16, 4096, 2048)
        )));
    }

    @Test
    void rejectsOverlappingPayloadRanges() {
        assertThrows(IllegalArgumentException.class, () -> new MeshletStreamingMetadata(List.of(
            new MeshletStreamUnitMetadata(0, 0, 32, 0, 4096),
            new MeshletStreamUnitMetadata(1, 32, 16, 2048, 2048)
        )));
    }
}
