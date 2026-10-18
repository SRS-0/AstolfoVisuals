package xyz.angames.astolfoclient.client.module.modules.render;

import xyz.angames.astolfoclient.client.compat.GlCompat;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.core.registries.BuiltInRegistries;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import xyz.angames.astolfoclient.client.compat.GL20;
import org.lwjgl.opengl.GL30;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(EnvType.CLIENT)
public class ShaderHand extends Module {
   public static boolean rendering = false;
   public final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Fill1", "Fill2", "Fill3");
   public final BooleanSetting glass = new BooleanSetting("Glass", true);
   public final NumberSetting glassAlpha = new NumberSetting("Glass Alpha", 0.5, 0.0, 1.0, 0.05);
   public final BooleanSetting blur = new BooleanSetting("Blur Background", true);
   public final NumberSetting blurStrength = new NumberSetting("Blur Strength", 3.0, 0.0, 100.0, 0.5);
   public final BooleanSetting glow = new BooleanSetting("Glow", true);
   public final NumberSetting glowRadius = new NumberSetting("Glow Radius", 3.0, 1.0, 5.0, 1.0);
   public final BooleanSetting outerGlow = new BooleanSetting("Outer Glow", true);
   public final NumberSetting outerExposure = new NumberSetting("Outer Exposure", 2.0, 0.5, 5.0, 0.1);
   public final BooleanSetting innerGlow = new BooleanSetting("Inner Glow", false);
   public final NumberSetting innerExposure = new NumberSetting("Inner Exposure", 2.0, 0.5, 5.0, 0.1);
   public final BooleanSetting handColor = new BooleanSetting("Hand Color", false);
   public final BooleanSetting motion = new BooleanSetting("Motion", false);
   public final NumberSetting motionStrength = new NumberSetting("Motion Strength", 0.8, 0.0, 0.99, 0.05);
   private RenderTarget handsBuffer;
   private RenderTarget motionBuffer;
   private RenderTarget tempMotionBuffer;
   private final List<RenderTarget> bloomBuffers = new ArrayList<>();
   private final Minecraft mc = Minecraft.getInstance();
   private int kawaseDownProgram = -1;
   private int kawaseUpProgram = -1;
   private int innerGlowProgram = -1;
   private int outerGlowProgram = -1;
   private int baseProgram = -1;
   private int blendProgram = -1;
   private long lastDrawTime = 0L;

   public static ShaderHand getInstance() {
      return (ShaderHand)AstolfoclientClient.moduleManager.getModuleByName("ShaderHand");
   }

   public ShaderHand() {
      super("ShaderHand", "Premium Kawase Blur Hand Chams", Module.Category.RENDER);
      this.addSettings(
         this.mode,
         this.glass,
         this.glassAlpha,
         this.blur,
         this.blurStrength,
         this.glow,
         this.glowRadius,
         this.outerGlow,
         this.outerExposure,
         this.innerGlow,
         this.innerExposure,
         this.handColor,
         this.motion,
         this.motionStrength
      );
   }

   @Override
   public void onEnable() {
      this.clearBuffers();
   }

   @Override
   public void onDisable() {
      this.clearBuffers();
   }

   private void clearBuffers() {
      if (this.handsBuffer != null) {
         this.handsBuffer.destroyBuffers();
         this.handsBuffer = null;
      }

      if (this.motionBuffer != null) {
         this.motionBuffer.destroyBuffers();
         this.motionBuffer = null;
      }

      if (this.tempMotionBuffer != null) {
         this.tempMotionBuffer.destroyBuffers();
         this.tempMotionBuffer = null;
      }

      this.bloomBuffers.forEach(RenderTarget::destroyBuffers);
      this.bloomBuffers.clear();
   }

   private void setLinearFilter(RenderTarget buffer) {
      if (buffer != null) {
         GL11.glBindTexture(3553, GlCompat.colorId(buffer));
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
      }
   }

   public RenderTarget getHandsBuffer() {
      int width = this.mc.getWindow().getWidth();
      int height = this.mc.getWindow().getHeight();
      if (this.handsBuffer == null || this.handsBuffer.width != width || this.handsBuffer.height != height) {
         if (this.handsBuffer != null) {
            this.handsBuffer.destroyBuffers();
         }

         this.handsBuffer = GlCompat.newTarget(width, height, true);
         GlCompat.setClearColor(this.handsBuffer, 0.0F, 0.0F, 0.0F, 0.0F);
         this.setLinearFilter(this.handsBuffer);
      }

      return this.handsBuffer;
   }

   public void beginRender() {
      RenderTarget fbo = this.getHandsBuffer();
      GlCompat.clear(fbo);
      GlCompat.bindWrite(fbo, true);
   }

   public void draw() {
      if (this.handsBuffer != null) {
         if (this.kawaseDownProgram == -1) {
            this.initShaders();
         }

         if (this.kawaseDownProgram > 0) {
            int prevProgram = GL11.glGetInteger(35725);
            int prevActiveTexture = GL11.glGetInteger(34016);
            int prevTexture = GL11.glGetInteger(32873);
            RenderTarget inputBuffer = this.handsBuffer;
            if (this.motion.get() && this.motionStrength.getFloat() > 0.0F) {
               long now = System.currentTimeMillis();
               double dt = this.lastDrawTime == 0L ? 0.016 : (now - this.lastDrawTime) / 1000.0;
               dt = Math.max(0.001, Math.min(0.1, dt));
               if (now - this.lastDrawTime > 100L) {
                  if (this.motionBuffer != null) {
                     GlCompat.clear(this.motionBuffer);
                  }

                  if (this.tempMotionBuffer != null) {
                     GlCompat.clear(this.tempMotionBuffer);
                  }

                  dt = 0.016;
               }

               this.lastDrawTime = now;
               float adjustedStrength = (float)Math.pow(this.motionStrength.getFloat(), dt * 60.0);
               int width = this.mc.getWindow().getWidth();
               int height = this.mc.getWindow().getHeight();
               if (this.motionBuffer == null || this.motionBuffer.width != width || this.motionBuffer.height != height) {
                  if (this.motionBuffer != null) {
                     this.motionBuffer.destroyBuffers();
                  }

                  this.motionBuffer = GlCompat.newTarget(width, height, true);
                  GlCompat.setClearColor(this.motionBuffer, 0.0F, 0.0F, 0.0F, 0.0F);
                  this.setLinearFilter(this.motionBuffer);
                  GlCompat.clear(this.motionBuffer);
               }

               if (this.tempMotionBuffer == null || this.tempMotionBuffer.width != width || this.tempMotionBuffer.height != height) {
                  if (this.tempMotionBuffer != null) {
                     this.tempMotionBuffer.destroyBuffers();
                  }

                  this.tempMotionBuffer = GlCompat.newTarget(width, height, true);
                  GlCompat.setClearColor(this.tempMotionBuffer, 0.0F, 0.0F, 0.0F, 0.0F);
                  this.setLinearFilter(this.tempMotionBuffer);
                  GlCompat.clear(this.tempMotionBuffer);
               }

               GlCompat.clear(this.tempMotionBuffer);
               GlCompat.bindWrite(this.tempMotionBuffer, true);
               GL20.glUseProgram(this.blendProgram);
               GL20.glUniform1i(GL20.glGetUniformLocation(this.blendProgram, "currentTexture"), 0);
               GL20.glUniform1i(GL20.glGetUniformLocation(this.blendProgram, "historyTexture"), 1);
               GL20.glUniform1f(GL20.glGetUniformLocation(this.blendProgram, "motionStrength"), adjustedStrength);
               GlStateManager._activeTexture(33985);
               GlStateManager._bindTexture(GlCompat.colorId(this.motionBuffer));
               GL11.glTexParameteri(3553, 10241, 9729);
               GL11.glTexParameteri(3553, 10240, 9729);
               GlStateManager._activeTexture(33984);
               GlStateManager._bindTexture(GlCompat.colorId(this.handsBuffer));
               GL11.glTexParameteri(3553, 10241, 9729);
               GL11.glTexParameteri(3553, 10240, 9729);
               this.drawQuads();
               RenderTarget temp = this.motionBuffer;
               this.motionBuffer = this.tempMotionBuffer;
               this.tempMotionBuffer = temp;
               inputBuffer = this.motionBuffer;
            } else {
               if (this.motionBuffer != null) {
                  this.motionBuffer.destroyBuffers();
                  this.motionBuffer = null;
               }

               if (this.tempMotionBuffer != null) {
                  this.tempMotionBuffer.destroyBuffers();
                  this.tempMotionBuffer = null;
               }
            }

            GlCompat.bindWrite(this.mc.gameRenderer.mainRenderTarget(), true);
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            if (!this.glass.get() && !this.blur.get()) {
               RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
            } else {
               RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
            }

            int color = ThemeManager.getThemedColor(0L);
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            GL20.glUseProgram(this.baseProgram);
            GL20.glUniform1i(GL20.glGetUniformLocation(this.baseProgram, "inTexture"), 0);
            GL20.glUniform1i(GL20.glGetUniformLocation(this.baseProgram, "bgTexture"), 1);
            float alpha = this.glass.get() ? this.glassAlpha.getFloat() : 1.0F;
            GL20.glUniform1f(GL20.glGetUniformLocation(this.baseProgram, "alpha"), alpha);
            GL20.glUniform1i(GL20.glGetUniformLocation(this.baseProgram, "doBlur"), this.blur.get() ? 1 : 0);
            GL20.glUniform1f(GL20.glGetUniformLocation(this.baseProgram, "blurStrength"), this.blurStrength.getFloat());
            GL20.glUniform2f(
               GL20.glGetUniformLocation(this.baseProgram, "resolution"), this.mc.getWindow().getWidth(), this.mc.getWindow().getHeight()
            );
            GL20.glUniform1f(GL20.glGetUniformLocation(this.baseProgram, "time"), (float)(System.nanoTime() / 1.0E9 % 100000.0));
            GL20.glUniform3f(GL20.glGetUniformLocation(this.baseProgram, "themeColor"), r, g, b);
            GL20.glUniform1i(GL20.glGetUniformLocation(this.baseProgram, "useHandColor"), this.handColor.get() ? 1 : 0);
            int modeVal = 0;
            if (this.mode.is("Fill1")) {
               modeVal = 1;
            } else if (this.mode.is("Fill2")) {
               modeVal = 2;
            } else if (this.mode.is("Fill3")) {
               modeVal = 3;
            }

            GL20.glUniform1i(GL20.glGetUniformLocation(this.baseProgram, "mode"), modeVal);
            GlStateManager._activeTexture(33985);
            GlStateManager._bindTexture(GlCompat.colorId(this.mc.gameRenderer.mainRenderTarget()));
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            GlStateManager._activeTexture(33984);
            GlStateManager._bindTexture(GlCompat.colorId(inputBuffer));
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            this.drawQuads();
            if (this.glow.get()) {
               this.renderKawaseBloom(inputBuffer);
            }

            GL20.glUseProgram(prevProgram);
            GlStateManager._activeTexture(prevActiveTexture);
            GlStateManager._bindTexture(prevTexture);
            RenderSystem.enableDepthTest();
            RenderSystem.defaultBlendFunc();
         }
      }
   }

   private void renderKawaseBloom(RenderTarget inputBuffer) {
      int iterations = this.glowRadius.getInt();
      this.setupBloomBuffers(iterations);
      int currentTexture = GlCompat.colorId(inputBuffer);
      int color = ThemeManager.getThemedColor(0L);
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      GL20.glUseProgram(this.kawaseDownProgram);
      GL20.glUniform1i(GL20.glGetUniformLocation(this.kawaseDownProgram, "inTexture"), 0);

      for (int i = 0; i < iterations; i++) {
         RenderTarget buffer = this.bloomBuffers.get(i);
         GlCompat.clear(buffer);
         GlCompat.bindWrite(buffer, true);
         GL20.glUniform2f(GL20.glGetUniformLocation(this.kawaseDownProgram, "uHalfPixel"), 0.5F / buffer.width, 0.5F / buffer.height);
         GL20.glUniform2f(GL20.glGetUniformLocation(this.kawaseDownProgram, "uOffset"), 1 + i, 1 + i);
         GlStateManager._activeTexture(33984);
         GlStateManager._bindTexture(currentTexture);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         this.drawQuads();
         currentTexture = GlCompat.colorId(buffer);
      }

      GL20.glUseProgram(this.kawaseUpProgram);
      GL20.glUniform1i(GL20.glGetUniformLocation(this.kawaseUpProgram, "inTexture"), 0);
      GL20.glUniform3f(GL20.glGetUniformLocation(this.kawaseUpProgram, "color"), 1.0F, 1.0F, 1.0F);

      for (int i = iterations - 1; i >= 0 && i != 0; i--) {
         RenderTarget buffer = this.bloomBuffers.get(i - 1);
         GlCompat.bindWrite(buffer, true);
         GL20.glUniform2f(GL20.glGetUniformLocation(this.kawaseUpProgram, "uHalfPixel"), 0.5F / buffer.width, 0.5F / buffer.height);
         GL20.glUniform2f(GL20.glGetUniformLocation(this.kawaseUpProgram, "uOffset"), 1 + i, 1 + i);
         GlStateManager._activeTexture(33984);
         GlStateManager._bindTexture(currentTexture);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         this.drawQuads();
         currentTexture = GlCompat.colorId(buffer);
      }

      GlCompat.bindWrite(this.mc.gameRenderer.mainRenderTarget(), true);
      RenderSystem.enableBlend();
      int handColorVal = this.handColor.get() ? 1 : 0;
      if (this.outerGlow.get()) {
         RenderSystem.blendFunc(770, 1);
         GL20.glUseProgram(this.outerGlowProgram);
         GL20.glUniform1i(GL20.glGetUniformLocation(this.outerGlowProgram, "bloomTexture"), 0);
         GL20.glUniform1i(GL20.glGetUniformLocation(this.outerGlowProgram, "maskTexture"), 1);
         GL20.glUniform3f(GL20.glGetUniformLocation(this.outerGlowProgram, "glowColor1"), r, g, b);
         GL20.glUniform3f(GL20.glGetUniformLocation(this.outerGlowProgram, "glowColor2"), r, g, b);
         GL20.glUniform1f(GL20.glGetUniformLocation(this.outerGlowProgram, "alpha"), this.outerExposure.getFloat());
         GL20.glUniform1i(GL20.glGetUniformLocation(this.outerGlowProgram, "useHandColor"), handColorVal);
         GlStateManager._activeTexture(33985);
         GlStateManager._bindTexture(GlCompat.colorId(inputBuffer));
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GlStateManager._activeTexture(33984);
         GlStateManager._bindTexture(currentTexture);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         this.drawQuads();
      }

      if (this.innerGlow.get()) {
         RenderSystem.blendFunc(770, 1);
         GL20.glUseProgram(this.innerGlowProgram);
         GL20.glUniform1i(GL20.glGetUniformLocation(this.innerGlowProgram, "bloomTexture"), 0);
         GL20.glUniform1i(GL20.glGetUniformLocation(this.innerGlowProgram, "maskTexture"), 1);
         GL20.glUniform3f(GL20.glGetUniformLocation(this.innerGlowProgram, "glowColor1"), r, g, b);
         GL20.glUniform3f(GL20.glGetUniformLocation(this.innerGlowProgram, "glowColor2"), r, g, b);
         GL20.glUniform1f(GL20.glGetUniformLocation(this.innerGlowProgram, "alpha"), this.innerExposure.getFloat());
         GL20.glUniform1i(GL20.glGetUniformLocation(this.innerGlowProgram, "useHandColor"), handColorVal);
         GlStateManager._activeTexture(33985);
         GlStateManager._bindTexture(GlCompat.colorId(inputBuffer));
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GlStateManager._activeTexture(33984);
         GlStateManager._bindTexture(currentTexture);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         this.drawQuads();
      }
   }

   private void setupBloomBuffers(int iterations) {
      if (this.bloomBuffers.size() < iterations) {
         this.bloomBuffers.forEach(RenderTarget::destroyBuffers);
         this.bloomBuffers.clear();

         for (int i = 0; i < iterations; i++) {
            int w = (int)Math.max(2.0, this.mc.getWindow().getWidth() / Math.pow(2.0, i + 1));
            int h = (int)Math.max(2.0, this.mc.getWindow().getHeight() / Math.pow(2.0, i + 1));
            RenderTarget fbo = GlCompat.newTarget(w, h, false);
            GlCompat.setClearColor(fbo, 0.0F, 0.0F, 0.0F, 0.0F);
            this.bloomBuffers.add(fbo);
            this.setLinearFilter(fbo);
         }
      }

      for (int i = 0; i < iterations; i++) {
         int w = (int)Math.max(2.0, this.mc.getWindow().getWidth() / Math.pow(2.0, i + 1));
         int h = (int)Math.max(2.0, this.mc.getWindow().getHeight() / Math.pow(2.0, i + 1));
         RenderTarget fbo = this.bloomBuffers.get(i);
         if (fbo.width != w || fbo.height != h) {
            fbo.resize(w, h);
            this.setLinearFilter(fbo);
         }
      }
   }

   private void drawQuads() {
      int vao = GL30.glGenVertexArrays();
      int vbo = GL15.glGenBuffers();
      GL30.glBindVertexArray(vao);
      GL15.glBindBuffer(34962, vbo);
      float[] vertices = new float[]{-1.0F, -1.0F, 0.0F, 0.0F, 1.0F, -1.0F, 1.0F, 0.0F, -1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};
      GL15.glBufferData(34962, vertices, 35044);
      GL20.glEnableVertexAttribArray(0);
      GL20.glVertexAttribPointer(0, 2, 5126, false, 16, 0L);
      GL20.glEnableVertexAttribArray(1);
      GL20.glVertexAttribPointer(1, 2, 5126, false, 16, 8L);
      GL11.glDrawArrays(5, 0, 4);
      GL30.glBindVertexArray(0);
      GL15.glBindBuffer(34962, 0);
      GL30.glDeleteVertexArrays(vao);
      GL15.glDeleteBuffers(vbo);
   }

   private void initShaders() {
      String vert = "#version 150 core\nin vec2 Position;\nin vec2 UV;\nout vec2 texCoord;\nvoid main() {\n    texCoord = UV;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}";
      String baseFrag = "#version 150 core\nuniform sampler2D inTexture;\nuniform sampler2D bgTexture;\nuniform float alpha;\nuniform int doBlur;\nuniform float blurStrength;\nuniform vec2 resolution;\nuniform float time;\nuniform vec3 themeColor;\nuniform int useHandColor;\nuniform int mode;\nin vec2 texCoord;\nout vec4 fragColor;\n\nfloat wave(float x, float y, float time) {\n    return sin(10.0*x + 10.0*y + time) / 5.0 +\n           sin(20.0*x + 15.0*y - time * 1.2) / 3.0 +\n           sin(4.0*x + 10.0*y + time * 0.8) / -4.0 +\n           sin(y + time * 0.5) / 2.0 +\n           sin(x*x*y*20.0 + time * 1.4) +\n           sin(x * 20.0 + 4.0 - time) / 5.0 +\n           sin(y * 30.0 + time * 1.1) / 5.0 +\n           sin(x - time * 0.6) / 4.0;\n}\n\nvoid main() {\n    vec4 tex = texture(inTexture, texCoord);\n    if(tex.a < 0.01) discard;\n    vec3 colorToUse = (useHandColor == 1) ? tex.rgb : themeColor;\n    if (mode == 1) {\n        vec2 uv = (2.0 * (texCoord * resolution) - resolution) / min(resolution.x, resolution.y);\n        for(float i = 1.0; i < 12.0; i++){\n            uv.x += 0.6 / i * cos(i * 2.5 * uv.y + time);\n            uv.y += 0.6 / i * cos(i * 1.5 * uv.x + time);\n        }\n        float intensity = 0.1 / abs(sin(time - uv.y - uv.x));\n        fragColor = vec4(intensity * colorToUse, tex.a * alpha);\n    } else if (mode == 2) {\n        vec2 uv = texCoord;\n        float z = wave(uv.x, uv.y, time) + 2.0;\n        z *= 5.0 + 2.0 * sin(time * 0.4);\n        float d = fract(z);\n        if (mod(z, 2.0) > 1.0) d = 1.0 - d;\n        d = d / fwidth(z);\n        float lineVal = clamp(1.0 - d, 0.0, 1.0);\n        fragColor = vec4(lineVal * colorToUse, tex.a * alpha);\n    } else if (mode == 3) {\n        float epsX = 2.0 / resolution.x;\n        float epsY = 2.0 / resolution.y;\n        float aLeft  = texture(inTexture, texCoord + vec2(-epsX, 0.0)).a;\n        float aRight = texture(inTexture, texCoord + vec2(epsX, 0.0)).a;\n        float aDown  = texture(inTexture, texCoord + vec2(0.0, -epsY)).a;\n        float aUp    = texture(inTexture, texCoord + vec2(0.0, epsY)).a;\n        vec2 grad = vec2(aRight - aLeft, aUp - aDown);\n        vec2 dir = length(grad) > 0.001 ? normalize(grad) : vec2(0.0);\n        vec2 mirrorCoord = vec2(1.0 - texCoord.x, texCoord.y);\n        vec2 distortedMirror = mirrorCoord + vec2(-dir.x, dir.y) * 0.05 * (1.0 - tex.a);\n        distortedMirror = clamp(distortedMirror, 0.001, 0.999);\n        vec3 reflected = texture(bgTexture, distortedMirror).rgb;\n        float shine = pow(max(0.0, 1.0 - length(texCoord - vec2(0.5))), 4.0);\n        vec3 glassColor = mix(reflected, colorToUse, 0.25) + vec3(shine * 0.15);\n        float edgeGlow = smoothstep(0.0, 1.0, length(grad) * 2.0);\n        glassColor = mix(glassColor, colorToUse * 1.5, edgeGlow * 0.5);\n        fragColor = vec4(glassColor, tex.a * alpha);\n    } else if (doBlur == 1) {\n        vec4 sum = vec4(0.0);\n        vec2 offset = (blurStrength * 0.5) / resolution;\n        sum += texture(bgTexture, texCoord + vec2(-offset.x * 1.5, -offset.y * 1.5)) * 0.09;\n        sum += texture(bgTexture, texCoord + vec2(0.0, -offset.y * 1.5)) * 0.12;\n        sum += texture(bgTexture, texCoord + vec2(offset.x * 1.5, -offset.y * 1.5)) * 0.09;\n        sum += texture(bgTexture, texCoord + vec2(-offset.x * 1.5, 0.0)) * 0.12;\n        sum += texture(bgTexture, texCoord) * 0.16;\n        sum += texture(bgTexture, texCoord + vec2(offset.x * 1.5, 0.0)) * 0.12;\n        sum += texture(bgTexture, texCoord + vec2(-offset.x * 1.5, offset.y * 1.5)) * 0.09;\n        sum += texture(bgTexture, texCoord + vec2(0.0, offset.y * 1.5)) * 0.12;\n        sum += texture(bgTexture, texCoord + vec2(offset.x * 1.5, offset.y * 1.5)) * 0.09;\n        vec4 bgColor = sum;\n        vec3 glassColor = mix(bgColor.rgb, tex.rgb, 0.4) + vec3(tex.a * 0.1);\n        fragColor = vec4(glassColor, alpha);\n    } else {\n        fragColor = vec4(tex.rgb, tex.a * alpha);\n    }\n}";
      String kawaseDown = "#version 150 core\nuniform sampler2D inTexture;\nuniform vec2 uOffset, uHalfPixel;\nin vec2 texCoord;\nout vec4 fragColor;\nvoid main() {\n    vec2 uv = texCoord;\n    vec2 halfPixel = uHalfPixel * uOffset;\n    vec4 sum = texture(inTexture, uv) * 4.0;\n    sum += texture(inTexture, uv - halfPixel);\n    sum += texture(inTexture, uv + halfPixel);\n    sum += texture(inTexture, uv + vec2(halfPixel.x, -halfPixel.y));\n    sum += texture(inTexture, uv - vec2(halfPixel.x, -halfPixel.y));\n    fragColor = sum / 8.0;\n}";
      String kawaseUp = "#version 150 core\nuniform sampler2D inTexture;\nuniform vec2 uOffset, uHalfPixel;\nuniform vec3 color;\nin vec2 texCoord;\nout vec4 fragColor;\nvoid main() {\n    vec2 uv = texCoord;\n    vec2 halfPixel = uHalfPixel * uOffset;\n    vec4 sum = texture(inTexture, uv + vec2(-halfPixel.x * 2.0, 0.0));\n    sum += texture(inTexture, uv + vec2(-halfPixel.x, halfPixel.y)) * 2.0;\n    sum += texture(inTexture, uv + vec2(0.0, halfPixel.y * 2.0));\n    sum += texture(inTexture, uv + vec2(halfPixel.x, halfPixel.y)) * 2.0;\n    sum += texture(inTexture, uv + vec2(halfPixel.x * 2.0, 0.0));\n    sum += texture(inTexture, uv + vec2(halfPixel.x, -halfPixel.y)) * 2.0;\n    sum += texture(inTexture, uv + vec2(0.0, -halfPixel.y * 2.0));\n    sum += texture(inTexture, uv + vec2(-halfPixel.x, -halfPixel.y)) * 2.0;\n    fragColor = vec4((sum / 12.0).rgb * color, (sum / 12.0).a);\n}";
      String innerGlowFrag = "#version 150 core\nuniform sampler2D bloomTexture;\nuniform sampler2D maskTexture;\nuniform vec3 glowColor1;\nuniform vec3 glowColor2;\nuniform float alpha;\nuniform int useHandColor;\nin vec2 texCoord;\nout vec4 fragColor;\nvoid main() {\n    vec2 uv = texCoord;\n    vec4 bloom = texture(bloomTexture, uv);\n    vec4 mask = texture(maskTexture, uv);\n    if (mask.a < 0.01) discard;\n    vec3 gradientColor = (useHandColor == 1) ? mask.rgb : mix(glowColor1, glowColor2, uv.y);\n    float edgeGlow = 1.0 - bloom.a;\n    float totalIntensity = (0.5 + edgeGlow * 0.5) * mask.a;\n    fragColor = vec4(gradientColor, totalIntensity * alpha);\n}";
      String outerGlowFrag = "#version 150 core\nuniform sampler2D bloomTexture;\nuniform sampler2D maskTexture;\nuniform vec3 glowColor1;\nuniform vec3 glowColor2;\nuniform float alpha;\nuniform int useHandColor;\nin vec2 texCoord;\nout vec4 fragColor;\nvoid main() {\n    vec2 uv = texCoord;\n    vec4 bloom = texture(bloomTexture, uv);\n    vec4 mask = texture(maskTexture, uv);\n    float baseGlow = bloom.a * (1.0 - mask.a);\n    vec3 gradientColor = (useHandColor == 1) ? (bloom.rgb / max(bloom.a, 0.001)) : mix(glowColor1, glowColor2, uv.y);\n    fragColor = vec4(gradientColor, baseGlow * alpha);\n}";
      String blendFrag = "#version 150 core\nuniform sampler2D currentTexture;\nuniform sampler2D historyTexture;\nuniform float motionStrength;\nin vec2 texCoord;\nout vec4 fragColor;\nvoid main() {\n    vec4 current = texture(currentTexture, texCoord);\n    vec4 history = texture(historyTexture, texCoord);\n    vec4 fadedHistory = history * motionStrength;\n    if (fadedHistory.a < 0.02) {\n        fadedHistory = vec4(0.0);\n    }\n    fragColor = current + fadedHistory * (1.0 - current.a);\n}";
      this.baseProgram = this.createProgram(vert, baseFrag);
      this.kawaseDownProgram = this.createProgram(vert, kawaseDown);
      this.kawaseUpProgram = this.createProgram(vert, kawaseUp);
      this.innerGlowProgram = this.createProgram(vert, innerGlowFrag);
      this.outerGlowProgram = this.createProgram(vert, outerGlowFrag);
      this.blendProgram = this.createProgram(vert, blendFrag);
   }

   private int createProgram(String vert, String frag) {
      int v = GL20.glCreateShader(35633);
      GL20.glShaderSource(v, vert);
      GL20.glCompileShader(v);
      int f = GL20.glCreateShader(35632);
      GL20.glShaderSource(f, frag);
      GL20.glCompileShader(f);
      int p = GL20.glCreateProgram();
      GL20.glAttachShader(p, v);
      GL20.glAttachShader(p, f);
      GL20.glBindAttribLocation(p, 0, "Position");
      GL20.glBindAttribLocation(p, 1, "UV");
      GL20.glLinkProgram(p);
      return p;
   }

   public boolean shouldRender() {
      if (!this.isEnabled()) {
         return false;
      }

      if (this.mc.player == null) {
         return false;
      }

      ItemStack mainHand = this.mc.player.getMainHandItem();
      ItemStack offHand = this.mc.player.getOffhandItem();
      return !this.isMap(mainHand) && !this.isMap(offHand);
   }

   private boolean isMap(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
      return "map".equals(path) || "filled_map".equals(path);
   }
}
