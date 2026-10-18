package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(EnvType.CLIENT)
@Mixin(PlayerTabOverlay.class)
public class PlayerListHudMixin {
   @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
   public void onGetPlayerName(PlayerInfo entry, CallbackInfoReturnable<Component> cir) {
      Component originalText = (Component)cir.getReturnValue();
      if (originalText != null) {
         cir.setReturnValue(NameProtectModule.getProtectedText(originalText));
      } else {
         String originalStr = entry.getProfile().name();
         String protectedStr = NameProtectModule.getProtectedName(originalStr);
         if (!originalStr.equals(protectedStr)) {
            cir.setReturnValue(Component.literal(protectedStr));
         }
      }
   }
}
