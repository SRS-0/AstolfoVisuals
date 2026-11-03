package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={PlayerListHud.class})
public class PlayerListHudMixin {
    @Inject(method={"getPlayerName"}, at={@At(value="RETURN")}, cancellable=true)
    public void onGetPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        Text originalText = (Text)cir.getReturnValue();
        if (originalText != null) {
            cir.setReturnValue(NameProtectModule.getProtectedText(originalText));
        } else {
            String protectedStr;
            String originalStr = entry.getProfile().name();
            if (!originalStr.equals(protectedStr = NameProtectModule.getProtectedName(originalStr))) {
                cir.setReturnValue(Text.literal((String)protectedStr));
            }
        }
    }
}

