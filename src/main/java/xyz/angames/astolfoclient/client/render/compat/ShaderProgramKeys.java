package xyz.angames.astolfoclient.client.render.compat;

import net.minecraft.client.gl.Defines;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public final class ShaderProgramKeys {
    private ShaderProgramKeys() {}
    public static final ShaderProgramKey POSITION_COLOR = new ShaderProgramKey(Identifier.of("astolfoclient", "core/compat_color"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    public static final ShaderProgramKey POSITION_TEX_COLOR = new ShaderProgramKey(Identifier.of("astolfoclient", "core/compat_texture"), VertexFormats.POSITION_TEXTURE_COLOR, Defines.EMPTY);
    public static final ShaderProgramKey POSITION_TEX = POSITION_TEX_COLOR;
}
