package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

public final class ShaderProgramKeys {
    private ShaderProgramKeys() {}
    public static final ShaderProgramKey POSITION_COLOR = new ShaderProgramKey(Identifier.fromNamespaceAndPath("astolfoclient", "core/compat_color"), DefaultVertexFormat.POSITION_COLOR, ShaderDefines.EMPTY);
    public static final ShaderProgramKey POSITION_TEX_COLOR = new ShaderProgramKey(Identifier.fromNamespaceAndPath("astolfoclient", "core/compat_texture"), DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY);
    public static final ShaderProgramKey POSITION_TEX = POSITION_TEX_COLOR;
}
