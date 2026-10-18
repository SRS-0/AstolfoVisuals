package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.effects.ChinaHatFeatureRenderer;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(EnvType.CLIENT)
@Mixin(AvatarRenderer.class)
public class PlayerEntityRendererMixin {
   @Inject(
      method = "extractRenderState",
      at = @At("HEAD")
   )
   private void onUpdateRenderState(net.minecraft.world.entity.Avatar player, AvatarRenderState state, float tickDelta, CallbackInfo ci) {
      ChinaHatFeatureRenderer.currentlyRenderingPlayer = player instanceof net.minecraft.world.entity.player.Player p ? p : null;
   }
}
