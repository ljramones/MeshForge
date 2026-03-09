package org.dynamisengine.meshforge.mgi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MgiMeshletModelTest {

    @Test
    void acceptsValidMeshletModel() {
        MgiMeshletData data = new MgiMeshletData(
            List.of(new MgiMeshletDescriptor(0, 0, 0, 4, 0, 2, 0, 0)),
            new int[] {0, 1, 2, 3},
            new int[] {0, 1, 2, 2, 1, 3},
            List.of(new MgiMeshletBounds(0f, 0f, 0f, 1f, 1f, 0f))
        );

        assertEquals(1, data.descriptors().size());
        assertEquals(4, data.vertexRemap().length);
        assertEquals(6, data.triangles().length);
        assertEquals(1, data.bounds().size());
    }

    @Test
    void rejectsDescriptorWithNegativeOffsets() {
        assertThrows(IllegalArgumentException.class, () ->
            new MgiMeshletDescriptor(0, 0, -1, 4, 0, 1, 0, 0));
    }

    @Test
    void rejectsDescriptorWithZeroCounts() {
        assertThrows(IllegalArgumentException.class, () ->
            new MgiMeshletDescriptor(0, 0, 0, 0, 0, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () ->
            new MgiMeshletDescriptor(0, 0, 0, 4, 0, 0, 0, 0));
    }

    @Test
    void rejectsInvalidBounds() {
        assertThrows(IllegalArgumentException.class, () ->
            new MgiMeshletBounds(1f, 0f, 0f, 0f, 1f, 1f));
    }

    @Test
    void rejectsOutOfRangeBoundsReference() {
        assertThrows(IllegalArgumentException.class, () ->
            new MgiMeshletData(
                List.of(new MgiMeshletDescriptor(0, 0, 0, 4, 0, 2, 1, 0)),
                new int[] {0, 1, 2, 3},
                new int[] {0, 1, 2, 2, 1, 3},
                List.of(new MgiMeshletBounds(0f, 0f, 0f, 1f, 1f, 0f))
            ));
    }

    @Test
    void rejectsTriangleIndexOutsideLocalVertexRange() {
        assertThrows(IllegalArgumentException.class, () ->
            new MgiMeshletData(
                List.of(new MgiMeshletDescriptor(0, 0, 0, 3, 0, 1, 0, 0)),
                new int[] {0, 1, 2},
                new int[] {0, 1, 3},
                List.of(new MgiMeshletBounds(0f, 0f, 0f, 1f, 1f, 0f))
            ));
    }

    @Test
    void rejectsRemapRangeOverflow() {
        assertThrows(IllegalArgumentException.class, () ->
            new MgiMeshletData(
                List.of(new MgiMeshletDescriptor(0, 0, 2, 4, 0, 1, 0, 0)),
                new int[] {0, 1, 2, 3},
                new int[] {0, 1, 2},
                List.of(new MgiMeshletBounds(0f, 0f, 0f, 1f, 1f, 0f))
            ));
    }
}
