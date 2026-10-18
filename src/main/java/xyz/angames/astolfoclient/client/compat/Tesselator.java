package xyz.angames.astolfoclient.client.compat;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.renderpearl.api.vertex.VertexFormat;

/** Legacy Tesselator: a shared immediate-mode buffer. */
public final class Tesselator {
   private static final Tesselator INSTANCE = new Tesselator();
   private final ByteBufferBuilder buffer = new ByteBufferBuilder(786432);

   public static Tesselator getInstance() {
      return INSTANCE;
   }

   public BufferBuilder begin(PrimitiveTopology topology, VertexFormat format) {
      return new BufferBuilder(this.buffer, topology, format);
   }
}
