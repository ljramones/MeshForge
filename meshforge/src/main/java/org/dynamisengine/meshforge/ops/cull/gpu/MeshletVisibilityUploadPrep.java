package org.dynamisengine.meshforge.ops.cull.gpu;

import org.dynamisengine.meshforge.core.bounds.Aabbf;

import java.util.List;

/**
 * CPU-side preparation of meshlet visibility input payloads for future GPU culling.
 * <p>
 * This class is a handoff seam only. It does not perform GPU upload or execution.
 */
public final class MeshletVisibilityUploadPrep {
    private MeshletVisibilityUploadPrep() {
    }

    /**
     * Flattens meshlet bounds into GPU-ready payload layout.
     *
     * Per meshlet order: minX, minY, minZ, maxX, maxY, maxZ.
     * Meshlet ordering is preserved exactly as provided.
     */
    public static GpuMeshletVisibilityPayload fromMeshletBounds(List<Aabbf> meshletBounds) {
        if (meshletBounds == null) {
            throw new NullPointerException("meshletBounds");
        }
        int count = meshletBounds.size();
        int stride = GpuMeshletVisibilityPayload.BOUNDS_COMPONENTS;
        float[] payload = new float[count * stride];

        int at = 0;
        for (int i = 0; i < count; i++) {
            Aabbf b = meshletBounds.get(i);
            if (b == null) {
                throw new NullPointerException("meshletBounds[" + i + "]");
            }
            payload[at++] = b.minX();
            payload[at++] = b.minY();
            payload[at++] = b.minZ();
            payload[at++] = b.maxX();
            payload[at++] = b.maxY();
            payload[at++] = b.maxZ();
        }

        return new GpuMeshletVisibilityPayload(
            count,
            0,
            stride,
            payload
        );
    }
}
