package org.meshforge.demo;

import org.meshforge.pack.buffer.MeshletBufferView;
import org.meshforge.pack.buffer.PackedMesh;

import java.util.Locale;

public final class MeshletStats {
    private MeshletStats() {
    }

    public static String summarize(PackedMesh packed) {
        if (packed == null || !packed.hasMeshlets()) {
            return "Meshlets: none";
        }
        MeshletBufferView view = packed.meshletsOrNull();
        if (view == null || view.meshletCount() == 0) {
            return "Meshlets: none";
        }

        int count = view.meshletCount();
        int totalVerts = 0;
        int totalTris = 0;
        int minVerts = Integer.MAX_VALUE;
        int maxVerts = Integer.MIN_VALUE;
        int minTris = Integer.MAX_VALUE;
        int maxTris = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            var m = view.meshlet(i);
            int v = m.uniqueVertexCount();
            int t = m.triangleCount();
            totalVerts += v;
            totalTris += t;
            if (v < minVerts) minVerts = v;
            if (v > maxVerts) maxVerts = v;
            if (t < minTris) minTris = t;
            if (t > maxTris) maxTris = t;
        }

        double avgVerts = (double) totalVerts / count;
        double avgTris = (double) totalTris / count;
        return String.format(
            Locale.ROOT,
            "Meshlets: count=%d avgVerts=%.2f avgTris=%.2f minVerts=%d maxVerts=%d minTris=%d maxTris=%d descriptorStride=%dB descriptorBytes=%d",
            count,
            avgVerts,
            avgTris,
            minVerts,
            maxVerts,
            minTris,
            maxTris,
            packed.meshletDescriptorStrideBytes(),
            packed.meshletDescriptorBufferOrNull() == null ? 0 : packed.meshletDescriptorBufferOrNull().capacity()
        );
    }

    public static void print(PackedMesh packed) {
        System.out.println(summarize(packed));
    }
}
