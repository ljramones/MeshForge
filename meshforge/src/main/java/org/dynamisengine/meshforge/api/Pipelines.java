package org.dynamisengine.meshforge.api;

import org.dynamisengine.meshforge.core.attr.AttributeSemantic;
import org.dynamisengine.meshforge.core.mesh.MeshData;
import org.dynamisengine.meshforge.ops.pipeline.MeshContext;
import org.dynamisengine.meshforge.ops.pipeline.MeshOp;
import org.dynamisengine.meshforge.ops.pipeline.MeshPipeline;
import org.dynamisengine.meshforge.pack.packer.MeshPacker;
import org.dynamisengine.meshforge.pack.spec.PackSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Preset mesh-processing pipelines.
 */
public final class Pipelines {
    private static final MeshOp VALIDATE = Ops.validate();
    private static final MeshOp REMOVE_DEGENERATES = Ops.removeDegenerates();
    private static final MeshOp BOUNDS = Ops.bounds();

    private Pipelines() {
    }

    /**
     * Precomputed runtime plan for repeated realtime-fast execution.
     * This caches operation selection/config so repeated create calls avoid per-call decision work.
     */
    public static final class RuntimeRealtimePlan {
        private final MeshOp[] ops;
        private final PackSpec packSpec;

        private RuntimeRealtimePlan(MeshOp[] ops, PackSpec packSpec) {
            this.ops = ops;
            this.packSpec = packSpec;
        }
    }

    /**
     * Reusable workspace for runtime planned realtime execution.
     */
    public static final class RuntimeRealtimeWorkspace {
        private final MeshPacker.RuntimePackWorkspace packWorkspace = new MeshPacker.RuntimePackWorkspace();
    }

    /**
     * Full realtime prep pipeline.
     * Includes weld and cache optimization for import-time quality/perf.
     */
    public static MeshData realtime(MeshData mesh) {
        return MeshPipeline.run(mesh, realtimeOps(mesh));
    }

    /**
     * Fast realtime prep pipeline.
     * For already-clean meshes: skips weld and cache optimization.
     */
    public static MeshData realtimeFast(MeshData mesh) {
        return MeshPipeline.run(mesh, realtimeFastOps(mesh));
    }

    /**
     * Returns realtimeOps.
     * @param mesh parameter value
     * @return resulting value
     */
    public static MeshOp[] realtimeOps(MeshData mesh) {
        List<MeshOp> ops = new ArrayList<>();
        ops.add(Ops.validate());
        ops.add(Ops.removeDegenerates());
        ops.add(Ops.weld(1.0e-6f));
        if (!mesh.has(AttributeSemantic.NORMAL, 0)) {
            ops.add(Ops.normals(60f));
        }
        if (mesh.has(AttributeSemantic.UV, 0) && !mesh.has(AttributeSemantic.TANGENT, 0)) {
            ops.add(Ops.tangents());
        }
        ops.add(Ops.optimizeVertexCache());
        ops.add(Ops.bounds());
        return ops.toArray(MeshOp[]::new);
    }

    /**
     * Returns realtimeFastOps.
     * @param mesh parameter value
     * @return resulting value
     */
    public static MeshOp[] realtimeFastOps(MeshData mesh) {
        List<MeshOp> ops = new ArrayList<>();
        ops.add(VALIDATE);
        ops.add(REMOVE_DEGENERATES);
        if (!mesh.has(AttributeSemantic.NORMAL, 0)) {
            // Runtime-focused fast path: use smooth normals without threshold adjacency build.
            ops.add(Ops.normals(180f));
        }
        if (mesh.has(AttributeSemantic.UV, 0) && !mesh.has(AttributeSemantic.TANGENT, 0)) {
            ops.add(Ops.tangents());
        }
        ops.add(BOUNDS);
        return ops.toArray(MeshOp[]::new);
    }

    /**
     * Builds a precomputed realtime-fast runtime plan for one mesh schema/feature shape.
     *
     * @param mesh representative mesh for operation selection
     * @param packSpec pack spec for runtime pack step
     * @return runtime plan
     */
    public static RuntimeRealtimePlan buildRealtimeFastPlan(MeshData mesh, PackSpec packSpec) {
        if (mesh == null) {
            throw new NullPointerException("mesh");
        }
        if (packSpec == null) {
            throw new NullPointerException("packSpec");
        }
        return new RuntimeRealtimePlan(realtimeFastOps(mesh), packSpec);
    }

    /**
     * Executes a precomputed realtime-fast plan and writes packed payload into workspace.
     *
     * @param mesh source mesh
     * @param plan precomputed runtime plan
     * @param workspace reusable destination workspace
     */
    public static void executeRealtimeFastPlanInto(
        MeshData mesh,
        RuntimeRealtimePlan plan,
        RuntimeRealtimeWorkspace workspace
    ) {
        if (mesh == null) {
            throw new NullPointerException("mesh");
        }
        if (plan == null) {
            throw new NullPointerException("plan");
        }
        if (workspace == null) {
            throw new NullPointerException("workspace");
        }
        MeshData processed = runPlan(mesh, plan.ops);
        MeshPacker.packInto(processed, plan.packSpec, workspace.packWorkspace);
    }

    private static MeshData runPlan(MeshData mesh, MeshOp[] ops) {
        MeshContext context = new MeshContext();
        MeshData current = mesh;
        for (MeshOp op : ops) {
            current = op.apply(current, context);
        }
        return current;
    }
}
