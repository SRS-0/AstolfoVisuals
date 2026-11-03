package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ClientPlayerInteractionManager.class})
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker(value="syncSelectedSlot")
    public void invokeSyncSelectedSlot();
}

