package org.dynamisengine.meshforge.ops.streaming;

/**
 * One streamable meshlet unit range and payload location metadata.
 *
 * @param streamUnitId stable stream unit id
 * @param meshletStart inclusive meshlet start index
 * @param meshletCount number of meshlets in this unit
 * @param payloadByteOffset payload byte offset for this stream unit
 * @param payloadByteSize payload byte size for this stream unit
 */
public record MeshletStreamUnitMetadata(
    int streamUnitId,
    int meshletStart,
    int meshletCount,
    int payloadByteOffset,
    int payloadByteSize
) {
    public MeshletStreamUnitMetadata {
        if (streamUnitId < 0) {
            throw new IllegalArgumentException("streamUnitId must be >= 0");
        }
        if (meshletStart < 0) {
            throw new IllegalArgumentException("meshletStart must be >= 0");
        }
        if (meshletCount <= 0) {
            throw new IllegalArgumentException("meshletCount must be > 0");
        }
        if (payloadByteOffset < 0) {
            throw new IllegalArgumentException("payloadByteOffset must be >= 0");
        }
        if (payloadByteSize <= 0) {
            throw new IllegalArgumentException("payloadByteSize must be > 0");
        }
    }
}
