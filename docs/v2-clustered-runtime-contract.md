# MeshForge v2: Clustered Runtime Contract

## Goal

Define the canonical runtime contract for clustered (meshlet-oriented) geometry so clustering is a first-class runtime product, not just an optional processing detail.

## Scope

This contract covers:

- clustered runtime data model
- required metadata for downstream GPU consumers
- bridge-facing upload-plan requirements
- relationship to existing `PackedMesh`/runtime payload flows

Out of scope:

- renderer dispatch policy
- GPU resource lifecycle
- scene graph ownership

## Proposed Runtime Types

Proposed package area:

- `org.dynamisengine.meshforge.runtime.cluster.*`

Core records/types:

- `ClusteredMesh`
  - immutable container for clustered runtime geometry product
- `ClusterRuntimePayload`
  - raw payload bytes + layout metadata for bridge translation
- `ClusterDescriptor`
  - per-cluster descriptor (offsets/counts/bounds/cone/material or group id)
- `ClusterGroupRange`
  - contiguous cluster range metadata for grouping/LOD/chunk layers

Bridge-side target:

- `ClusteredGeometryUploadPlan`
  - GPU-facing upload/binding metadata for clustered payloads

## Minimum Contract Fields

`ClusterRuntimePayload` should include at least:

- vertex payload bytes
- index payload bytes
- cluster descriptor payload (or structured descriptor list)
- vertex layout id/hash
- index type + index count
- cluster count
- cluster group ranges
- aggregate bounds

`ClusterDescriptor` minimum:

- `firstIndex`
- `indexCount`
- `vertexWindowStart`
- `vertexWindowCount`
- `bounds`
- `cone` (if enabled by spec)
- `groupId` or `materialId`

## Relationship to v1 Contracts

v2 clustered runtime should not break v1 canonical paths:

- v1 remains:
  - `RuntimeGeometryLoader -> RuntimeGeometryPayload -> MeshForgeGpuBridge -> GpuGeometryUploadPlan`
- v2 adds:
  - `RuntimeClusteredGeometryLoader -> ClusterRuntimePayload -> ClusterBridge -> ClusteredGeometryUploadPlan`

Compatibility principle:

- clustered output is an explicit runtime variant, not an implicit replacement.

## PackSpec Integration

Clustered outputs should be selected explicitly by `PackSpec` (or a tightly related clustered spec):

- default runtime layout remains v1 canonical interleaved
- clustered contract enabled via explicit spec flags/preset

## Acceptance Criteria

This contract is ready when:

- types are defined and documented
- required metadata is explicit and versionable
- bridge translation requirements are clear
- cache format strategy has a compatible target payload shape

## Next Step

Convert this contract into a formal ADR once cache strategy decisions are finalized.
