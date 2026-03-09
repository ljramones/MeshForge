package org.dynamisengine.meshforge.mgi;

import org.dynamisengine.meshforge.core.attr.AttributeSemantic;
import org.dynamisengine.meshforge.core.attr.VertexAttributeView;
import org.dynamisengine.meshforge.core.attr.VertexFormat;
import org.dynamisengine.meshforge.core.attr.VertexSchema;
import org.dynamisengine.meshforge.core.mesh.MeshData;
import org.dynamisengine.meshforge.core.mesh.Submesh;
import org.dynamisengine.meshforge.core.topology.Topology;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MgiMeshDataCodecTest {

    @Test
    void roundTripsCanonicalMeshDataThroughMgi() throws Exception {
        MeshData input = sampleTriangleMesh();
        MgiMeshDataCodec codec = new MgiMeshDataCodec();

        byte[] bytes = codec.write(input);
        MeshData output = codec.read(bytes);

        assertEquals(Topology.TRIANGLES, output.topology());
        assertEquals(input.vertexCount(), output.vertexCount());
        assertArrayEquals(input.indicesOrNull(), output.indicesOrNull());
        assertEquals(input.submeshes(), output.submeshes());

        VertexAttributeView inPos = input.attribute(AttributeSemantic.POSITION, 0);
        VertexAttributeView outPos = output.attribute(AttributeSemantic.POSITION, 0);
        for (int i = 0; i < input.vertexCount(); i++) {
            assertEquals(inPos.getFloat(i, 0), outPos.getFloat(i, 0));
            assertEquals(inPos.getFloat(i, 1), outPos.getFloat(i, 1));
            assertEquals(inPos.getFloat(i, 2), outPos.getFloat(i, 2));
        }
    }

    @Test
    void rejectsNonTriangleTopology() {
        VertexSchema schema = VertexSchema.builder()
            .add(AttributeSemantic.POSITION, 0, VertexFormat.F32x3)
            .build();
        MeshData lines = new MeshData(
            Topology.LINES,
            schema,
            2,
            new int[] {0, 1},
            List.of(new Submesh(0, 2, 0))
        );

        MgiMeshDataCodec codec = new MgiMeshDataCodec();
        assertThrows(IllegalArgumentException.class, () -> codec.write(lines));
    }

    @Test
    void rejectsNonNumericMaterialId() {
        VertexSchema schema = VertexSchema.builder()
            .add(AttributeSemantic.POSITION, 0, VertexFormat.F32x3)
            .build();
        MeshData mesh = new MeshData(
            Topology.TRIANGLES,
            schema,
            3,
            new int[] {0, 1, 2},
            List.of(new Submesh(0, 3, "matA"))
        );
        VertexAttributeView pos = mesh.attribute(AttributeSemantic.POSITION, 0);
        pos.set3f(0, 0f, 0f, 0f);
        pos.set3f(1, 1f, 0f, 0f);
        pos.set3f(2, 0f, 1f, 0f);

        MgiMeshDataCodec codec = new MgiMeshDataCodec();
        assertThrows(IllegalArgumentException.class, () -> codec.write(mesh));
    }

    private static MeshData sampleTriangleMesh() {
        VertexSchema schema = VertexSchema.builder()
            .add(AttributeSemantic.POSITION, 0, VertexFormat.F32x3)
            .build();
        MeshData mesh = new MeshData(
            Topology.TRIANGLES,
            schema,
            4,
            new int[] {0, 1, 2, 1, 3, 2},
            List.of(new Submesh(0, 3, 0), new Submesh(3, 3, 1))
        );
        VertexAttributeView pos = mesh.attribute(AttributeSemantic.POSITION, 0);
        pos.set3f(0, 0f, 0f, 0f);
        pos.set3f(1, 1f, 0f, 0f);
        pos.set3f(2, 0f, 1f, 0f);
        pos.set3f(3, 1f, 1f, 0f);
        return mesh;
    }
}
