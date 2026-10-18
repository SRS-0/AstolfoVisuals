package xyz.angames.astolfoclient.client.module.modules.render;

import xyz.angames.astolfoclient.client.compat.GlCompat;
import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents.BeforeEntities;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents.Last;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.pipeline.RenderTarget;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import xyz.angames.astolfoclient.client.compat.GL20;
import org.lwjgl.opengl.GL30;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(EnvType.CLIENT)
public class HitGlowModule extends Module {
   public static HitGlowModule INSTANCE;
   public final NumberSetting radius = new NumberSetting("Radius", 22.0, 5.0, 40.0, 1.0);
   public final NumberSetting glowIntensity = new NumberSetting("Glow Intensity", 1.2, 0.2, 3.0, 0.1);
   public final NumberSetting speed = new NumberSetting("Speed", 1.0, 0.2, 3.0, 0.1);
   public final NumberSetting ringWidth = new NumberSetting("Ring Width", 1.6, 0.5, 4.0, 0.1);
   public final BooleanSetting distortion = new BooleanSetting("Distortion Wave", true);
   public final NumberSetting distortionStrength = new NumberSetting("Distortion Strength", 1.0, 0.1, 3.0, 0.1) {
      @Override
      public boolean isVisible() {
         return HitGlowModule.this.distortion.get();
      }
   };
   public final BooleanSetting distortEntities = new BooleanSetting("Distort Entities", false) {
      @Override
      public boolean isVisible() {
         return HitGlowModule.this.distortion.get();
      }
   };
   public final BooleanSetting chromatic = new BooleanSetting("Chromatic", true) {
      @Override
      public boolean isVisible() {
         return HitGlowModule.this.distortion.get();
      }
   };
   public final BooleanSetting blockCorners = new BooleanSetting("Block Corners Glow", true);
   public final NumberSetting cornerGlow = new NumberSetting("Corner Glow", 2.2, 0.2, 5.0, 0.1) {
      @Override
      public boolean isVisible() {
         return HitGlowModule.this.blockCorners.get();
      }
   };
   public final NumberSetting cornerSize = new NumberSetting("Corner Size", 0.18, 0.04, 0.45, 0.01) {
      @Override
      public boolean isVisible() {
         return HitGlowModule.this.blockCorners.get();
      }
   };
   public final BooleanSetting edgeLines = new BooleanSetting("Edge Lines", false) {
      @Override
      public boolean isVisible() {
         return HitGlowModule.this.blockCorners.get();
      }
   };
   public final NumberSetting edgeWidth = new NumberSetting("Edge Width", 0.035, 0.01, 0.1, 0.005) {
      @Override
      public boolean isVisible() {
         return HitGlowModule.this.blockCorners.get() && HitGlowModule.this.edgeLines.get();
      }
   };
   public final BooleanSetting particles = new BooleanSetting("Particles", true);
   public final ModeSetting particleMode = new ModeSetting("Particle Mode", "Soul Wisps", "Soul Wisps", "Vanilla Souls", "Both", "Glow Dots") {
      @Override
      public boolean isVisible() {
         return HitGlowModule.this.particles.get();
      }
   };
   public final NumberSetting particleDensity = new NumberSetting("Particle Density", 1.0, 0.2, 3.0, 0.1) {
      @Override
      public boolean isVisible() {
         return HitGlowModule.this.particles.get();
      }
   };
   private static final Identifier BLOOM_TEXTURE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/bloom.png");
   private final Minecraft mc = Minecraft.getInstance();
   private final List<HitGlowModule.GlowWave> activeWaves = new CopyOnWriteArrayList<>();
   private final List<HitGlowModule.GlowParticle> activeParticles = new CopyOnWriteArrayList<>();
   private RenderTarget glowFbo = null;
   private int glowProgram = -1;
   private int glowVao = -1;
   private int glowVbo = -1;

   public HitGlowModule() {
      super("HitGlow", Module.Category.RENDER);
      INSTANCE = this;
      this.addSettings(
         this.radius,
         this.glowIntensity,
         this.speed,
         this.ringWidth,
         this.distortion,
         this.distortionStrength,
         this.distortEntities,
         this.chromatic,
         this.blockCorners,
         this.cornerGlow,
         this.cornerSize,
         this.edgeLines,
         this.edgeWidth,
         this.particles,
         this.particleMode,
         this.particleDensity
      );
      AttackEntityCallback.EVENT.register((AttackEntityCallback)(player, world, hand, entity, hitResult) -> {
         if (this.isEnabled() && player == this.mc.player && entity != null) {
            addWave(entity.position());
         }

         return InteractionResult.PASS;
      });
      WorldRenderEvents.BEFORE_ENTITIES.register((BeforeEntities)context -> {
         if (this.isEnabled() && !this.distortEntities.get()) {
            this.renderWorldPass(context);
         }
      });
      WorldRenderEvents.LAST.register((Last)context -> {
         if (this.isEnabled()) {
            if (this.distortEntities.get()) {
               this.renderWorldPass(context);
            }

            this.renderParticlesPass(context);
         }
      });
   }

   public static void addWave(Vec3 pos) {
      if (INSTANCE != null && INSTANCE.isEnabled() && pos != null) {
         INSTANCE.activeWaves.add(new HitGlowModule.GlowWave(pos));
         INSTANCE.spawnWaveParticles(pos);
      }
   }

   @Override
   public void onEnable() {
      this.activeWaves.clear();
      this.activeParticles.clear();
   }

   @Override
   public void onDisable() {
      this.activeWaves.clear();
      this.activeParticles.clear();
   }

   private void spawnWaveParticles(Vec3 center) {
      if (this.particles.get()) {
         int rgb = ThemeManager.getThemedColor(0L);
         float r = (rgb >> 16 & 0xFF) / 255.0F;
         float g = (rgb >> 8 & 0xFF) / 255.0F;
         float b = (rgb & 0xFF) / 255.0F;
         int count = (int)(14.0 * this.particleDensity.get());
         boolean isGlowDot = this.particleMode.is("Glow Dots");

         for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double dist = Math.random() * 1.8;
            double px = center.x + Math.cos(angle) * dist;
            double py = center.y + 0.1 + Math.random() * 0.8;
            double pz = center.z + Math.sin(angle) * dist;
            double driftX = Math.cos(angle) * (0.2 + Math.random() * 0.3);
            double driftZ = Math.sin(angle) * (0.2 + Math.random() * 0.3);
            float scale = isGlowDot ? 0.09F + (float)Math.random() * 0.06F : 0.16F + (float)Math.random() * 0.12F;
            long maxAge = isGlowDot ? 600L + (long)(Math.random() * 500.0) : 1100L + (long)(Math.random() * 800.0);
            if (!this.particleMode.is("Vanilla Souls")) {
               this.activeParticles.add(new HitGlowModule.GlowParticle(px, py, pz, driftX, driftZ, r, g, b, scale, maxAge));
            }

            if ((this.particleMode.is("Vanilla Souls") || this.particleMode.is("Both")) && this.mc.level != null && Math.random() < 0.4) {
               this.mc
                  .level
                  .addParticle(
                     Math.random() < 0.5 ? ParticleTypes.SOUL : ParticleTypes.SCULK_SOUL, px, py, pz, driftX * 0.5, 0.05 + Math.random() * 0.04, driftZ * 0.5
                  );
            }
         }
      }
   }

   private void renderWorldPass(WorldRenderContext context) {
      if (this.isEnabled() && this.mc.level != null && this.mc.player != null) {
         long now = System.currentTimeMillis();
         double currentSpeed = this.speed.get();
         this.activeWaves.removeIf(wave -> (now - wave.startTime) * currentSpeed > 1400.0);
         if (!this.activeWaves.isEmpty()) {
            this.renderGlowShader(context, now, currentSpeed);
         }
      }
   }

   private void renderParticlesPass(WorldRenderContext context) {
      if (this.isEnabled() && this.mc.level != null && this.mc.player != null) {
         long now = System.currentTimeMillis();
         this.activeParticles.removeIf(p -> now - p.spawnTime > p.maxAge);
         if (this.particles.get() && !this.activeParticles.isEmpty() && !this.particleMode.is("Vanilla Souls")) {
            this.renderParticles(context, now);
         }
      }
   }

   private void renderGlowShader(WorldRenderContext context, long now, double currentSpeed) {
      RenderTarget mainFbo = this.mc.gameRenderer.mainRenderTarget();
      if (mainFbo != null) {
         int width = mainFbo.width;
         int height = mainFbo.height;
         if (width > 0 && height > 0) {
            this.initGlowShader();
            if (this.glowProgram != -1) {
               this.initGlowQuad();
               if (this.glowVao != -1) {
                  this.ensureGlowFbo(width, height);
                  int prevReadFbo = GL11.glGetInteger(36010);
                  int prevDrawFbo = GL11.glGetInteger(36006);
                  GL30.glBindFramebuffer(36008, GlCompat.fbo(mainFbo));
                  GL30.glBindFramebuffer(36009, GlCompat.fbo(this.glowFbo));
                  GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, 16384, 9728);
                  GL30.glBindFramebuffer(36008, prevReadFbo);
                  GL30.glBindFramebuffer(36009, prevDrawFbo);
                  GL30.glBindFramebuffer(36160, GlCompat.fbo(mainFbo));
                  int prevProgram = GL11.glGetInteger(35725);
                  int prevVao = GL11.glGetInteger(34229);
                  int prevVbo = GL11.glGetInteger(34964);
                  int prevActiveTex = GL11.glGetInteger(34016);
                  GL13.glActiveTexture(33984);
                  int prevTex0 = GL11.glGetInteger(32873);
                  GL13.glActiveTexture(33985);
                  int prevTex1 = GL11.glGetInteger(32873);
                  boolean prevBlend = GL11.glIsEnabled(3042);
                  boolean prevDepthTest = GL11.glIsEnabled(2929);
                  boolean prevCull = GL11.glIsEnabled(2884);
                  boolean prevDepthMask = GL11.glGetBoolean(2930);
                  RenderSystem.disableDepthTest();
                  RenderSystem.depthMask(false);
                  RenderSystem.disableCull();
                  RenderSystem.disableBlend();
                  GL20.glUseProgram(this.glowProgram);
                  Matrix4f viewRotMat = new Matrix4f().rotation(new Quaternionf(context.camera().rotation()).conjugate());
                  Matrix4f projMat = new Matrix4f(context.projectionMatrix());
                  Matrix4f viewProjMat = new Matrix4f(projMat).mul(viewRotMat);
                  Matrix4f invViewProjMat = new Matrix4f(viewProjMat).invert();
                  float[] invViewProjArr = new float[16];
                  invViewProjMat.get(invViewProjArr);
                  float[] viewProjArr = new float[16];
                  viewProjMat.get(viewProjArr);
                  GL13.glActiveTexture(33984);
                  GL11.glBindTexture(3553, GlCompat.colorId(this.glowFbo));
                  GL13.glActiveTexture(33985);
                  GL11.glBindTexture(3553, GlCompat.depthId(mainFbo));
                  GL11.glTexParameteri(3553, 34892, 0);
                  GL11.glTexParameteri(3553, 10241, 9728);
                  GL11.glTexParameteri(3553, 10240, 9728);
                  GL11.glTexParameteri(3553, 10242, 33071);
                  GL11.glTexParameteri(3553, 10243, 33071);
                  this.setUniform1i("uColorTexture", 0);
                  this.setUniform1i("uDepthTexture", 1);
                  this.setUniformMatrix4fv("uInvViewProjMat", invViewProjArr);
                  this.setUniformMatrix4fv("uViewProjMat", viewProjArr);
                  this.setUniform2f("uResolution", width, height);
                  this.setUniform1f("uGlowIntensity", (float)this.glowIntensity.get());
                  this.setUniform1f("uRingWidth", (float)this.ringWidth.get());
                  this.setUniform1i("uDistortion", this.distortion.get() ? 1 : 0);
                  this.setUniform1f("uDistortionStrength", (float)this.distortionStrength.get());
                  this.setUniform1i("uChromatic", this.chromatic.get() ? 1 : 0);
                  Vec3 camPos = context.camera().position();
                  this.setUniform3f("uCameraPos", (float)camPos.x, (float)camPos.y, (float)camPos.z);
                  this.setUniform1i("uBlockCorners", this.blockCorners.get() ? 1 : 0);
                  this.setUniform1f("uCornerGlow", (float)this.cornerGlow.get());
                  this.setUniform1f("uCornerSize", (float)this.cornerSize.get());
                  this.setUniform1i("uEdgeLines", this.edgeLines.get() ? 1 : 0);
                  this.setUniform1f("uEdgeWidth", (float)this.edgeWidth.get());
                  int colorInt = ThemeManager.getThemedColor(0L);
                  float tr = (colorInt >> 16 & 0xFF) / 255.0F;
                  float tg = (colorInt >> 8 & 0xFF) / 255.0F;
                  float tb = (colorInt & 0xFF) / 255.0F;
                  this.setUniform3f("uThemeColor", tr, tg, tb);
                  double maxRad = this.radius.get();
                  int maxWaves = 16;
                  int count = 0;

                  for (HitGlowModule.GlowWave wave : this.activeWaves) {
                     if (count >= maxWaves) {
                        break;
                     }

                     double progress = (now - wave.startTime) * currentSpeed / 1400.0;
                     if (!(progress >= 1.0)) {
                        float easeRadius = (float)(maxRad * Math.sin(progress * Math.PI / 2.0));
                        float alpha = (float)(1.0 - Math.pow(progress, 2.0));
                        this.setUniform3f(
                           "uWavePos[" + count + "]",
                           (float)(wave.pos.x - camPos.x),
                           (float)(wave.pos.y - camPos.y),
                           (float)(wave.pos.z - camPos.z)
                        );
                        this.setUniform1f("uWaveRadius[" + count + "]", easeRadius);
                        this.setUniform1f("uWaveAlpha[" + count + "]", alpha);
                        this.setUniform1f("uWaveProgress[" + count + "]", (float)progress);
                        count++;
                     }
                  }

                  this.setUniform1i("uWaveCount", count);
                  GL30.glBindVertexArray(this.glowVao);
                  GL11.glDrawArrays(4, 0, 6);
                  GL30.glBindVertexArray(prevVao);
                  GL15.glBindBuffer(34962, prevVbo);
                  GL13.glActiveTexture(33985);
                  GL11.glBindTexture(3553, prevTex1);
                  GL13.glActiveTexture(33984);
                  GL11.glBindTexture(3553, prevTex0);
                  RenderSystem.setShaderTexture(0, prevTex0);
                  GL13.glActiveTexture(prevActiveTex);
                  GL20.glUseProgram(prevProgram);
                  if (prevDepthTest) {
                     RenderSystem.enableDepthTest();
                  }

                  if (prevCull) {
                     RenderSystem.enableCull();
                  }

                  if (prevBlend) {
                     RenderSystem.enableBlend();
                  }

                  RenderSystem.depthMask(prevDepthMask);
               }
            }
         }
      }
   }

   private void renderParticles(WorldRenderContext context, long time) {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
      RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder particleBuffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      PoseStack matrices = context.matrixStack();
      Vec3 camPos = context.camera().position();
      Quaternionf camRot = context.camera().rotation();
      boolean isGlowDot = this.particleMode.is("Glow Dots");

      for (HitGlowModule.GlowParticle p : this.activeParticles) {
         float lifePC = (float)(time - p.spawnTime) / (float)p.maxAge;
         if (!(lifePC < 0.0F) && !(lifePC >= 1.0F)) {
            float pAlpha = (float)Math.sin(lifePC * Math.PI) * (float)this.glowIntensity.get() * 0.85F;
            if (!(pAlpha <= 0.01F)) {
               double px = p.getX(time) - camPos.x;
               double py = p.getY(time) - camPos.y;
               double pz = p.getZ(time) - camPos.z;
               float scale = p.baseScale * (0.6F + 0.6F * (float)Math.sin(lifePC * Math.PI));
               matrices.pushPose();
               matrices.translate(px, py, pz);
               matrices.rotate(camRot);
               Matrix4f matrix = matrices.last().pose();
               if (!isGlowDot) {
                  float auraScale = scale * 1.5F;
                  float auraAlpha = pAlpha * 0.4F;
                  Vtx.vertex(particleBuffer, matrix, -auraScale, -auraScale, 0.0F).setUv(0.0F, 1.0F).setColor(p.r, p.g, p.b, auraAlpha);
                  Vtx.vertex(particleBuffer, matrix, auraScale, -auraScale, 0.0F).setUv(1.0F, 1.0F).setColor(p.r, p.g, p.b, auraAlpha);
                  Vtx.vertex(particleBuffer, matrix, auraScale, auraScale, 0.0F).setUv(1.0F, 0.0F).setColor(p.r, p.g, p.b, auraAlpha);
                  Vtx.vertex(particleBuffer, matrix, -auraScale, auraScale, 0.0F).setUv(0.0F, 0.0F).setColor(p.r, p.g, p.b, auraAlpha);
                  float coreR = Math.min(1.0F, p.r * 0.65F + 0.35F);
                  float coreG = Math.min(1.0F, p.g * 0.65F + 0.35F);
                  float coreB = Math.min(1.0F, p.b * 0.65F + 0.35F);
                  Vtx.vertex(particleBuffer, matrix, -scale, -scale, 0.0F).setUv(0.0F, 1.0F).setColor(coreR, coreG, coreB, pAlpha);
                  Vtx.vertex(particleBuffer, matrix, scale, -scale, 0.0F).setUv(1.0F, 1.0F).setColor(coreR, coreG, coreB, pAlpha);
                  Vtx.vertex(particleBuffer, matrix, scale, scale, 0.0F).setUv(1.0F, 0.0F).setColor(coreR, coreG, coreB, pAlpha);
                  Vtx.vertex(particleBuffer, matrix, -scale, scale, 0.0F).setUv(0.0F, 0.0F).setColor(coreR, coreG, coreB, pAlpha);
               } else {
                  Vtx.vertex(particleBuffer, matrix, -scale, -scale, 0.0F).setUv(0.0F, 1.0F).setColor(p.r, p.g, p.b, pAlpha);
                  Vtx.vertex(particleBuffer, matrix, scale, -scale, 0.0F).setUv(1.0F, 1.0F).setColor(p.r, p.g, p.b, pAlpha);
                  Vtx.vertex(particleBuffer, matrix, scale, scale, 0.0F).setUv(1.0F, 0.0F).setColor(p.r, p.g, p.b, pAlpha);
                  Vtx.vertex(particleBuffer, matrix, -scale, scale, 0.0F).setUv(0.0F, 0.0F).setColor(p.r, p.g, p.b, pAlpha);
               }

               matrices.popPose();
            }
         }
      }

      MeshData built = particleBuffer.build();
      if (built != null) {
         BufferUploader.drawWithShader(built);
      }

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void ensureGlowFbo(int width, int height) {
      if (this.glowFbo == null || this.glowFbo.width != width || this.glowFbo.height != height) {
         if (this.glowFbo != null) {
            this.glowFbo.destroyBuffers();
         }

         this.glowFbo = GlCompat.newTarget(width, height, false);
         GlCompat.setClearColor(this.glowFbo, 0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glBindTexture(3553, GlCompat.colorId(this.glowFbo));
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glBindTexture(3553, 0);
      }
   }

   private void initGlowQuad() {
      if (this.glowVao == -1) {
         float[] vertices = new float[]{
            -1.0F,
            -1.0F,
            0.0F,
            0.0F,
            1.0F,
            -1.0F,
            1.0F,
            0.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            0.0F,
            1.0F
         };
         int prevVao = GL11.glGetInteger(34229);
         int prevVbo = GL11.glGetInteger(34964);
         this.glowVao = GL30.glGenVertexArrays();
         this.glowVbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.glowVao);
         GL15.glBindBuffer(34962, this.glowVbo);
         FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
         buffer.put(vertices).flip();
         GL15.glBufferData(34962, buffer, 35044);
         GL20.glVertexAttribPointer(0, 2, 5126, false, 16, 0L);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(1, 2, 5126, false, 16, 8L);
         GL20.glEnableVertexAttribArray(1);
         GL15.glBindBuffer(34962, prevVbo);
         GL30.glBindVertexArray(prevVao);
      }
   }

   private void initGlowShader() {
      if (this.glowProgram == -1) {
         String vert = "#version 150 core\nin vec2 Position;\nin vec2 TexCoord;\nout vec2 vTexCoord;\nvoid main() {\n    vTexCoord = TexCoord;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}";
         String frag = "#version 150 core\nin vec2 vTexCoord;\nout vec4 fragColor;\n\nuniform sampler2D uColorTexture;\nuniform sampler2D uDepthTexture;\nuniform mat4 uInvViewProjMat;\nuniform mat4 uViewProjMat;\nuniform vec2 uResolution;\nuniform float uGlowIntensity;\nuniform float uRingWidth;\nuniform int uDistortion;\nuniform float uDistortionStrength;\nuniform int uChromatic;\nuniform vec3 uThemeColor;\nuniform vec3 uCameraPos;\n\nuniform int uBlockCorners;\nuniform float uCornerGlow;\nuniform float uCornerSize;\nuniform int uEdgeLines;\nuniform float uEdgeWidth;\n\nconst int MAX_WAVES = 16;\nuniform int uWaveCount;\nuniform vec3 uWavePos[MAX_WAVES];\nuniform float uWaveRadius[MAX_WAVES];\nuniform float uWaveAlpha[MAX_WAVES];\nuniform float uWaveProgress[MAX_WAVES];\n\nvoid main() {\n    vec4 baseColor = texture(uColorTexture, vTexCoord);\n    if (uWaveCount <= 0 || (uGlowIntensity <= 0.001 && uCornerGlow <= 0.001)) {\n        fragColor = baseColor;\n        return;\n    }\n\n    float rawDepth = texture(uDepthTexture, vTexCoord).r;\n    if (rawDepth >= 0.999999) {\n        fragColor = baseColor;\n        return;\n    }\n\n    // Reconstruct 3D camera-relative world position\n    vec4 clipPos = vec4(vTexCoord * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);\n    vec4 camRelPosH = uInvViewProjMat * clipPos;\n    vec3 camRelPos = camRelPosH.xyz / max(0.0001, camRelPosH.w);\n    vec3 worldPos = camRelPos + uCameraPos;\n\n    // Calculate distance to 3D block face boundaries\n    vec3 bFract = fract(worldPos);\n    vec3 d = min(bFract, vec3(1.0) - bFract);\n\n    // Identify 2D face coordinates on the nearest block face\n    vec2 faceCoords;\n    if (d.x <= d.y && d.x <= d.z) {\n        faceCoords = vec2(d.y, d.z);\n    } else if (d.y <= d.x && d.y <= d.z) {\n        faceCoords = vec2(d.x, d.z);\n    } else {\n        faceCoords = vec2(d.x, d.y);\n    }\n\n    // 2D distance to the nearest corner on this block face\n    float cornerDist = length(faceCoords);\n    float cornerFactor = smoothstep(max(0.01, uCornerSize), 0.0, cornerDist);\n    float cornerPattern = pow(cornerFactor, 1.3) * 3.0;\n\n    // 2D distance to the nearest edge on this block face\n    float edgeDist = min(faceCoords.x, faceCoords.y);\n    float edgeFactor = smoothstep(max(0.005, uEdgeWidth), 0.0, edgeDist);\n\n    float blockPattern = (uEdgeLines == 1) ? (cornerPattern + edgeFactor * 1.4) : cornerPattern;\n\n    vec2 totalUVShift = vec2(0.0);\n    float totalChromatic = 0.0;\n    float totalGlow = 0.0;\n    float totalBlockGlow = 0.0;\n\n    for (int i = 0; i < uWaveCount; i++) {\n        if (i >= MAX_WAVES) break;\n\n        vec3 wPos = uWavePos[i];\n        float radius = uWaveRadius[i];\n        float alpha = uWaveAlpha[i];\n        float progress = uWaveProgress[i];\n\n        if (alpha <= 0.001 || radius <= 0.1) continue;\n\n        vec3 delta = camRelPos - wPos;\n        float dist3D = length(delta);\n\n        float maxEffectRadius = radius + uRingWidth * 2.0;\n        if (dist3D > maxEffectRadius) continue;\n\n        // 1. Smooth 3D Expanding Energy Ring across all block surfaces\n        float ringDist = abs(dist3D - radius);\n        float ringFactor = smoothstep(uRingWidth, 0.0, ringDist);\n\n        // 2. Inner smooth glowing aura\n        float innerFill = smoothstep(radius, 0.0, dist3D) * 0.20 * (1.0 - progress);\n\n        // 3. Block Corner Glow strictly synchronized with the expanding wave ring\n        if (uBlockCorners == 1) {\n            // Peaks at the wave crest and smoothly fades behind the wave with alpha\n            float cornerWaveSync = ringFactor * alpha * (1.0 - progress * 0.3);\n            totalBlockGlow += blockPattern * cornerWaveSync * uCornerGlow;\n        }\n\n        float waveGlow = (ringFactor * 1.6 + innerFill) * alpha * uGlowIntensity;\n        totalGlow += waveGlow;\n\n        // 4. Subtle physical refractive distortion wave on the ring crest\n        if (uDistortion == 1) {\n            float ripple = sin((dist3D - radius) / max(0.1, uRingWidth) * 3.14159265) * ringFactor;\n            vec3 normalDir = delta / max(0.001, dist3D);\n            vec3 worldDisplace = normalDir * (ripple * 0.22 * uDistortionStrength) * alpha;\n\n            vec3 displacedWorldPos = camRelPos + worldDisplace;\n            vec4 distClip = uViewProjMat * vec4(displacedWorldPos, 1.0);\n            if (distClip.w > 0.01) {\n                vec2 distUV = (distClip.xy / distClip.w) * 0.5 + 0.5;\n                totalUVShift += (distUV - vTexCoord);\n            }\n\n            if (uChromatic == 1) {\n                totalChromatic += abs(ripple) * 0.008 * uDistortionStrength * alpha;\n            }\n        }\n    }\n\n    vec2 uvR = vTexCoord + totalUVShift + vec2(totalChromatic, 0.0);\n    vec2 uvG = vTexCoord + totalUVShift;\n    vec2 uvB = vTexCoord + totalUVShift - vec2(totalChromatic, 0.0);\n\n    float r = texture(uColorTexture, clamp(uvR, 0.001, 0.999)).r;\n    float g = texture(uColorTexture, clamp(uvG, 0.001, 0.999)).g;\n    float b = texture(uColorTexture, clamp(uvB, 0.001, 0.999)).b;\n\n    vec3 sceneColor = vec3(r, g, b);\n\n    // Apply smooth neon theme glow directly onto block textures and corners\n    float combinedGlow = totalGlow + totalBlockGlow;\n    if (combinedGlow > 0.005) {\n        vec3 glowColor = uThemeColor * combinedGlow;\n        sceneColor = sceneColor + glowColor + (sceneColor * glowColor * 0.45);\n    }\n\n    fragColor = vec4(sceneColor, baseColor.a);\n}";
         this.glowProgram = this.createProgram(vert, frag);
      }
   }

   private int createProgram(String vert, String frag) {
      int v = GL20.glCreateShader(35633);
      GL20.glShaderSource(v, vert);
      GL20.glCompileShader(v);
      if (GL20.glGetShaderi(v, 35713) == 0) {
         System.err.println("HitGlow Vertex shader compile error:\n" + GL20.glGetShaderInfoLog(v, 1024));
      }

      int f = GL20.glCreateShader(35632);
      GL20.glShaderSource(f, frag);
      GL20.glCompileShader(f);
      if (GL20.glGetShaderi(f, 35713) == 0) {
         System.err.println("HitGlow Fragment shader compile error:\n" + GL20.glGetShaderInfoLog(f, 1024));
      }

      int p = GL20.glCreateProgram();
      GL20.glAttachShader(p, v);
      GL20.glAttachShader(p, f);
      GL20.glBindAttribLocation(p, 0, "Position");
      GL20.glBindAttribLocation(p, 1, "TexCoord");
      GL20.glLinkProgram(p);
      if (GL20.glGetProgrami(p, 35714) == 0) {
         System.err.println("HitGlow Shader program link error:\n" + GL20.glGetProgramInfoLog(p, 1024));
      }

      return p;
   }

   private void setUniform1i(String name, int val) {
      int loc = GL20.glGetUniformLocation(this.glowProgram, name);
      if (loc != -1) {
         GL20.glUniform1i(loc, val);
      }
   }

   private void setUniform1f(String name, float val) {
      int loc = GL20.glGetUniformLocation(this.glowProgram, name);
      if (loc != -1) {
         GL20.glUniform1f(loc, val);
      }
   }

   private void setUniform2f(String name, float x, float y) {
      int loc = GL20.glGetUniformLocation(this.glowProgram, name);
      if (loc != -1) {
         GL20.glUniform2f(loc, x, y);
      }
   }

   private void setUniform3f(String name, float x, float y, float z) {
      int loc = GL20.glGetUniformLocation(this.glowProgram, name);
      if (loc != -1) {
         GL20.glUniform3f(loc, x, y, z);
      }
   }

   private void setUniformMatrix4fv(String name, float[] mat) {
      int loc = GL20.glGetUniformLocation(this.glowProgram, name);
      if (loc != -1) {
         FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
         buffer.put(mat).flip();
         GL20.glUniformMatrix4fv(loc, false, buffer);
      }
   }

   @Environment(EnvType.CLIENT)
   private static class GlowParticle {
      double startX;
      double startY;
      double startZ;
      double riseSpeed;
      double swaySpeedX;
      double swayAmpX;
      double swayPhaseX;
      double swaySpeedZ;
      double swayAmpZ;
      double swayPhaseZ;
      double driftX;
      double driftZ;
      float r;
      float g;
      float b;
      float baseScale;
      long spawnTime;
      long maxAge;

      public GlowParticle(double x, double y, double z, double driftX, double driftZ, float r, float g, float b, float scale, long maxAge) {
         this.startX = x;
         this.startY = y;
         this.startZ = z;
         this.driftX = driftX;
         this.driftZ = driftZ;
         this.riseSpeed = 0.7 + Math.random() * 0.8;
         this.swaySpeedX = 2.5 + Math.random() * 2.0;
         this.swayAmpX = 0.06 + Math.random() * 0.1;
         this.swayPhaseX = Math.random() * Math.PI * 2.0;
         this.swaySpeedZ = 2.5 + Math.random() * 2.0;
         this.swayAmpZ = 0.06 + Math.random() * 0.1;
         this.swayPhaseZ = Math.random() * Math.PI * 2.0;
         this.r = r;
         this.g = g;
         this.b = b;
         this.baseScale = scale;
         this.spawnTime = System.currentTimeMillis();
         this.maxAge = maxAge;
      }

      public double getX(long currentTime) {
         double dt = (currentTime - this.spawnTime) / 1000.0;
         return this.startX + this.driftX * dt + Math.sin(dt * this.swaySpeedX + this.swayPhaseX) * this.swayAmpX;
      }

      public double getY(long currentTime) {
         double dt = (currentTime - this.spawnTime) / 1000.0;
         return this.startY + this.riseSpeed * dt;
      }

      public double getZ(long currentTime) {
         double dt = (currentTime - this.spawnTime) / 1000.0;
         return this.startZ + this.driftZ * dt + Math.cos(dt * this.swaySpeedZ + this.swayPhaseZ) * this.swayAmpZ;
      }
   }

   @Environment(EnvType.CLIENT)
   public static class GlowWave {
      public final Vec3 pos;
      public final long startTime;

      public GlowWave(Vec3 pos) {
         this.pos = pos;
         this.startTime = System.currentTimeMillis();
      }
   }
}
