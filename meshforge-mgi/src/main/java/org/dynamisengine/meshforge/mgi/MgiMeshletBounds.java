package org.dynamisengine.meshforge.mgi;

/**
 * Meshlet bounds payload (AABB).
 *
 * @param minX minimum x
 * @param minY minimum y
 * @param minZ minimum z
 * @param maxX maximum x
 * @param maxY maximum y
 * @param maxZ maximum z
 */
public record MgiMeshletBounds(
    float minX,
    float minY,
    float minZ,
    float maxX,
    float maxY,
    float maxZ
) {
    public MgiMeshletBounds {
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)
            || !Float.isFinite(maxX) || !Float.isFinite(maxY) || !Float.isFinite(maxZ)) {
            throw new IllegalArgumentException("meshlet bounds values must be finite");
        }
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("invalid meshlet bounds extents (min > max)");
        }
    }
}
