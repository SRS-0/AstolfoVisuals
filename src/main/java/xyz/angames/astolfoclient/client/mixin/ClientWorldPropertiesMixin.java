package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ClientWorld.Properties.class})
public class ClientWorldPropertiesMixin {
    @Inject(method={"getTimeOfDay"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        if (AstolfoclientClient.moduleManager == null) {
            return;
        }
        AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
        if (ambients != null && ambients.isEnabled() && ambients.customTime.get()) {
            cir.setReturnValue(((long)ambients.time.get()));
        }
    }
}

