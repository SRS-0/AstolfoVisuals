package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.misc.PasswordHiderModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ChatScreen.class})
public class ChatScreenMixin {
    @Shadow
    protected TextFieldWidget chatField;

    @Inject(method={"init"}, at={@At(value="TAIL")})
    private void onInit(CallbackInfo ci) {
        if (this.chatField != null) {
            PasswordHiderModule.setupChatField(this.chatField);
        }
    }

    @Inject(method={"render"}, at={@At(value="TAIL")})
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.chatField != null) {
            PasswordHiderModule.renderChatFieldOverlay(context, this.chatField);
        }
    }
}

