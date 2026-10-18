package xyz.angames.astolfoclient.client.render;

import xyz.angames.astolfoclient.client.compat.GlCompat;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import xyz.angames.astolfoclient.client.compat.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import xyz.angames.astolfoclient.client.compat.GL20;
import org.lwjgl.opengl.GL30;

@Environment(EnvType.CLIENT)
public final class WetSurfaceRenderer implements AutoCloseable {
   private static final WetSurfaceRenderer INSTANCE = new WetSurfaceRenderer();
   private static final float EPSILON = 1.0E-4F;
   private final WetSurfaceRenderer.SceneTarget scene = new WetSurfaceRenderer.SceneTarget();
   private int shaderProgram = -1;
   private int blitFramebuffer;
   private int drawFramebuffer;
   private int vertexArray;
   private int vertexBuffer;
   private boolean initialized;
   private boolean disabled;
   private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

   private WetSurfaceRenderer() {
   }

   public static WetSurfaceRenderer getInstance() {
      return INSTANCE;
   }

   public void apply(Minecraft mc, Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix, WetSurfaceRenderer.Parameters parameters) {
      if (!this.disabled && mc != null && camera != null && viewMatrix != null && projectionMatrix != null && parameters != null) {
         if (mc.level != null
            && mc.player != null
            && isWindowValid(mc)
            && !(parameters.wetness <= 1.0E-4F)
            && !(parameters.reflectionStrength <= 1.0E-4F)) {
            Window window = mc.getWindow();
            int width = window.getWidth();
            int height = window.getHeight();
            if (width > 1 && height > 1) {
               RenderTarget framebuffer = mc.gameRenderer.mainRenderTarget();
               if (framebuffer != null) {
                  int colorTexture = GlCompat.colorId(framebuffer);
                  int depthTexture = GlCompat.depthId(framebuffer);
                  if (colorTexture > 0 && depthTexture > 0) {
                     int prevProgram = GL11.glGetInteger(35725);
                     int prevReadFbo = GL11.glGetInteger(36010);
                     int prevDrawFbo = GL11.glGetInteger(36006);
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
                     boolean prevScissor = GL11.glIsEnabled(3089);
                     int[] prevViewport = new int[4];
                     GL11.glGetIntegerv(2978, prevViewport);
                     boolean attached = false;

                     try {
                        this.ensureInitialized();
                        if (!this.disabled && this.ensureScene(width, height) && this.copyColorToScene(colorTexture, width, height)) {
                           Matrix4f inverseProjection = new Matrix4f(projectionMatrix).invert();
                           Matrix4f inverseView = new Matrix4f(viewMatrix).invert();
                           Vec3 cameraPos = camera.position();
                           inverseView.m30((float)cameraPos.x);
                           inverseView.m31((float)cameraPos.y);
                           inverseView.m32((float)cameraPos.z);
                           parameters.cameraX = (float)cameraPos.x;
                           parameters.cameraY = (float)cameraPos.y;
                           parameters.cameraZ = (float)cameraPos.z;
                           attached = this.renderPass(
                              colorTexture, depthTexture, width, height, viewMatrix, projectionMatrix, inverseProjection, inverseView, parameters
                           );
                           return;
                        }
                     } catch (Throwable throwable) {
                        this.disabled = true;
                        System.err.println("[Rain] WetSurfaceRenderer disabled due to error: " + throwable.getMessage());
                        throwable.printStackTrace();
                        return;
                     } finally {
                        if (attached && this.drawFramebuffer != 0) {
                           GL30.glBindFramebuffer(36160, this.drawFramebuffer);
                           GL30.glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
                        }

                        GL30.glBindVertexArray(prevVao);
                        GL15.glBindBuffer(34962, prevVbo);
                        GL13.glActiveTexture(33985);
                        GL11.glBindTexture(3553, prevTex1);
                        GL13.glActiveTexture(33984);
                        GL11.glBindTexture(3553, prevTex0);
                        RenderSystem.setShaderTexture(0, prevTex0);
                        GL13.glActiveTexture(prevActiveTex);
                        GL20.glUseProgram(prevProgram);
                        GL30.glBindFramebuffer(36008, prevReadFbo);
                        GL30.glBindFramebuffer(36009, prevDrawFbo);
                        GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
                        if (prevScissor) {
                           GL11.glEnable(3089);
                        } else {
                           GL11.glDisable(3089);
                        }

                        if (prevDepthTest) {
                           GL11.glEnable(2929);
                        } else {
                           GL11.glDisable(2929);
                        }

                        if (prevCull) {
                           GL11.glEnable(2884);
                        } else {
                           GL11.glDisable(2884);
                        }

                        if (prevBlend) {
                           GL11.glEnable(3042);
                        } else {
                           GL11.glDisable(3042);
                        }

                        GL11.glDepthMask(prevDepthMask);
                        RenderSystem.defaultBlendFunc();
                     }
                  }
               }
            }
         }
      }
   }

   private boolean renderPass(
      int colorTexture,
      int depthTexture,
      int width,
      int height,
      Matrix4f viewMatrix,
      Matrix4f projectionMatrix,
      Matrix4f inverseProjection,
      Matrix4f inverseView,
      WetSurfaceRenderer.Parameters parameters
   ) {
      if (this.drawFramebuffer == 0) {
         this.drawFramebuffer = GL30.glGenFramebuffers();
      }

      GL30.glBindFramebuffer(36160, this.drawFramebuffer);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, colorTexture, 0);
      GL11.glDrawBuffer(36064);
      if (GL30.glCheckFramebufferStatus(36160) != 36053) {
         return true;
      }

      GL11.glViewport(0, 0, width, height);
      GL11.glDisable(3089);
      GL11.glDisable(2929);
      GL11.glDisable(2884);
      GL11.glDisable(3042);
      GL11.glColorMask(true, true, true, true);
      GL11.glDepthMask(false);
      GL20.glUseProgram(this.shaderProgram);
      this.setUniform1i("u_SceneTexture", 0);
      this.setUniform1i("u_DepthTexture", 1);
      this.setUniform2f("u_Resolution", width, height);
      this.setUniformMatrix4f("u_ViewMatrix", viewMatrix);
      this.setUniformMatrix4f("u_ProjectionMatrix", projectionMatrix);
      this.setUniformMatrix4f("u_InverseProjectionMatrix", inverseProjection);
      this.setUniformMatrix4f("u_InverseViewMatrix", inverseView);
      this.setUniform3f("u_SunDir", parameters.sunDirX, parameters.sunDirY, parameters.sunDirZ);
      this.setUniform3f("u_CameraPos", parameters.cameraX, parameters.cameraY, parameters.cameraZ);
      this.setUniform1f("u_Time", (float)(System.currentTimeMillis() % 1000000L) / 1000.0F);
      this.setUniform1f("u_Wetness", clamp(parameters.wetness, 0.0F, 2.0F));
      this.setUniform1f("u_PuddleCoverage", clamp(parameters.puddleCoverage, 0.0F, 1.0F));
      this.setUniform1f("u_ReflectionStrength", clamp(parameters.reflectionStrength, 0.0F, 2.5F));
      this.setUniform1f("u_MaxDistance", clamp(parameters.maxDistance, 4.0F, 128.0F));
      this.setUniform1f("u_RippleStrength", clamp(parameters.rippleStrength, 0.0F, 2.0F));
      this.setUniform1f("u_RainAmount", clamp(parameters.rainAmount, 0.0F, 2.0F));
      this.setUniform1f("u_AmbientLight", clamp(parameters.ambientLight, 0.1F, 1.5F));
      this.setUniform1f("u_LightningFlash", clamp(parameters.lightningFlash, 0.0F, 1.5F));
      this.setUniform1i("u_ReflectionSteps", Math.max(4, Math.min(16, parameters.reflectionSteps)));
      this.setUniform1i("u_Ripples", parameters.ripples ? 1 : 0);
      this.setUniform3f("u_ThemeColor", parameters.themeR, parameters.themeG, parameters.themeB);
      this.setUniform1i("u_UseThemeColor", parameters.useThemeColor ? 1 : 0);
      GL13.glActiveTexture(33984);
      GL11.glBindTexture(3553, this.scene.texture);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL13.glActiveTexture(33985);
      GL11.glBindTexture(3553, depthTexture);
      GL11.glTexParameteri(3553, 34892, 0);
      GL11.glTexParameteri(3553, 10241, 9728);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      GL13.glActiveTexture(33984);
      GL30.glBindVertexArray(this.vertexArray);
      GL11.glDrawArrays(4, 0, 6);
      GL30.glBindVertexArray(0);
      return true;
   }

   private boolean copyColorToScene(int colorTexture, int width, int height) {
      if (colorTexture > 0 && this.scene.framebuffer > 0 && width > 0 && height > 0) {
         if (this.blitFramebuffer == 0) {
            this.blitFramebuffer = GL30.glGenFramebuffers();
         }

         GL30.glBindFramebuffer(36008, this.blitFramebuffer);
         GL30.glFramebufferTexture2D(36008, 36064, 3553, colorTexture, 0);
         if (GL30.glCheckFramebufferStatus(36008) != 36053) {
            GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
            return false;
         } else {
            GL30.glBindFramebuffer(36009, this.scene.framebuffer);
            GL11.glReadBuffer(36064);
            GL11.glDrawBuffer(36064);
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, 16384, 9728);
            GL30.glBindFramebuffer(36008, this.blitFramebuffer);
            GL30.glFramebufferTexture2D(36008, 36064, 3553, 0, 0);
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean ensureScene(int width, int height) {
      if (this.scene.texture != 0 && (this.scene.width != width || this.scene.height != height || this.scene.framebuffer == 0)) {
         this.deleteScene();
      }

      if (this.scene.texture == 0) {
         this.scene.texture = GL11.glGenTextures();
         GL11.glBindTexture(3553, this.scene.texture);
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         allocTexture2D(32856, width, height, 6408, 5121);
         this.scene.framebuffer = GL30.glGenFramebuffers();
         GL30.glBindFramebuffer(36160, this.scene.framebuffer);
         GL30.glFramebufferTexture2D(36160, 36064, 3553, this.scene.texture, 0);
         GL11.glDrawBuffer(36064);
         if (GL30.glCheckFramebufferStatus(36160) != 36053) {
            this.deleteScene();
            return false;
         }
      }

      this.scene.width = width;
      this.scene.height = height;
      return true;
   }

   private static void allocTexture2D(int internalFormat, int width, int height, int format, int type) {
      int unpackBuffer = GL11.glGetInteger(35055);
      if (unpackBuffer != 0) {
         GL15.glBindBuffer(35052, 0);
      }

      try {
         GL11.glTexImage2D(3553, 0, internalFormat, width, height, 0, format, type, (ByteBuffer)null);
      } finally {
         if (unpackBuffer != 0) {
            GL15.glBindBuffer(35052, unpackBuffer);
         }
      }
   }

   private void ensureInitialized() {
      if (!this.initialized) {
         this.vertexArray = GL30.glGenVertexArrays();
         this.vertexBuffer = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.vertexArray);
         GL15.glBindBuffer(34962, this.vertexBuffer);
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
         FloatBuffer vBuffer = BufferUtils.createFloatBuffer(vertices.length);
         vBuffer.put(vertices).flip();
         GL15.glBufferData(34962, vBuffer, 35044);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(0, 2, 5126, false, 16, 0L);
         GL20.glEnableVertexAttribArray(1);
         GL20.glVertexAttribPointer(1, 2, 5126, false, 16, 8L);
         GL15.glBindBuffer(34962, 0);
         GL30.glBindVertexArray(0);
         String vertSource = this.loadShaderResource("shaders/post/wet_surface.vsh");
         String fragSource = this.loadShaderResource("shaders/post/wet_surface.fsh");
         if (vertSource.isEmpty()) {
            vertSource = "#version 150 core\nin vec2 aPos;\nin vec2 aUv;\nout vec2 vUv;\nvoid main() {\n    vUv = aUv;\n    gl_Position = vec4(aPos, 0.0, 1.0);\n}";
         }

         this.shaderProgram = this.createProgram(vertSource, fragSource);
         this.initialized = true;
         System.out.println("[Rain] WetSurfaceRenderer initialized successfully");
      }
   }

   private String loadShaderResource(String relativePath) {
      try {
         InputStream stream = WetSurfaceRenderer.class.getClassLoader().getResourceAsStream("assets/astolfoclient/" + relativePath);
         if (stream == null) {
            stream = Minecraft.getInstance().getResourceManager().open(Identifier.fromNamespaceAndPath("astolfoclient", relativePath));
         }

         if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
               return reader.lines().collect(Collectors.joining("\n"));
            }
         }
      } catch (Exception var8) {
      }

      return "";
   }

   private int createProgram(String vert, String frag) {
      int v = GL20.glCreateShader(35633);
      GL20.glShaderSource(v, vert);
      GL20.glCompileShader(v);
      if (GL20.glGetShaderi(v, 35713) == 0) {
         System.err.println("WetSurface vertex shader compile error:\n" + GL20.glGetShaderInfoLog(v, 1024));
      }

      int f = GL20.glCreateShader(35632);
      GL20.glShaderSource(f, frag);
      GL20.glCompileShader(f);
      if (GL20.glGetShaderi(f, 35713) == 0) {
         System.err.println("WetSurface fragment shader compile error:\n" + GL20.glGetShaderInfoLog(f, 1024));
      }

      int p = GL20.glCreateProgram();
      GL20.glAttachShader(p, v);
      GL20.glAttachShader(p, f);
      GL20.glBindAttribLocation(p, 0, "aPos");
      GL20.glBindAttribLocation(p, 1, "aUv");
      GL20.glLinkProgram(p);
      if (GL20.glGetProgrami(p, 35714) == 0) {
         System.err.println("WetSurface shader link error:\n" + GL20.glGetProgramInfoLog(p, 1024));
      }

      GL20.glDeleteShader(v);
      GL20.glDeleteShader(f);
      return p;
   }

   private void setUniform1i(String name, int val) {
      int loc = GL20.glGetUniformLocation(this.shaderProgram, name);
      if (loc != -1) {
         GL20.glUniform1i(loc, val);
      }
   }

   private void setUniform1f(String name, float val) {
      int loc = GL20.glGetUniformLocation(this.shaderProgram, name);
      if (loc != -1) {
         GL20.glUniform1f(loc, val);
      }
   }

   private void setUniform2f(String name, float x, float y) {
      int loc = GL20.glGetUniformLocation(this.shaderProgram, name);
      if (loc != -1) {
         GL20.glUniform2f(loc, x, y);
      }
   }

   private void setUniform3f(String name, float x, float y, float z) {
      int loc = GL20.glGetUniformLocation(this.shaderProgram, name);
      if (loc != -1) {
         GL20.glUniform3f(loc, x, y, z);
      }
   }

   private void setUniformMatrix4f(String name, Matrix4f mat) {
      int loc = GL20.glGetUniformLocation(this.shaderProgram, name);
      if (loc != -1) {
         this.matrixBuffer.clear();
         mat.get(this.matrixBuffer);
         GL20.glUniformMatrix4fv(loc, false, this.matrixBuffer);
      }
   }

   public void reset() {
      this.disabled = false;
   }

   private static boolean isWindowValid(Minecraft mc) {
      Window window = mc == null ? null : mc.getWindow();
      return window != null && window.getWidth() > 0 && window.getHeight() > 0;
   }

   private static float clamp(float value, float min, float max) {
      return !Float.isFinite(value) ? min : Math.max(min, Math.min(max, value));
   }

   private static boolean canModifyGlObjects() {
      return RenderSystem.isOnRenderThread() && GLFW.glfwGetCurrentContext() != 0L;
   }

   private void deleteScene() {
      if (this.scene.framebuffer != 0 && canModifyGlObjects()) {
         GL30.glDeleteFramebuffers(this.scene.framebuffer);
      }

      if (this.scene.texture != 0 && canModifyGlObjects()) {
         GL11.glDeleteTextures(this.scene.texture);
      }

      this.scene.framebuffer = 0;
      this.scene.texture = 0;
      this.scene.width = 0;
      this.scene.height = 0;
   }

   @Override
   public void close() {
      if (!canModifyGlObjects()) {
         this.clearObjectIds();
      } else {
         this.deleteScene();
         if (this.blitFramebuffer != 0) {
            GL30.glDeleteFramebuffers(this.blitFramebuffer);
         }

         if (this.drawFramebuffer != 0) {
            GL30.glDeleteFramebuffers(this.drawFramebuffer);
         }

         if (this.vertexArray != 0) {
            GL30.glDeleteVertexArrays(this.vertexArray);
         }

         if (this.vertexBuffer != 0) {
            GL15.glDeleteBuffers(this.vertexBuffer);
         }

         if (this.shaderProgram != -1) {
            GL20.glDeleteProgram(this.shaderProgram);
         }

         this.clearObjectIds();
      }
   }

   private void clearObjectIds() {
      this.scene.framebuffer = 0;
      this.scene.texture = 0;
      this.scene.width = 0;
      this.scene.height = 0;
      this.blitFramebuffer = 0;
      this.drawFramebuffer = 0;
      this.vertexArray = 0;
      this.vertexBuffer = 0;
      this.shaderProgram = -1;
      this.initialized = false;
      this.disabled = false;
   }

   @Environment(EnvType.CLIENT)
   private static final class GL21 {
      private static final int GL_PIXEL_UNPACK_BUFFER = 35052;
      private static final int GL_PIXEL_UNPACK_BUFFER_BINDING = 35055;
   }

   @Environment(EnvType.CLIENT)
   public static final class Parameters {
      public float wetness = 1.0F;
      public float puddleCoverage = 0.82F;
      public float reflectionStrength = 1.25F;
      public float maxDistance = 56.0F;
      public float rippleStrength = 0.8F;
      public float rainAmount = 1.0F;
      public int reflectionSteps = 10;
      public boolean ripples = true;
      public float themeR = 1.0F;
      public float themeG = 1.0F;
      public float themeB = 1.0F;
      public boolean useThemeColor = false;
      public float sunDirX = 0.0F;
      public float sunDirY = 1.0F;
      public float sunDirZ = 0.0F;
      public float ambientLight = 1.0F;
      public float lightningFlash = 0.0F;
      public float cameraX = 0.0F;
      public float cameraY = 0.0F;
      public float cameraZ = 0.0F;
   }

   @Environment(EnvType.CLIENT)
   private static final class SceneTarget {
      int framebuffer;
      int texture;
      int width;
      int height;
   }
}
