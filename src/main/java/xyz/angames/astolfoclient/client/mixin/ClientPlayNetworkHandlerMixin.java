package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;

@Environment(EnvType.CLIENT)
@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
   @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
   private void onSendChatMessage(String message, CallbackInfo ci) {
      if (AstolfoclientClient.commandManager != null) {
         if (AstolfoclientClient.commandManager.handleCommand(message)) {
            ci.cancel();
         }
      }
   }
}
