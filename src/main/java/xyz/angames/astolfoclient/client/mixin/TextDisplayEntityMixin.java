package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={DisplayEntity.TextDisplayEntity.class})
public class TextDisplayEntityMixin {
    @Inject(method={"getText"}, at={@At(value="RETURN")}, cancellable=true)
    public void onGetText(CallbackInfoReturnable<Text> cir) {
        if (cir.getReturnValue() != null) {
            cir.setReturnValue(NameProtectModule.getProtectedText((Text)cir.getReturnValue()));
        }
    }
}

