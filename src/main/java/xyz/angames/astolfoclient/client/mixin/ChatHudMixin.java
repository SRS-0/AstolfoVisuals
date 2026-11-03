package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;
import xyz.angames.astolfoclient.client.module.modules.misc.PasswordHiderModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ChatHud.class})
public class ChatHudMixin {
    @ModifyVariable(method={"addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V"}, at=@At(value="HEAD"), argsOnly=true)
    private Text modifyChatMessages(Text message) {
        message = PasswordHiderModule.getProtectedChat(message);
        return NameProtectModule.getProtectedText(message);
    }
}

