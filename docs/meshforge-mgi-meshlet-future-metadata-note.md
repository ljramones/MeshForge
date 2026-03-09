# MeshForge MGI Meshlet Future Metadata Note

## Purpose
This note captures optional meshlet metadata that may be valuable after the base descriptor/remap/triangle/bounds path is validated in production workflows.

Current guidance remains:
- Keep first meshlet storage implementation limited to `MESHLET_DESCRIPTORS`, `MESHLET_VERTEX_REMAP`, `MESHLET_TRIANGLES`, and `MESHLET_BOUNDS`.
- Add new metadata only when it has a measured benefit.

## Current Baseline
The current meshlet storage path is intentionally minimal and API-agnostic:
- static descriptors (counts/offsets/submesh/material references)
- vertex remap payload
- triangle payload
- per-meshlet bounds

This is sufficient for:
- meshlet presence/load validation
- basic frustum-style culling experiments
- classic and future GPU-driven path compatibility

## Future Optional Metadata

### 1. Cull Cones
Potential chunk: `MESHLET_CULL_CONES` or extension fields in `MESHLET_METADATA`.

Data candidates:
- cone axis (`x,y,z`)
- cutoff cosine

Use case:
- cluster backface/cone culling
- lower candidate meshlet count before draw dispatch

Risk:
- preprocessing complexity increases
- value depends on scene/view characteristics

### 2. LOD Group Relationships
Potential chunk: `MESHLET_LOD_GROUPS`.

Data candidates:
- lod group id per meshlet
- lod level
- optional parent/children references

Use case:
- coarse-to-fine meshlet LOD transitions
- future progressive rendering paths

Risk:
- introduces policy coupling to renderer LOD behavior
- should not be added before baseline meshlet path is stable

### 3. Parent/Child Cluster Metadata
Potential chunk: `MESHLET_HIERARCHY`.

Data candidates:
- parent index
- child range or offsets

Use case:
- hierarchical culling
- faster traversal for large assets

Risk:
- significant complexity
- requires clear runtime consumers before storage is justified

### 4. Streaming Hints
Potential chunk: `MESHLET_STREAMING_HINTS`.

Data candidates:
- residency priority
- spatial region id
- optional working set grouping

Use case:
- meshlet-aware streaming systems
- staged residency of large assets

Risk:
- tightly coupled to streaming policy that is not finalized yet

### 5. Draw/Material Binning Hints
Potential chunk: `MESHLET_BINNING_HINTS`.

Data candidates:
- pass/material bin ids
- sort keys

Use case:
- faster runtime batching for indirect/classic draw paths

Risk:
- can overfit one renderer implementation
- keep optional and ignorable

## Compatibility Guidance
If future metadata is added:
- keep chunks optional
- keep base meshlet chunks sufficient for correctness
- keep classic non-meshlet fallback valid
- avoid backend/API-specific command encoding

## Decision Rule
Promote a future metadata chunk only when all conditions hold:
1. baseline meshlet storage/load path is stable
2. benchmark shows a meaningful gain
3. runtime has a clear consumer for the metadata
4. compatibility/fallback behavior remains straightforward

## Conclusion
Cull cones, LOD/group relationships, hierarchy links, streaming hints, and binning hints are all plausible future extensions, but none are required for the first meshlet-storage implementation.

The immediate priority remains validating and using the existing descriptor/remap/triangle/bounds model end-to-end before widening metadata scope.
