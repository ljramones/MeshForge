package org.dynamisengine.meshforge.mgi;

import org.dynamisengine.meshforge.core.attr.AttributeSemantic;
import org.dynamisengine.meshforge.core.attr.VertexAttributeView;
import org.dynamisengine.meshforge.core.attr.VertexFormat;
import org.dynamisengine.meshforge.core.attr.VertexSchema;
import org.dynamisengine.meshforge.core.mesh.MeshData;
import org.dynamisengine.meshforge.core.mesh.Submesh;
import org.dynamisengine.meshforge.core.topology.Topology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter between canonical MeshForge {@link MeshData} and MGI static mesh payload bytes.
 */
public final class MgiMeshDataCodec {
    private final MgiStaticMeshCodec staticMeshCodec = new MgiStaticMeshCodec();

    public byte[] write(MeshData meshData) throws IOException {
        if (meshData == null) {
            throw new NullPointerException("meshData");
        }
        return staticMeshCodec.write(toMgiStaticMesh(meshData));
    }

    public MeshData read(byte[] bytes) throws IOException {
        if (bytes == null) {
            throw new NullPointerException("bytes");
        }
        return toMeshData(staticMeshCodec.read(bytes));
    }

    public static MgiStaticMesh toMgiStaticMesh(MeshData meshData) {
        if (meshData == null) {
            throw new NullPointerException("meshData");
        }
        if (meshData.topology() != Topology.TRIANGLES) {
            throw new IllegalArgumentException("MGI v1 static mesh codec requires TRIANGLES topology");
        }

        VertexAttributeView positions = meshData.attribute(AttributeSemantic.POSITION, 0);
        if (positions.format() != VertexFormat.F32x3) {
            throw new IllegalArgumentException("MGI v1 static mesh codec requires POSITION[0] format F32x3");
        }

        int[] indices = meshData.indicesOrNull();
        if (indices == null) {
            throw new IllegalArgumentException("MGI v1 static mesh codec requires indexed geometry");
        }

        float[] packedPositions = extractPositions(positions);
        List<MgiSubmeshRange> ranges = convertSubmeshes(meshData.submeshes(), indices.length);
        return new MgiStaticMesh(packedPositions, indices, ranges);
    }

    public static MeshData toMeshData(MgiStaticMesh mesh) {
        if (mesh == null) {
            throw new NullPointerException("mesh");
        }

        VertexSchema schema = VertexSchema.builder()
            .add(AttributeSemantic.POSITION, 0, VertexFormat.F32x3)
            .build();

        List<Submesh> submeshes = new ArrayList<>(mesh.submeshes().size());
        for (MgiSubmeshRange range : mesh.submeshes()) {
            submeshes.add(new Submesh(range.firstIndex(), range.indexCount(), Integer.valueOf(range.materialSlot())));
        }

        MeshData data = new MeshData(
            Topology.TRIANGLES,
            schema,
            mesh.vertexCount(),
            mesh.indices(),
            submeshes
        );

        VertexAttributeView positions = data.attribute(AttributeSemantic.POSITION, 0);
        float[] positionData = mesh.positions();
        float[] raw = positions.rawFloatArrayOrNull();
        if (raw != null) {
            System.arraycopy(positionData, 0, raw, 0, positionData.length);
        } else {
            int v = 0;
            for (int i = 0; i < data.vertexCount(); i++) {
                positions.setFloat(i, 0, positionData[v++]);
                positions.setFloat(i, 1, positionData[v++]);
                positions.setFloat(i, 2, positionData[v++]);
            }
        }
        return data;
    }

    private static float[] extractPositions(VertexAttributeView positions) {
        float[] raw = positions.rawFloatArrayOrNull();
        if (raw != null) {
            return raw.clone();
        }

        int vertexCount = positions.vertexCount();
        float[] packed = new float[vertexCount * 3];
        int at = 0;
        for (int i = 0; i < vertexCount; i++) {
            packed[at++] = positions.getFloat(i, 0);
            packed[at++] = positions.getFloat(i, 1);
            packed[at++] = positions.getFloat(i, 2);
        }
        return packed;
    }

    private static List<MgiSubmeshRange> convertSubmeshes(List<Submesh> source, int indexCount) {
        if (source.isEmpty()) {
            return List.of(new MgiSubmeshRange(0, indexCount, 0));
        }

        ArrayList<MgiSubmeshRange> out = new ArrayList<>(source.size());
        for (Submesh submesh : source) {
            int slot = materialSlot(submesh.materialId());
            long end = (long) submesh.firstIndex() + submesh.indexCount();
            if (submesh.firstIndex() < 0 || submesh.indexCount() < 0 || end > indexCount) {
                throw new IllegalArgumentException("Submesh range exceeds index buffer: first=" + submesh.firstIndex()
                    + ", count=" + submesh.indexCount() + ", indexCount=" + indexCount);
            }
            out.add(new MgiSubmeshRange(submesh.firstIndex(), submesh.indexCount(), slot));
        }
        return List.copyOf(out);
    }

    private static int materialSlot(Object materialId) {
        if (materialId == null) {
            return 0;
        }
        if (materialId instanceof Number number) {
            int slot = number.intValue();
            if (slot < 0) {
                throw new IllegalArgumentException("material slot must be >= 0");
            }
            return slot;
        }
        throw new IllegalArgumentException("materialId must be numeric or null for MGI v1");
    }
}
