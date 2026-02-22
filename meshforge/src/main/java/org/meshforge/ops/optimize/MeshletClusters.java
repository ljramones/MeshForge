package org.meshforge.ops.optimize;

import org.meshforge.core.bounds.Aabbf;
import org.meshforge.core.mesh.MeshData;
import org.meshforge.pack.buffer.Meshlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Deterministic CPU meshlet clustering helpers.
 */
public final class MeshletClusters {
    private MeshletClusters() {
    }

    public static int[] reorderIndicesByMeshlets(int[] indices, int maxVerts, int maxTris) {
        if (indices == null || indices.length == 0) {
            return indices == null ? null : new int[0];
        }
        if ((indices.length % 3) != 0) {
            throw new IllegalStateException("Triangle index buffer length must be divisible by 3");
        }

        int triCount = indices.length / 3;
        boolean[] used = new boolean[triCount];
        int usedCount = 0;
        int[] out = new int[indices.length];
        int outPos = 0;

        while (usedCount < triCount) {
            int seed = nextUnused(used);
            int[] localVerts = new int[maxVerts];
            int localVertCount = 0;
            int trisInCluster = 0;

            int current = seed;
            while (current >= 0 && trisInCluster < maxTris) {
                int base = current * 3;
                int a = indices[base];
                int b = indices[base + 1];
                int c = indices[base + 2];
                int added = addedVertices(localVerts, localVertCount, a, b, c);
                if (localVertCount + added > maxVerts) {
                    if (trisInCluster == 0) {
                        // Always allow one triangle per meshlet even if limits are tiny.
                    } else {
                        break;
                    }
                }

                used[current] = true;
                usedCount++;
                out[outPos++] = a;
                out[outPos++] = b;
                out[outPos++] = c;
                localVertCount = appendUnique(localVerts, localVertCount, a);
                localVertCount = appendUnique(localVerts, localVertCount, b);
                localVertCount = appendUnique(localVerts, localVertCount, c);
                trisInCluster++;

                if (trisInCluster >= maxTris) {
                    break;
                }
                current = bestNext(indices, used, localVerts, localVertCount, maxVerts);
            }
        }

        return out;
    }

    public static List<Meshlet> buildMeshlets(MeshData mesh, int[] indices, int maxVerts, int maxTris) {
        if (indices == null || indices.length == 0) {
            return List.of();
        }
        if ((indices.length % 3) != 0) {
            throw new IllegalStateException("Triangle index buffer length must be divisible by 3");
        }

        float[] pos = mesh.attribute(org.meshforge.core.attr.AttributeSemantic.POSITION, 0).rawFloatArrayOrNull();
        if (pos == null) {
            throw new IllegalStateException("POSITION[0] must be float-backed");
        }

        List<Meshlet> out = new ArrayList<>();
        int[] localVerts = new int[maxVerts];
        int[] clusterTriStarts = new int[maxTris];
        int localVertCount = 0;
        int clusterTris = 0;
        int firstTri = 0;
        int triCount = indices.length / 3;

        for (int t = 0; t < triCount; t++) {
            int triBase = t * 3;
            int a = indices[triBase];
            int b = indices[triBase + 1];
            int c = indices[triBase + 2];
            int add = addedVertices(localVerts, localVertCount, a, b, c);
            boolean overflowVerts = (localVertCount + add) > maxVerts;
            boolean overflowTris = clusterTris >= maxTris;
            if ((overflowVerts || overflowTris) && clusterTris > 0) {
                out.add(buildMeshletFromCluster(firstTri, clusterTriStarts, clusterTris, indices, localVerts, localVertCount, pos));
                firstTri = t;
                localVertCount = 0;
                clusterTris = 0;
                Arrays.fill(localVerts, -1);
            }
            clusterTriStarts[clusterTris++] = triBase;
            localVertCount = appendUnique(localVerts, localVertCount, a);
            localVertCount = appendUnique(localVerts, localVertCount, b);
            localVertCount = appendUnique(localVerts, localVertCount, c);
        }
        if (clusterTris > 0) {
            out.add(buildMeshletFromCluster(firstTri, clusterTriStarts, clusterTris, indices, localVerts, localVertCount, pos));
        }
        return out;
    }

    private static Meshlet buildMeshletFromCluster(
        int firstTri,
        int[] clusterTriStarts,
        int clusterTris,
        int[] indices,
        int[] localVerts,
        int localVertCount,
        float[] pos
    ) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < localVertCount; i++) {
            int v = localVerts[i];
            int p = v * 3;
            float x = pos[p];
            float y = pos[p + 1];
            float z = pos[p + 2];
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        float axisX = 0.0f;
        float axisY = 0.0f;
        float axisZ = 1.0f;
        float cutoff = -1.0f;
        if (clusterTris > 0) {
            float sx = 0.0f;
            float sy = 0.0f;
            float sz = 0.0f;
            for (int i = 0; i < clusterTris; i++) {
                int triBase = clusterTriStarts[i];
                int ia = indices[triBase] * 3;
                int ib = indices[triBase + 1] * 3;
                int ic = indices[triBase + 2] * 3;

                float ax = pos[ia], ay = pos[ia + 1], az = pos[ia + 2];
                float bx = pos[ib], by = pos[ib + 1], bz = pos[ib + 2];
                float cx = pos[ic], cy = pos[ic + 1], cz = pos[ic + 2];
                float e1x = bx - ax, e1y = by - ay, e1z = bz - az;
                float e2x = cx - ax, e2y = cy - ay, e2z = cz - az;
                float nx = e1y * e2z - e1z * e2y;
                float ny = e1z * e2x - e1x * e2z;
                float nz = e1x * e2y - e1y * e2x;
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 1.0e-12f) {
                    sx += nx / len;
                    sy += ny / len;
                    sz += nz / len;
                }
            }
            float alen = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (alen > 1.0e-12f) {
                axisX = sx / alen;
                axisY = sy / alen;
                axisZ = sz / alen;
                cutoff = 1.0f;
                for (int i = 0; i < clusterTris; i++) {
                    int triBase = clusterTriStarts[i];
                    int ia = indices[triBase] * 3;
                    int ib = indices[triBase + 1] * 3;
                    int ic = indices[triBase + 2] * 3;
                    float ax = pos[ia], ay = pos[ia + 1], az = pos[ia + 2];
                    float bx = pos[ib], by = pos[ib + 1], bz = pos[ib + 2];
                    float cx = pos[ic], cy = pos[ic + 1], cz = pos[ic + 2];
                    float e1x = bx - ax, e1y = by - ay, e1z = bz - az;
                    float e2x = cx - ax, e2y = cy - ay, e2z = cz - az;
                    float nx = e1y * e2z - e1z * e2y;
                    float ny = e1z * e2x - e1x * e2z;
                    float nz = e1x * e2y - e1y * e2x;
                    float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    if (len > 1.0e-12f) {
                        float d = (axisX * (nx / len)) + (axisY * (ny / len)) + (axisZ * (nz / len));
                        if (d < cutoff) {
                            cutoff = d;
                        }
                    }
                }
            }
        }

        int firstIndex = firstTri * 3;
        int indexCount = clusterTris * 3;
        return new Meshlet(
            firstTri,
            clusterTris,
            firstIndex,
            indexCount,
            localVertCount,
            new Aabbf(minX, minY, minZ, maxX, maxY, maxZ),
            axisX,
            axisY,
            axisZ,
            cutoff
        );
    }

    private static int nextUnused(boolean[] used) {
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                return i;
            }
        }
        return -1;
    }

    private static int bestNext(int[] indices, boolean[] used, int[] localVerts, int localVertCount, int maxVerts) {
        int bestTri = -1;
        int bestScore = -1;
        int triCount = indices.length / 3;
        for (int t = 0; t < triCount; t++) {
            if (used[t]) {
                continue;
            }
            int base = t * 3;
            int a = indices[base];
            int b = indices[base + 1];
            int c = indices[base + 2];
            int add = addedVertices(localVerts, localVertCount, a, b, c);
            if ((localVertCount + add) > maxVerts) {
                continue;
            }
            int score = sharedVertices(localVerts, localVertCount, a, b, c);
            if (score > bestScore) {
                bestScore = score;
                bestTri = t;
            }
        }
        return bestTri;
    }

    private static int sharedVertices(int[] localVerts, int localVertCount, int a, int b, int c) {
        int score = 0;
        if (contains(localVerts, localVertCount, a)) score++;
        if (contains(localVerts, localVertCount, b)) score++;
        if (contains(localVerts, localVertCount, c)) score++;
        return score;
    }

    private static int addedVertices(int[] localVerts, int localVertCount, int a, int b, int c) {
        int add = 0;
        if (!contains(localVerts, localVertCount, a)) add++;
        if (!contains(localVerts, localVertCount, b) && b != a) add++;
        if (!contains(localVerts, localVertCount, c) && c != a && c != b) add++;
        return add;
    }

    private static boolean contains(int[] localVerts, int localVertCount, int value) {
        for (int i = 0; i < localVertCount; i++) {
            if (localVerts[i] == value) {
                return true;
            }
        }
        return false;
    }

    private static int appendUnique(int[] localVerts, int localVertCount, int value) {
        if (contains(localVerts, localVertCount, value)) {
            return localVertCount;
        }
        localVerts[localVertCount] = value;
        return localVertCount + 1;
    }
}
