package xyz.angames.astolfoclient.client.mixin;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={SkyRenderer.class})
public class ClientWorldMixin {
    @Inject(method="extractRenderState", at=@At("RETURN"))
    private void onGetSkyColor(ClientLevel world, float tickDelta, Camera camera, SkyRenderState state, CallbackInfo ci) {
        float strength;
        if (AstolfoclientClient.moduleManager == null) {
            return;
        }
        AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
        if (ambients != null && ambients.isEnabled() && ambients.customSkybox.get() && (strength = (float)ambients.skyboxStrength.get()) > 0.0f) {
            Color themeColor = new Color(ThemeManager.getThemedColor(0L));
            state.skyColor = new org.joml.Vector3f(state.skyColor).lerp(new org.joml.Vector3f(themeColor.getRed()/255f,themeColor.getGreen()/255f,themeColor.getBlue()/255f),strength);
        }
    }
}

