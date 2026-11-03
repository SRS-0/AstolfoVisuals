package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(value=EnvType.CLIENT)
@Mixin(value={MinecraftClient.class})
public interface MinecraftClientAccessor {
    @Accessor(value="itemUseCooldown")
    public int getItemUseCooldown();

    @Accessor(value="itemUseCooldown")
    public void setItemUseCooldown(int var1);

    @Invoker(value="doAttack")
    public boolean invokeDoAttack();

    @Invoker(value="doItemUse")
    public void invokeDoItemUse();
}

