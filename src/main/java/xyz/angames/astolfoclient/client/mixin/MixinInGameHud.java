package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={InGameHud.class})
public class MixinInGameHud {
    @Inject(method={"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.scoreboard.get()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderPortalOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderPortalOverlay(DrawContext context, float f, CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.portal.get()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderPumpkinOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.pumpkin.get() && texture.getPath().contains("pumpkin")) {
            ci.cancel();
        }
    }
}

