package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.misc.PasswordHiderModule;

@Environment(EnvType.CLIENT)
@Mixin(ChatScreen.class)
public class ChatScreenMixin {
   @Shadow
   protected EditBox input;

   @Inject(method = "init", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      if (this.input != null) {
         PasswordHiderModule.setupChatField(this.input);
      }
   }

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void onRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (this.input != null) {
         PasswordHiderModule.renderChatFieldOverlay(context, this.input);
      }
   }
}
