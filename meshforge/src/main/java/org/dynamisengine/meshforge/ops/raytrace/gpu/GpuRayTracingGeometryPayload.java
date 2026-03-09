package org.dynamisengine.meshforge.ops.raytrace.gpu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * GPU-ready ray tracing region metadata payload.
 *
 * Layout v1 (int32 words per region):
 * - submeshIndex
 * - firstIndex
 * - indexCount
 * - materialSlot
 * - flags
 */
public record GpuRayTracingGeometryPayload(
    int regionCount,
    int regionsOffsetInts,
    int regionsStrideInts,
    int[] regionsPayload
) {
    public static final int REGION_COMPONENTS = 5;

    public GpuRayTracingGeometryPayload {
        if (regionCount < 0) {
            throw new IllegalArgumentException("regionCount must be >= 0");
        }
        if (regionsOffsetInts < 0) {
            throw new IllegalArgumentException("regionsOffsetInts must be >= 0");
        }
        if (regionsStrideInts < REGION_COMPONENTS) {
            throw new IllegalArgumentException("regionsStrideInts must be >= " + REGION_COMPONENTS);
        }
        if (regionsPayload == null) {
            throw new NullPointerException("regionsPayload");
        }
        int required = expectedRegionsPayloadLengthInts(regionCount, regionsOffsetInts, regionsStrideInts);
        if (regionsPayload.length != required) {
            throw new IllegalArgumentException(
                "regionsPayload length mismatch: expected=" + required + " actual=" + regionsPayload.length
            );
        }
        regionsPayload = regionsPayload.clone();
    }

    public int regionsIntCount() {
        return regionsPayload.length;
    }

    public int regionsByteSize() {
        return regionsIntCount() * Integer.BYTES;
    }

    public int regionsStrideBytes() {
        return regionsStrideInts * Integer.BYTES;
    }

    public int expectedRegionsPayloadLengthInts() {
        return expectedRegionsPayloadLengthInts(regionCount, regionsOffsetInts, regionsStrideInts);
    }

    @Override
    public int[] regionsPayload() {
        return regionsPayload.clone();
    }

    public IntBuffer toRegionsIntBuffer() {
        return toRegionsByteBuffer().asIntBuffer().asReadOnlyBuffer();
    }

    public ByteBuffer toRegionsByteBuffer() {
        ByteBuffer out = ByteBuffer.allocateDirect(regionsByteSize()).order(ByteOrder.LITTLE_ENDIAN);
        out.asIntBuffer().put(regionsPayload);
        out.position(0);
        out.limit(regionsByteSize());
        return out;
    }

    private static int expectedRegionsPayloadLengthInts(int regionCount, int offset, int stride) {
        return regionCount == 0 ? 0 : (offset + (regionCount * stride));
    }
}

