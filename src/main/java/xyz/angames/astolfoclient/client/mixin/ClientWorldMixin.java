package xyz.angames.astolfoclient.client.mixin;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.state.SkyRenderState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={SkyRendering.class})
public class ClientWorldMixin {
    @Inject(method="updateRenderState", at=@At("RETURN"))
    private void onGetSkyColor(ClientWorld world, float tickDelta, Camera camera, SkyRenderState state, CallbackInfo ci) {
        float strength;
        if (AstolfoclientClient.moduleManager == null) {
            return;
        }
        AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
        if (ambients != null && ambients.isEnabled() && ambients.customSkybox.get() && (strength = (float)ambients.skyboxStrength.get()) > 0.0f) {
            Color themeColor = new Color(ThemeManager.getThemedColor(0L));
            int originalColor = state.skyColor;
            int origR = originalColor >> 16 & 0xFF;
            int origG = originalColor >> 8 & 0xFF;
            int origB = originalColor & 0xFF;
            int themeR = themeColor.getRed();
            int themeG = themeColor.getGreen();
            int themeB = themeColor.getBlue();
            int finalR = (int)((float)origR * (1.0f - strength) + (float)themeR * strength);
            int finalG = (int)((float)origG * (1.0f - strength) + (float)themeG * strength);
            int finalB = (int)((float)origB * (1.0f - strength) + (float)themeB * strength);
            int finalColor = finalR << 16 | finalG << 8 | finalB;
            state.skyColor = finalColor;
        }
    }
}

