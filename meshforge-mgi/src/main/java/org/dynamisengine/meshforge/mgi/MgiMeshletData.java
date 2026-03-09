package org.dynamisengine.meshforge.mgi;

import java.util.List;

/**
 * Container for meshlet descriptor/remap/triangle/bounds payloads.
 *
 * @param descriptors meshlet descriptors
 * @param vertexRemap local->parent vertex index remap payload
 * @param triangles packed local triangle indices
 * @param bounds meshlet bounds payload
 */
public record MgiMeshletData(
    List<MgiMeshletDescriptor> descriptors,
    int[] vertexRemap,
    int[] triangles,
    List<MgiMeshletBounds> bounds
) {
    public MgiMeshletData {
        if (descriptors == null) {
            throw new NullPointerException("descriptors");
        }
        if (vertexRemap == null) {
            throw new NullPointerException("vertexRemap");
        }
        if (triangles == null) {
            throw new NullPointerException("triangles");
        }
        if (bounds == null) {
            throw new NullPointerException("bounds");
        }
        if (descriptors.isEmpty()) {
            throw new IllegalArgumentException("descriptors must not be empty");
        }
        if ((triangles.length % 3) != 0) {
            throw new IllegalArgumentException("triangles length must be divisible by 3");
        }

        descriptors = List.copyOf(descriptors);
        vertexRemap = vertexRemap.clone();
        triangles = triangles.clone();
        bounds = List.copyOf(bounds);

        for (MgiMeshletDescriptor descriptor : descriptors) {
            if (descriptor == null) {
                throw new NullPointerException("descriptor");
            }
            long remapEnd = (long) descriptor.vertexRemapOffset() + descriptor.vertexCount();
            if (remapEnd > vertexRemap.length) {
                throw new IllegalArgumentException("meshlet descriptor remap range exceeds payload");
            }
            long triIndexEnd = (long) descriptor.triangleOffset() + (long) descriptor.triangleCount() * 3L;
            if (triIndexEnd > triangles.length) {
                throw new IllegalArgumentException("meshlet descriptor triangle range exceeds payload");
            }
            if (descriptor.boundsIndex() >= bounds.size()) {
                throw new IllegalArgumentException("meshlet descriptor boundsIndex out of range");
            }

            int remapStart = descriptor.vertexRemapOffset();
            int remapLimit = remapStart + descriptor.vertexCount();
            for (int i = remapStart; i < remapLimit; i++) {
                if (vertexRemap[i] < 0) {
                    throw new IllegalArgumentException("vertex remap entries must be >= 0");
                }
            }

            int triStart = descriptor.triangleOffset() * 3;
            int triLimit = triStart + descriptor.triangleCount() * 3;
            for (int i = triStart; i < triLimit; i++) {
                int local = triangles[i];
                if (local < 0 || local >= descriptor.vertexCount()) {
                    throw new IllegalArgumentException("triangle local index out of meshlet-local vertex range");
                }
            }
        }
    }
}
