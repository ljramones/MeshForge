# MeshForge Capability Priority

## Purpose
Define the execution order for future geometry capabilities, with objective gates that keep work measurable and prevent premature expansion.

## Priority Order
1. Meshlet frustum culling
2. GPU-driven cluster visibility
3. Meshlet LOD
4. Geometry streaming
5. Packed payload optimization
6. Compression
7. Ray tracing support
8. Tessellation/subdivision

## Stage Gates

### 1) Meshlet Frustum Culling
Entry criteria:
- MGI meshlet descriptor/remap/triangle/bounds path is stable.
- Prebaked meshlet load path is validated.

Implementation scope:
- CPU frustum test against meshlet bounds.
- Visibility list generation for classic draw submission.

Benchmark gates:
- Report visible triangle reduction vs whole-mesh baseline on `RevitHouse` and `xyzrgb_dragon`.
- Report culling pass cost (median/p95).

Exit criteria:
- Measurable triangle reduction with acceptable CPU overhead.
- Stable behavior across both fixtures.

### 2) GPU-Driven Cluster Visibility
Entry criteria:
- Meshlet frustum culling baseline is stable and benchmarked.

Implementation scope:
- Compute/indirect path that builds visible meshlet lists on GPU.
- Keep classic CPU visibility path as fallback.

Benchmark gates:
- Compare CPU vs GPU visibility pass time.
- Compare frame-time impact and submission overhead.

Exit criteria:
- GPU path demonstrates net benefit in at least one heavy fixture.
- Fallback parity maintained.

### 3) Meshlet LOD
Entry criteria:
- Visibility pipeline (CPU and/or GPU) is stable.

Implementation scope:
- Add meshlet LOD grouping/selection policy.
- Start with deterministic distance-based policy.

Benchmark gates:
- Report triangle count and draw work reduction by distance bands.
- Report quality regression checks (artifact threshold snapshots).

Exit criteria:
- Clear geometry cost reduction with acceptable visual quality.

### 4) Geometry Streaming
Entry criteria:
- Meshlet visibility + LOD behavior is understood.
- UploadManager telemetry remains stable under staged arrivals.

Implementation scope:
- Stream mesh/cluster data by residency budget.
- Integrate with backlog/inflight controls already in place.

Benchmark gates:
- Residency misses, pop-in latency, upload queue pressure.
- p95 TTFU under streaming arrival patterns.

Exit criteria:
- Predictable residency behavior with bounded pressure.

### 5) Packed Payload Optimization
Entry criteria:
- Streaming and visibility policies are stable enough to define hot runtime layout.

Implementation scope:
- Optional runtime-ready packed payload chunk(s) in MGI.
- Preserve canonical fallback path.

Benchmark gates:
- Compare `mgi-trusted-fast` vs packed payload path.
- Measure runtime prep reduction and activation latency deltas.

Exit criteria:
- Packed path yields meaningful gain without unacceptable format lock-in.

### 6) Compression
Entry criteria:
- Packed/canonical payload decisions are stable.

Implementation scope:
- Optional chunk compression with clear decode boundaries.
- Keep uncompressed fallback for tooling and validation.

Benchmark gates:
- IO size reduction.
- Decode/decompress CPU cost.
- Net load/activation impact.

Exit criteria:
- Net end-to-end improvement for targeted deployment profiles.

### 7) Ray Tracing Support
Entry criteria:
- Raster/visibility path and asset representation are stable.

Implementation scope:
- Add acceleration-structure input path from canonical/meshlet data.
- Keep it isolated from raster baseline path.

Benchmark gates:
- BLAS/TLAS build/update cost.
- Memory overhead.
- Ray query/trace workload impact.

Exit criteria:
- RT path is additive and does not destabilize baseline geometry path.

### 8) Tessellation/Subdivision
Entry criteria:
- Clear product need; not speculative.

Implementation scope:
- Evaluate offline vs runtime subdivision policy.
- Integrate only if compatible with meshlet/LOD/streaming strategy.

Benchmark gates:
- Geometry amplification cost.
- Frame-time impact.
- Memory overhead and content complexity impact.

Exit criteria:
- Demonstrable quality/performance value for target content.

## Global Rules
- Keep each phase isolated and benchmarked before moving to the next.
- Maintain backward-compatible fallback paths when introducing optional formats/features.
- Reject changes that add complexity without measurable wins.
- Preserve reproducible harness commands and fixture-based reporting for every phase.
