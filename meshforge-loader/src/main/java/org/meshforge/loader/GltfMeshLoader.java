package org.meshforge.loader;

import org.meshforge.core.mesh.MeshData;
import org.meshforge.loader.gltf.read.MeshoptCompressionDetector;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Placeholder glTF/glb loader entry point with meshopt decode option hook.
 */
public final class GltfMeshLoader implements MeshFileLoader {
    @Override
    public MeshData load(Path path) throws IOException {
        return load(path, MeshLoadOptions.defaults());
    }

    @Override
    public MeshData load(Path path, MeshLoadOptions options) throws IOException {
        boolean hasMeshopt = MeshoptCompressionDetector.containsMeshoptCompression(path);
        if (hasMeshopt && (options == null || !options.meshoptDecodeEnabled())) {
            throw new IOException("gltf/glb uses KHR_meshopt_compression but meshopt decode is disabled: " + path);
        }
        if (hasMeshopt) {
            throw new IOException("gltf/glb loader is not implemented yet; meshopt decode hook is enabled and detected KHR_meshopt_compression: " + path);
        }
        throw new IOException("Loader for format gltf/glb is not implemented yet: " + path);
    }
}

