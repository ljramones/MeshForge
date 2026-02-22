package org.meshforge.loader.gltf.read;

import java.util.Locale;

public enum MeshoptCompressionMode {
    ATTRIBUTES,
    TRIANGLES;

    public static MeshoptCompressionMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("meshopt mode must not be blank");
        }
        return valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
