package org.dynamisengine.meshforge.mgi;

/**
 * API-agnostic meshlet descriptor.
 *
 * @param submeshIndex owning submesh index
 * @param materialSlot material slot id
 * @param vertexRemapOffset offset into meshlet vertex remap payload
 * @param vertexCount local meshlet vertex count
 * @param triangleOffset offset into meshlet triangle payload
 * @param triangleCount local meshlet triangle count
 * @param boundsIndex index into meshlet bounds payload
 * @param flags descriptor flags
 */
public record MgiMeshletDescriptor(
    int submeshIndex,
    int materialSlot,
    int vertexRemapOffset,
    int vertexCount,
    int triangleOffset,
    int triangleCount,
    int boundsIndex,
    int flags
) {
    public MgiMeshletDescriptor {
        if (submeshIndex < 0) {
            throw new IllegalArgumentException("submeshIndex must be >= 0");
        }
        if (materialSlot < 0) {
            throw new IllegalArgumentException("materialSlot must be >= 0");
        }
        if (vertexRemapOffset < 0) {
            throw new IllegalArgumentException("vertexRemapOffset must be >= 0");
        }
        if (vertexCount <= 0) {
            throw new IllegalArgumentException("vertexCount must be > 0");
        }
        if (triangleOffset < 0) {
            throw new IllegalArgumentException("triangleOffset must be >= 0");
        }
        if (triangleCount <= 0) {
            throw new IllegalArgumentException("triangleCount must be > 0");
        }
        if (boundsIndex < 0) {
            throw new IllegalArgumentException("boundsIndex must be >= 0");
        }
    }
}
