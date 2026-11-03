package xyz.angames.astolfoclient.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgram;

@Environment(value=EnvType.CLIENT)
public class ShaderManager {
    public static ShaderProgram ROUNDED_RECT_PROGRAM = null;
    public static ShaderProgram ROUNDED_BORDER_PROGRAM = null;
}

