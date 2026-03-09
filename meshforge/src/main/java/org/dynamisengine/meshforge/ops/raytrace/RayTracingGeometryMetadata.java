package org.dynamisengine.meshforge.ops.raytrace;

import java.util.List;

/**
 * Runtime handoff metadata describing RT-relevant geometry regions.
 */
public record RayTracingGeometryMetadata(List<RayTracingGeometryRegionMetadata> regions) {
    public RayTracingGeometryMetadata {
        if (regions == null) {
            throw new NullPointerException("regions");
        }
        if (regions.isEmpty()) {
            throw new IllegalArgumentException("regions must not be empty");
        }
        regions = List.copyOf(regions);

        int previousSubmesh = -1;
        for (RayTracingGeometryRegionMetadata region : regions) {
            if (region == null) {
                throw new NullPointerException("region");
            }
            if (region.submeshIndex() <= previousSubmesh) {
                throw new IllegalArgumentException("region submesh indexes must be strictly increasing");
            }
            previousSubmesh = region.submeshIndex();
        }
    }

    public int regionCount() {
        return regions.size();
    }
}
