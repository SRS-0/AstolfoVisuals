package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.vertex.VertexFormat;

/** Scratch mesh storage for extracted geometry; MeshData owns each allocation until closed. */
public final class Tesselator {
    private static final ThreadLocal<Tesselator> INSTANCE=ThreadLocal.withInitial(Tesselator::new);
    private final ByteBufferBuilder buffer=new ByteBufferBuilder(786432);
    private Tesselator() {}
    public static Tesselator getInstance(){return INSTANCE.get();}
    public BufferBuilder begin(PrimitiveTopology topology,VertexFormat format){return new BufferBuilder(buffer,topology,format);}
}
