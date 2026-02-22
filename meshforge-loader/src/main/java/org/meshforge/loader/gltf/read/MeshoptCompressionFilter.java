package org.meshforge.loader.gltf.read;

import java.util.Locale;

public enum MeshoptCompressionFilter {
    NONE,
    OCTAHEDRAL;

    public static MeshoptCompressionFilter fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return NONE;
        }
        return valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
