package org.meshforge.pack.buffer;

import java.util.List;

/**
 * Immutable meshlet collection view for renderer upload and culling logic.
 */
public interface MeshletBufferView {
    int meshletCount();

    Meshlet meshlet(int index);

    List<Meshlet> asList();

    static MeshletBufferView of(List<Meshlet> meshlets) {
        return new SimpleMeshletBufferView(meshlets);
    }
}
