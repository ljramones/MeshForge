package org.meshforge.test;

import org.junit.jupiter.api.Test;
import org.meshforge.pack.simd.SimdNormalPacker;
import org.vectrix.gpu.OctaNormal;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimdNormalPackerTest {
    @Test
    void simdOctaPackingMatchesScalarReference() {
        int vertexCount = 257; // non-multiple of typical SIMD lane sizes to exercise scalar tail
        float[] normals = new float[vertexCount * 3];
        Random rnd = new Random(42L);
        for (int i = 0; i < vertexCount; i++) {
            int off = i * 3;
            float x = (rnd.nextFloat() * 2.0f) - 1.0f;
            float y = (rnd.nextFloat() * 2.0f) - 1.0f;
            float z = (rnd.nextFloat() * 2.0f) - 1.0f;
            normals[off] = x;
            normals[off + 1] = y;
            normals[off + 2] = z;
        }

        int[] simd = new int[vertexCount];
        SimdNormalPacker.packOctaNormals(normals, vertexCount, simd);

        for (int i = 0; i < vertexCount; i++) {
            int off = i * 3;
            int expected = OctaNormal.encodeSnorm16(normals[off], normals[off + 1], normals[off + 2]);
            assertEquals(expected, simd[i], "packed mismatch at vertex " + i);
        }
    }
}
