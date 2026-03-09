# MeshForge Packed Runtime Payload Optimization

## Scope

This pass is a measurement + optimization-foundation slice for roadmap item #5 (packed runtime payload optimization).

It intentionally excludes:

- compression
- renderer integration
- broad MGI redesign
- DynamisGPU policy changes

## Runtime Payload Audit

### Active GPU handoff payloads

- `GpuMeshletVisibilityPayload`
  - layout: 6 `float` per meshlet (`minX,minY,minZ,maxX,maxY,maxZ`)
  - stride: 24 bytes per meshlet (no padding)
- `GpuMeshletLodPayload`
  - layout: 4 `int32` per LOD level (`lodLevel,meshletStart,meshletCount,geometricErrorBits`)
  - stride: 16 bytes per level (no padding)
- `GpuMeshletStreamingPayload`
  - layout: 5 `int32` per stream unit (`streamUnitId,meshletStart,meshletCount,payloadByteOffset,payloadByteSize`)
  - stride: 20 bytes per unit (no padding)

### Findings

1. Payload layouts are already dense and tightly packed; no obvious schema-level padding waste was found.
2. A shared hot-path inefficiency existed in payload export:
   - `to*ByteBuffer()` methods used per-element scalar loops (`putFloat` / `putInt`) into direct buffers.
   - This incurs extra per-element Java loop overhead during upload-prep materialization.
3. The inefficiency is local and low-risk to fix without changing payload contracts.

## Optimization Implemented

A single minimal optimization slice was applied:

- Replaced scalar per-element buffer writes with bulk primitive-buffer writes:
  - `FloatBuffer.put(float[])` for visibility payload export
  - `IntBuffer.put(int[])` for LOD payload export
  - `IntBuffer.put(int[])` for streaming payload export

Changed methods:

- `GpuMeshletVisibilityPayload.toBoundsByteBuffer()`
- `GpuMeshletLodPayload.toLevelsByteBuffer()`
- `GpuMeshletStreamingPayload.toUnitsByteBuffer()`

### Why this slice

- Preserves all existing payload schemas and byte layouts.
- Avoids changing subsystem boundaries.
- Reduces CPU-side export overhead in a shared, repeated code path.
- Keeps this pass safely below compression and format redesign work.

## Validation

Existing payload tests verify unchanged byte order and field ordering:

- `GpuMeshletVisibilityPayloadTest`
- `GpuMeshletLodPayloadTest`
- `GpuMeshletStreamingPayloadTest`

## Deferred Work

- Compression strategy and codec choices (roadmap item #6)
- Optional packed payload schema evolution beyond current dense layouts
- Cross-payload deduplication or hot/cold splitting requiring larger design changes

## Conclusion

This pass confirms that current runtime payload schemas are already compact, and applies one concrete low-risk optimization to reduce export overhead without contract or architecture churn. It establishes a cleaner baseline for upcoming compression work.

