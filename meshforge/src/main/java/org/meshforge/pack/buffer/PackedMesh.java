package org.meshforge.pack.buffer;

import org.meshforge.pack.layout.VertexLayout;

import java.nio.ByteBuffer;
import java.util.List;

public final class PackedMesh {
    private final VertexLayout layout;
    private final ByteBuffer vertexBuffer;
    private final IndexBufferView indexBuffer;
    private final List<SubmeshRange> submeshes;
    private final MeshletBufferView meshlets;

    public PackedMesh(
        VertexLayout layout,
        ByteBuffer vertexBuffer,
        IndexBufferView indexBuffer,
        List<SubmeshRange> submeshes
    ) {
        this(layout, vertexBuffer, indexBuffer, submeshes, null);
    }

    public PackedMesh(
        VertexLayout layout,
        ByteBuffer vertexBuffer,
        IndexBufferView indexBuffer,
        List<SubmeshRange> submeshes,
        MeshletBufferView meshlets
    ) {
        this.layout = layout;
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = indexBuffer;
        this.submeshes = List.copyOf(submeshes);
        this.meshlets = meshlets;
    }

    public VertexLayout layout() {
        return layout;
    }

    public ByteBuffer vertexBuffer() {
        return vertexBuffer;
    }

    public IndexBufferView indexBuffer() {
        return indexBuffer;
    }

    public List<SubmeshRange> submeshes() {
        return submeshes;
    }

    public MeshletBufferView meshletsOrNull() {
        return meshlets;
    }

    public boolean hasMeshlets() {
        return meshlets != null && meshlets.meshletCount() > 0;
    }

    public record SubmeshRange(int firstIndex, int indexCount, Object materialId) {
    }

    public enum IndexType { UINT16, UINT32 }

    public record IndexBufferView(IndexType type, ByteBuffer buffer, int indexCount) {
    }
}
