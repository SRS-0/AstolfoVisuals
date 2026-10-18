package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.GlCompat;
import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.nio.FloatBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import com.mojang.blaze3d.pipeline.RenderTarget;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import xyz.angames.astolfoclient.client.compat.GL20;
import org.lwjgl.opengl.GL30;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.JumpCircleModule;

@Environment(EnvType.CLIENT)
public class JumpCircleRenderer {
   private static final Identifier TEXTURE_CLIENT = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/circle.png");
   private static final Identifier TEXTURE_LARGE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/jump_circle/circle_large.png");
   private static final Identifier TEXTURE_SLIM = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/jump_circle/circle_slim.png");
   private final JumpCircleManager manager;
   private RenderTarget distortionFbo = null;
   private int distortionProgram = -1;
   private int distortionVao = -1;
   private int distortionVbo = -1;

   public JumpCircleRenderer(JumpCircleManager manager) {
      this.manager = manager;
   }

   private Identifier getCircleTexture(JumpCircleModule module) {
      return switch (module.style.get()) {
         case "circle_large.png", "Large", "circle_large" -> TEXTURE_LARGE;
         case "circle_slim.png", "Slim", "circle_slim" -> TEXTURE_SLIM;
         default -> TEXTURE_CLIENT;
      };
   }

   public void render(WorldRenderContext context) {
      Module jumpCircleModule = AstolfoclientClient.moduleManager.getModuleByName("JumpCircle");
      if (jumpCircleModule != null && jumpCircleModule.isEnabled()) {
         if (jumpCircleModule instanceof JumpCircleModule jcm) {
            List<JumpCircle> circles = this.manager.getCircles();
            if (!circles.isEmpty()) {
               if (jcm.distortion.get()) {
                  this.renderDistortionShader(context, jcm, circles);
               }

               if (jcm.texture.get()) {
                  this.renderJumpCircleMeshes(context, jcm, circles);
               }
            }
         }
      }
   }

   private void renderDistortionShader(WorldRenderContext context, JumpCircleModule jcm, List<JumpCircle> circles) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && context.camera() != null) {
         RenderTarget mainFbo = mc.gameRenderer.mainRenderTarget();
         if (mainFbo != null) {
            int width = mainFbo.width;
            int height = mainFbo.height;
            if (width > 0 && height > 0) {
               this.initDistortionShader();
               if (this.distortionProgram != -1) {
                  this.initDistortionQuad();
                  if (this.distortionVao != -1) {
                     this.ensureDistortionFbo(width, height);
                     int prevReadFbo = GL11.glGetInteger(36010);
                     int prevDrawFbo = GL11.glGetInteger(36006);
                     GL30.glBindFramebuffer(36008, GlCompat.fbo(mainFbo));
                     GL30.glBindFramebuffer(36009, GlCompat.fbo(this.distortionFbo));
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
                     GL20.glUseProgram(this.distortionProgram);
                     Matrix4f viewRotMat = new Matrix4f().rotation(new Quaternionf(context.camera().rotation()).conjugate());
                     Matrix4f projMat = new Matrix4f(context.projectionMatrix());
                     Matrix4f viewProjMat = new Matrix4f(projMat).mul(viewRotMat);
                     Matrix4f invViewProjMat = new Matrix4f(viewProjMat).invert();
                     float[] invViewProjArr = new float[16];
                     invViewProjMat.get(invViewProjArr);
                     float[] viewProjArr = new float[16];
                     viewProjMat.get(viewProjArr);
                     GL13.glActiveTexture(33984);
                     GL11.glBindTexture(3553, GlCompat.colorId(this.distortionFbo));
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
                     this.setUniform1f("uDistortionStrength", (float)jcm.distortionStrength.get());
                     this.setUniform1i("uChromatic", jcm.chromatic.get() ? 1 : 0);
                     double camX = context.camera().position().x;
                     double camY = context.camera().position().y;
                     double camZ = context.camera().position().z;
                     float baseRadius = (float)jcm.radius.get();
                     int maxCircles = 16;
                     int count = 0;

                     for (JumpCircle circle : circles) {
                        if (count >= maxCircles) {
                           break;
                        }

                        long age = System.currentTimeMillis() - circle.creationTime;
                        if (age <= 1300L) {
                           float progress = (float)age / 1300.0F;
                           float sizeEase = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
                           float radius = sizeEase * baseRadius * 0.5F;
                           float alpha = 1.0F - progress;
                           if (!(alpha <= 0.0F)) {
                              float relX = (float)(circle.x - camX);
                              float relY = (float)(circle.y - camY);
                              float relZ = (float)(circle.z - camZ);
                              this.setUniform3f("uCirclePos[" + count + "]", relX, relY, relZ);
                              this.setUniform1f("uCircleRadius[" + count + "]", radius);
                              this.setUniform1f("uCircleProgress[" + count + "]", progress);
                              this.setUniform1f("uCircleAlpha[" + count + "]", alpha);
                              count++;
                           }
                        }
                     }

                     this.setUniform1i("uCircleCount", count);
                     GL30.glBindVertexArray(this.distortionVao);
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

   private void renderJumpCircleMeshes(WorldRenderContext context, JumpCircleModule jcm, List<JumpCircle> circles) {
      double camX = context.camera().position().x;
      double camY = context.camera().position().y;
      double camZ = context.camera().position().z;
      int themeColorInt = ThemeManager.getThemedColor(0L);
      float r = (themeColorInt >> 16 & 0xFF) / 255.0F;
      float g = (themeColorInt >> 8 & 0xFF) / 255.0F;
      float b = (themeColorInt & 0xFF) / 255.0F;
      float baseRadius = (float)jcm.radius.get();
      Tesselator tessellator = Tesselator.getInstance();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515);
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, this.getCircleTexture(jcm));
      RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);

      for (JumpCircle circle : circles) {
         long age = System.currentTimeMillis() - circle.creationTime;
         if (age <= 1300L) {
            float progress = (float)age / 1300.0F;
            float sizeEase = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
            float size = sizeEase * baseRadius;
            float spinEase = progress * progress * progress;
            float rotationAngle = spinEase * 360.0F;
            float alpha = 1.0F - progress;
            if (!(alpha <= 0.0F)) {
               PoseStack matrixStack = context.matrixStack();
               matrixStack.pushPose();
               matrixStack.translate(circle.x - camX, circle.y - camY + 0.02, circle.z - camZ);
               matrixStack.rotate(Axis.YP.rotationDegrees(rotationAngle));
               matrixStack.rotate(Axis.XP.rotationDegrees(90.0F));
               matrixStack.scale(size, size, size);
               Matrix4f matrix = matrixStack.last().pose();
               BufferBuilder buffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
               Vtx.vertex(buffer, matrix, -0.5F, -0.5F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, alpha);
               Vtx.vertex(buffer, matrix, -0.5F, 0.5F, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, alpha);
               Vtx.vertex(buffer, matrix, 0.5F, 0.5F, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, alpha);
               Vtx.vertex(buffer, matrix, 0.5F, -0.5F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, alpha);
               BufferUploader.drawWithShader(buffer.buildOrThrow());
               matrixStack.popPose();
            }
         }
      }

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
   }

   private void ensureDistortionFbo(int width, int height) {
      if (this.distortionFbo == null || this.distortionFbo.width != width || this.distortionFbo.height != height) {
         if (this.distortionFbo != null) {
            this.distortionFbo.destroyBuffers();
         }

         this.distortionFbo = GlCompat.newTarget(width, height, false);
         GlCompat.setClearColor(this.distortionFbo, 0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glBindTexture(3553, GlCompat.colorId(this.distortionFbo));
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glBindTexture(3553, 0);
      }
   }

   private void initDistortionQuad() {
      if (this.distortionVao == -1) {
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
         this.distortionVao = GL30.glGenVertexArrays();
         this.distortionVbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.distortionVao);
         GL15.glBindBuffer(34962, this.distortionVbo);
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

   private void initDistortionShader() {
      if (this.distortionProgram == -1) {
         String vert = "#version 150 core\nin vec2 Position;\nin vec2 TexCoord;\nout vec2 vTexCoord;\nvoid main() {\n    vTexCoord = TexCoord;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}";
         String frag = "#version 150 core\nin vec2 vTexCoord;\nout vec4 fragColor;\n\nuniform sampler2D uColorTexture;\nuniform sampler2D uDepthTexture;\nuniform mat4 uInvViewProjMat;\nuniform mat4 uViewProjMat;\nuniform vec2 uResolution;\nuniform float uDistortionStrength;\nuniform int uChromatic;\n\nconst int MAX_CIRCLES = 16;\nuniform int uCircleCount;\nuniform vec3 uCirclePos[MAX_CIRCLES];\nuniform float uCircleRadius[MAX_CIRCLES];\nuniform float uCircleProgress[MAX_CIRCLES];\nuniform float uCircleAlpha[MAX_CIRCLES];\n\nvoid main() {\n    vec4 baseColor = texture(uColorTexture, vTexCoord);\n    if (uCircleCount <= 0 || uDistortionStrength <= 0.001) {\n        fragColor = baseColor;\n        return;\n    }\n\n    float rawDepth = texture(uDepthTexture, vTexCoord).r;\n    if (rawDepth >= 0.999999) {\n        // Don't distort sky: Jump circle is a 2D planar floor effect\n        fragColor = baseColor;\n        return;\n    }\n\n    // Reconstruct 3D camera-relative world position\n    vec4 clipPos = vec4(vTexCoord * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);\n    vec4 camRelPosH = uInvViewProjMat * clipPos;\n    vec3 camRelPos = camRelPosH.xyz / max(0.0001, camRelPosH.w);\n\n    vec2 totalUVShift = vec2(0.0);\n    float totalChromatic = 0.0;\n\n    for (int i = 0; i < uCircleCount; i++) {\n        if (i >= MAX_CIRCLES) break;\n\n        vec3 cPos = uCirclePos[i];\n        float radius = uCircleRadius[i];\n        float progress = uCircleProgress[i];\n        float alpha = uCircleAlpha[i];\n\n        if (alpha <= 0.001 || radius <= 0.01) continue;\n\n        // 3D vector from jump circle epicenter to the block surface\n        vec3 delta = camRelPos - cPos;\n        float dist3D = length(delta);\n\n        // Strictly bounded by jump circle radius in world space\n        float maxEffectRadius = radius * 1.05;\n        if (dist3D > maxEffectRadius) continue;\n\n        // 1. Expanding Shockwave Wavefront along block surfaces in 3D\n        float waveWidth = max(0.12, radius * 0.28);\n        float ringDist = abs(dist3D - radius);\n        float ringFactor = smoothstep(waveWidth, 0.0, ringDist);\n        float ripple = sin((dist3D - radius) / waveWidth * 3.14159265) * ringFactor;\n\n        // 2. Black Hole Gravitational Singularity Pull towards epicenter\n        float blackholePull = smoothstep(radius, 0.0, dist3D) * (1.0 - progress);\n\n        // 3. Radial normal and tangential swirl in 3D space\n        vec3 normalDir = delta / max(0.001, dist3D);\n        vec3 tangent = normalize(cross(vec3(0.0, 1.0, 0.0), delta + vec3(0.0001)));\n        float swirl = blackholePull * 0.40 * sin(progress * 3.14159265);\n\n        // Combined 3D world displacement across walls, floors, and holes\n        vec3 worldDisplace = (normalDir * (ripple * 0.38 - blackholePull * 0.28) + tangent * swirl)\n                             * alpha * uDistortionStrength;\n\n        // Apply 3D shift to block surface in world space\n        vec3 displacedWorldPos = camRelPos + worldDisplace;\n\n        // Project displaced 3D point to screen UV coordinates\n        vec4 distClip = uViewProjMat * vec4(displacedWorldPos, 1.0);\n        if (distClip.w > 0.01) {\n            vec2 distUV = (distClip.xy / distClip.w) * 0.5 + 0.5;\n            totalUVShift += (distUV - vTexCoord);\n        }\n\n        if (uChromatic == 1) {\n            totalChromatic += (abs(ripple) * 0.010 + blackholePull * 0.007) * alpha * uDistortionStrength;\n        }\n    }\n\n    if (length(totalUVShift) < 0.00005 && totalChromatic < 0.00005) {\n        fragColor = baseColor;\n        return;\n    }\n\n    // Pure world refraction with optical chromatic aberration\n    vec2 uvR = vTexCoord + totalUVShift + vec2(totalChromatic, 0.0);\n    vec2 uvG = vTexCoord + totalUVShift;\n    vec2 uvB = vTexCoord + totalUVShift - vec2(totalChromatic, 0.0);\n\n    float r = texture(uColorTexture, clamp(uvR, 0.001, 0.999)).r;\n    float g = texture(uColorTexture, clamp(uvG, 0.001, 0.999)).g;\n    float b = texture(uColorTexture, clamp(uvB, 0.001, 0.999)).b;\n\n    fragColor = vec4(r, g, b, baseColor.a);\n}";
         this.distortionProgram = this.createProgram(vert, frag);
      }
   }

   private int createProgram(String vert, String frag) {
      int v = GL20.glCreateShader(35633);
      GL20.glShaderSource(v, vert);
      GL20.glCompileShader(v);
      if (GL20.glGetShaderi(v, 35713) == 0) {
         System.err.println("JumpCircle Vertex shader compile error:\n" + GL20.glGetShaderInfoLog(v, 1024));
      }

      int f = GL20.glCreateShader(35632);
      GL20.glShaderSource(f, frag);
      GL20.glCompileShader(f);
      if (GL20.glGetShaderi(f, 35713) == 0) {
         System.err.println("JumpCircle Fragment shader compile error:\n" + GL20.glGetShaderInfoLog(f, 1024));
      }

      int p = GL20.glCreateProgram();
      GL20.glAttachShader(p, v);
      GL20.glAttachShader(p, f);
      GL20.glBindAttribLocation(p, 0, "Position");
      GL20.glBindAttribLocation(p, 1, "TexCoord");
      GL20.glLinkProgram(p);
      if (GL20.glGetProgrami(p, 35714) == 0) {
         System.err.println("JumpCircle Shader program link error:\n" + GL20.glGetProgramInfoLog(p, 1024));
      }

      return p;
   }

   private void setUniform1i(String name, int val) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         GL20.glUniform1i(loc, val);
      }
   }

   private void setUniform1f(String name, float val) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         GL20.glUniform1f(loc, val);
      }
   }

   private void setUniform2f(String name, float x, float y) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         GL20.glUniform2f(loc, x, y);
      }
   }

   private void setUniform3f(String name, float x, float y, float z) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         GL20.glUniform3f(loc, x, y, z);
      }
   }

   private void setUniformMatrix4fv(String name, float[] mat) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
         buffer.put(mat).flip();
         GL20.glUniformMatrix4fv(loc, false, buffer);
      }
   }
}
