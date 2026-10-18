package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;
import xyz.angames.astolfoclient.client.module.modules.misc.PasswordHiderModule;

@Environment(EnvType.CLIENT)
@Mixin(ChatComponent.class)
public class ChatHudMixin {
   @ModifyVariable(
      method = "addMessage",
      at = @At("HEAD"),
      argsOnly = true
   )
   private Component modifyChatMessages(Component message) {
      message = PasswordHiderModule.getProtectedChat(message);
      return NameProtectModule.getProtectedText(message);
   }
}
