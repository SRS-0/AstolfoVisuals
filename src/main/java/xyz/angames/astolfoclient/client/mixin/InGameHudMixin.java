package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={InGameHud.class})
public class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method={"renderStatusEffectOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        InterfaceModule iface;
        Module interfaceMod;
        if (AstolfoclientClient.moduleManager != null && (interfaceMod = AstolfoclientClient.moduleManager.getModuleByName("Interface")) instanceof InterfaceModule && (iface = (InterfaceModule)interfaceMod).isEnabled() && iface.effectHud.get()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderCrosshair"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Module crosshairModule = AstolfoclientClient.moduleManager.getModuleByName("Crosshair");
        if (crosshairModule != null && crosshairModule.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderHotbar"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Module hotbarModule = AstolfoclientClient.moduleManager.getModuleByName("CustomHotbar");
        if (hotbarModule != null && hotbarModule.isEnabled()) {
            ci.cancel();
            return;
        }
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0.0f, -offset);
        }
    }

    @Inject(method={"renderHotbar"}, at={@At(value="RETURN")})
    private void onAfterRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Module hotbarModule = AstolfoclientClient.moduleManager.getModuleByName("CustomHotbar");
        if (hotbarModule != null && hotbarModule.isEnabled()) {
            return;
        }
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.getMatrices().popMatrix();
        }
    }

    @Inject(method={"renderStatusBars"}, at={@At(value="HEAD")})
    private void onBeforeRenderStatusBars(DrawContext context, CallbackInfo ci) {
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0.0f, -offset);
        }
    }

    @Inject(method={"renderStatusBars"}, at={@At(value="RETURN")})
    private void onAfterRenderStatusBars(DrawContext context, CallbackInfo ci) {
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.getMatrices().popMatrix();
        }
    }

    

    

    

    

    @Inject(method={"renderHeldItemTooltip"}, at={@At(value="HEAD")})
    private void onBeforeRenderHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0.0f, -offset);
        }
    }

    @Inject(method={"renderHeldItemTooltip"}, at={@At(value="RETURN")})
    private void onAfterRenderHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.getMatrices().popMatrix();
        }
    }
}

