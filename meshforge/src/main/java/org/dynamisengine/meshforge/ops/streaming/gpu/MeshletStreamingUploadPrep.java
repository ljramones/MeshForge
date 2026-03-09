package org.dynamisengine.meshforge.ops.streaming.gpu;

import org.dynamisengine.meshforge.ops.streaming.MeshletStreamUnitMetadata;
import org.dynamisengine.meshforge.ops.streaming.MeshletStreamingMetadata;

import java.util.List;

/**
 * CPU-side preparation of meshlet streaming metadata payload for future GPU-side streaming work.
 * <p>
 * This is a handoff seam only; it does not perform residency management or upload.
 */
public final class MeshletStreamingUploadPrep {
    private MeshletStreamingUploadPrep() {
    }

    /**
     * Flattens stream units into GPU-ready int32 payload layout.
     *
     * Per unit order:
     * - streamUnitId
     * - meshletStart
     * - meshletCount
     * - payloadByteOffset
     * - payloadByteSize
     */
    public static GpuMeshletStreamingPayload fromMetadata(MeshletStreamingMetadata metadata) {
        if (metadata == null) {
            throw new NullPointerException("metadata");
        }

        List<MeshletStreamUnitMetadata> units = metadata.units();
        int count = units.size();
        int stride = GpuMeshletStreamingPayload.UNIT_COMPONENTS;
        int[] payload = new int[count * stride];

        int at = 0;
        for (int i = 0; i < count; i++) {
            MeshletStreamUnitMetadata unit = units.get(i);
            payload[at++] = unit.streamUnitId();
            payload[at++] = unit.meshletStart();
            payload[at++] = unit.meshletCount();
            payload[at++] = unit.payloadByteOffset();
            payload[at++] = unit.payloadByteSize();
        }

        return new GpuMeshletStreamingPayload(
            count,
            0,
            stride,
            payload
        );
    }
}
