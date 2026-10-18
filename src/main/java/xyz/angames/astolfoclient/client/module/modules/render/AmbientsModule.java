package xyz.angames.astolfoclient.client.module.modules.render;

import xyz.angames.astolfoclient.client.compat.GlCompat;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents.AfterEntities;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents.Last;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.pipeline.TextureTarget;
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
public class AmbientsModule extends Module {
   public final BooleanSetting sphereBlur = new BooleanSetting("Sphere Blur", false);
   public final NumberSetting sphereRadius = new NumberSetting("Sphere Radius", 30.0, 3.0, 150.0, 1.0) {
      @Override
      public boolean isVisible() {
         return AmbientsModule.this.sphereBlur.get();
      }
   };
   public final NumberSetting blurSmoothness = new NumberSetting("Blur Smoothness", 12.0, 1.0, 60.0, 1.0) {
      @Override
      public boolean isVisible() {
         return AmbientsModule.this.sphereBlur.get();
      }
   };
   public final NumberSetting blurStrength = new NumberSetting("Blur Strength", 12.0, 1.0, 50.0, 0.5) {
      @Override
      public boolean isVisible() {
         return AmbientsModule.this.sphereBlur.get();
      }
   };
   public final ModeSetting blurQuality = new ModeSetting("Blur Quality", "High", "Low", "Medium", "High", "Ultra") {
      @Override
      public boolean isVisible() {
         return AmbientsModule.this.sphereBlur.get();
      }
   };
   public final BooleanSetting smoothFollow = new BooleanSetting("Smooth Follow", true) {
      @Override
      public boolean isVisible() {
         return AmbientsModule.this.sphereBlur.get();
      }
   };
   public final NumberSetting blurFollowSpeed = new NumberSetting("Follow Speed", 8.0, 1.0, 25.0, 0.5) {
      @Override
      public boolean isVisible() {
         return AmbientsModule.this.sphereBlur.get() && AmbientsModule.this.smoothFollow.get();
      }
   };
   public final BooleanSetting blurSky = new BooleanSetting("Blur Sky", true) {
      @Override
      public boolean isVisible() {
         return AmbientsModule.this.sphereBlur.get();
      }
   };
   public final BooleanSetting blurThemeTint = new BooleanSetting("Theme Tint", false) {
      @Override
      public boolean isVisible() {
         return AmbientsModule.this.sphereBlur.get();
      }
   };
   public final NumberSetting blurThemeStrength = new NumberSetting("Tint Strength", 0.15, 0.0, 1.0, 0.05) {
      @Override
      public boolean isVisible() {
         return AmbientsModule.this.sphereBlur.get() && AmbientsModule.this.blurThemeTint.get();
      }
   };
   public final BooleanSetting customTime = new BooleanSetting("Custom Time", true);
   public final NumberSetting time = new NumberSetting("Time", 18000.0, 0.0, 24000.0, 100.0);
   public final BooleanSetting customSkybox = new BooleanSetting("Custom Skybox", true);
   public final ModeSetting skyboxMode = new ModeSetting("Mode", "Clouds", "Clouds", "Smoke", "Caustic", "Nebula", "Bright Clouds", "Cloud Space");
   public final NumberSetting skyboxStrength = new NumberSetting("Skybox Strength", 0.6, 0.0, 1.0, 0.05);
   public final BooleanSetting customFog = new BooleanSetting("Custom Fog", true);
   public final NumberSetting fogStart = new NumberSetting("Fog Start", 5.0, 0.0, 100.0, 1.0);
   public final NumberSetting fogEnd = new NumberSetting("Fog End", 50.0, 5.0, 200.0, 1.0);
   public final BooleanSetting fogColorEnabled = new BooleanSetting("Fog Color Enabled", true);
   public final BooleanSetting themeSync = new BooleanSetting("Theme Sync", true);
   public final NumberSetting fogStrength = new NumberSetting("Fog Color Strength", 0.6, 0.0, 1.0, 0.05);
   public final BooleanSetting fogShader = new BooleanSetting("Fog Shader", false);
   public final ModeSetting fogShaderMode = new ModeSetting("Fog Shader Mode", "Mist", "Mist", "Smoke", "Caustic", "Nebula", "Waves", "Abyss");
   public final NumberSetting fogShaderStrength = new NumberSetting("Fog Shader Strength", 0.6, 0.0, 1.0, 0.05);
   public final NumberSetting fogShaderSpeed = new NumberSetting("Fog Shader Speed", 1.0, 0.1, 5.0, 0.1);
   public final NumberSetting fogShaderDensity = new NumberSetting("Fog Shader Density", 1.0, 0.1, 3.0, 0.1);
   public final NumberSetting fogShaderHorizon = new NumberSetting("Fog Shader Horizon", 0.8, 0.0, 2.0, 0.05);
   public final BooleanSetting fogShaderThemeSync = new BooleanSetting("Fog Shader Theme", true);
   public final NumberSetting blockBrightness = new NumberSetting("Block Brightness", 1.0, 0.0, 1.0, 0.01);
   public final NumberSetting saturation = new NumberSetting("Saturation", 1.0, 0.0, 2.0, 0.05);
   private int cloudsProgram = -1;
   private int smokeProgram = -1;
   private int causticProgram = -1;
   private int nebulaProgram = -1;
   private int radiantProgram = -1;
   private int sunsetProgram = -1;
   private int texturedProgram = -1;
   private int fogMistProgram = -1;
   private int fogSmokeProgram = -1;
   private int fogCausticProgram = -1;
   private int fogNebulaProgram = -1;
   private int fogWavesProgram = -1;
   private int fogAbyssProgram = -1;
   private int skyboxVao = -1;
   private int skyboxVbo = -1;
   private int texturedSkyboxVao = -1;
   private int texturedSkyboxVbo = -1;
   private int blackProgram = -1;
   private int sphereBlurProgram = -1;
   private int sphereBlurVao = -1;
   private int sphereBlurVbo = -1;
   private RenderTarget sphereBlurFbo = null;
   private Vec3 smoothSphereCenter = null;
   private long lastSphereUpdateTime = 0L;
   private RenderTarget saturationFbo = null;
   private int saturationProgram = -1;
   private int saturationVao = -1;
   private int saturationVbo = -1;

   public AmbientsModule() {
      super("Ambients", Module.Category.RENDER);
      this.addSetting(this.customTime);
      this.addSetting(this.time);
      this.addSetting(this.customSkybox);
      this.addSetting(this.skyboxMode);
      this.addSetting(this.skyboxStrength);
      this.addSetting(this.customFog);
      this.addSetting(this.fogStart);
      this.addSetting(this.fogEnd);
      this.addSetting(this.fogColorEnabled);
      this.addSetting(this.themeSync);
      this.addSetting(this.fogStrength);
      this.addSetting(this.fogShader);
      this.addSetting(this.fogShaderMode);
      this.addSetting(this.fogShaderStrength);
      this.addSetting(this.fogShaderSpeed);
      this.addSetting(this.fogShaderDensity);
      this.addSetting(this.fogShaderHorizon);
      this.addSetting(this.fogShaderThemeSync);
      this.addSetting(this.blockBrightness);
      this.addSetting(this.saturation);
      this.addSetting(this.sphereBlur);
      this.addSetting(this.sphereRadius);
      this.addSetting(this.blurSmoothness);
      this.addSetting(this.blurStrength);
      this.addSetting(this.blurQuality);
      this.addSetting(this.smoothFollow);
      this.addSetting(this.blurFollowSpeed);
      this.addSetting(this.blurSky);
      this.addSetting(this.blurThemeTint);
      this.addSetting(this.blurThemeStrength);
      WorldRenderEvents.AFTER_ENTITIES.register((AfterEntities)context -> {
         if (this.isEnabled() && this.customSkybox.get()) {
            this.renderShaderSkybox(context);
         }
      });
      WorldRenderEvents.AFTER_ENTITIES.register((AfterEntities)context -> {
         if (this.isEnabled() && this.fogShader.get()) {
            this.renderShaderFog(context);
         }
      });
      WorldRenderEvents.AFTER_ENTITIES.register((AfterEntities)context -> {
         if (this.isEnabled() && this.blockBrightness.get() < 1.0) {
            this.renderBlackShader(context);
         }
      });
      WorldRenderEvents.LAST.register((Last)context -> {
         if (this.isEnabled() && this.sphereBlur.get()) {
            this.renderSphereBlur(context);
         }
      });
      WorldRenderEvents.LAST.register((Last)context -> {
         if (this.isEnabled() && Math.abs(this.saturation.get() - 1.0) > 0.001) {
            this.renderSaturation(context);
         }
      });
   }

   private void initSkyboxGeometry() {
      if (this.skyboxVao == -1) {
         float[] vertices = new float[]{
            -1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            1.0F
         };
         int prevVao = GL11.glGetInteger(34229);
         int prevVbo = GL11.glGetInteger(34964);
         this.skyboxVao = GL30.glGenVertexArrays();
         this.skyboxVbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.skyboxVao);
         GL15.glBindBuffer(34962, this.skyboxVbo);
         FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
         buffer.put(vertices).flip();
         GL15.glBufferData(34962, buffer, 35044);
         GL20.glVertexAttribPointer(0, 3, 5126, false, 12, 0L);
         GL20.glEnableVertexAttribArray(0);
         GL15.glBindBuffer(34962, prevVbo);
         GL30.glBindVertexArray(prevVao);
      }
   }

   private void initTexturedSkyboxGeometry() {
      if (this.texturedSkyboxVao == -1) {
         float size = 100.0F;
         float[] vertices = new float[180];
         int index = 0;
         int[] faces = new int[]{1, 0, 2, 4, 3, 5};
         float[][] coords = new float[][]{
            {-size, -size, size, -size, -size, -size, size, -size, -size, size, -size, size},
            {-size, size, -size, -size, size, size, size, size, size, size, size, -size},
            {size, size, -size, size, -size, -size, -size, -size, -size, -size, size, -size},
            {-size, size, -size, -size, -size, -size, -size, -size, size, -size, size, size},
            {-size, size, size, -size, -size, size, size, -size, size, size, size, size},
            {size, size, size, size, -size, size, size, -size, -size, size, size, -size}
         };

         for (int face : faces) {
            float[] c = coords[face];
            int col = face % 3;
            int row = face / 3;
            float u1 = col / 3.0F;
            float u2 = (col + 1) / 3.0F;
            float v1 = row / 2.0F;
            float v2 = (row + 1) / 2.0F;
            float x1 = c[0];
            float y1 = c[1];
            float z1 = c[2];
            float x2 = c[3];
            float y2 = c[4];
            float z2 = c[5];
            float x3 = c[6];
            float y3 = c[7];
            float z3 = c[8];
            float x4 = c[9];
            float y4 = c[10];
            float z4 = c[11];
            vertices[index++] = x1;
            vertices[index++] = y1;
            vertices[index++] = z1;
            vertices[index++] = u1;
            vertices[index++] = v1;
            vertices[index++] = x2;
            vertices[index++] = y2;
            vertices[index++] = z2;
            vertices[index++] = u1;
            vertices[index++] = v2;
            vertices[index++] = x3;
            vertices[index++] = y3;
            vertices[index++] = z3;
            vertices[index++] = u2;
            vertices[index++] = v2;
            vertices[index++] = x1;
            vertices[index++] = y1;
            vertices[index++] = z1;
            vertices[index++] = u1;
            vertices[index++] = v1;
            vertices[index++] = x3;
            vertices[index++] = y3;
            vertices[index++] = z3;
            vertices[index++] = u2;
            vertices[index++] = v2;
            vertices[index++] = x4;
            vertices[index++] = y4;
            vertices[index++] = z4;
            vertices[index++] = u2;
            vertices[index++] = v1;
         }

         int prevVao = GL11.glGetInteger(34229);
         int prevVbo = GL11.glGetInteger(34964);
         this.texturedSkyboxVao = GL30.glGenVertexArrays();
         this.texturedSkyboxVbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.texturedSkyboxVao);
         GL15.glBindBuffer(34962, this.texturedSkyboxVbo);
         FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
         buffer.put(vertices).flip();
         GL15.glBufferData(34962, buffer, 35044);
         GL20.glVertexAttribPointer(0, 3, 5126, false, 20, 0L);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(1, 2, 5126, false, 20, 12L);
         GL20.glEnableVertexAttribArray(1);
         GL15.glBindBuffer(34962, prevVbo);
         GL30.glBindVertexArray(prevVao);
      }
   }

   private String loadShaderSource(String path) {
      try (
         InputStream is = Minecraft.getInstance().getResourceManager().open(Identifier.fromNamespaceAndPath("astolfoclient", path));
         BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      ) {
         return reader.lines().collect(Collectors.joining("\n"));
      } catch (Exception e) {
         e.printStackTrace();
         return "";
      }
   }

   private void initCloudsShader() {
      if (this.cloudsProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 lookDir;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    lookDir = Position;\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = "#version 150 core\nin vec3 lookDir;\nout vec4 fragColor;\nuniform float time;\nuniform vec3 themeColor;\nuniform float alpha;\n\nfloat hash3(vec3 p) {\n    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);\n}\n\nfloat noise3(vec3 p) {\n    vec3 i = floor(p);\n    vec3 f = fract(p);\n    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);\n    return mix(\n        mix(mix(hash3(i + vec3(0.0,0.0,0.0)), hash3(i + vec3(1.0,0.0,0.0)), u.x),\n            mix(hash3(i + vec3(0.0,1.0,0.0)), hash3(i + vec3(1.0,1.0,0.0)), u.x), u.y),\n        mix(mix(hash3(i + vec3(0.0,0.0,1.0)), hash3(i + vec3(1.0,0.0,1.0)), u.x),\n            mix(hash3(i + vec3(0.0,1.0,1.0)), hash3(i + vec3(1.0,1.0,1.0)), u.x), u.y), u.z);\n}\n\nfloat fbm3(vec3 p) {\n    float value = 0.0;\n    float amplitude = 0.5;\n    vec3 shift = vec3(100.0);\n    mat3 rot = mat3(0.00,  0.80,  0.60,\n                    -0.80,  0.36, -0.48,\n                    -0.60, -0.48,  0.64);\n    for (int i = 0; i < 5; i++) {\n        value += amplitude * noise3(p);\n        p = rot * p * 2.2 + shift;\n        amplitude *= 0.5;\n    }\n    return value;\n}\n\nvoid main() {\n    vec3 dir = normalize(lookDir);\n\n    vec3 skyTop    = themeColor * 0.15;\n    vec3 skyBottom = themeColor * 0.02;\n    vec3 skyColor  = mix(skyBottom, skyTop, smoothstep(-1.0, 1.0, dir.y));\n\n    vec3 wind = vec3(time * 0.015, time * 0.008, time * 0.01);\n    vec3 q = vec3(fbm3(dir * 3.6 + wind), fbm3(dir * 2.4 - wind), fbm3(dir * 3.0 + wind * 0.5));\n    vec3 r = vec3(fbm3(dir * 4.5 + q + vec3(1.2, 4.3, 2.8) + wind * 0.5), \n                  fbm3(dir * 3.9 + q + vec3(7.4, 2.9, 1.5) + wind * 0.3),\n                  fbm3(dir * 3.3 + q + vec3(3.1, 8.6, 5.2) + wind * 0.4));\n    float cloudDensity = fbm3(dir * 5.4 + r);\n\n    vec3 crimsonGlow  = themeColor;\n    vec3 magmaHotSpot = clamp(themeColor * 1.3 + vec3(0.1, 0.1, 0.0), 0.0, 1.0);\n    vec3 darkSmoke    = themeColor * 0.12;\n\n    float horizonBand = exp(-abs(dir.y - 0.1) * 1.5);\n    vec3 cloudColor = mix(darkSmoke, crimsonGlow, smoothstep(0.15, 0.6, cloudDensity));\n    cloudColor += magmaHotSpot * pow(max(0.0, cloudDensity), 3.0) * 1.2;\n    cloudColor += crimsonGlow * horizonBand * 0.5;\n\n    float cloudAlpha = smoothstep(0.2, 0.65, cloudDensity);\n    vec3 finalSky = mix(skyColor, cloudColor, cloudAlpha * 0.9);\n    finalSky += crimsonGlow * (pow(max(0.0, cloudDensity), 2.0) * 0.25 + horizonBand * 0.3);\n\n    float horizonMist = exp(-abs(dir.y + 0.4) * 2.5);\n    finalSky = mix(finalSky, themeColor * 0.4, horizonMist * 0.4);\n\n    vec3 starGrid = dir * 60.0;\n    vec3 gridId = floor(starGrid);\n    vec3 gridFract = fract(starGrid) - 0.5;\n    float starRand = hash3(gridId);\n    float starDot = smoothstep(0.25, 0.0, length(gridFract)) * step(0.992, starRand);\n    starDot *= sin(time * 2.5 + starRand * 6.28) * 0.5 + 0.5;\n    starDot *= smoothstep(-0.1, 0.6, dir.y);\n    starDot *= (1.0 - cloudAlpha * 0.85);\n    finalSky += vec3(1.0, 0.9, 0.9) * starDot * 1.5;\n\n    finalSky = 1.0 - exp(-finalSky * 1.5);\n    fragColor = vec4(finalSky, alpha);\n}";
         this.cloudsProgram = this.createProgram(vert, frag);
      }
   }

   private void initSmokeShader() {
      if (this.smokeProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 lookDir;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    lookDir = Position;\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = "#version 150 core\nin vec3 lookDir;\nout vec4 fragColor;\nuniform float time;\nuniform vec3 themeColor;\nuniform float alpha;\n\nvec3 safe_tanh(vec3 x) {\n    return 1.0 - 2.0 / (exp(2.0 * x) + 1.0);\n}\n\nfloat orb(vec3 p) {\n    float t = time * 4.0;\n    return length(p - vec3(\n            sin(sin(t * 0.2) + t * 0.4) * 6.0,\n            12.0 + time + cos(t * 0.3) * 8.0,\n            1.0 + sin(sin(t * 0.5) + t * 0.2) * 4.0));\n}\n\nvoid main() {\n    vec3 dir = normalize(lookDir);\n    float d = 0.0;\n    float e = 0.0;\n    float s = 0.0;\n    float t = time;\n    vec3 o = vec3(0.0);\n    \n    for (int i = 0; i < 72; i++) {\n        vec3 p = dir * d + vec3(0.0, t, 0.0);\n        e = orb(p) - 0.1;\n        \n        vec4 c = cos(0.1 * t + p.y / 8.0 + vec4(0.0, 33.0, 11.0, 0.0));\n        p.xz *= mat2(c.x, c.y, c.z, c.w);\n        s = 4.0 - abs(p.z);\n        \n        float a = 0.8;\n        for (int j = 0; j < 4; j++) {\n            p += cos(0.7 * t + p.yzx) * 0.2;\n            s -= abs(dot(sin(0.1 * t + p * a), vec3(0.6))) / a;\n            a += a;\n        }\n        \n        e = max(0.5 * e, 0.01);\n        s = min(0.03 + 0.2 * abs(s), e);\n        d += s;\n        o += themeColor * (1.0 / (s + e * 3.0));\n        \n        if (max(o.x, max(o.y, o.z)) > 25.0) break;\n    }\n    \n    o = safe_tanh(o / 10.0);\n    fragColor = vec4(o, alpha);\n}";
         this.smokeProgram = this.createProgram(vert, frag);
      }
   }

   private void initCausticShader() {
      if (this.causticProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 Direction;\nout vec4 vColor;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    Direction = Position;\n    vColor = vec4(1.0);\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = this.loadShaderSource("shaders/core/sky_caustic/fragment.fsh");
         if (!frag.isEmpty()) {
            this.causticProgram = this.createProgram(vert, frag);
         }
      }
   }

   private void initNebulaShader() {
      if (this.nebulaProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 Direction;\nout vec4 vColor;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    Direction = Position;\n    vColor = vec4(1.0);\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = this.loadShaderSource("shaders/core/sky_nebula/fragment.fsh");
         if (!frag.isEmpty()) {
            this.nebulaProgram = this.createProgram(vert, frag);
         }
      }
   }

   private void initRadiantShader() {
      if (this.radiantProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 Direction;\nout vec4 vColor;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    Direction = Position;\n    vColor = vec4(1.0);\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = this.loadShaderSource("shaders/core/sky_radiant/fragment.fsh");
         if (!frag.isEmpty()) {
            this.radiantProgram = this.createProgram(vert, frag);
         }
      }
   }

   private void initSunsetShader() {
      if (this.sunsetProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 Direction;\nout vec4 vColor;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    Direction = Position;\n    vColor = vec4(1.0);\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = this.loadShaderSource("shaders/core/sky_sunset/fragment.fsh");
         if (!frag.isEmpty()) {
            this.sunsetProgram = this.createProgram(vert, frag);
         }
      }
   }

   private void initTexturedShader() {
      if (this.texturedProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nin vec2 TexCoord;\nout vec2 vTexCoord;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    vTexCoord = TexCoord;\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = "#version 150 core\nin vec2 vTexCoord;\nout vec4 fragColor;\nuniform sampler2D uTexture;\nuniform float alpha;\nuniform vec3 themeColor;\nuniform bool themeSync;\nvoid main() {\n    vec4 col = texture(uTexture, vTexCoord);\n    if (themeSync) {\n        col.rgb *= themeColor;\n    }\n    fragColor = vec4(col.rgb, col.a * alpha);\n}";
         this.texturedProgram = this.createProgram(vert, frag);
      }
   }

   private int createProgram(String vert, String frag) {
      int v = GL20.glCreateShader(35633);
      GL20.glShaderSource(v, vert);
      GL20.glCompileShader(v);
      if (GL20.glGetShaderi(v, 35713) == 0) {
         System.err.println("Vertex shader compile error:\n" + GL20.glGetShaderInfoLog(v, 1024));
      }

      int f = GL20.glCreateShader(35632);
      GL20.glShaderSource(f, frag);
      GL20.glCompileShader(f);
      if (GL20.glGetShaderi(f, 35713) == 0) {
         System.err.println("Fragment shader compile error:\n" + GL20.glGetShaderInfoLog(f, 1024));
      }

      int p = GL20.glCreateProgram();
      GL20.glAttachShader(p, v);
      GL20.glAttachShader(p, f);
      GL20.glBindAttribLocation(p, 0, "Position");
      GL20.glBindAttribLocation(p, 1, "TexCoord");
      GL20.glBindAttribLocation(p, 1, "UV0");
      GL20.glLinkProgram(p);
      if (GL20.glGetProgrami(p, 35714) == 0) {
         System.err.println("Shader program link error:\n" + GL20.glGetProgramInfoLog(p, 1024));
      }

      return p;
   }

   private void renderShaderSkybox(WorldRenderContext context) {
      if (context.matrixStack() != null) {
         int program = -1;
         boolean isTextureMode = false;
         Identifier textureId = null;
         String mode = this.skyboxMode.get();
         if (mode.equals("Smoke")) {
            this.initSmokeShader();
            program = this.smokeProgram;
         } else if (mode.equals("Clouds")) {
            this.initCloudsShader();
            program = this.cloudsProgram;
         } else if (mode.equals("Caustic")) {
            this.initCausticShader();
            program = this.causticProgram;
         } else if (mode.equals("Nebula")) {
            this.initNebulaShader();
            program = this.nebulaProgram;
         } else if (mode.equals("Radiant")) {
            this.initRadiantShader();
            program = this.radiantProgram;
         } else if (mode.equals("Sunset")) {
            this.initSunsetShader();
            program = this.sunsetProgram;
         } else {
            isTextureMode = true;
            this.initTexturedShader();
            this.initTexturedSkyboxGeometry();
            program = this.texturedProgram;
            int textureIndex = 1;
            if (mode.equals("Bright Clouds")) {
               textureIndex = 1;
            } else if (mode.equals("Lake")) {
               textureIndex = 2;
            } else if (mode.equals("Cloud Space")) {
               textureIndex = 3;
            } else if (mode.equals("Clear Evening")) {
               textureIndex = 4;
            } else if (mode.equals("Underwater")) {
               textureIndex = 5;
            }

            textureId = Identifier.fromNamespaceAndPath("astolfoclient", "sky/" + textureIndex + ".png");
         }

         if (program != -1) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
               int prevProgram = GL11.glGetInteger(35725);
               int prevVao = GL11.glGetInteger(34229);
               int prevVbo = GL11.glGetInteger(34964);
               boolean prevDepthTest = GL11.glIsEnabled(2929);
               boolean prevCull = GL11.glIsEnabled(2884);
               boolean prevBlend = GL11.glIsEnabled(3042);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableCull();
               RenderSystem.depthMask(false);
               RenderSystem.enableDepthTest();
               GL20.glUseProgram(program);
               Matrix4f modelViewMat = new Matrix4f().rotation(new Quaternionf(context.camera().rotation()).conjugate());
               Matrix4f projMat = context.projectionMatrix();
               float[] modelViewArr = new float[16];
               modelViewMat.get(modelViewArr);
               float[] projArr = new float[16];
               projMat.get(projArr);
               int modelViewLoc = GL20.glGetUniformLocation(program, "uModelViewMat");
               int projLoc = GL20.glGetUniformLocation(program, "uProjMat");
               GL20.glUniformMatrix4fv(modelViewLoc, false, modelViewArr);
               GL20.glUniformMatrix4fv(projLoc, false, projArr);
               float timeVal = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
               Color themeColor = new Color(ThemeManager.getThemedColor(0L));
               float r = themeColor.getRed() / 255.0F;
               float g = themeColor.getGreen() / 255.0F;
               float b = themeColor.getBlue() / 255.0F;
               float strength = (float)this.skyboxStrength.get();
               if (isTextureMode) {
                  if (textureId != null) {
                     RenderSystem.setShaderTexture(0, textureId);
                     AbstractTexture texture = mc.getTextureManager().getTexture(textureId);
                     int glId = texture != null ? GlCompat.glId(texture.getTexture()) : 0;
                     RenderSystem.activeTexture(33984);
                     GL11.glBindTexture(3553, glId);
                  }

                  int texLoc = GL20.glGetUniformLocation(program, "uTexture");
                  int alphaLoc = GL20.glGetUniformLocation(program, "alpha");
                  int themeColorLoc = GL20.glGetUniformLocation(program, "themeColor");
                  int themeSyncLoc = GL20.glGetUniformLocation(program, "themeSync");
                  if (texLoc != -1) {
                     GL20.glUniform1i(texLoc, 0);
                  }

                  if (alphaLoc != -1) {
                     GL20.glUniform1f(alphaLoc, strength);
                  }

                  if (themeColorLoc != -1) {
                     GL20.glUniform3f(themeColorLoc, r, g, b);
                  }

                  if (themeSyncLoc != -1) {
                     GL20.glUniform1i(themeSyncLoc, this.themeSync.get() ? 1 : 0);
                  }

                  GL30.glBindVertexArray(this.texturedSkyboxVao);
                  GL11.glDrawArrays(4, 0, 36);
               } else {
                  if (!mode.equals("Clouds") && !mode.equals("Smoke")) {
                     int timeLoc = GL20.glGetUniformLocation(program, "Time");
                     int accentLoc = GL20.glGetUniformLocation(program, "Accent");
                     int colorModulatorLoc = GL20.glGetUniformLocation(program, "ColorModulator");
                     if (timeLoc != -1) {
                        GL20.glUniform1f(timeLoc, timeVal);
                     }

                     if (accentLoc != -1) {
                        GL20.glUniform3f(accentLoc, r, g, b);
                     }

                     if (colorModulatorLoc != -1) {
                        GL20.glUniform4f(colorModulatorLoc, 1.0F, 1.0F, 1.0F, strength);
                     }
                  } else {
                     int timeLoc = GL20.glGetUniformLocation(program, "time");
                     int themeColorLoc = GL20.glGetUniformLocation(program, "themeColor");
                     int alphaLoc = GL20.glGetUniformLocation(program, "alpha");
                     if (timeLoc != -1) {
                        GL20.glUniform1f(timeLoc, timeVal);
                     }

                     if (themeColorLoc != -1) {
                        GL20.glUniform3f(themeColorLoc, r, g, b);
                     }

                     if (alphaLoc != -1) {
                        GL20.glUniform1f(alphaLoc, strength);
                     }
                  }

                  this.initSkyboxGeometry();
                  GL30.glBindVertexArray(this.skyboxVao);
                  GL11.glDrawArrays(4, 0, 36);
               }

               GL30.glBindVertexArray(prevVao);
               GL15.glBindBuffer(34962, prevVbo);
               GL20.glUseProgram(prevProgram);
               if (prevDepthTest) {
                  RenderSystem.enableDepthTest();
               } else {
                  RenderSystem.disableDepthTest();
               }

               if (prevCull) {
                  RenderSystem.enableCull();
               } else {
                  RenderSystem.disableCull();
               }

               if (!prevBlend) {
                  RenderSystem.disableBlend();
               }

               RenderSystem.depthMask(true);
            }
         }
      }
   }

   private void initBlackShader() {
      if (this.blackProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = pos.xyww;\n}";
         String frag = "#version 150 core\nout vec4 fragColor;\nuniform float alpha;\nvoid main() {\n    fragColor = vec4(0.0, 0.0, 0.0, alpha);\n}";
         this.blackProgram = this.createProgram(vert, frag);
      }
   }

   private void renderBlackShader(WorldRenderContext context) {
      if (context.matrixStack() != null) {
         this.initSkyboxGeometry();
         this.initBlackShader();
         if (this.blackProgram != -1) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
               int prevProgram = GL11.glGetInteger(35725);
               int prevVao = GL11.glGetInteger(34229);
               int prevVbo = GL11.glGetInteger(34964);
               int prevDepthFunc = GL11.glGetInteger(2932);
               boolean prevDepthTest = GL11.glIsEnabled(2929);
               boolean prevCull = GL11.glIsEnabled(2884);
               boolean prevBlend = GL11.glIsEnabled(3042);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableCull();
               RenderSystem.depthMask(false);
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(516);
               GL20.glUseProgram(this.blackProgram);
               Matrix4f modelViewMat = new Matrix4f().rotation(new Quaternionf(context.camera().rotation()).conjugate());
               Matrix4f projMat = context.projectionMatrix();
               float[] modelViewArr = new float[16];
               modelViewMat.get(modelViewArr);
               float[] projArr = new float[16];
               projMat.get(projArr);
               int modelViewLoc = GL20.glGetUniformLocation(this.blackProgram, "uModelViewMat");
               int projLoc = GL20.glGetUniformLocation(this.blackProgram, "uProjMat");
               GL20.glUniformMatrix4fv(modelViewLoc, false, modelViewArr);
               GL20.glUniformMatrix4fv(projLoc, false, projArr);
               float strength = 1.0F - (float)this.blockBrightness.get();
               int alphaLoc = GL20.glGetUniformLocation(this.blackProgram, "alpha");
               GL20.glUniform1f(alphaLoc, strength);
               GL30.glBindVertexArray(this.skyboxVao);
               GL11.glDrawArrays(4, 0, 36);
               GL30.glBindVertexArray(prevVao);
               GL15.glBindBuffer(34962, prevVbo);
               GL20.glUseProgram(prevProgram);
               RenderSystem.depthFunc(prevDepthFunc);
               if (prevDepthTest) {
                  RenderSystem.enableDepthTest();
               } else {
                  RenderSystem.disableDepthTest();
               }

               if (prevCull) {
                  RenderSystem.enableCull();
               } else {
                  RenderSystem.disableCull();
               }

               if (!prevBlend) {
                  RenderSystem.disableBlend();
               }

               RenderSystem.depthMask(true);
            }
         }
      }
   }

   private void initFogMistShader() {
      if (this.fogMistProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 lookDir;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    lookDir = Position;\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = "#version 150 core\nin vec3 lookDir;\nout vec4 fragColor;\nuniform float time;\nuniform vec3 themeColor;\nuniform float alpha;\nuniform float speed;\nuniform float density;\nuniform float horizon;\n\nfloat hash3(vec3 p) {\n    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);\n}\n\nfloat noise3(vec3 p) {\n    vec3 i = floor(p);\n    vec3 f = fract(p);\n    vec3 u = f * f * (3.0 - 2.0 * f);\n    return mix(\n        mix(mix(hash3(i + vec3(0.0,0.0,0.0)), hash3(i + vec3(1.0,0.0,0.0)), u.x),\n            mix(hash3(i + vec3(0.0,1.0,0.0)), hash3(i + vec3(1.0,1.0,0.0)), u.x), u.y),\n        mix(mix(hash3(i + vec3(0.0,0.0,1.0)), hash3(i + vec3(1.0,0.0,1.0)), u.x),\n            mix(hash3(i + vec3(0.0,1.0,1.0)), hash3(i + vec3(1.0,1.0,1.0)), u.x), u.y), u.z);\n}\n\nfloat fbm3(vec3 p) {\n    float v = 0.0;\n    float a = 0.5;\n    mat3 rot = mat3(0.00,  0.80,  0.60,\n                    -0.80,  0.36, -0.48,\n                    -0.60, -0.48,  0.64);\n    for (int i = 0; i < 4; i++) {\n        v += a * noise3(p);\n        p = rot * p * 2.1 + vec3(1.2, 3.4, 5.6);\n        a *= 0.5;\n    }\n    return v;\n}\n\nvoid main() {\n    vec3 dir = normalize(lookDir);\n    float t = time * 0.25 * speed;\n    float hBand = exp(-abs(dir.y * 3.0) * max(0.2, (2.2 - horizon * 0.8)));\n    float ground = smoothstep(0.3, -0.6, dir.y) * 0.6;\n    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;\n    vec3 wind = vec3(t * 0.15, t * 0.05, t * 0.1);\n    vec3 p = dir * 4.0 + wind;\n    vec3 q = vec3(fbm3(p + wind * 0.5), fbm3(p - wind * 0.3), fbm3(p + vec3(2.1, 4.3, 1.7)));\n    float mistFbm = fbm3(p + q * 1.8);\n    vec3 baseColor = themeColor * 0.35 + vec3(0.02, 0.02, 0.03);\n    vec3 glowColor = themeColor * 1.2 + vec3(0.1);\n    vec3 finalColor = mix(baseColor, glowColor, smoothstep(0.3, 0.8, mistFbm));\n    float mistAlpha = smoothstep(0.15, 0.75, mistFbm) * envelope * alpha;\n    fragColor = vec4(finalColor, clamp(mistAlpha, 0.0, 1.0));\n}";
         this.fogMistProgram = this.createProgram(vert, frag);
      }
   }

   private void initFogSmokeShader() {
      if (this.fogSmokeProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 lookDir;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    lookDir = Position;\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = "#version 150 core\nin vec3 lookDir;\nout vec4 fragColor;\nuniform float time;\nuniform vec3 themeColor;\nuniform float alpha;\nuniform float speed;\nuniform float density;\nuniform float horizon;\n\nvec3 safe_tanh(vec3 x) {\n    return 1.0 - 2.0 / (exp(2.0 * x) + 1.0);\n}\n\nfloat orb(vec3 p, float t) {\n    return length(p - vec3(\n            sin(t * 0.3) * 4.0,\n            cos(t * 0.25) * 3.0,\n            sin(t * 0.4) * 4.0));\n}\n\nvoid main() {\n    vec3 dir = normalize(lookDir);\n    float t = time * 0.8 * speed;\n    float hBand = exp(-abs(dir.y * 2.8) * max(0.2, (2.0 - horizon * 0.7)));\n    float ground = smoothstep(0.2, -0.5, dir.y) * 0.5;\n    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;\n    float d = 0.0;\n    float e = 0.0;\n    float s = 0.0;\n    vec3 o = vec3(0.0);\n    for (int i = 0; i < 36; i++) {\n        vec3 p = dir * d + vec3(0.0, t * 0.5, 0.0);\n        e = orb(p, t) - 0.1;\n        vec4 c = cos(0.15 * t + p.y * 0.2 + vec4(0.0, 33.0, 11.0, 0.0));\n        p.xz = mat2(c.x, c.y, c.z, c.w) * p.xz;\n        s = 3.0 - abs(p.z);\n        float a = 0.7;\n        for (int j = 0; j < 3; j++) {\n            p += cos(0.6 * t + p.yzx) * 0.25;\n            s -= abs(dot(sin(0.12 * t + p * a), vec3(0.6))) / a;\n            a += a;\n        }\n        e = max(0.5 * e, 0.02);\n        s = min(0.05 + 0.2 * abs(s), e);\n        d += s;\n        o += themeColor * (1.0 / (s + e * 4.0));\n        if (max(o.x, max(o.y, o.z)) > 18.0) break;\n    }\n    o = safe_tanh(o * 0.06);\n    float smokeAlpha = length(o) * 0.6 * envelope * alpha;\n    fragColor = vec4(o, clamp(smokeAlpha, 0.0, 1.0));\n}";
         this.fogSmokeProgram = this.createProgram(vert, frag);
      }
   }

   private void initFogCausticShader() {
      if (this.fogCausticProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 lookDir;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    lookDir = Position;\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = "#version 150 core\nin vec3 lookDir;\nout vec4 fragColor;\nuniform float time;\nuniform vec3 themeColor;\nuniform float alpha;\nuniform float speed;\nuniform float density;\nuniform float horizon;\n\nfloat caustic2D(vec2 uv, float t) {\n    vec2 p = mod(uv * 6.28318, 6.28318) - 250.0;\n    vec2 i = p;\n    float c = 1.0;\n    float inten = 0.006;\n    for (int n = 0; n < 5; n++) {\n        float tn = t * 0.5 + float(n);\n        i = p + vec2(cos(tn - i.x) + sin(tn + i.y),\n                     sin(tn - i.y) + cos(tn + i.x));\n        c += 1.0 / length(vec2(p.x / (sin(i.x + tn) / inten),\n                               p.y / (cos(i.y + tn) / inten)));\n    }\n    c /= 5.0;\n    c = 1.17 - pow(c, 1.4);\n    return clamp(pow(abs(c), 6.0), 0.0, 4.0);\n}";
         frag = frag
            + "\nvoid main() {\n    vec3 dir = normalize(lookDir);\n    float t = time * 0.4 * speed;\n    float hBand = exp(-abs(dir.y * 2.5) * max(0.2, (2.0 - horizon * 0.7)));\n    float ground = smoothstep(0.1, -0.7, dir.y) * 0.7;\n    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;\n    vec2 uv = dir.xz / (abs(dir.y) + 0.35) * 0.7;\n    float c1 = caustic2D(uv + vec2(t * 0.05, 0.0), t);\n    float c2 = caustic2D(uv * 1.4 + vec2(5.2, 3.1) - vec2(0.0, t * 0.04), t * 0.8 + 2.0) * 0.6;\n    float c = c1 + c2;\n    vec3 deep = themeColor * 0.25;\n    vec3 bright = mix(themeColor, vec3(1.0), 0.6);\n    vec3 color = mix(deep, bright, smoothstep(0.2, 1.5, c)) + bright * pow(max(c - 0.8, 0.0), 2.0) * 0.5;\n    float causticAlpha = (0.2 + smoothstep(0.1, 1.2, c) * 0.8) * envelope * alpha;\n    fragColor = vec4(color, clamp(causticAlpha, 0.0, 1.0));\n}";
         this.fogCausticProgram = this.createProgram(vert, frag);
      }
   }

   private void initFogNebulaShader() {
      if (this.fogNebulaProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 lookDir;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    lookDir = Position;\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = "#version 150 core\nin vec3 lookDir;\nout vec4 fragColor;\nuniform float time;\nuniform vec3 themeColor;\nuniform float alpha;\nuniform float speed;\nuniform float density;\nuniform float horizon;\n\nfloat hash13(vec3 p) {\n    p = fract(p * vec3(443.8975, 397.2973, 491.1871));\n    p += dot(p, p.yxz + 19.19);\n    return fract((p.x + p.y) * p.z);\n}\n\nfloat noise(vec3 x) {\n    vec3 i = floor(x);\n    vec3 f = fract(x);\n    f = f * f * (3.0 - 2.0 * f);\n    float a = mix(hash13(i + vec3(0.0, 0.0, 0.0)), hash13(i + vec3(1.0, 0.0, 0.0)), f.x);\n    float b = mix(hash13(i + vec3(0.0, 1.0, 0.0)), hash13(i + vec3(1.0, 1.0, 0.0)), f.x);\n    float c = mix(hash13(i + vec3(0.0, 0.0, 1.0)), hash13(i + vec3(1.0, 0.0, 1.0)), f.x);\n    float d = mix(hash13(i + vec3(0.0, 1.0, 1.0)), hash13(i + vec3(1.0, 1.0, 1.0)), f.x);\n    return mix(mix(a, b, f.y), mix(c, d, f.y), f.z);\n}\n\nfloat fbm3(vec3 p) {\n    float v = 0.0;\n    float a = 0.55;\n    for (int i = 0; i < 4; i++) {\n        v += noise(p) * a;\n        p = p * 2.05 + vec3(3.1, 7.4, 2.8);\n        a *= 0.5;\n    }\n    return v;\n}\n\nvoid main() {\n    vec3 dir = normalize(lookDir);\n    float t = time * 0.2 * speed;\n    float hBand = exp(-abs(dir.y * 2.2) * max(0.2, (1.8 - horizon * 0.6)));\n    float ground = smoothstep(0.2, -0.6, dir.y) * 0.5;\n    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;\n    vec3 p = dir * 3.5 + vec3(t * 0.1, t * 0.03, -t * 0.07);\n    float clouds = fbm3(p);\n    float filaments = fbm3(p * 2.2 + vec3(-t * 0.05, t * 0.08, 0.0));\n    float neb = smoothstep(0.35, 0.85, clouds) * 0.75 + smoothstep(0.45, 0.9, filaments) * 0.35;\n    vec3 warm = mix(themeColor, vec3(1.0, 0.6, 0.3), 0.35);\n    vec3 cool = mix(themeColor * 0.6, vec3(0.2, 0.5, 1.0), 0.4);\n    vec3 nebCol = mix(cool, warm, smoothstep(0.2, 0.8, clouds));\n    vec3 core = mix(themeColor * 1.4, vec3(1.0), 0.4);\n    vec3 finalCol = nebCol * neb * 1.3 + core * pow(max(neb - 0.45, 0.0), 1.6) * 1.2;\n    float nebAlpha = neb * envelope * alpha;\n    fragColor = vec4(finalCol, clamp(nebAlpha, 0.0, 1.0));\n}";
         this.fogNebulaProgram = this.createProgram(vert, frag);
      }
   }

   private void initFogWavesShader() {
      if (this.fogWavesProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 lookDir;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    lookDir = Position;\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = "#version 150 core\nin vec3 lookDir;\nout vec4 fragColor;\nuniform float time;\nuniform vec3 themeColor;\nuniform float alpha;\nuniform float speed;\nuniform float density;\nuniform float horizon;\n\nvoid main() {\n    vec3 dir = normalize(lookDir);\n    float t = time * 0.8 * speed;\n    float hBand = exp(-abs(dir.y * 3.0) * max(0.2, (2.2 - horizon * 0.8)));\n    float ground = smoothstep(0.15, -0.5, dir.y) * 0.4;\n    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;\n    float angle = atan(dir.z, dir.x);\n    float wave1 = sin(angle * 4.0 + dir.y * 8.0 + t) * 0.5 + 0.5;\n    float wave2 = sin(angle * 7.0 - dir.y * 12.0 - t * 1.3) * 0.5 + 0.5;\n    float wave3 = sin(angle * 11.0 + t * 0.7) * 0.5 + 0.5;\n    float waveTotal = wave1 * 0.5 + wave2 * 0.3 + wave3 * 0.2;\n    float curtain = pow(waveTotal, 2.2);\n    vec3 c1 = themeColor;\n    vec3 c2 = mix(themeColor, vec3(0.2, 1.0, 0.8), 0.5);\n    vec3 c3 = mix(themeColor, vec3(1.0, 0.3, 0.9), 0.4);\n    vec3 waveColor = mix(c1, c2, wave1);\n    waveColor = mix(waveColor, c3, wave2 * 0.6) * (curtain * 1.5 + 0.2);\n    float waveAlpha = (curtain * 0.85 + 0.15) * envelope * alpha;\n    fragColor = vec4(waveColor, clamp(waveAlpha, 0.0, 1.0));\n}";
         this.fogWavesProgram = this.createProgram(vert, frag);
      }
   }

   private void initFogAbyssShader() {
      if (this.fogAbyssProgram == -1) {
         String vert = "#version 150 core\nin vec3 Position;\nout vec3 lookDir;\nuniform mat4 uModelViewMat;\nuniform mat4 uProjMat;\nvoid main() {\n    lookDir = Position;\n    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);\n    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);\n}";
         String frag = "#version 150 core\nin vec3 lookDir;\nout vec4 fragColor;\nuniform float time;\nuniform vec3 themeColor;\nuniform float alpha;\nuniform float speed;\nuniform float density;\nuniform float horizon;\n\nvoid main() {\n    vec3 dir = normalize(lookDir);\n    float t = time * 0.6 * speed;\n    float hBand = exp(-abs(dir.y * 2.6) * max(0.2, (2.0 - horizon * 0.7)));\n    float ground = smoothstep(0.2, -0.6, dir.y) * 0.6;\n    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;\n    vec2 p = dir.xz / (abs(dir.y) + 0.25) * 2.0;\n    float r = length(p);\n    float ripple = sin(r * 8.0 - t * 3.0) * 0.5 + 0.5;\n    ripple = pow(ripple, 4.0);\n    float grid = abs(sin(p.x * 3.0 + t)) * abs(sin(p.y * 3.0 - t));\n    grid = smoothstep(0.4, 0.95, grid);\n    vec3 voidDark = themeColor * 0.15 + vec3(0.01, 0.01, 0.02);\n    vec3 neonCore = themeColor * 1.5 + vec3(0.2);\n    vec3 color = mix(voidDark, neonCore, ripple * 0.7 + grid * 0.5);\n    float abyssAlpha = (0.25 + ripple * 0.5 + grid * 0.25) * envelope * alpha;\n    fragColor = vec4(color, clamp(abyssAlpha, 0.0, 1.0));\n}";
         this.fogAbyssProgram = this.createProgram(vert, frag);
      }
   }

   private void renderShaderFog(WorldRenderContext context) {
      if (context.matrixStack() != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            int program = -1;
            String mode = this.fogShaderMode.get();
            if (mode.equals("Mist")) {
               this.initFogMistShader();
               program = this.fogMistProgram;
            } else if (mode.equals("Smoke")) {
               this.initFogSmokeShader();
               program = this.fogSmokeProgram;
            } else if (mode.equals("Caustic")) {
               this.initFogCausticShader();
               program = this.fogCausticProgram;
            } else if (mode.equals("Nebula")) {
               this.initFogNebulaShader();
               program = this.fogNebulaProgram;
            } else if (mode.equals("Waves")) {
               this.initFogWavesShader();
               program = this.fogWavesProgram;
            } else if (mode.equals("Abyss")) {
               this.initFogAbyssShader();
               program = this.fogAbyssProgram;
            }

            if (program != -1) {
               int prevProgram = GL11.glGetInteger(35725);
               int prevVao = GL11.glGetInteger(34229);
               int prevVbo = GL11.glGetInteger(34964);
               boolean prevDepthTest = GL11.glIsEnabled(2929);
               boolean prevCull = GL11.glIsEnabled(2884);
               boolean prevBlend = GL11.glIsEnabled(3042);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableCull();
               RenderSystem.depthMask(false);
               RenderSystem.enableDepthTest();
               GL20.glUseProgram(program);
               Matrix4f modelViewMat = new Matrix4f().rotation(new Quaternionf(context.camera().rotation()).conjugate());
               Matrix4f projMat = context.projectionMatrix();
               float[] modelViewArr = new float[16];
               modelViewMat.get(modelViewArr);
               float[] projArr = new float[16];
               projMat.get(projArr);
               int modelViewLoc = GL20.glGetUniformLocation(program, "uModelViewMat");
               int projLoc = GL20.glGetUniformLocation(program, "uProjMat");
               GL20.glUniformMatrix4fv(modelViewLoc, false, modelViewArr);
               GL20.glUniformMatrix4fv(projLoc, false, projArr);
               float timeVal = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
               Color themeColor = new Color(ThemeManager.getThemedColor(0L));
               float r = themeColor.getRed() / 255.0F;
               float g = themeColor.getGreen() / 255.0F;
               float b = themeColor.getBlue() / 255.0F;
               if (!this.fogShaderThemeSync.get()) {
                  r = 1.0F;
                  g = 1.0F;
                  b = 1.0F;
               }

               int timeLoc = GL20.glGetUniformLocation(program, "time");
               int themeColorLoc = GL20.glGetUniformLocation(program, "themeColor");
               int alphaLoc = GL20.glGetUniformLocation(program, "alpha");
               int speedLoc = GL20.glGetUniformLocation(program, "speed");
               int densityLoc = GL20.glGetUniformLocation(program, "density");
               int horizonLoc = GL20.glGetUniformLocation(program, "horizon");
               if (timeLoc != -1) {
                  GL20.glUniform1f(timeLoc, timeVal);
               }

               if (themeColorLoc != -1) {
                  GL20.glUniform3f(themeColorLoc, r, g, b);
               }

               if (alphaLoc != -1) {
                  GL20.glUniform1f(alphaLoc, (float)this.fogShaderStrength.get());
               }

               if (speedLoc != -1) {
                  GL20.glUniform1f(speedLoc, (float)this.fogShaderSpeed.get());
               }

               if (densityLoc != -1) {
                  GL20.glUniform1f(densityLoc, (float)this.fogShaderDensity.get());
               }

               if (horizonLoc != -1) {
                  GL20.glUniform1f(horizonLoc, (float)this.fogShaderHorizon.get());
               }

               this.initSkyboxGeometry();
               GL30.glBindVertexArray(this.skyboxVao);
               GL11.glDrawArrays(4, 0, 36);
               GL30.glBindVertexArray(prevVao);
               GL15.glBindBuffer(34962, prevVbo);
               GL20.glUseProgram(prevProgram);
               if (prevDepthTest) {
                  RenderSystem.enableDepthTest();
               } else {
                  RenderSystem.disableDepthTest();
               }

               if (prevCull) {
                  RenderSystem.enableCull();
               } else {
                  RenderSystem.disableCull();
               }

               if (!prevBlend) {
                  RenderSystem.disableBlend();
               }

               RenderSystem.depthMask(true);
            }
         }
      }
   }

   private void initSphereBlurQuad() {
      if (this.sphereBlurVao == -1) {
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
         this.sphereBlurVao = GL30.glGenVertexArrays();
         this.sphereBlurVbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.sphereBlurVao);
         GL15.glBindBuffer(34962, this.sphereBlurVbo);
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

   private void initSphereBlurShader() {
      if (this.sphereBlurProgram == -1) {
         String vert = "#version 150 core\nin vec2 Position;\nin vec2 TexCoord;\nout vec2 vTexCoord;\nvoid main() {\n    vTexCoord = TexCoord;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}";
         String frag = "#version 150 core\nin vec2 vTexCoord;\nout vec4 fragColor;\n\nuniform sampler2D uColorTexture;\nuniform sampler2D uDepthTexture;\nuniform mat4 uInvViewProjMat;\nuniform vec3 uSphereCenterRelCam;\nuniform float uSphereRadius;\nuniform float uSmoothness;\nuniform float uBlurStrength;\nuniform vec2 uResolution;\nuniform int uSampleCount;\nuniform int uBlurSky;\nuniform vec3 uThemeColor;\nuniform float uThemeTint;\n\nconst float GOLDEN_ANGLE = 2.39996323;\n\nvoid main() {\n    vec4 centerColor = texture(uColorTexture, vTexCoord);\n    float rawDepth = texture(uDepthTexture, vTexCoord).r;\n\n    float dist = 0.0;\n    float weight = 0.0;\n\n    if (rawDepth >= 0.999999) {\n        // Far plane / Sky\n        if (uBlurSky == 0) {\n            fragColor = centerColor;\n            return;\n        }\n        weight = 1.0;\n    } else {\n        // Reconstruct camera-relative world position from depth buffer\n        vec4 clipPos = vec4(vTexCoord * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);\n        vec4 camRelPosH = uInvViewProjMat * clipPos;\n        vec3 camRelPos = camRelPosH.xyz / camRelPosH.w;\n\n        // Distance from 3D sphere center\n        dist = length(camRelPos - uSphereCenterRelCam);\n\n        float innerRadius = max(0.0, uSphereRadius - uSmoothness);\n        float factor = clamp((dist - innerRadius) / max(0.001, uSmoothness), 0.0, 1.0);\n        weight = smoothstep(0.0, 1.0, factor);\n    }\n\n    if (weight <= 0.001 || uBlurStrength <= 0.1) {\n        fragColor = centerColor;\n        return;\n    }\n\n    // Vogel disk golden-angle spiral sampling with Gaussian falloff\n    vec2 texel = 1.0 / uResolution;\n    float effectiveRadius = uBlurStrength * weight;\n    vec4 accum = centerColor;\n    float totalWeight = 1.0;\n\n    int samples = clamp(uSampleCount, 8, 64);\n    for (int i = 1; i <= samples; i++) {\n        float r = sqrt(float(i) / float(samples));\n        float theta = float(i) * GOLDEN_ANGLE;\n        vec2 offset = vec2(cos(theta), sin(theta)) * (r * effectiveRadius) * texel;\n        float sampleW = exp(-2.0 * r * r);\n        accum += texture(uColorTexture, vTexCoord + offset) * sampleW;\n        totalWeight += sampleW;\n    }\n\n    vec4 blurred = accum / totalWeight;\n\n    if (uThemeTint > 0.001) {\n        blurred.rgb = mix(blurred.rgb, uThemeColor, uThemeTint * weight);\n    }\n\n    fragColor = mix(centerColor, blurred, weight);\n}";
         this.sphereBlurProgram = this.createProgram(vert, frag);
      }
   }

   private void ensureSphereBlurFbo(int width, int height) {
      if (this.sphereBlurFbo == null || this.sphereBlurFbo.width != width || this.sphereBlurFbo.height != height) {
         if (this.sphereBlurFbo != null) {
            this.sphereBlurFbo.destroyBuffers();
         }

         this.sphereBlurFbo = GlCompat.newTarget(width, height, false);
         GlCompat.setClearColor(this.sphereBlurFbo, 0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glBindTexture(3553, GlCompat.colorId(this.sphereBlurFbo));
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glBindTexture(3553, 0);
      }
   }

   private Vec3 getSmoothSphereCenter(float tickDelta) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return Vec3.ZERO;
      }

      double targetX = Mth.lerp(tickDelta, mc.player.xOld, mc.player.getX());
      double targetY = Mth.lerp(tickDelta, mc.player.yOld, mc.player.getY()) + mc.player.getBbHeight() * 0.5;
      double targetZ = Mth.lerp(tickDelta, mc.player.zOld, mc.player.getZ());
      Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
      if (this.smoothSphereCenter != null && this.smoothFollow.get()) {
         if (this.smoothSphereCenter.distanceToSqr(targetPos) > 40000.0) {
            this.smoothSphereCenter = targetPos;
            return targetPos;
         } else {
            long now = System.currentTimeMillis();
            float dt = this.lastSphereUpdateTime == 0L ? 0.016F : (float)(now - this.lastSphereUpdateTime) / 1000.0F;
            dt = Mth.clamp(dt, 0.001F, 0.1F);
            this.lastSphereUpdateTime = now;
            float speed = (float)this.blurFollowSpeed.get();
            float t = 1.0F - (float)Math.exp(-speed * dt);
            this.smoothSphereCenter = new Vec3(
               Mth.lerp(t, this.smoothSphereCenter.x, targetPos.x),
               Mth.lerp(t, this.smoothSphereCenter.y, targetPos.y),
               Mth.lerp(t, this.smoothSphereCenter.z, targetPos.z)
            );
            return this.smoothSphereCenter;
         }
      } else {
         this.smoothSphereCenter = targetPos;
         return targetPos;
      }
   }

   private void renderSphereBlur(WorldRenderContext context) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && context.camera() != null) {
         RenderTarget mainFbo = mc.gameRenderer.mainRenderTarget();
         if (mainFbo != null) {
            int width = mainFbo.width;
            int height = mainFbo.height;
            if (width > 0 && height > 0) {
               this.initSphereBlurShader();
               if (this.sphereBlurProgram != -1) {
                  this.initSphereBlurQuad();
                  if (this.sphereBlurVao != -1) {
                     this.ensureSphereBlurFbo(width, height);
                     int prevReadFbo = GL11.glGetInteger(36010);
                     int prevDrawFbo = GL11.glGetInteger(36006);
                     GL30.glBindFramebuffer(36008, GlCompat.fbo(mainFbo));
                     GL30.glBindFramebuffer(36009, GlCompat.fbo(this.sphereBlurFbo));
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
                     GL20.glUseProgram(this.sphereBlurProgram);
                     Matrix4f viewRotMat = new Matrix4f().rotation(new Quaternionf(context.camera().rotation()).conjugate());
                     Matrix4f projMat = new Matrix4f(context.projectionMatrix());
                     Matrix4f invViewProjMat = new Matrix4f(projMat).mul(viewRotMat).invert();
                     float[] invViewProjArr = new float[16];
                     invViewProjMat.get(invViewProjArr);
                     float tickDelta = context.tickCounter() != null ? context.tickCounter().getGameTimeDeltaPartialTick(true) : 1.0F;
                     Vec3 sphereCenter = this.getSmoothSphereCenter(tickDelta);
                     Vec3 camPos = context.camera().position();
                     Vec3 sphereCenterRelCam = sphereCenter.subtract(camPos);
                     int sampleCount = 32;
                     String quality = this.blurQuality.get();
                     if (quality.equals("Low")) {
                        sampleCount = 12;
                     } else if (quality.equals("Medium")) {
                        sampleCount = 20;
                     } else if (quality.equals("High")) {
                        sampleCount = 32;
                     } else if (quality.equals("Ultra")) {
                        sampleCount = 48;
                     }

                     Color themeColor = new Color(ThemeManager.getThemedColor(0L));
                     float r = themeColor.getRed() / 255.0F;
                     float g = themeColor.getGreen() / 255.0F;
                     float b = themeColor.getBlue() / 255.0F;
                     float tintStrength = this.blurThemeTint.get() ? (float)this.blurThemeStrength.get() : 0.0F;
                     GL13.glActiveTexture(33984);
                     GL11.glBindTexture(3553, GlCompat.colorId(this.sphereBlurFbo));
                     GL13.glActiveTexture(33985);
                     GL11.glBindTexture(3553, GlCompat.depthId(mainFbo));
                     GL11.glTexParameteri(3553, 34892, 0);
                     GL11.glTexParameteri(3553, 10241, 9728);
                     GL11.glTexParameteri(3553, 10240, 9728);
                     GL11.glTexParameteri(3553, 10242, 33071);
                     GL11.glTexParameteri(3553, 10243, 33071);
                     int locColorTex = GL20.glGetUniformLocation(this.sphereBlurProgram, "uColorTexture");
                     int locDepthTex = GL20.glGetUniformLocation(this.sphereBlurProgram, "uDepthTexture");
                     int locInvViewProj = GL20.glGetUniformLocation(this.sphereBlurProgram, "uInvViewProjMat");
                     int locSphereCenter = GL20.glGetUniformLocation(this.sphereBlurProgram, "uSphereCenterRelCam");
                     int locSphereRadius = GL20.glGetUniformLocation(this.sphereBlurProgram, "uSphereRadius");
                     int locSmoothness = GL20.glGetUniformLocation(this.sphereBlurProgram, "uSmoothness");
                     int locBlurStrength = GL20.glGetUniformLocation(this.sphereBlurProgram, "uBlurStrength");
                     int locResolution = GL20.glGetUniformLocation(this.sphereBlurProgram, "uResolution");
                     int locSampleCount = GL20.glGetUniformLocation(this.sphereBlurProgram, "uSampleCount");
                     int locBlurSky = GL20.glGetUniformLocation(this.sphereBlurProgram, "uBlurSky");
                     int locThemeColor = GL20.glGetUniformLocation(this.sphereBlurProgram, "uThemeColor");
                     int locThemeTint = GL20.glGetUniformLocation(this.sphereBlurProgram, "uThemeTint");
                     if (locColorTex != -1) {
                        GL20.glUniform1i(locColorTex, 0);
                     }

                     if (locDepthTex != -1) {
                        GL20.glUniform1i(locDepthTex, 1);
                     }

                     if (locInvViewProj != -1) {
                        GL20.glUniformMatrix4fv(locInvViewProj, false, invViewProjArr);
                     }

                     if (locSphereCenter != -1) {
                        GL20.glUniform3f(
                           locSphereCenter, (float)sphereCenterRelCam.x, (float)sphereCenterRelCam.y, (float)sphereCenterRelCam.z
                        );
                     }

                     if (locSphereRadius != -1) {
                        GL20.glUniform1f(locSphereRadius, (float)this.sphereRadius.get());
                     }

                     if (locSmoothness != -1) {
                        GL20.glUniform1f(locSmoothness, (float)this.blurSmoothness.get());
                     }

                     if (locBlurStrength != -1) {
                        GL20.glUniform1f(locBlurStrength, (float)this.blurStrength.get());
                     }

                     if (locResolution != -1) {
                        GL20.glUniform2f(locResolution, width, height);
                     }

                     if (locSampleCount != -1) {
                        GL20.glUniform1i(locSampleCount, sampleCount);
                     }

                     if (locBlurSky != -1) {
                        GL20.glUniform1i(locBlurSky, this.blurSky.get() ? 1 : 0);
                     }

                     if (locThemeColor != -1) {
                        GL20.glUniform3f(locThemeColor, r, g, b);
                     }

                     if (locThemeTint != -1) {
                        GL20.glUniform1f(locThemeTint, tintStrength);
                     }

                     GL30.glBindVertexArray(this.sphereBlurVao);
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
   }

   private void initSaturationShader() {
      if (this.saturationProgram == -1) {
         String vert = "#version 150 core\nin vec2 Position;\nin vec2 TexCoord;\nout vec2 vTexCoord;\nvoid main() {\n    vTexCoord = TexCoord;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}";
         String frag = "#version 150 core\nin vec2 vTexCoord;\nout vec4 fragColor;\nuniform sampler2D uColorTexture;\nuniform float uSaturation;\nvoid main() {\n    vec4 baseColor = texture(uColorTexture, vTexCoord);\n    float luma = dot(baseColor.rgb, vec3(0.2126, 0.7152, 0.0722));\n    vec3 sat = mix(vec3(luma), baseColor.rgb, uSaturation);\n    fragColor = vec4(clamp(sat, 0.0, 1.0), baseColor.a);\n}";
         this.saturationProgram = this.createProgram(vert, frag);
      }
   }

   private void ensureSaturationFbo(int width, int height) {
      if (this.saturationFbo == null || this.saturationFbo.width != width || this.saturationFbo.height != height) {
         if (this.saturationFbo != null) {
            this.saturationFbo.destroyBuffers();
         }

         this.saturationFbo = GlCompat.newTarget(width, height, false);
         GlCompat.setClearColor(this.saturationFbo, 0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glBindTexture(3553, GlCompat.colorId(this.saturationFbo));
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glBindTexture(3553, 0);
      }
   }

   private void initSaturationQuad() {
      if (this.saturationVao == -1) {
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
         this.saturationVao = GL30.glGenVertexArrays();
         this.saturationVbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.saturationVao);
         GL15.glBindBuffer(34962, this.saturationVbo);
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

   private void renderSaturation(WorldRenderContext context) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         float satVal = (float)this.saturation.get();
         if (!(Math.abs(satVal - 1.0F) < 0.001F)) {
            RenderTarget mainFbo = mc.gameRenderer.mainRenderTarget();
            if (mainFbo != null) {
               int width = mainFbo.width;
               int height = mainFbo.height;
               if (width > 0 && height > 0) {
                  this.initSaturationShader();
                  if (this.saturationProgram != -1) {
                     this.initSaturationQuad();
                     if (this.saturationVao != -1) {
                        this.ensureSaturationFbo(width, height);
                        int prevReadFbo = GL11.glGetInteger(36010);
                        int prevDrawFbo = GL11.glGetInteger(36006);
                        GL30.glBindFramebuffer(36008, GlCompat.fbo(mainFbo));
                        GL30.glBindFramebuffer(36009, GlCompat.fbo(this.saturationFbo));
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
                        boolean prevBlend = GL11.glIsEnabled(3042);
                        boolean prevDepthTest = GL11.glIsEnabled(2929);
                        boolean prevCull = GL11.glIsEnabled(2884);
                        boolean prevDepthMask = GL11.glGetBoolean(2930);
                        RenderSystem.disableDepthTest();
                        RenderSystem.depthMask(false);
                        RenderSystem.disableCull();
                        RenderSystem.disableBlend();
                        GL20.glUseProgram(this.saturationProgram);
                        GL13.glActiveTexture(33984);
                        GL11.glBindTexture(3553, GlCompat.colorId(this.saturationFbo));
                        int locTex = GL20.glGetUniformLocation(this.saturationProgram, "uColorTexture");
                        int locSat = GL20.glGetUniformLocation(this.saturationProgram, "uSaturation");
                        if (locTex != -1) {
                           GL20.glUniform1i(locTex, 0);
                        }

                        if (locSat != -1) {
                           GL20.glUniform1f(locSat, satVal);
                        }

                        GL30.glBindVertexArray(this.saturationVao);
                        GL11.glDrawArrays(4, 0, 6);
                        GL30.glBindVertexArray(prevVao);
                        GL15.glBindBuffer(34962, prevVbo);
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
      }
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
      if (this.sphereBlurFbo != null) {
         this.sphereBlurFbo.destroyBuffers();
         this.sphereBlurFbo = null;
      }

      if (this.saturationFbo != null) {
         this.saturationFbo.destroyBuffers();
         this.saturationFbo = null;
      }

      this.smoothSphereCenter = null;
   }

   @Override
   public void onTick() {
   }
}
