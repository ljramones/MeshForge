package org.dynamisengine.meshforge.mgi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MgiRayTracingModelTest {

    @Test
    void acceptsOrderedRegions() {
        MgiRayTracingData data = new MgiRayTracingData(List.of(
            new MgiRayTracingRegion(0, 0, 12, 0, 1),
            new MgiRayTracingRegion(1, 12, 9, 2, 0)
        ));

        assertEquals(2, data.regions().size());
    }

    @Test
    void rejectsNonIncreasingSubmeshIndex() {
        assertThrows(IllegalArgumentException.class, () -> new MgiRayTracingData(List.of(
            new MgiRayTracingRegion(1, 0, 12, 0, 1),
            new MgiRayTracingRegion(1, 12, 9, 2, 0)
        )));
    }

    @Test
    void allowsOverlappingRangesWhenSubmeshOrderIsStable() {
        MgiRayTracingData data = new MgiRayTracingData(List.of(
            new MgiRayTracingRegion(0, 0, 12, 0, 1),
            new MgiRayTracingRegion(1, 8, 9, 2, 0)
        ));
        assertEquals(2, data.regions().size());
    }
}
