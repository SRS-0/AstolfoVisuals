package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={net.minecraft.client.ClientClockManager.class})
public class ClientWorldPropertiesMixin {
    @Inject(method={"getTotalTicks"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetTimeOfDay(net.minecraft.core.Holder<net.minecraft.world.clock.WorldClock> clock, CallbackInfoReturnable<Long> cir) {
        if (!clock.is(net.minecraft.world.clock.WorldClocks.OVERWORLD) || AstolfoclientClient.moduleManager == null) {
            return;
        }
        AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
        if (ambients != null && ambients.isEnabled() && ambients.customTime.get()) {
            cir.setReturnValue(((long)ambients.time.get()));
        }
    }
}

