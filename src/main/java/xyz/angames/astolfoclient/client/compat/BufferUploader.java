package xyz.angames.astolfoclient.client.compat;

import com.mojang.blaze3d.vertex.MeshData;

/** Legacy BufferUploader, drawing through {@link RenderSystem}'s emulated state. */
public final class BufferUploader {
   private BufferUploader() {
   }

   public static void drawWithShader(MeshData mesh) {
      RenderSystem.draw(mesh);
   }

   public static void draw(MeshData mesh) {
      RenderSystem.draw(mesh);
   }
}
