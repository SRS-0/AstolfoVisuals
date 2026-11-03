package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Minecraft.class})
public interface MinecraftClientAccessor {
    @Accessor(value="rightClickDelay")
    public int getItemUseCooldown();

    @Accessor(value="rightClickDelay")
    public void setItemUseCooldown(int var1);

    @Invoker(value="startAttack")
    public boolean invokeDoAttack();

    @Invoker(value="startUseItem")
    public void invokeDoItemUse();
}

