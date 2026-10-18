package xyz.angames.astolfoclient.client.render.shader;

import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.GL20;

@Environment(EnvType.CLIENT)
public class ShaderUtil {
   private final int programID;

   public ShaderUtil(String fragmentShaderLocation) {
      int program = GL20.glCreateProgram();
      int vertexShader = this.createShader("assets/astolfoclient/shaders/vertex.vsh", 35633);
      int fragmentShader = this.createShader(fragmentShaderLocation, 35632);
      GL20.glAttachShader(program, vertexShader);
      GL20.glAttachShader(program, fragmentShader);
      GL20.glBindAttribLocation(program, 0, "position");
      GL20.glBindAttribLocation(program, 1, "texCoord");
      GL20.glLinkProgram(program);
      GL20.glDeleteShader(vertexShader);
      GL20.glDeleteShader(fragmentShader);
      this.programID = program;
   }

   public void bind() {
      RenderSystem.assertOnRenderThread();
      GL20.glUseProgram(this.programID);
   }

   public void unbind() {
      GL20.glUseProgram(0);
   }

   public int getProgramID() {
      return this.programID;
   }

   public void setUniform1f(String name, float value) {
      GL20.glUniform1f(GL20.glGetUniformLocation(this.programID, name), value);
   }

   public void setUniform2f(String name, float x, float y) {
      GL20.glUniform2f(GL20.glGetUniformLocation(this.programID, name), x, y);
   }

   public void setUniform3f(String name, float x, float y, float z) {
      GL20.glUniform3f(GL20.glGetUniformLocation(this.programID, name), x, y, z);
   }

   public void setUniform1i(String name, int value) {
      GL20.glUniform1i(GL20.glGetUniformLocation(this.programID, name), value);
   }

   private int createShader(String path, int shaderType) {
      int shader = GL20.glCreateShader(shaderType);

      try {
         String resPath = path.startsWith("/") ? path.substring(1) : path;
         InputStream stream = ShaderUtil.class.getClassLoader().getResourceAsStream(resPath);
         if (stream == null) {
            stream = ShaderUtil.class.getResourceAsStream("/" + resPath);
         }

         if (stream == null) {
            throw new RuntimeException("Cannot find shader: " + path);
         }

         String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
         GL20.glShaderSource(shader, source);
         GL20.glCompileShader(shader);
         if (GL20.glGetShaderi(shader, 35713) == 0) {
            System.out.println(GL20.glGetShaderInfoLog(shader, 512));
            throw new RuntimeException("Shader compilation failed: " + path);
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      return shader;
   }
}
