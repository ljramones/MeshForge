# MGI Packed Runtime Payload Design Note

## Context
MGI + trusted fast path has already removed the largest runtime canonicalization costs (topology cleanup and bounds generation) for trusted assets.

Recent measurements:

- `mgi-trusted-fast` still has measurable TTFU overhead on large assets.
- Task 6 payload-copy floor is already very low:
  - RevitHouse (`24.8 MB`): `0.402 ms` (`61.7 GB/s`)
  - dragon (`5.0 MB`): `0.064 ms`
  - lucy (`1.4 MB`): `0.027 ms`

Conclusion: remaining gap is not dominated by raw copy bandwidth; it is mainly prep/pack/control-path overhead.

## Problem Statement
Should MGI support an optional runtime-ready packed payload representation to reduce residual activation overhead beyond the current trusted canonical path?

## Options
### Option A: Canonical Planar Streams Only
MGI stores canonical attribute/index streams only; runtime always builds packed payload.

Pros:

- highest format flexibility
- lowest renderer-layout lock-in
- simplest long-term schema evolution

Cons:

- runtime pack/setup cost remains on hot path
- cannot fully approach copy-floor behavior for large assets

### Option B: Partially Packed Attribute Streams
MGI stores canonical data plus partially packed streams (for example preconverted normals/tangents or selected stream grouping).

Pros:

- moderate runtime speedup potential
- lower lock-in than fully interleaved payload
- incremental migration path

Cons:

- adds schema/logic complexity
- may still retain nontrivial runtime assembly overhead
- mixed-path maintenance burden

### Option C: Final Interleaved Runtime Payload (Optional Chunk)
MGI stores an optional chunk containing runtime-ready vertex/index payload for a specific layout profile.

Pros:

- best runtime activation speed potential
- closest to measured copy/setup floor
- strongest large-asset wins under repeated activation

Cons:

- highest layout/shader lock-in risk
- higher file size potential
- requires clear compatibility + invalidation policy when layout profile changes

## Tradeoff Matrix
| Dimension | Option A: Canonical | Option B: Partial | Option C: Final Packed |
|---|---|---|---|
| Runtime speed | Medium | Medium-High | Highest |
| Flexibility | Highest | High | Lowest |
| Layout lock-in risk | Lowest | Medium | Highest |
| File size | Lowest | Medium | Medium-High |
| Implementation complexity | Low | Medium | Medium-High |
| Fallback simplicity | High | Medium | High (if optional) |
| Meshlet/GPU-driven future fit | High | High | High (with profile/versioning) |

## Recommendation
Near-term recommendation:

1. Keep canonical MGI as the baseline contract.
2. Add an **optional packed runtime payload chunk** (not mandatory).
3. Preserve trusted-canonical fallback path for all assets lacking packed chunk support.
4. Gate packed-path activation by explicit layout/profile compatibility checks.

This preserves flexibility while enabling a high-performance fast path where layout stability justifies it.

## Guardrails
### Compatibility and Versioning
- Treat packed payload as an optional extension chunk.
- Include explicit payload profile/version identifier in chunk metadata.
- Runtime must verify profile compatibility before using packed payload.
- On mismatch, fallback to canonical trusted path.

### Runtime Selection Policy
Use packed payload only when all are true:

- packed chunk present
- payload profile matches active runtime layout profile
- trust metadata indicates canonical/preprocessed asset

Else fallback:

- trusted canonical path when eligible
- safe path otherwise

### Scope Constraints
- Do not remove existing canonical/trusted paths.
- Do not make packed payload mandatory for all assets.
- Do not couple v1 MGI baseline to a single renderer forever.

## Decision Criteria For Implementation
Proceed with optional packed payload implementation when:

- large-asset residual TTFU remains materially above measured copy floor,
- runtime layout/profile is stable enough to version explicitly,
- file-size increase is acceptable for target content pipelines.

Defer if:

- layout profile remains volatile,
- fallback complexity outweighs expected runtime gain,
- content pipeline cannot reliably precompute payload variants.

## Suggested Next Validation Plan
Before implementation:

1. Quantify residual overhead (`mgi-trusted-fast` minus Task 6 copy floor) per fixture.
2. Define candidate runtime payload profile ID format.
3. Prototype a single optional packed chunk for one profile.
4. Benchmark packed-chunk path vs trusted-canonical path.
5. Decide whether gains justify broader rollout.

## Current Direction
Given current measurements, the pragmatic path is:

- maintain canonical MGI contract as default,
- add optional packed payload extension as a targeted acceleration path,
- keep robust fallback behavior to avoid brittle format lock-in.
