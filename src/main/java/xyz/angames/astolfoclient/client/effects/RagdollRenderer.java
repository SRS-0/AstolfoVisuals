package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import xyz.angames.astolfoclient.client.compat.ShaderProgram;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(EnvType.CLIENT)
public class RagdollRenderer {
   private final List<RagdollRenderer.Ragdoll> ragdolls = new ArrayList<>();
   private ClientLevel lastWorld = null;

   /**
    * The 1.21.4 ragdoll captured the entity's immediate-mode vertices. 26.x entities are submitted to a node collector
    * and drawn later, so there is nothing to snapshot yet; ragdolls are currently not spawned.
    */
   public void addRagdoll(LivingEntity entity) {
   }

   public static Vec3 rotateAroundAxis(Vec3 point, Vec3 axis, double angle) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      double dot = point.x * axis.x + point.y * axis.y + point.z * axis.z;
      double crossX = axis.y * point.z - axis.z * point.y;
      double crossY = axis.z * point.x - axis.x * point.z;
      double crossZ = axis.x * point.y - axis.y * point.x;
      double rx = point.x * cos + crossX * sin + axis.x * dot * (1.0 - cos);
      double ry = point.y * cos + crossY * sin + axis.y * dot * (1.0 - cos);
      double rz = point.z * cos + crossZ * sin + axis.z * dot * (1.0 - cos);
      return new Vec3(rx, ry, rz);
   }

   public void render(WorldRenderContext context) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         synchronized (this.ragdolls) {
            this.ragdolls.clear();
         }

         this.lastWorld = null;
      } else {
         if (mc.level != this.lastWorld) {
            synchronized (this.ragdolls) {
               this.ragdolls.clear();
            }

            this.lastWorld = mc.level;
         }

         Module ragdollModule = AstolfoclientClient.moduleManager.getModuleByName("Ragdoll");
         if (ragdollModule != null && ragdollModule.isEnabled()) {
            long now = System.currentTimeMillis();
            List<RagdollRenderer.Ragdoll> active = new ArrayList<>();
            synchronized (this.ragdolls) {
               Iterator<RagdollRenderer.Ragdoll> it = this.ragdolls.iterator();

               while (it.hasNext()) {
                  RagdollRenderer.Ragdoll r = it.next();
                  if (now - r.spawnTime > 1500L) {
                     it.remove();
                  } else {
                     active.add(r);
                  }
               }
            }

            if (!active.isEmpty()) {
               Vec3 cameraPos = context.camera().position();
               PoseStack matrices = context.matrixStack();
               Tesselator tessellator = Tesselator.getInstance();
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.enableCull();
               matrices.pushPose();
               matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
               BufferBuilder[] bufferHolder = new BufferBuilder[]{tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)};
               Matrix4f m = matrices.last().pose();

               for (RagdollRenderer.Ragdoll ragdoll : active) {
                  long age = now - ragdoll.spawnTime;
                  float explosionProgress = 0.0F;
                  float explosionAgeSecs = 0.0F;
                  if (age >= 800L) {
                     explosionProgress = (float)(age - 800L) / 700.0F;
                     explosionAgeSecs = (float)(age - 800L) / 1000.0F;
                  }

                  if (explosionProgress > 1.0F) {
                     explosionProgress = 1.0F;
                  }

                  float progress = (float)age / 1500.0F;
                  float alphaVal = 0.5F * (1.0F - progress);

                  for (RagdollRenderer.Ragdoll.Shard shard : ragdoll.shards) {
                     for (int j = 0; j < 4; j++) {
                        RagdollRenderer.SnapshotVertexConsumer.CapturedVertex v = shard.vertices[j];
                        Vec3 distortionOffset = shard.vertexDistortions[j].scale(explosionProgress);
                        double vxLocal = v.x + distortionOffset.x;
                        double vyLocal = v.y + distortionOffset.y;
                        double vzLocal = v.z + distortionOffset.z;
                        Vec3 localToCenter = new Vec3(
                           vxLocal - shard.center.x, vyLocal - shard.center.y, vzLocal - shard.center.z
                        );
                        Vec3 rotated = rotateAroundAxis(localToCenter, shard.rotationAxis, shard.rotationSpeed * explosionAgeSecs);
                        Vec3 rotatedScaled = rotated.scale(1.0 - explosionProgress);
                        Vec3 finalLocalPos = shard.center.add(rotatedScaled).add(shard.velocity.scale(explosionAgeSecs));
                        double vx = ragdoll.x + finalLocalPos.x;
                        double vy = ragdoll.y + finalLocalPos.y;
                        double vz = ragdoll.z + finalLocalPos.z;
                        Vtx.vertex(bufferHolder[0], m, (float)vx, (float)vy, (float)vz).setUv(v.u, v.v).setColor(v.r, v.g, v.b, alphaVal);
                     }
                  }

                  ShaderProgram shader = RenderSystem.setShader(AstolfoclientClient.COSMOS_FILL_SHADER);
                  if (shader != null) {
                     float timeSecs = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
                     if (shader.getUniform("uTime") != null) {
                        shader.getUniform("uTime").set(timeSecs);
                     }

                     if (shader.getUniform("uResolution") != null) {
                        shader.getUniform("uResolution").set(1.0F, 1.0F);
                     }

                     int color1 = ThemeManager.getThemedColor(0L);
                     float r1 = (color1 >> 16 & 0xFF) / 255.0F;
                     float g1 = (color1 >> 8 & 0xFF) / 255.0F;
                     float b1 = (color1 & 0xFF) / 255.0F;
                     if (shader.getUniform("uColor1") != null) {
                        shader.getUniform("uColor1").set(r1, g1, b1);
                     }

                     if (shader.getUniform("uColor2") != null) {
                        shader.getUniform("uColor2").set(0.0F, 0.0F, 0.0F);
                     }

                     if (shader.getUniform("uBlockCenter") != null) {
                        shader.getUniform("uBlockCenter").set((float)ragdoll.x, (float)ragdoll.y, (float)ragdoll.z);
                     }

                     if (shader.getUniform("ColorModulator") != null) {
                        shader.getUniform("ColorModulator").set(1.0F, 1.0F, 1.0F, 1.0F);
                     }

                     if (shader.getUniform("uAlphaMode") != null) {
                        shader.getUniform("uAlphaMode").set(1.0F);
                     }
                  }

                  try {
                     BufferUploader.drawWithShader(bufferHolder[0].buildOrThrow());
                  } catch (Exception var42) {
                  }

                  if (ragdoll != active.get(active.size() - 1)) {
                     bufferHolder[0] = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                  }
               }

               matrices.popPose();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.enableCull();
               RenderSystem.disableBlend();
            }
         } else {
            synchronized (this.ragdolls) {
               this.ragdolls.clear();
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static class Ragdoll {
      public final List<RagdollRenderer.Ragdoll.Shard> shards = new ArrayList<>();
      public final double x;
      public final double y;
      public final double z;
      public final long spawnTime;
      public static final long LIFESPAN = 1500L;

      public Ragdoll(List<RagdollRenderer.SnapshotVertexConsumer.CapturedVertex> capturedVertices, double x, double y, double z, float entityHeight) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.spawnTime = System.currentTimeMillis();
         Vec3 entityCenter = new Vec3(0.0, entityHeight / 2.0, 0.0);

         for (int i = 0; i + 3 < capturedVertices.size(); i += 4) {
            RagdollRenderer.SnapshotVertexConsumer.CapturedVertex v0 = capturedVertices.get(i);
            RagdollRenderer.SnapshotVertexConsumer.CapturedVertex v1 = capturedVertices.get(i + 1);
            RagdollRenderer.SnapshotVertexConsumer.CapturedVertex v2 = capturedVertices.get(i + 2);
            RagdollRenderer.SnapshotVertexConsumer.CapturedVertex v3 = capturedVertices.get(i + 3);
            this.shards.add(new RagdollRenderer.Ragdoll.Shard(v0, v1, v2, v2, entityCenter));
            this.shards.add(new RagdollRenderer.Ragdoll.Shard(v0, v2, v3, v3, entityCenter));
         }
      }

      @Environment(EnvType.CLIENT)
      public static class Shard {
         public final RagdollRenderer.SnapshotVertexConsumer.CapturedVertex[] vertices = new RagdollRenderer.SnapshotVertexConsumer.CapturedVertex[4];
         public final Vec3[] vertexDistortions = new Vec3[4];
         public final Vec3 center;
         public final Vec3 velocity;
         public final Vec3 rotationAxis;
         public final float rotationSpeed;

         public Shard(
            RagdollRenderer.SnapshotVertexConsumer.CapturedVertex v0,
            RagdollRenderer.SnapshotVertexConsumer.CapturedVertex v1,
            RagdollRenderer.SnapshotVertexConsumer.CapturedVertex v2,
            RagdollRenderer.SnapshotVertexConsumer.CapturedVertex v3,
            Vec3 entityCenter
         ) {
            this.vertices[0] = v0;
            this.vertices[1] = v1;
            this.vertices[2] = v2;
            this.vertices[3] = v3;
            double cx = (v0.x + v1.x + v2.x + v3.x) / 4.0;
            double cy = (v0.y + v1.y + v2.y + v3.y) / 4.0;
            double cz = (v0.z + v1.z + v2.z + v3.z) / 4.0;
            this.center = new Vec3(cx, cy, cz);

            for (int i = 0; i < 4; i++) {
               this.vertexDistortions[i] = new Vec3((Math.random() - 0.5) * 0.25, (Math.random() - 0.5) * 0.25, (Math.random() - 0.5) * 0.25);
            }

            double dx = cx - entityCenter.x;
            double dy = cy - entityCenter.y;
            double dz = cz - entityCenter.z;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.01) {
               dx = Math.random() - 0.5;
               dy = Math.random() - 0.5;
               dz = Math.random() - 0.5;
               len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            }

            double speed = 0.4 + Math.random() * 0.8;
            this.velocity = new Vec3(
               dx / len * speed + (Math.random() - 0.5) * 0.2, dy / len * speed + Math.random() * 0.5 + 0.3, dz / len * speed + (Math.random() - 0.5) * 0.2
            );
            this.rotationAxis = new Vec3(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize();
            this.rotationSpeed = (float)(Math.random() * 8.0 + 3.0);
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static class SnapshotVertexConsumer implements VertexConsumer {
      @Override
      public VertexConsumer setUv3(float u, float v) {
         return this;
      }

      @Override
      public VertexConsumer setLineWidth(float width) {
         return this;
      }

      public final List<RagdollRenderer.SnapshotVertexConsumer.CapturedVertex> vertices = new ArrayList<>();
      private RagdollRenderer.SnapshotVertexConsumer.CapturedVertex currentVertex = new RagdollRenderer.SnapshotVertexConsumer.CapturedVertex();
      private boolean hasVertex = false;

      public VertexConsumer addVertex(float x, float y, float z) {
         if (this.hasVertex) {
            this.vertices.add(this.currentVertex);
         }

         this.currentVertex = new RagdollRenderer.SnapshotVertexConsumer.CapturedVertex();
         this.currentVertex.x = x;
         this.currentVertex.y = y;
         this.currentVertex.z = z;
         this.hasVertex = true;
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         this.currentVertex.r = r / 255.0F;
         this.currentVertex.g = g / 255.0F;
         this.currentVertex.b = b / 255.0F;
         this.currentVertex.a = a / 255.0F;
         return this;
      }

      public VertexConsumer setColor(int argb) {
         return this.setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF, argb & 0xFF, argb >>> 24);
      }

      public VertexConsumer setColor(float r, float g, float b, float a) {
         this.currentVertex.r = r;
         this.currentVertex.g = g;
         this.currentVertex.b = b;
         this.currentVertex.a = a;
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.currentVertex.u = u;
         this.currentVertex.v = v;
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         return this;
      }

      public void commitLast() {
         if (this.hasVertex) {
            this.vertices.add(this.currentVertex);
            this.hasVertex = false;
         }
      }

      @Environment(EnvType.CLIENT)
      public static class CapturedVertex {
         public float x;
         public float y;
         public float z;
         public float u;
         public float v;
         public float r = 1.0F;
         public float g = 1.0F;
         public float b = 1.0F;
         public float a = 1.0F;
      }
   }
}
