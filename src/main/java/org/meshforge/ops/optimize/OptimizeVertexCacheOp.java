package org.meshforge.ops.optimize;

import org.meshforge.core.mesh.MeshData;
import org.meshforge.ops.pipeline.MeshContext;
import org.meshforge.ops.pipeline.MeshOp;

import java.util.Arrays;

/**
 * Forsyth-style vertex cache optimization for indexed triangle lists.
 *
 * Produces reordered indices and a vertex remap, then applies attribute reorder+compaction.
 */
public final class OptimizeVertexCacheOp implements MeshOp {
    private final int cacheSize;

    public OptimizeVertexCacheOp() {
        this(32);
    }

    public OptimizeVertexCacheOp(int cacheSize) {
        this.cacheSize = Math.max(3, cacheSize);
    }

    @Override
    public MeshData apply(MeshData mesh, MeshContext context) {
        int[] indices = mesh.indicesOrNull();
        if (indices == null || indices.length < 3) {
            return mesh;
        }

        Result result = optimize(indices, mesh.vertexCount(), cacheSize);
        context.put("optimizeVertexCache.vertexRemap", result.vertexRemap());
        context.put("optimizeVertexCache.acmrBefore", CacheMetrics.acmr(indices, cacheSize));
        context.put("optimizeVertexCache.acmrAfter", CacheMetrics.acmr(result.indices(), cacheSize));

        return CompactVerticesOp.reorderAndCompact(
            mesh,
            result.indices(),
            result.vertexRemap(),
            result.vertexCount()
        );
    }

    public static Result optimize(int[] indices, int vertexCount, int cacheSize) {
        if ((indices.length % 3) != 0) {
            throw new IllegalArgumentException("Triangle index buffer length must be divisible by 3");
        }

        int triCount = indices.length / 3;
        if (triCount == 0) {
            int[] identity = new int[vertexCount];
            Arrays.fill(identity, -1);
            return new Result(indices.clone(), identity, 0);
        }

        int[] liveTriCount = new int[vertexCount];
        for (int i = 0; i < indices.length; i++) {
            int idx = indices[i];
            if (idx < 0 || idx >= vertexCount) {
                throw new IllegalArgumentException("Index out of range at " + i + ": " + idx);
            }
            liveTriCount[idx]++;
        }

        int[][] adjacency = new int[vertexCount][];
        int[] cursor = new int[vertexCount];
        for (int v = 0; v < vertexCount; v++) {
            adjacency[v] = new int[liveTriCount[v]];
        }
        for (int t = 0; t < triCount; t++) {
            int a = indices[t * 3];
            int b = indices[t * 3 + 1];
            int c = indices[t * 3 + 2];
            adjacency[a][cursor[a]++] = t;
            adjacency[b][cursor[b]++] = t;
            adjacency[c][cursor[c]++] = t;
        }

        boolean[] emitted = new boolean[triCount];
        boolean[] candidate = new boolean[triCount];
        int[] candidateList = new int[triCount];
        int candidateCount = 0;

        int[] cachePos = new int[vertexCount];
        Arrays.fill(cachePos, -1);

        float[] vertexScore = new float[vertexCount];
        for (int v = 0; v < vertexCount; v++) {
            vertexScore[v] = scoreVertex(cachePos[v], liveTriCount[v], cacheSize);
        }

        float[] triangleScore = new float[triCount];
        for (int t = 0; t < triCount; t++) {
            int a = indices[t * 3];
            int b = indices[t * 3 + 1];
            int c = indices[t * 3 + 2];
            triangleScore[t] = vertexScore[a] + vertexScore[b] + vertexScore[c];
        }

        int[] output = new int[indices.length];
        int[] lru = new int[cacheSize];
        Arrays.fill(lru, -1);

        int nextSeed = 0;
        int outTri = 0;

        while (outTri < triCount) {
            if (candidateCount == 0) {
                nextSeed = nextUnemitted(emitted, nextSeed);
                if (nextSeed >= triCount) {
                    break;
                }
                candidateCount = addCandidate(nextSeed, emitted, candidate, candidateList, candidateCount);
            }

            int bestSlot = -1;
            int bestTri = -1;
            float bestScore = -1.0f;
            for (int i = 0; i < candidateCount; i++) {
                int t = candidateList[i];
                if (emitted[t]) {
                    continue;
                }
                float score = triangleScore[t];
                if (score > bestScore) {
                    bestScore = score;
                    bestTri = t;
                    bestSlot = i;
                }
            }

            if (bestTri < 0) {
                candidateCount = 0;
                continue;
            }

            emitted[bestTri] = true;
            removeCandidate(bestSlot, bestTri, candidate, candidateList, candidateCount);
            candidateCount--;

            int a = indices[bestTri * 3];
            int b = indices[bestTri * 3 + 1];
            int c = indices[bestTri * 3 + 2];

            output[outTri * 3] = a;
            output[outTri * 3 + 1] = b;
            output[outTri * 3 + 2] = c;
            outTri++;

            touch(lru, a);
            touch(lru, b);
            touch(lru, c);

            Arrays.fill(cachePos, -1);
            for (int i = 0; i < cacheSize; i++) {
                int v = lru[i];
                if (v >= 0) {
                    cachePos[v] = i;
                }
            }

            liveTriCount[a]--;
            liveTriCount[b]--;
            liveTriCount[c]--;

            for (int i = 0; i < cacheSize; i++) {
                int v = lru[i];
                if (v >= 0) {
                    vertexScore[v] = scoreVertex(cachePos[v], liveTriCount[v], cacheSize);
                    for (int t : adjacency[v]) {
                        if (!emitted[t]) {
                            int i0 = indices[t * 3];
                            int i1 = indices[t * 3 + 1];
                            int i2 = indices[t * 3 + 2];
                            triangleScore[t] = vertexScore[i0] + vertexScore[i1] + vertexScore[i2];
                            candidateCount = addCandidate(t, emitted, candidate, candidateList, candidateCount);
                        }
                    }
                }
            }
        }

        int[] oldToNew = new int[vertexCount];
        Arrays.fill(oldToNew, -1);
        int[] compactedIndices = new int[output.length];
        int nextVertex = 0;
        for (int i = 0; i < output.length; i++) {
            int oldIndex = output[i];
            int mapped = oldToNew[oldIndex];
            if (mapped < 0) {
                mapped = nextVertex++;
                oldToNew[oldIndex] = mapped;
            }
            compactedIndices[i] = mapped;
        }

        return new Result(compactedIndices, oldToNew, nextVertex);
    }

    private static int nextUnemitted(boolean[] emitted, int from) {
        int i = Math.max(0, from);
        while (i < emitted.length && emitted[i]) {
            i++;
        }
        return i;
    }

    private static int addCandidate(
        int tri,
        boolean[] emitted,
        boolean[] candidate,
        int[] candidateList,
        int count
    ) {
        if (!emitted[tri] && !candidate[tri]) {
            candidate[tri] = true;
            candidateList[count++] = tri;
        }
        return count;
    }

    private static void removeCandidate(int slot, int tri, boolean[] candidate, int[] candidateList, int count) {
        candidate[tri] = false;
        int last = candidateList[count - 1];
        candidateList[slot] = last;
    }

    private static void touch(int[] lru, int vertex) {
        int index = -1;
        for (int i = 0; i < lru.length; i++) {
            if (lru[i] == vertex) {
                index = i;
                break;
            }
        }
        if (index == 0) {
            return;
        }
        if (index > 0) {
            int value = lru[index];
            System.arraycopy(lru, 0, lru, 1, index);
            lru[0] = value;
        } else {
            System.arraycopy(lru, 0, lru, 1, lru.length - 1);
            lru[0] = vertex;
        }
    }

    private static float scoreVertex(int cachePos, int liveTris, int cacheSize) {
        final float cacheDecayPower = 1.5f;
        final float lastTriScore = 0.75f;
        final float valenceBoostScale = 2.0f;
        final float valenceBoostPower = 0.5f;

        if (liveTris <= 0) {
            return -1.0f;
        }

        float score = 0.0f;
        if (cachePos < 0) {
            // not in cache
        } else if (cachePos < 3) {
            score = lastTriScore;
        } else {
            float scaler = 1.0f / (cacheSize - 3);
            float rank = 1.0f - (cachePos - 3) * scaler;
            score = (float) Math.pow(rank, cacheDecayPower);
        }

        float valenceBoost = valenceBoostScale * (float) Math.pow(liveTris, -valenceBoostPower);
        return score + valenceBoost;
    }

    public record Result(int[] indices, int[] vertexRemap, int vertexCount) {
    }
}
