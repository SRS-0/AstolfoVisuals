package xyz.angames.astolfoclient.client.compat;

/** The two vanilla core shaders the mod drew with in immediate mode. */
public enum CoreShaders {
   POSITION_COLOR("core/position_color", false),
   POSITION_TEX_COLOR("core/position_tex_color", true);

   public final String shader;
   public final boolean textured;

   CoreShaders(String shader, boolean textured) {
      this.shader = shader;
      this.textured = textured;
   }
}
