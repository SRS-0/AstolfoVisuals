package xyz.angames.astolfoclient.client.render.models;

import xyz.angames.astolfoclient.client.compat.Vtx;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class CowModel {
   private static final Identifier MODEL_LOCATION = Identifier.fromNamespaceAndPath("astolfoclient", "models/cow_mesh.json");
   private static final Identifier TEXTURE_LOCATION = Identifier.fromNamespaceAndPath("astolfoclient", "textures/models/cow.png");
   private static final float MODEL_SCALE = 0.48F;
   private final List<CowModel.MeshTriangle> triangles = new ArrayList<>();
   private boolean loaded;
   private boolean failed;
   private float centerX;
   private float centerZ;
   private float minY;
   private float bodyPivotY;

   public void render(PoseStack matrixStack, SubmitNodeCollector collector, AvatarRenderState state, int light) {
      this.ensureLoaded();
      if (!this.failed && !this.triangles.isEmpty()) {
         float ageInTicks = state.ageInTicks;
         float walkAmount = Math.min(state.walkAnimationSpeed, 1.0F);
         float bob = (float)Math.sin(state.walkAnimationPos * 0.6662F) * walkAmount * 0.035F + (float)Math.sin(ageInTicks * 0.08F) * 0.01F;
         float sway = (float)Math.sin(ageInTicks * 0.05F) * 0.9F;
         float pitch = -2.0F + walkAmount * 5.5F + (float)Math.cos(ageInTicks * 0.07F) * 0.6F;
         matrixStack.pushPose();
         matrixStack.translate(0.0, bob, 0.0);
         matrixStack.rotate(Axis.YP.rotationDegrees(state.bodyRot));
         matrixStack.scale(0.48F, 0.48F, 0.48F);
         matrixStack.translate(this.centerX, this.bodyPivotY, this.centerZ);
         matrixStack.rotate(Axis.ZP.rotationDegrees(sway));
         matrixStack.rotate(Axis.XP.rotationDegrees(pitch));
         matrixStack.translate(-this.centerX, -this.bodyPivotY, -this.centerZ);
         matrixStack.translate(-this.centerX, -this.minY, -this.centerZ);
         collector.submitCustomGeometry(matrixStack, RenderTypes.entityTranslucent(TEXTURE_LOCATION), (pose, buffer) -> this.renderTriangles(pose.pose(), buffer, light));
         matrixStack.popPose();
      }
   }

   private void ensureLoaded() {
      if (!this.loaded && !this.failed) {
         try (
            InputStream stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(MODEL_LOCATION).open();
            InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
         ) {
            this.parse(JsonParser.parseReader(reader).getAsJsonObject());
            this.loaded = true;
         } catch (Exception e) {
            this.failed = true;
            System.err.println("Failed to load cow pet model from " + MODEL_LOCATION);
            e.printStackTrace();
         }
      }
   }

   private void parse(JsonObject root) {
      this.triangles.clear();
      JsonObject bounds = root.getAsJsonObject("bounds");
      JsonArray min = bounds.getAsJsonArray("min");
      JsonArray max = bounds.getAsJsonArray("max");
      float minX = min.get(0).getAsFloat();
      this.minY = min.get(1).getAsFloat();
      float minZ = min.get(2).getAsFloat();
      float maxX = max.get(0).getAsFloat();
      float maxY = max.get(1).getAsFloat();
      float maxZ = max.get(2).getAsFloat();
      this.centerX = (minX + maxX) * 0.5F;
      this.centerZ = (minZ + maxZ) * 0.5F;
      this.bodyPivotY = this.minY + (maxY - this.minY) * 0.52F;
      JsonArray trianglesArray = root.getAsJsonArray("triangles");

      for (int i = 0; i < trianglesArray.size(); i++) {
         JsonArray triangleArray = trianglesArray.get(i).getAsJsonArray();
         if (triangleArray.size() >= 3) {
            this.triangles
               .add(
                  new CowModel.MeshTriangle(
                     this.readVertex(triangleArray.get(0).getAsJsonArray()),
                     this.readVertex(triangleArray.get(1).getAsJsonArray()),
                     this.readVertex(triangleArray.get(2).getAsJsonArray())
                  )
               );
         }
      }
   }

   private CowModel.MeshVertex readVertex(JsonArray vertexArray) {
      return new CowModel.MeshVertex(
         vertexArray.get(0).getAsFloat(),
         vertexArray.get(1).getAsFloat(),
         vertexArray.get(2).getAsFloat(),
         vertexArray.get(3).getAsFloat(),
         vertexArray.get(4).getAsFloat()
      );
   }

   private void renderTriangles(Matrix4f matrix, VertexConsumer buffer, int light) {

      for (CowModel.MeshTriangle triangle : this.triangles) {
         this.putVertex(buffer, matrix, triangle.a, light);
         this.putVertex(buffer, matrix, triangle.b, light);
         this.putVertex(buffer, matrix, triangle.c, light);
      }
   }

   private void putVertex(VertexConsumer buffer, Matrix4f matrix, CowModel.MeshVertex vertex, int light) {
      Vtx.vertex(buffer, matrix, vertex.x, vertex.y, vertex.z)
         .setColor(255, 255, 255, 255)
         .setUv(vertex.u, vertex.v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(0.0F, 1.0F, 0.0F);
   }

   @Environment(EnvType.CLIENT)
   private static final class MeshTriangle {
      private final CowModel.MeshVertex a;
      private final CowModel.MeshVertex b;
      private final CowModel.MeshVertex c;

      private MeshTriangle(CowModel.MeshVertex a, CowModel.MeshVertex b, CowModel.MeshVertex c) {
         this.a = a;
         this.b = b;
         this.c = c;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class MeshVertex {
      private final float x;
      private final float y;
      private final float z;
      private final float u;
      private final float v;

      private MeshVertex(float x, float y, float z, float u, float v) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.u = u;
         this.v = v;
      }
   }
}
