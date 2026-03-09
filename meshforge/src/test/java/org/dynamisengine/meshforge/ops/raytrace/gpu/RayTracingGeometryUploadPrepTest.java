package org.dynamisengine.meshforge.ops.raytrace.gpu;

import org.dynamisengine.meshforge.ops.raytrace.RayTracingGeometryMetadata;
import org.dynamisengine.meshforge.ops.raytrace.RayTracingGeometryRegionMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RayTracingGeometryUploadPrepTest {

    @Test
    void packsRegionsInExpectedOrder() {
        RayTracingGeometryMetadata metadata = new RayTracingGeometryMetadata(List.of(
            new RayTracingGeometryRegionMetadata(0, 0, 12, 0, RayTracingGeometryRegionMetadata.FLAG_OPAQUE),
            new RayTracingGeometryRegionMetadata(1, 12, 9, 2, RayTracingGeometryRegionMetadata.FLAG_DOUBLE_SIDED)
        ));

        GpuRayTracingGeometryPayload payload = RayTracingGeometryUploadPrep.fromMetadata(metadata);

        assertEquals(2, payload.regionCount());
        assertEquals(0, payload.regionsOffsetInts());
        assertEquals(5, payload.regionsStrideInts());
        assertEquals(40, payload.regionsByteSize());
        assertArrayEquals(
            new int[] {
                0, 0, 12, 0, RayTracingGeometryRegionMetadata.FLAG_OPAQUE,
                1, 12, 9, 2, RayTracingGeometryRegionMetadata.FLAG_DOUBLE_SIDED
            },
            payload.regionsPayload()
        );
    }
}

