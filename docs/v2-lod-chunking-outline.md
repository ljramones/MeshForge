# MeshForge v2: LOD and Chunking Outline

## Goal

Define how clustered geometry should scale to large worlds via LOD-aware outputs and chunk-oriented preparation.

## Phasing

## v2.2: LOD + Cluster Integration

Deliverables:

- LOD-aware clustering pipeline output
- metadata linking clusters to LOD tiers
- grouped cluster ranges per LOD
- deterministic mapping between source mesh regions and LOD cluster outputs

Proposed metadata:

- `lodLevel`
- `clusterRangeStart`
- `clusterRangeCount`
- `lodBounds`
- optional error metric

Acceptance:

- runtime can select clustered LOD tier from metadata
- cache stores multiple clustered LOD variants deterministically

## v2.3: Chunking and Streaming Preparation

Deliverables:

- chunk model for independently loadable geometry units
- chunk-to-cluster mapping tables
- per-chunk bounds and residency-relevant metadata
- cache support for chunked clustered payload sections

Proposed metadata:

- `chunkId`
- `clusterRangeStart`
- `clusterRangeCount`
- `chunkBounds`
- optional dependency/preload hints

Acceptance:

- large assets can be partitioned into chunked clustered products
- chunk metadata is sufficient for downstream streaming systems
- clustered loader/cache path can retrieve chunk-relevant payloads deterministically

## Contract Boundaries

MeshForge v2 responsibilities:

- generate clustered/LOD/chunked geometry products
- emit deterministic metadata and cache payloads

Out of MeshForge:

- streaming scheduler
- residency manager
- renderer dispatch policy

## Dependencies on Earlier v2 Work

Requires:

- v2.0 clustered runtime contract
- v2.1 clustered cache variant support

Should not start full implementation before these are locked.

## Open Questions

- canonical chunk granularity rules (triangle budget, spatial budget, both)
- whether chunk boundaries are mesh-authoring-aware or purely runtime-derived
- whether LOD generation is internal to MeshForge or integrated via pipeline hooks
- storage layout for chunk-local descriptor tables
