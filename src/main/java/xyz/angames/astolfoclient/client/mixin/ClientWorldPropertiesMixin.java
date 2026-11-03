package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.ClientClockManager;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.render.compat.CustomClock;
@Mixin(ClientClockManager.class)
public class ClientWorldPropertiesMixin {
 @Inject(method="getInstance(Lnet/minecraft/core/Holder;)Lnet/minecraft/client/ClientClockManager$ClientClockInstance;",at=@At("RETURN"))
 private void astolfo$tag(Holder<WorldClock> clock,CallbackInfoReturnable<ClientClockManager.ClientClockInstance> cir){((CustomClock)cir.getReturnValue()).astolfo$setOverworld(clock.is(WorldClocks.OVERWORLD));}
}
