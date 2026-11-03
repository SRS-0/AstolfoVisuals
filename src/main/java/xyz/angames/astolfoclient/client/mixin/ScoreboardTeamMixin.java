package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Team.class})
public class ScoreboardTeamMixin {
    @Inject(method={"getPrefix"}, at={@At(value="RETURN")}, cancellable=true)
    public void onGetPrefix(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(NameProtectModule.getProtectedText((Text)cir.getReturnValue()));
    }

    @Inject(method={"getSuffix"}, at={@At(value="RETURN")}, cancellable=true)
    public void onGetSuffix(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(NameProtectModule.getProtectedText((Text)cir.getReturnValue()));
    }

    @Inject(method={"decorateName"}, at={@At(value="RETURN")}, cancellable=true)
    public void onDecorateName(CallbackInfoReturnable<MutableText> cir) {
        Text protectedText = NameProtectModule.getProtectedText((Text)cir.getReturnValue());
        if (protectedText != null) {
            cir.setReturnValue(protectedText.copy());
        }
    }
}

