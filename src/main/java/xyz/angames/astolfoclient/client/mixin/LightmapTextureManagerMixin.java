package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Lightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Lightmap.class})
public class LightmapTextureManagerMixin {
    @Unique
    private NativeImage astolfo$image = null;
    @Unique
    private Object astolfo$texture = null;
    @Unique
    private Method astolfo$uploadMethod = null;

    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void onUpdateLightmap(CallbackInfo ci) {
        if (AstolfoclientClient.moduleManager == null) {
            return;
        }
        AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
        if (this.astolfo$image == null || this.astolfo$texture == null) {
            block4: for (Field field : Lightmap.class.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    String className;
                    Object value = field.get(this);
                    if (value instanceof NativeImage) {
                        this.astolfo$image = (NativeImage)value;
                        continue;
                    }
                    if (value == null || !(className = value.getClass().getSimpleName()).equals("DynamicTexture") && !className.equals("NativeImageBackedTexture")) continue;
                    this.astolfo$texture = value;
                    for (Method m : value.getClass().getMethods()) {
                        if (!m.getName().equals("upload") || m.getParameterCount() != 0) continue;
                        this.astolfo$uploadMethod = m;
                        continue block4;
                    }
                }
                catch (Exception value) {
                    // empty catch block
                }
            }
        }
        if (this.astolfo$image == null || this.astolfo$texture == null || this.astolfo$uploadMethod == null) {
            return;
        }
        Color themeColor = new Color(ThemeManager.getThemedColor(0L));
        int themeR = themeColor.getRed();
        int themeG = themeColor.getGreen();
        int themeB = themeColor.getBlue();
        for (int x = 0; x < 16; ++x) {
            for (int y = 0; y < 16; ++y) {
                int color = this.astolfo$image.getPixel(x, y);
                int a = color >> 24 & 0xFF;
                int r = color >> 16 & 0xFF;
                int g = color >> 8 & 0xFF;
                int b = color & 0xFF;
                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));
                int newColor = a << 24 | r << 16 | g << 8 | b;
                this.astolfo$image.setPixel(x, y, newColor);
            }
        }
        try {
            this.astolfo$uploadMethod.invoke(this.astolfo$texture, new Object[0]);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }
}

