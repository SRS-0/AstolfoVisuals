package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.render.compat.CustomClock;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;
@Mixin(ClientClockManager.ClientClockInstance.class)
public class ClockInstanceMixin implements CustomClock {
 @Unique private boolean astolfo$overworld;
 public void astolfo$setOverworld(boolean value){astolfo$overworld=value;}
 @Inject(method="totalTicks",at=@At("HEAD"),cancellable=true)
 private void astolfo$time(CallbackInfoReturnable<Long> cir){if(!astolfo$overworld||AstolfoclientClient.moduleManager==null)return;var mod=(AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");if(mod!=null&&mod.isEnabled()&&mod.customTime.get())cir.setReturnValue((long)mod.time.get());}
}
