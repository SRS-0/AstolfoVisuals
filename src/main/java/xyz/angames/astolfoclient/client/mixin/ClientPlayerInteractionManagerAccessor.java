package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(value=EnvType.CLIENT)
@Mixin(value={MultiPlayerGameMode.class})
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker(value="ensureHasSentCarriedItem")
    public void invokeSyncSelectedSlot();
}

