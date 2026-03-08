# Runtime Geometry Flow

## Canonical Engine Path

MeshForge runtime geometry should enter the engine through `RuntimeGeometryLoader`,
then pass through the MeshForge->GPU bridge.

```java
RuntimeGeometryLoader loader =
    new RuntimeGeometryLoader(MeshLoaders.defaultsFast(), Packers.realtime());

RuntimeGeometryLoader.Result loaded = loader.load(assetPath);
RuntimeGeometryPayload payload = loaded.payload();

GpuGeometryUploadPlan plan = MeshForgeGpuBridge.buildUploadPlan(payload);
// DynamisGPU upload integration consumes the plan here.
```

## Lifecycle Modes

- `load(asset)`:
  - cache hit -> return payload from `.mfgc`
  - cache miss/stale -> import + runtime prep + pack + write cache + return payload
- `prebuild(asset)`:
  - create or refresh cache ahead of runtime load
  - returns whether cache was reused or rebuilt

## Recommended Usage

- Engine runtime path: `load(...)` then `buildUploadPlan(...)`
- CI/tooling/editor path: `prebuild(...)` before runtime sessions
- Prefer sidecar cache policy (`asset.obj` + `asset.mfgc`) unless tooling overrides cache root
