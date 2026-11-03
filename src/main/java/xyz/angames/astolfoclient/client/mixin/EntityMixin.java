package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Entity.class})
public abstract class EntityMixin {
    @Inject(method={"getCustomName"}, at={@At(value="RETURN")}, cancellable=true)
    private void onGetCustomName(CallbackInfoReturnable<Component> cir) {
        Component protectedText;
        Component original = (Component)cir.getReturnValue();
        if (original != null && (protectedText = NameProtectModule.getProtectedText(original)) != null && protectedText != original) {
            cir.setReturnValue(protectedText);
        }
    }
}

