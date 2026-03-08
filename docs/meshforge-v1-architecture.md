# MeshForge v1 Architecture Summary

## Overview

MeshForge v1 is the runtime geometry subsystem for Dynamis. It is responsible for importing mesh assets, preparing runtime-ready geometry, managing runtime geometry caching, and producing a GPU upload-ready plan through a dedicated bridge layer.

MeshForge is intentionally GPU-agnostic. It does not create GPU resources directly; it produces stable runtime geometry contracts consumed by `meshforge-dynamisgpu` and then DynamisGPU.

## Canonical Runtime Geometry Pipeline

```text
Asset (OBJ/GLTF/etc)
        |
        v
RuntimeGeometryLoader
        |
        +-- cache hit -> RuntimeGeometryPayload
        |
        +-- cache miss/stale
             import -> runtime prep -> pack -> cache write
        |
        v
MeshForgeGpuBridge
        |
        v
GpuGeometryUploadPlan
        |
        v
DynamisGPU
```

### Slow Path (Source Import)

Used when no valid `.mfgc` cache exists:

1. source mesh is loaded via `MeshLoaders`
2. runtime preprocessing runs (`Pipelines.realtimeFast`)
3. runtime packing is executed
4. runtime payload is cached
5. payload is translated into a GPU upload plan

### Fast Path (Cache Hit)

Used when a valid `.mfgc` cache exists:

1. runtime payload is loaded directly from cache
2. payload is translated into a GPU upload plan
3. source parse and runtime prep are bypassed

This path is the preferred runtime ingestion path for low cold-load latency.

## Packing Doctrine

MeshForge uses a three-tier packing model:

- `pack(...)`: friendly, allocating, ergonomic API
- `packInto(...)`: runtime workspace path with lower allocation
- `packPlannedInto(...)`: repeated-runtime low-overhead path using a precomputed plan

This split allows tooling-friendly usage while preserving an engine-grade path for repeated scene/world construction.

## Runtime Geometry Cache

The runtime cache uses `.mfgc` sidecar files next to source assets by default:

```text
asset.obj
asset.mfgc
```

Validation checks include:

- magic/version compatibility
- endianness support
- header flags support
- layout hash consistency
- payload integrity (not truncated/corrupt)

Lifecycle behavior is load-or-build:

- valid cache -> load cache
- missing/stale/invalid cache -> rebuild from source and rewrite cache

Prebuild support is exposed through `RuntimeGeometryLoader.prebuild(...)` for CI, editor, and tooling cache warming workflows.

## MeshForge -> GPU Seam

The integration seam is:

- `RuntimeGeometryPayload`: neutral runtime geometry contract
- `MeshForgeGpuBridge`: translator from payload to upload plan
- `GpuGeometryUploadPlan`: GPU-facing binding/upload metadata

Separation of responsibilities:

- MeshForge: geometry processing, packing, runtime payloads, cache lifecycle
- bridge module: translation only
- DynamisGPU: resource allocation, upload, residency, backend-specific execution

## Canonical Engine Usage

```java
RuntimeGeometryLoader loader =
    new RuntimeGeometryLoader(MeshLoaders.defaultsFast(), Packers.realtime());

RuntimeGeometryLoader.Result loaded = loader.load(assetPath);
RuntimeGeometryPayload payload = loaded.payload();

GpuGeometryUploadPlan plan = MeshForgeGpuBridge.buildUploadPlan(payload);
// DynamisGPU consumes plan for buffer upload/binding.
```

Optional cache warmup:

```java
RuntimeGeometryLoader.PrebuildResult prebuilt = loader.prebuild(assetPath);
```

## Key Design Decisions

- Canonical runtime vertex layout is interleaved (ADR-0002).
- Runtime cache is a first-class path, not a benchmark-only feature.
- Planned packing is the preferred repeated-runtime path.
- MeshForge remains GPU-agnostic; translation is isolated in `meshforge-dynamisgpu`.

## Out of Scope for v1

The following are intentionally deferred beyond MeshForge v1:

- meshlet/cluster generation
- chunked/partial runtime geometry streaming
- renderer-specific layout variants beyond the canonical contract

These are expected to be handled in later DynamisGPU/renderer phases on top of the stable MeshForge runtime geometry contracts.
