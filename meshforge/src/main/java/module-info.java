module org.dynamisengine.meshforge {
    requires org.dynamisengine.vectrix;
    requires jdk.incubator.vector;
    requires java.logging;

    exports org.dynamisengine.meshforge.api;
    exports org.dynamisengine.meshforge.core.attr;
    exports org.dynamisengine.meshforge.core.bounds;
    exports org.dynamisengine.meshforge.core.builder;
    exports org.dynamisengine.meshforge.core.mesh;
    exports org.dynamisengine.meshforge.core.topology;
    exports org.dynamisengine.meshforge.ops.compress.gpu;
    exports org.dynamisengine.meshforge.ops.cull;
    exports org.dynamisengine.meshforge.ops.cull.gpu;
    exports org.dynamisengine.meshforge.ops.generate;
    exports org.dynamisengine.meshforge.ops.lod;
    exports org.dynamisengine.meshforge.ops.lod.gpu;
    exports org.dynamisengine.meshforge.ops.modify;
    exports org.dynamisengine.meshforge.ops.optimize;
    exports org.dynamisengine.meshforge.ops.pipeline;
    exports org.dynamisengine.meshforge.ops.raytrace;
    exports org.dynamisengine.meshforge.ops.raytrace.gpu;
    exports org.dynamisengine.meshforge.ops.streaming;
    exports org.dynamisengine.meshforge.ops.streaming.gpu;
    exports org.dynamisengine.meshforge.ops.tessellation;
    exports org.dynamisengine.meshforge.ops.tessellation.gpu;
    exports org.dynamisengine.meshforge.ops.weld;
    exports org.dynamisengine.meshforge.pack.buffer;
    exports org.dynamisengine.meshforge.pack.layout;
    exports org.dynamisengine.meshforge.pack.packer;
    exports org.dynamisengine.meshforge.pack.simd;
    exports org.dynamisengine.meshforge.pack.spec;
}
