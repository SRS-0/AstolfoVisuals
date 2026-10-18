package xyz.angames.astolfoclient.client.compat;

import xyz.angames.astolfoclient.client.compat.Vtx;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/** Replacement for the removed {@code Vtx.vertex(VertexConsumer, Matrix4f, x, y, z)} overload. */
public final class Vtx {
   private Vtx() {
   }

   public static VertexConsumer vertex(VertexConsumer consumer, Matrix4fc m, float x, float y, float z) {
      Vector3f v = m.transformPosition(x, y, z, new Vector3f());
      return consumer.addVertex(v.x, v.y, v.z);
   }
}
