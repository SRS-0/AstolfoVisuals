package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public interface MinecraftClientAccessor {
   @Accessor("rightClickDelay")
   int getItemUseCooldown();

   @Accessor("rightClickDelay")
   void setItemUseCooldown(int var1);

   @Invoker("startAttack")
   boolean invokeDoAttack();

   @Invoker("startUseItem")
   void invokeDoItemUse();
}
