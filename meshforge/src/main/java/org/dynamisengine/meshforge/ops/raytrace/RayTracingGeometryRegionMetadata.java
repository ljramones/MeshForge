package org.dynamisengine.meshforge.ops.raytrace;

/**
 * One RT-relevant geometry region mapped to a submesh index range.
 *
 * @param submeshIndex source submesh index
 * @param firstIndex inclusive first index in parent index buffer
 * @param indexCount index count for this region
 * @param materialSlot stable material slot for this region
 * @param flags bit flags describing RT behavior hints
 */
public record RayTracingGeometryRegionMetadata(
    int submeshIndex,
    int firstIndex,
    int indexCount,
    int materialSlot,
    int flags
) {
    public static final int FLAG_OPAQUE = 1 << 0;
    public static final int FLAG_DOUBLE_SIDED = 1 << 1;

    public RayTracingGeometryRegionMetadata {
        if (submeshIndex < 0) {
            throw new IllegalArgumentException("submeshIndex must be >= 0");
        }
        if (firstIndex < 0) {
            throw new IllegalArgumentException("firstIndex must be >= 0");
        }
        if (indexCount <= 0) {
            throw new IllegalArgumentException("indexCount must be > 0");
        }
        if (materialSlot < 0) {
            throw new IllegalArgumentException("materialSlot must be >= 0");
        }
        if (flags < 0) {
            throw new IllegalArgumentException("flags must be >= 0");
        }
    }
}

