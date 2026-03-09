package org.dynamisengine.meshforge.ops.raytrace.gpu;

import org.dynamisengine.meshforge.ops.raytrace.RayTracingGeometryMetadata;
import org.dynamisengine.meshforge.ops.raytrace.RayTracingGeometryRegionMetadata;

import java.util.List;

/**
 * CPU-side preparation of RT region metadata payload for future GPU-side RT resource work.
 */
public final class RayTracingGeometryUploadPrep {
    private RayTracingGeometryUploadPrep() {
    }

    /**
     * Flattens RT regions into GPU-ready int32 payload layout.
     */
    public static GpuRayTracingGeometryPayload fromMetadata(RayTracingGeometryMetadata metadata) {
        if (metadata == null) {
            throw new NullPointerException("metadata");
        }

        List<RayTracingGeometryRegionMetadata> regions = metadata.regions();
        int count = regions.size();
        int stride = GpuRayTracingGeometryPayload.REGION_COMPONENTS;
        int[] payload = new int[count * stride];

        int at = 0;
        for (int i = 0; i < count; i++) {
            RayTracingGeometryRegionMetadata region = regions.get(i);
            payload[at++] = region.submeshIndex();
            payload[at++] = region.firstIndex();
            payload[at++] = region.indexCount();
            payload[at++] = region.materialSlot();
            payload[at++] = region.flags();
        }

        return new GpuRayTracingGeometryPayload(
            count,
            0,
            stride,
            payload
        );
    }
}

