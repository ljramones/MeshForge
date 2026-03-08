# Runtime Geometry Regression Baseline

This baseline is used as a production guardrail for cold-load geometry performance.

## Environment

- Commit: `0606a24`
- JDK: `openjdk 25.0.1` (preview enabled)
- Flags: `--enable-preview --add-modules jdk.incubator.vector`
- Host: `Darwin arm64`
- Date: `2026-03-07`

## End-to-End Table

| Fixture | Old Total ms | New OBJ Total ms | Cache Total ms | Speedup (Old Total / Cache) |
|---|---:|---:|---:|---:|
| RevitHouse.obj | 80.997 | 81.538 | 3.995 | 20.27x |
| xyzrgb_dragon.obj | 18.290 | 17.929 | 1.243 | 14.71x |
| lucy.obj | 7.427 | 6.994 | 0.422 | 17.60x |

## Reproduction

```bash
mvn -q -pl meshforge-dynamisgpu,meshforge-demo -am test

mvn -q -pl meshforge-demo -DskipTests dependency:build-classpath \
  -Dmdep.includeScope=runtime \
  -Dmdep.outputFile=/tmp/mf_demo_cp_cache_guardrail.txt

CP="meshforge/target/classes:meshforge-loader/target/classes:meshforge-dynamisgpu/target/classes:meshforge-demo/target/classes:$(cat /tmp/mf_demo_cp_cache_guardrail.txt)"

java --enable-preview --add-modules jdk.incubator.vector \
  -cp "$CP" org.dynamisengine.meshforge.demo.CacheVsObjFixtureTiming --fixture=revithouse
java --enable-preview --add-modules jdk.incubator.vector \
  -cp "$CP" org.dynamisengine.meshforge.demo.CacheVsObjFixtureTiming --fixture=dragon
java --enable-preview --add-modules jdk.incubator.vector \
  -cp "$CP" org.dynamisengine.meshforge.demo.CacheVsObjFixtureTiming --fixture=lucy
```
