package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.util.TimerManager;

@Environment(value=EnvType.CLIENT)
@Mixin(value={DeltaTracker.Timer.class})
public class RenderTickCounterMixin {
    @Shadow
    private float msPerTick;
    @Shadow
    private long lastMs;
    @Shadow
    private float deltaTicks;
    @Shadow
    private float deltaTickResidual;

    @Inject(method={"advanceGameTime(J)I"}, at={@At(value="HEAD")}, cancellable=true)
    private void onBeginRenderTick(long timeMillis, CallbackInfoReturnable<Integer> cir) {
        if (AstolfoclientClient.getInstance() != null) {
            float multiplier = TimerManager.getTimer();
            if(multiplier==1.0f)return;
            float elapsed = (float)(timeMillis - this.lastMs) / this.msPerTick;
            this.deltaTicks = elapsed * multiplier;
            this.lastMs = timeMillis;
            this.deltaTickResidual += this.deltaTicks;
            int i = (int)this.deltaTickResidual;
            this.deltaTickResidual -= (float)i;
            cir.setReturnValue(i);
        }
    }
}

