package xyz.angames.astolfoclient.client.compat;

/**
 * Drop-in for {@link org.lwjgl.opengl.GL20} used by the mod's raw-GL effects. Every other call is inherited; shader
 * sources are rewritten for the 26.x depth convention (reversed Z, zero-to-one clip range) before compiling.
 */
public class GL20 extends org.lwjgl.opengl.GL20 {
   protected GL20() {
   }

   public static void glShaderSource(int shader, CharSequence source) {
      org.lwjgl.opengl.GL20.glShaderSource(shader, reverseZ(source.toString()));
   }

   public static void glShaderSource(int shader, CharSequence... sources) {
      StringBuilder sb = new StringBuilder();
      for (CharSequence s : sources) {
         sb.append(s);
      }

      glShaderSource(shader, sb);
   }

   /** Rewrites the forward-Z depth idioms used by the 1.21.4 shaders. */
   public static String reverseZ(String src) {
      return src
         // "push to the far plane": far is now depth 0.
         .replace("pos.w * 0.9999", "pos.w * 0.0001")
         .replaceAll("(\\w+)\\.xyww", "vec4($1.xy, 0.0, $1.w)")
         // sky test on a sampled depth buffer: sky is now 0.
         .replaceAll("(\\w*[dD]epth\\w*) >= 0\\.9999", "$1 <= 0.0001")
         // depth -> NDC z: already in [0, 1] with zero-to-one clip control.
         .replaceAll("(\\w*[dD]epth\\w*) \\* 2\\.0 - 1\\.0", "$1");
   }
}
