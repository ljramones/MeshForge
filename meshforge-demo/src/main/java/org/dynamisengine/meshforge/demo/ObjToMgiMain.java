package org.dynamisengine.meshforge.demo;

import org.dynamisengine.meshforge.core.mesh.MeshData;
import org.dynamisengine.meshforge.loader.MeshLoaders;
import org.dynamisengine.meshforge.mgi.MgiMeshDataCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Converts OBJ fixtures to MGI sidecars.
 */
public final class ObjToMgiMain {
    private ObjToMgiMain() {
    }

    public static void main(String[] args) throws Exception {
        Path input = Path.of("fixtures", "baseline");
        boolean overwrite = false;

        for (String arg : args) {
            if (arg.startsWith("--input=")) {
                input = Path.of(arg.substring("--input=".length()));
            } else if (arg.equals("--overwrite")) {
                overwrite = true;
            }
        }

        if (!Files.exists(input)) {
            throw new IllegalArgumentException("Input path does not exist: " + input.toAbsolutePath());
        }

        List<Path> objFiles;
        if (Files.isRegularFile(input)) {
            objFiles = List.of(input);
        } else {
            try (var walk = Files.walk(input)) {
                objFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            }
        }

        if (objFiles.isEmpty()) {
            System.out.println("No OBJ files found.");
            return;
        }

        MeshLoaders loaders = MeshLoaders.defaultsFast();
        MgiMeshDataCodec codec = new MgiMeshDataCodec();

        int converted = 0;
        int skipped = 0;
        for (Path obj : objFiles) {
            Path mgi = mgiPathFor(obj);
            if (Files.exists(mgi) && !overwrite) {
                skipped++;
                continue;
            }

            MeshData mesh = loaders.load(obj);
            byte[] bytes = codec.write(mesh);
            Files.write(mgi, bytes);
            converted++;
            System.out.printf(Locale.ROOT, "converted=%s -> %s bytes=%d%n", obj, mgi, bytes.length);
        }

        System.out.printf(Locale.ROOT, "done converted=%d skipped=%d%n", converted, skipped);
    }

    private static Path mgiPathFor(Path sourceObj) {
        String file = sourceObj.getFileName().toString();
        int dot = file.lastIndexOf('.');
        String base = dot <= 0 ? file : file.substring(0, dot);
        return sourceObj.resolveSibling(base + ".mgi");
    }
}
