package org.meshforge.demo;

import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.scene.shape.TriangleMesh;
import org.meshforge.core.attr.AttributeSemantic;
import org.meshforge.core.mesh.MeshData;

final class MeshFxBridge {
    private MeshFxBridge() {
    }

    static TriangleMesh toTriangleMesh(MeshData mesh) {
        if (!mesh.has(AttributeSemantic.POSITION, 0)) {
            throw new IllegalStateException("MeshData is missing POSITION[0]");
        }

        float[] positions = mesh.attribute(AttributeSemantic.POSITION, 0).rawFloatArrayOrNull();
        if (positions == null) {
            throw new IllegalStateException("POSITION[0] must be float-backed");
        }

        int[] indices = mesh.indicesOrNull();
        if (indices == null || indices.length == 0) {
            throw new IllegalStateException("MeshData has no index buffer for JavaFX rendering");
        }

        TriangleMesh fx = new TriangleMesh();
        ObservableFloatArray points = fx.getPoints();
        points.setAll(positions);

        // JavaFX requires at least one texCoord entry.
        fx.getTexCoords().setAll(0.0f, 0.0f);

        ObservableIntegerArray faces = fx.getFaces();
        int[] fxFaces = new int[indices.length * 2];
        for (int i = 0; i < indices.length; i++) {
            fxFaces[i * 2] = indices[i];
            fxFaces[i * 2 + 1] = 0;
        }
        faces.setAll(fxFaces);

        return fx;
    }
}
