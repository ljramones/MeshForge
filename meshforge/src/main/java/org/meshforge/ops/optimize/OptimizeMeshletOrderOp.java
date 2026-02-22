package org.meshforge.ops.optimize;

import org.meshforge.core.mesh.MeshData;
import org.meshforge.core.topology.Topology;
import org.meshforge.ops.pipeline.MeshContext;
import org.meshforge.ops.pipeline.MeshOp;

/**
 * Reorders triangle index ranges by meshlet Morton order to improve spatial locality.
 */
public final class OptimizeMeshletOrderOp implements MeshOp {
    private final int maxVertices;
    private final int maxTriangles;

    public OptimizeMeshletOrderOp(int maxVertices, int maxTriangles) {
        if (maxVertices < 3 || maxVertices > 256) {
            throw new IllegalArgumentException("maxVertices must be in [3, 256]");
        }
        if (maxTriangles < 1 || maxTriangles > 256) {
            throw new IllegalArgumentException("maxTriangles must be in [1, 256]");
        }
        this.maxVertices = maxVertices;
        this.maxTriangles = maxTriangles;
    }

    @Override
    public MeshData apply(MeshData mesh, MeshContext context) {
        if (mesh.topology() != Topology.TRIANGLES) {
            throw new UnsupportedOperationException("OptimizeMeshletOrderOp requires TRIANGLES topology");
        }
        int[] indices = mesh.indicesOrNull();
        if (indices == null || indices.length == 0) {
            return mesh;
        }
        if ((indices.length % 3) != 0) {
            throw new IllegalStateException("Triangle index buffer length must be divisible by 3");
        }

        int[] reordered = MeshletClusters.reorderIndicesByMeshletMorton(mesh, indices, maxVertices, maxTriangles);
        mesh.setIndices(reordered);
        return mesh;
    }
}
