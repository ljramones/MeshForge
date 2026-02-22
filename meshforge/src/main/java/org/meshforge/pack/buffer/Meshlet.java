package org.meshforge.pack.buffer;

import org.meshforge.core.bounds.Aabbf;

/**
 * Immutable meshlet descriptor for clustered triangle ranges.
 */
public record Meshlet(
    int firstTriangle,
    int triangleCount,
    int firstIndex,
    int indexCount,
    int uniqueVertexCount,
    Aabbf bounds,
    float coneAxisX,
    float coneAxisY,
    float coneAxisZ,
    float coneCutoffCos
) {
}
