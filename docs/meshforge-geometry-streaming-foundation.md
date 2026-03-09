# MeshForge Geometry Streaming Foundation

## Purpose

This document defines the MeshForge-side data foundation for roadmap item #4: Geometry streaming.

Scope is intentionally limited to prepared-data representation and runtime handoff metadata.
It does not introduce residency orchestration, renderer integration, or streaming+visibility/LOD fusion.

## Architectural Boundary

- MeshForge: owns geometry preparation and runtime-ready payload definition.
- DynamisGPU: will own upload/orchestration/execution and residency mechanisms.
- DynamisLightEngine: will own render-planning policy.

This pass keeps streaming in MeshForge as a prepared-data concept only.

## Data Model Added

MGI-side optional meshlet streaming metadata:

- `MgiMeshletStreamUnit`
  - `streamUnitId`
  - `meshletStart`
  - `meshletCount`
  - `payloadByteOffset`
  - `payloadByteSize`
- `MgiMeshletStreamingData`
  - ordered stream-unit list with invariants (strictly increasing ids, non-overlapping ordered meshlet/payload ranges)

MeshForge runtime handoff metadata:

- `MeshletStreamUnitMetadata`
- `MeshletStreamingMetadata`

These types describe how meshlet ranges are partitioned into streamable units and where each unit's payload range lives.

## Runtime Handoff Metadata

Runtime decode now exposes optional streaming metadata via `MgiMeshDataCodec.RuntimeDecodeResult`:

- raw optional `MgiMeshletStreamingData`
- derived optional MeshForge handoff `MeshletStreamingMetadata` via `meshletStreamingMetadataOrNull()`
- `meshletStreamingUnitCount()` convenience count

This keeps streaming structure available without adding residency policy to MeshForge.

## MGI Extension Hook

A new optional MGI chunk type is introduced:

- `MESHLET_STREAM_UNITS` (`0x1106`)

Chunk payload v1 is fixed-width per unit (int32 x 5):

1. `streamUnitId`
2. `meshletStart`
3. `meshletCount`
4. `payloadByteOffset`
5. `payloadByteSize`

Rules:

- optional chunk
- valid only when meshlet descriptor payload exists
- meshlet ranges must remain within descriptor count

## GPU-Ready Upload Prep Seam (MeshForge-side only)

For later DynamisGPU consumption, MeshForge now includes a minimal streaming payload seam:

- `GpuMeshletStreamingPayload`
- `MeshletStreamingUploadPrep`

Layout v1 uses 5 int32 words per stream unit:

1. `streamUnitId`
2. `meshletStart`
3. `meshletCount`
4. `payloadByteOffset`
5. `payloadByteSize`

This is a handoff contract only; no upload/residency management is performed here.

## Deferred Work

This pass intentionally defers:

- streaming residency/orchestration
- GPU-side streaming resource management
- visibility + LOD + streaming fusion
- renderer/frame-graph integration

## Outcome

MeshForge now has a clear, tested, minimal geometry streaming data and runtime handoff foundation.

This sets up later DynamisGPU-side streaming/resource-residency work without changing subsystem boundaries.
