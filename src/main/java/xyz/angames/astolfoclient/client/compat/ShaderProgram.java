package xyz.angames.astolfoclient.client.compat;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/**
 * A mod shader whose parameters live in a single std140 {@code CustomUniforms} block. The block layout is read from
 * the fragment (or vertex) GLSL source so the Java side only has to call {@link Uniform#set} by name, mirroring the
 * old {@code CompiledShaderProgram} API.
 */
public final class ShaderProgram {
   private static final Pattern BLOCK = Pattern.compile("uniform\\s+CustomUniforms\\s*\\{([^}]*)\\}");
   private static final Pattern FIELD = Pattern.compile("(\\w+)\\s+(\\w+)\\s*;");
   private static final Pattern SAMPLER = Pattern.compile("uniform\\s+sampler2D\\s+(\\w+)\\s*;");

   public final Identifier id;
   public final VertexFormat format;
   private Map<String, Uniform> uniforms;
   private List<String> samplers;

   public ShaderProgram(Identifier id, VertexFormat format) {
      this.id = id;
      this.format = format;
   }

   /** Shader name as passed to the pipeline builder (namespace:core/name). */
   public Identifier shaderId() {
      return this.id;
   }

   private void load() {
      if (this.uniforms != null) {
         return;
      }

      Map<String, Uniform> map = new LinkedHashMap<>();
      List<String> samplerNames = new ArrayList<>();
      for (String ext : new String[]{"vsh", "fsh"}) {
         String src = read(this.id.withPath("shaders/" + this.id.getPath() + "." + ext));
         Matcher sm = SAMPLER.matcher(src);
         while (sm.find()) {
            if (!samplerNames.contains(sm.group(1))) {
               samplerNames.add(sm.group(1));
            }
         }

         Matcher bm = BLOCK.matcher(src);
         if (bm.find() && map.isEmpty()) {
            Matcher fm = FIELD.matcher(bm.group(1));
            while (fm.find()) {
               map.put(fm.group(2), new Uniform(fm.group(1)));
            }
         }
      }

      this.samplers = samplerNames;
      this.uniforms = map;
   }

   private static String read(Identifier resource) {
      try (InputStream in = Minecraft.getInstance().getResourceManager().open(resource)) {
         return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      } catch (Exception e) {
         return "";
      }
   }

   public Uniform getUniform(String name) {
      this.load();
      return this.uniforms.get(name);
   }

   public List<String> samplers() {
      this.load();
      return this.samplers;
   }

   public boolean hasUniforms() {
      this.load();
      return !this.uniforms.isEmpty();
   }

   /** Writes the block in declaration order. */
   public void write(Std140Builder b) {
      this.load();
      for (Uniform u : this.uniforms.values()) {
         u.write(b);
      }
   }

   public int blockSize() {
      this.load();
      return Math.max(16, this.uniforms.size() * 64);
   }

   public static final class Uniform {
      private final String type;
      private final float[] f = new float[16];

      private Uniform(String type) {
         this.type = type;
      }

      public void set(float a) {
         this.f[0] = a;
      }

      public void set(int a) {
         this.f[0] = a;
      }

      public void set(float a, float b) {
         this.f[0] = a;
         this.f[1] = b;
      }

      public void set(float a, float b, float c) {
         this.f[0] = a;
         this.f[1] = b;
         this.f[2] = c;
      }

      public void set(float a, float b, float c, float d) {
         this.f[0] = a;
         this.f[1] = b;
         this.f[2] = c;
         this.f[3] = d;
      }

      public void set(Matrix4f m) {
         m.get(this.f);
      }

      public void set(float[] values) {
         System.arraycopy(values, 0, this.f, 0, Math.min(values.length, this.f.length));
      }

      private void write(Std140Builder b) {
         switch (this.type) {
            case "float" -> b.putFloat(this.f[0]);
            case "int", "bool" -> b.putInt((int)this.f[0]);
            case "vec2" -> b.putVec2(this.f[0], this.f[1]);
            case "vec3" -> b.putVec3(this.f[0], this.f[1], this.f[2]);
            case "vec4" -> b.putVec4(this.f[0], this.f[1], this.f[2], this.f[3]);
            case "ivec2" -> b.putIVec2((int)this.f[0], (int)this.f[1]);
            case "mat4" -> b.putMat4f(new Matrix4f().set(this.f));
            default -> throw new IllegalStateException("Unsupported uniform type " + this.type);
         }
      }
   }
}
