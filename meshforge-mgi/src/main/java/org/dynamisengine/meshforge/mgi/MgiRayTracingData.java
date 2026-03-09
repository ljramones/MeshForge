package org.dynamisengine.meshforge.mgi;

import java.util.List;

/**
 * Optional RT-relevant geometry metadata payload for MGI static meshes.
 */
public record MgiRayTracingData(List<MgiRayTracingRegion> regions) {
    public MgiRayTracingData {
        if (regions == null) {
            throw new NullPointerException("regions");
        }
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("regions must not be empty");
        }
        regions = List.copyOf(regions);

        int previousSubmesh = -1;
        for (MgiRayTracingRegion region : regions) {
            if (region == null) {
                throw new NullPointerException("region");
            }
            if (region.submeshIndex() <= previousSubmesh) {
                throw new IllegalArgumentException("RT region submesh indexes must be strictly increasing");
            }
            previousSubmesh = region.submeshIndex();
        }
    }
}
