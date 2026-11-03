package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
@Mixin(value={Hud.class})
public class InGameHudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method={"extractEffects"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderStatusEffectOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        InterfaceModule iface;
        Module interfaceMod;
        if (AstolfoclientClient.moduleManager != null && (interfaceMod = AstolfoclientClient.moduleManager.getModuleByName("Interface")) instanceof InterfaceModule && (iface = (InterfaceModule)interfaceMod).isEnabled() && iface.effectHud.get()) {
            ci.cancel();
        }
    }

    @Inject(method={"extractCrosshair"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        Module crosshairModule = AstolfoclientClient.moduleManager.getModuleByName("Crosshair");
        if (crosshairModule != null && crosshairModule.isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method={"extractItemHotbar"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderHotbar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        Module hotbarModule = AstolfoclientClient.moduleManager.getModuleByName("CustomHotbar");
        if (hotbarModule != null && hotbarModule.isEnabled()) {
            ci.cancel();
            return;
        }
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.pose().pushMatrix();
            context.pose().translate(0.0f, -offset);
        }
    }

    @Inject(method={"extractItemHotbar"}, at={@At(value="RETURN")})
    private void onAfterRenderHotbar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        Module hotbarModule = AstolfoclientClient.moduleManager.getModuleByName("CustomHotbar");
        if (hotbarModule != null && hotbarModule.isEnabled()) {
            return;
        }
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.pose().popMatrix();
        }
    }

    @Inject(method={"extractPlayerHealth"}, at={@At(value="HEAD")})
    private void onBeforeRenderStatusBars(GuiGraphicsExtractor context, CallbackInfo ci) {
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.pose().pushMatrix();
            context.pose().translate(0.0f, -offset);
        }
    }

    @Inject(method={"extractPlayerHealth"}, at={@At(value="RETURN")})
    private void onAfterRenderStatusBars(GuiGraphicsExtractor context, CallbackInfo ci) {
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.pose().popMatrix();
        }
    }

    

    

    

    

    @Inject(method={"extractSelectedItemName"}, at={@At(value="HEAD")})
    private void onBeforeRenderHeldItemTooltip(GuiGraphicsExtractor context, CallbackInfo ci) {
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.pose().pushMatrix();
            context.pose().translate(0.0f, -offset);
        }
    }

    @Inject(method={"extractSelectedItemName"}, at={@At(value="RETURN")})
    private void onAfterRenderHeldItemTooltip(GuiGraphicsExtractor context, CallbackInfo ci) {
        float offset = LogoRenderer.getHotbarYOffset();
        if (offset > 0.01f) {
            context.pose().popMatrix();
        }
    }
}

