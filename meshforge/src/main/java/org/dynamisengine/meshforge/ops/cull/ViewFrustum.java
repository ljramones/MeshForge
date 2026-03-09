package org.dynamisengine.meshforge.ops.cull;

import org.dynamisengine.meshforge.core.bounds.Aabbf;

/**
 * Minimal six-plane view frustum with AABB intersection tests.
 */
public final class ViewFrustum {
    private final Plane[] planes;

    private ViewFrustum(Plane[] planes) {
        this.planes = planes;
    }

    /**
     * Builds an axis-aligned frustum window from an AABB. Useful for deterministic fixture experiments.
     *
     * @param window axis-aligned view window
     * @return frustum
     */
    public static ViewFrustum fromAabbWindow(Aabbf window) {
        if (window == null) {
            throw new NullPointerException("window");
        }
        return new ViewFrustum(new Plane[] {
            new Plane(1f, 0f, 0f, -window.minX()),
            new Plane(-1f, 0f, 0f, window.maxX()),
            new Plane(0f, 1f, 0f, -window.minY()),
            new Plane(0f, -1f, 0f, window.maxY()),
            new Plane(0f, 0f, 1f, -window.minZ()),
            new Plane(0f, 0f, -1f, window.maxZ())
        });
    }

    /**
     * Returns true when the input bounds intersects the frustum.
     */
    public boolean intersects(Aabbf bounds) {
        if (bounds == null) {
            throw new NullPointerException("bounds");
        }
        for (Plane plane : planes) {
            float x = plane.nx >= 0f ? bounds.maxX() : bounds.minX();
            float y = plane.ny >= 0f ? bounds.maxY() : bounds.minY();
            float z = plane.nz >= 0f ? bounds.maxZ() : bounds.minZ();
            float distance = (plane.nx * x) + (plane.ny * y) + (plane.nz * z) + plane.d;
            if (distance < 0f) {
                return false;
            }
        }
        return true;
    }

    private record Plane(float nx, float ny, float nz, float d) {
    }
}
