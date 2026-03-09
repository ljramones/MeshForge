package org.dynamisengine.meshforge.mgi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MgiMeshletStreamingModelTest {

    @Test
    void acceptsOrderedNonOverlappingUnits() {
        MgiMeshletStreamingData data = new MgiMeshletStreamingData(List.of(
            new MgiMeshletStreamUnit(0, 0, 32, 0, 4096),
            new MgiMeshletStreamUnit(1, 32, 16, 4096, 2048)
        ));

        assertEquals(2, data.units().size());
    }

    @Test
    void rejectsNonIncreasingUnitIds() {
        assertThrows(IllegalArgumentException.class, () -> new MgiMeshletStreamingData(List.of(
            new MgiMeshletStreamUnit(0, 0, 32, 0, 4096),
            new MgiMeshletStreamUnit(0, 32, 16, 4096, 2048)
        )));
    }

    @Test
    void rejectsOverlappingMeshletRanges() {
        assertThrows(IllegalArgumentException.class, () -> new MgiMeshletStreamingData(List.of(
            new MgiMeshletStreamUnit(0, 0, 32, 0, 4096),
            new MgiMeshletStreamUnit(1, 16, 16, 4096, 2048)
        )));
    }
}
