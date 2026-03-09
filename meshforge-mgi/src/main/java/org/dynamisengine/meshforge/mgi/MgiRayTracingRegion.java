package org.dynamisengine.meshforge.mgi;

/**
 * One RT-relevant geometry region in MGI metadata.
 *
 * @param submeshIndex source submesh index
 * @param firstIndex inclusive first index in parent index buffer
 * @param indexCount index count for this region
 * @param materialSlot stable material slot
 * @param flags RT behavior flags
 */
public record MgiRayTracingRegion(
    int submeshIndex,
    int firstIndex,
    int indexCount,
    int materialSlot,
    int flags
) {
    public MgiRayTracingRegion {
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

