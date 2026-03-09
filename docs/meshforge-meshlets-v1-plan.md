# MeshForge Meshlets v1 Plan

Date: 2026-03-08

## Why Now

Ingestion and upload are now controlled:

- MGI removes most source-ingest tax.
- UploadManager provides stable bounded upload behavior.
- Runtime-only cost is now much closer to true engine-side geometry cost.

With front-end tax reduced, the next wins are in making already-loaded geometry cheaper to cull, submit, and shade. Meshlets are the right next representation-level experiment.

## Goal

Prototype a minimal, measurable meshlet path for static meshes to evaluate culling/submission benefits and generation cost.

## Non-Goals (v1)

- no full mesh-shader pipeline work
- no hierarchical meshlet trees
- no residency/streaming system redesign
- no full GPU-driven renderer integration
- no broad file-format redesign in this phase

## v1 Scope

- static meshes only
- bounded cluster constraints (max vertices / max triangles per meshlet)
- per-meshlet bounds (AABB or sphere)
- optional cone metadata only if straightforward
- CPU-side generation first

## Pipeline Placement

Initial placement should be runtime-prep-side experimentation:

```text
MeshData
  -> runtime prep
  -> meshlet partition prototype
  -> packed mesh (existing path)
```

After behavior is validated, promote stable meshlet metadata into future MGI extension chunks.

## Target Data Model (Initial)

```text
MeshletSet
  meshletCount
  meshletTable[]
    firstTriangleOffset
    triangleCount
    vertexStart
    vertexCount
    bounds
```

Enough for culling/submission experiments without overbuilding.

## Experiment Plan

### 1. Generation Cost

Measure meshlet generation overhead per fixture:

- dragon
- RevitHouse
- one small mesh control (for example suzanne)

Capture median/p95 generation time and output meshlet counts.

### 2. Culling Granularity Benefit

Run a simple camera-frustum visibility experiment and compare:

- whole-mesh visible triangles
- meshlet-visible triangles
- triangle submission reduction

### 3. Submission Behavior Impact

Measure whether meshlet granularity improves submission/culling effectiveness on larger partially visible assets.

## Success Criteria

Meshlets v1 is considered successful if at least one of the following is demonstrated on representative fixtures:

- clear reduction in submitted/processed triangles for partial visibility
- measurable frame-relevant culling granularity benefit
- generation cost low enough for selected integration mode (runtime or precomputed)

## Decision Gate: Runtime vs Precompute

After v1 measurements:

- if generation cost is low and benefits are clear, keep runtime generation path for iteration.
- if generation cost is significant, move meshlet generation to import/precompute and store metadata in future MGI extension chunks.

## Expected Next Phase

If v1 succeeds:

1. define MGI meshlet extension chunk draft
2. store/reload meshlet metadata through MGI
3. integrate with higher-level GPU-driven submission/culling experiments
