# ADR-0002: Runtime Geometry Layout Strategy

- Status: Accepted
- Date: 2026-03-08

## Context

MeshForge runtime geometry now feeds:

- runtime cache format (`.mfgc`)
- MeshForge -> DynamisGPU seam (`RuntimeGeometryPayload -> GpuGeometryUploadPlan`)
- renderer upload/binding paths

Layout stability is therefore a system contract, not an implementation detail. The chosen layout must balance:

- cache/seam simplicity and stability today
- efficient runtime upload/fetch behavior
- future extensibility for meshlets, streaming, and specialized pipelines

## Options Considered

1. Fully interleaved vertex layout
2. Multi-stream vertex layout
3. Hybrid (one canonical default + PackSpec-driven alternatives)

### 1) Fully interleaved

Pros:

- simplest cache schema and seam translation
- straightforward GPU binding model
- stable default runtime contract

Cons:

- less flexible for partial per-attribute updates/streaming

### 2) Multi-stream

Pros:

- highest flexibility for partial updates and stream-specific residency

Cons:

- higher contract and metadata complexity
- more complex cache and seam design
- larger short-term implementation surface

### 3) Hybrid

Pros:

- preserves a stable default while allowing targeted specialization later

Cons:

- requires discipline to avoid default-contract drift

## Decision

The canonical runtime geometry layout is **interleaved vertex layout**.

MeshForge runtime/cache/seam contracts standardize on interleaved payloads now, with `PackSpec` reserved as the controlled extension point for future specialized layouts.

## Consequences

### Positive

- stable runtime cache contract
- minimal seam translation overhead
- simple and predictable renderer binding path
- low conceptual/maintenance overhead for current productionization phase

### Trade-offs

- reduced immediate flexibility for per-attribute streaming/update patterns
- multi-stream and meshlet-specialized layouts remain future work

### Future Extension Path

- Additive extension only: keep canonical interleaved default intact.
- Introduce alternative layout modes via explicit `PackSpec` variants.
- Version cache format when introducing non-canonical payload shapes.
- Extend upload-plan contracts for multi-stream only when a concrete pipeline requires it.
