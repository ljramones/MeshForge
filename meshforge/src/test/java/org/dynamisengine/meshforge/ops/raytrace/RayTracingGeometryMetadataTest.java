package org.dynamisengine.meshforge.ops.raytrace;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RayTracingGeometryMetadataTest {

    @Test
    void acceptsOrderedNonOverlappingRegions() {
        RayTracingGeometryMetadata metadata = new RayTracingGeometryMetadata(List.of(
            new RayTracingGeometryRegionMetadata(0, 0, 12, 0, RayTracingGeometryRegionMetadata.FLAG_OPAQUE),
            new RayTracingGeometryRegionMetadata(1, 12, 9, 2, RayTracingGeometryRegionMetadata.FLAG_DOUBLE_SIDED)
        ));

        assertEquals(2, metadata.regionCount());
    }

    @Test
    void rejectsNonIncreasingSubmeshIndex() {
        assertThrows(IllegalArgumentException.class, () -> new RayTracingGeometryMetadata(List.of(
            new RayTracingGeometryRegionMetadata(1, 0, 12, 0, 0),
            new RayTracingGeometryRegionMetadata(1, 12, 9, 2, 0)
        )));
    }

    @Test
    void allowsOverlappingRangesWhenSubmeshOrderIsStable() {
        RayTracingGeometryMetadata metadata = new RayTracingGeometryMetadata(List.of(
            new RayTracingGeometryRegionMetadata(0, 0, 12, 0, 0),
            new RayTracingGeometryRegionMetadata(1, 8, 9, 2, 0)
        ));
        assertEquals(2, metadata.regionCount());
    }
}
