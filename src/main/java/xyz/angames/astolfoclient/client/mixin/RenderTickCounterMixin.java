package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.util.TimerManager;

@Environment(value=EnvType.CLIENT)
@Mixin(value={RenderTickCounter.Dynamic.class})
public class RenderTickCounterMixin {
    @Shadow
    private float tickTime;
    @Shadow
    private long lastTimeMillis;
    @Shadow
    private float dynamicDeltaTicks;
    @Shadow
    private float tickProgress;

    @Inject(method={"beginRenderTick(J)I"}, at={@At(value="HEAD")}, cancellable=true)
    private void onBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        if (AstolfoclientClient.getInstance() != null) {
            float multiplier = TimerManager.getTimer();
            if(multiplier==1.0f)return;
            float elapsed = (float)(timeMillis - this.lastTimeMillis) / this.tickTime;
            this.dynamicDeltaTicks = elapsed * multiplier;
            this.lastTimeMillis = timeMillis;
            this.tickProgress += this.dynamicDeltaTicks;
            int i = (int)this.tickProgress;
            this.tickProgress -= (float)i;
            cir.setReturnValue(i);
        }
    }
}

