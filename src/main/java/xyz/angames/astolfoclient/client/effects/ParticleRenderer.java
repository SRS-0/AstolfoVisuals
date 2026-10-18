package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.awt.Color;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;

@Environment(EnvType.CLIENT)
public class ParticleRenderer {
   private static final Identifier BUBBLES_TEX = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/bubbles.png");
   private static final Identifier STARS_TEX = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/stars.png");
   private static final Identifier DOLLARS_TEX = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/dollars.png");
   private static final Identifier HEART_TEX = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/bloom.png");
   private static final Identifier SMOLESTAR_TEX = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/mini-star.png");
   private final ParticleManager manager;

   public ParticleRenderer(ParticleManager manager) {
      this.manager = manager;
   }

   public void render(WorldRenderContext context) {
      ParticlesModule module = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
      if (module != null && module.isEnabled()) {
         List<Particle> allParticles = this.manager.getParticles();
         if (!allParticles.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            Tesselator tessellator = Tesselator.getInstance();

            for (ParticlesModule.ParticleType type : ParticlesModule.ParticleType.values()) {
               boolean bound = false;
               BufferBuilder buffer = null;

               for (Particle particle : allParticles) {
                  if (particle.type == type) {
                     long age = currentTime - particle.creationTime;
                     if (age <= particle.lifespan) {
                        if (!bound) {
                           RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
                           RenderSystem.setShaderTexture(0, this.getTextureForType(type));
                           buffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                           bound = true;
                        }

                        float progress = (float)age / (float)particle.lifespan;
                        float alpha = progress > 0.6F ? 1.0F - (progress - 0.6F) / 0.4F : 1.0F;
                        Color particleColor = particle.color != null ? particle.color : new Color(ThemeManager.getThemedColor(particle.creationTime));
                        float r = particleColor.getRed() / 255.0F;
                        float g = particleColor.getGreen() / 255.0F;
                        float b = particleColor.getBlue() / 255.0F;
                        double x = Mth.lerp(tickDelta, particle.prevPosition.x, particle.position.x);
                        double y = Mth.lerp(tickDelta, particle.prevPosition.y, particle.position.y);
                        double z = Mth.lerp(tickDelta, particle.prevPosition.z, particle.position.z);
                        PoseStack matrixStack = context.matrixStack();
                        matrixStack.pushPose();
                        matrixStack.translate(
                           x - context.camera().position().x,
                           y - context.camera().position().y,
                           z - context.camera().position().z
                        );
                        matrixStack.rotate(context.camera().rotation());
                        float finalScale = particle.scale / 2.5F;
                        matrixStack.scale(finalScale, finalScale, finalScale);
                        matrixStack.rotate(Axis.ZP.rotationDegrees(particle.rotation + (float)age * 0.1F));
                        Matrix4f matrix = matrixStack.last().pose();
                        Vtx.vertex(buffer, matrix, -0.5F, -0.5F, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, alpha);
                        Vtx.vertex(buffer, matrix, 0.5F, -0.5F, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, alpha);
                        Vtx.vertex(buffer, matrix, 0.5F, 0.5F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, alpha);
                        Vtx.vertex(buffer, matrix, -0.5F, 0.5F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, alpha);
                        matrixStack.popPose();
                     }
                  }
               }

               if (bound && buffer != null) {
                  BufferUploader.drawWithShader(buffer.buildOrThrow());
               }
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }

   private Identifier getTextureForType(ParticlesModule.ParticleType type) {
      switch (type) {
         case BUBBLES:
            return BUBBLES_TEX;
         case DOLLARS:
            return DOLLARS_TEX;
         case HEART:
            return HEART_TEX;
         case SMOLESTAR:
            return SMOLESTAR_TEX;
         case STARS:
         default:
            return STARS_TEX;
      }
   }
}
