package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ScoreboardObjective.class})
public class ScoreboardObjectiveMixin {
    @Inject(method={"getDisplayName"}, at={@At(value="RETURN")}, cancellable=true)
    public void onGetDisplayName(CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(NameProtectModule.getProtectedText((Text)cir.getReturnValue()));
    }
}

