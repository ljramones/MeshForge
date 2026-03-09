package org.dynamisengine.meshforge.ops.cull.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * GPU-ready meshlet visibility payload containing flattened meshlet bounds.
 *
 * Layout v1:
 * - meshlet bounds are packed as 6 float values per meshlet
 * - order per meshlet: minX, minY, minZ, maxX, maxY, maxZ
 */
public record GpuMeshletVisibilityPayload(
    int meshletCount,
    int boundsOffsetFloats,
    int boundsStrideFloats,
    float[] boundsPayload
) {
    public static final int BOUNDS_COMPONENTS = 6;

    public GpuMeshletVisibilityPayload {
        if (meshletCount < 0) {
            throw new IllegalArgumentException("meshletCount must be >= 0");
        }
        if (boundsOffsetFloats < 0) {
            throw new IllegalArgumentException("boundsOffsetFloats must be >= 0");
        }
        if (boundsStrideFloats < BOUNDS_COMPONENTS) {
            throw new IllegalArgumentException("boundsStrideFloats must be >= " + BOUNDS_COMPONENTS);
        }
        if (boundsPayload == null) {
            throw new NullPointerException("boundsPayload");
        }

        int required = meshletCount == 0 ? 0 : (boundsOffsetFloats + (meshletCount * boundsStrideFloats));
        if (boundsPayload.length != required) {
            throw new IllegalArgumentException(
                "boundsPayload length mismatch: expected=" + required + " actual=" + boundsPayload.length
            );
        }

        boundsPayload = boundsPayload.clone();
    }

    public int boundsByteSize() {
        return boundsPayload.length * Float.BYTES;
    }

    public int boundsStrideBytes() {
        return boundsStrideFloats * Float.BYTES;
    }

    /**
     * Materializes a little-endian byte buffer suitable for upload.
     */
    public ByteBuffer toBoundsByteBuffer() {
        ByteBuffer out = ByteBuffer.allocateDirect(boundsByteSize()).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : boundsPayload) {
            out.putFloat(value);
        }
        out.flip();
        return out;
    }
}
