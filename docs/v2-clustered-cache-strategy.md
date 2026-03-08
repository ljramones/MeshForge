# MeshForge v2: Clustered Cache Strategy

## Goal

Persist clustered runtime geometry so cluster structures are loaded directly at runtime and do not require rebuild on each load.

## Strategy Options

## Option A: Extend `.mfgc` with Clustered Sections

Single cache artifact per source asset with variant sections:

- base runtime payload section (v1)
- optional clustered section (v2)

Pros:

- single artifact lifecycle
- shared header/version/invalidation logic
- straightforward sidecar policy reuse

Cons:

- more complex reader/writer implementation
- larger file even when consumers only need base payload

## Option B: Separate Clustered Cache Artifact

Two sidecars per source:

- `asset.mfgc` for base runtime payload
- `asset.mfgcc` (or similar) for clustered payload

Pros:

- independent generation/invalidation
- cleaner separation of base vs clustered products

Cons:

- more lifecycle logic and synchronization complexity
- two-file management overhead

## Recommendation (v2.1)

Start with Option A (single `.mfgc` with variant sections).

Reason:

- preserves v1 lifecycle simplicity
- reuses existing validation pipeline
- avoids cache management sprawl in first clustered rollout

## Header/Validation Extensions

Extend cache metadata with:

- variant bit flags (base, clustered, lod/chunk extensions)
- clustered contract version
- clustered layout hash
- descriptor format version/hash

Rebuild triggers (in addition to v1):

- clustered contract version mismatch
- clustered layout hash mismatch
- requested clustered variant absent
- descriptor payload invalid/corrupt

## Loader Behavior

Canonical clustered load behavior:

1. check `.mfgc` for clustered variant
2. if valid clustered variant exists, load directly
3. otherwise rebuild clustered payload from source/base runtime path
4. write updated cache with clustered variant section

## Acceptance Criteria

- clustered variant can be loaded without cluster rebuild
- invalid clustered sections trigger deterministic rebuild
- base v1 runtime path still works unchanged
- cache lifecycle behavior remains sidecar-based and predictable

## Open Decisions

- exact binary section layout for clustered payload/descriptors
- whether cluster descriptors are AoS or packed SoA on disk
- whether chunk/LOD metadata lands in v2.1 or v2.2+
