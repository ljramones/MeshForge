# MeshForge MGI Trusted Fast Path Plan

## Purpose
Runtime prep still spends most time in preprocessing-oriented stages for complex meshes (for example RevitHouse-like assets):

- `pack.vertexPayload`
- `pipeline.topology` (`removeDegenerates`, `bounds`)
- `pipeline` attribute processing

MGI already removed most source-ingest cost. The next step is to move canonicalization work out of runtime for trusted MGI assets.

## Problem Statement
Current runtime activation still pays for work that is deterministic and import-time friendly:

- degenerate cleanup
- bounds generation
- optional attribute generation/normalization (when source is already canonical)

This keeps runtime prep above target for large assets and duplicates work that can be persisted once.

## Scope
This plan introduces a trusted fast path for MGI-backed assets.

In scope:

- trusted runtime path for MGI assets that declare required canonical metadata
- explicit fallback to existing safe path when trust requirements are not met
- debug/safe validation policy for development builds

Out of scope:

- replacing existing safe path
- full zero-copy commitment
- runtime meshlet generation
- immediate final GPU payload lock-in

## Trusted Fast Path Runtime Policy
When trust mode is enabled and required metadata is present:

Skip:

- runtime `validate`
- runtime `removeDegenerates`
- runtime `bounds`
- attribute recompute when prebaked attributes are present and valid

Still perform:

- minimal structural/header/chunk sanity checks
- payload/view setup
- plan/build/pack (unless a later packed-payload extension is used)
- upload dispatch

Fallback behavior:

- any missing required trust metadata -> safe path
- older MGI versions/chunks -> safe path
- explicit non-trust mode -> safe path

## Proposed MGI Additions
### MGI v1.1 (incremental)
- Prebaked bounds metadata (AABB minimum and maximum)
- Canonical metadata block:
  - canonical vertex count
  - canonical index count
  - `degenerateFree` flag
  - `trustedCanonical` flag (or equivalent trust bit)

### MGI v2+ (optional/forward)
- Optional chunk for packed runtime payload
- Optional meshlet metadata hook chunk

Design intent:

- keep v1.1 additive and backwards-compatible
- use optional chunks + flags for progressive adoption

## Compatibility and Versioning
Version strategy:

- v1.1: additive metadata/chunks, old files remain readable
- v2+: optional higher-performance payload forms

Reader policy:

- if required trusted metadata is absent or invalid, downgrade to safe path
- trust mode never bypasses structural/chunk-level validation

## Safety and Validation Policy
Recommended policy levels:

- `SAFE` (default): full current runtime prep/validation behavior
- `TRUSTED`: skip canonicalization stages when trust requirements are met
- `TRUSTED_DEBUG`: trusted fast path plus cheap assertion checks/logging

`TRUSTED_DEBUG` is intended for CI/perf validation phases to catch regressions early.

## Non-Goals
- No immediate replacement of canonical planar MGI streams with a single locked GPU layout
- No runtime meshlet generation on the hot path
- No broad redesign of upload architecture
- No unrelated cleanup/refactors in this phase

## Recommended Implementation Order
1. Add prebaked bounds support to MGI
2. Add canonical/trust metadata (`degenerateFree`, canonical counts, trust flag)
3. Implement trusted runtime fast-path guard + safe fallback
4. Benchmark: `runtime-only` vs `mgi-full` vs `mgi-trusted-fast`
5. If needed, evaluate optional packed-payload chunk (design first)

## Benchmark Expectations
Primary expected effect of trusted fast path:

- topology work (`removeDegenerates`, `bounds`) approaches zero at runtime
- reduced `pipeline.total` for trusted MGI assets
- runtime prep shifts closer to payload copy/pack floor

Target usage scenarios:

- complex static meshes (RevitHouse-like)
- repeated runtime activations where import-time canonicalization is feasible

## Architecture Fit
This plan preserves current layering:

`source -> loader -> MGI -> runtime prep -> packed mesh -> upload manager -> GPU`

It narrows runtime prep responsibilities for trusted MGI assets without removing safe behavior for other sources.
