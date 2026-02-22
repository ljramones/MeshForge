package org.meshforge.demo;

import org.meshforge.api.Packers;
import org.meshforge.api.Pipelines;
import org.meshforge.api.Ops;
import org.meshforge.loader.MeshLoaders;
import org.meshforge.ops.pipeline.MeshPipeline;
import org.meshforge.pack.packer.MeshPacker;

import java.nio.file.Path;

public final class MeshForgeDemo {
    private MeshForgeDemo() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: MeshForgeDemo <path-to-obj>");
            return;
        }

        var mesh = MeshLoaders.defaults().load(Path.of(args[0]));
        mesh = Pipelines.realtimeFast(mesh);
        mesh = MeshPipeline.run(mesh, Ops.clusterizeMeshlets(128, 64));
        mesh = MeshPipeline.run(mesh, Ops.optimizeMeshletOrder(128, 64));

        var packed = MeshPacker.pack(mesh, Packers.realtimeWithMeshlets());
        System.out.println("Vertices: " + mesh.vertexCount());
        System.out.println("Indices: " + (mesh.indicesOrNull() == null ? 0 : mesh.indicesOrNull().length));
        System.out.println("Packed stride: " + packed.layout().strideBytes());
        System.out.println("Packed vertex bytes: " + packed.vertexBuffer().capacity());
        System.out.println("Index type: " + (packed.indexBuffer() == null ? "none" : packed.indexBuffer().type()));
        MeshletStats.print(packed);
    }
}
