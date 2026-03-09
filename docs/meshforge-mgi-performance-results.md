# MeshForge MGI Performance Results

Date: 2026-03-08

## Context

Profiling showed full-path mesh activation was dominated by source-format load/parse. MGI was introduced as the canonical binary ingest boundary to remove source-ingest tax while preserving runtime preparation behavior.

## Benchmark Command

```bash
mvn -q -f meshforge-demo/pom.xml exec:java \
  -Dexec.mainClass=org.dynamisengine.meshforge.demo.PrepQueueTransferTtfuFixtureTiming \
  -Dexec.args="--mode=all --max-inflight=2 --warmup=2 --runs=9"
```

## Pipeline

```text
OBJ / glTF / other source
        ↓
meshforge-loader
        ↓
MGI
        ↓
MeshForge runtime prep (pipeline + pack)
        ↓
Packed mesh
        ↓
UploadManager
        ↓
GPU
```

## Median Comparison (Representative Fixtures)

| Fixture | Mode | Load/Clone ms | Pipeline ms | Plan ms | Pack ms | Bridge ms | Queue ms | Transfer ms | Total TTFU ms |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| `xyzrgb_dragon.obj` | full | 14.263 | 1.699 | 0.002 | 1.064 | 0.001 | 0.000 | 0.359 | 17.260 |
| `xyzrgb_dragon.obj` | mgi-full | 1.710 | 1.695 | 0.001 | 0.897 | 0.001 | 0.000 | 0.374 | 4.704 |
| `xyzrgb_dragon.obj` | runtime-only | 0.442 | 1.675 | 0.001 | 0.685 | 0.001 | 0.000 | 0.143 | 2.939 |
| `lucy.obj` | full | 5.808 | 0.659 | 0.004 | 0.623 | 0.003 | 0.001 | 0.135 | 7.219 |
| `lucy.obj` | mgi-full | 0.699 | 0.632 | 0.004 | 0.552 | 0.003 | 0.001 | 0.122 | 2.042 |
| `lucy.obj` | runtime-only | 0.295 | 0.626 | 0.004 | 0.491 | 0.003 | 0.001 | 0.124 | 1.549 |
| `RevitHouse.obj` | full | 69.287 | 7.663 | 0.008 | 4.877 | 0.008 | 0.004 | 1.884 | 83.472 |
| `RevitHouse.obj` | mgi-full | 6.081 | 6.710 | 0.008 | 3.541 | 0.006 | 0.003 | 0.689 | 17.085 |
| `RevitHouse.obj` | runtime-only | 2.911 | 6.126 | 0.005 | 4.512 | 0.004 | 0.002 | 1.822 | 15.312 |

## Interpretation

- `mgi-full` reduces load/clone cost by about 88-91% versus `full`.
- Total TTFU improves by about 72-80% versus `full`.
- `mgi-full` approaches the runtime-only lower bound.
- Queue/transfer/bridge remain small; dominant cost after MGI is now pipeline + pack.

## Architectural Conclusion

MGI successfully removes the source-ingest tax from mesh activation. The canonical runtime ingest boundary should be MGI, not source formats.

Future optimization focus should move to MeshForge runtime preparation:

- pipeline execution
- packing
- geometry representation upgrades (meshlets/clusters)

## Next Steps

1. Continue runtime prep profiling with MGI as the default ingest path.
2. Optimize pipeline + pack hot paths where measurement justifies it.
3. Prototype meshlets as the next representation-level improvement.
4. Add MGI tooling (`mgi-info`, `mgi-validate`) after core path stabilization.
