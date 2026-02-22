package org.meshforge.ops.modify;

import org.meshforge.core.mesh.MeshData;
import org.meshforge.core.mesh.Submesh;
import org.meshforge.core.topology.Topology;
import org.meshforge.ops.pipeline.MeshContext;
import org.meshforge.ops.pipeline.MeshOp;

import java.util.List;

/**
 * Ensures mesh indices are triangle-indexed for TRIANGLES topology.
 * <p>
 * v1 behavior:
 * - TRIANGLES + indexed: no-op
 * - TRIANGLES + non-indexed: generate sequential indices (vertexCount must be multiple of 3)
 * - LINES/POINTS: fail fast
 */
public final class EnsureTrianglesOp implements MeshOp {

    @Override
    public MeshData apply(MeshData mesh, MeshContext context) {
        if (mesh.topology() != Topology.TRIANGLES) {
            throw new UnsupportedOperationException(
                "EnsureTrianglesOp only supports TRIANGLES topology, got: " + mesh.topology()
            );
        }

        int[] existing = mesh.indicesOrNull();
        if (existing != null) {
            return mesh;
        }

        int vertexCount = mesh.vertexCount();
        if ((vertexCount % 3) != 0) {
            throw new IllegalStateException(
                "Non-indexed TRIANGLES mesh vertexCount must be multiple of 3; got " + vertexCount
            );
        }

        int[] generated = new int[vertexCount];
        for (int i = 0; i < generated.length; i++) {
            generated[i] = i;
        }
        mesh.setIndices(generated);
        mesh.setSubmeshes(generated.length == 0 ? List.of() : List.of(new Submesh(0, generated.length, "default")));
        return mesh;
    }
}
