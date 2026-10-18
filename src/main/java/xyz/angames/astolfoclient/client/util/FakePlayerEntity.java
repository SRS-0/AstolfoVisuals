package xyz.angames.astolfoclient.client.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;

@Environment(EnvType.CLIENT)
public class FakePlayerEntity extends RemotePlayer {
   public static FakePlayerEntity instance = null;

   public FakePlayerEntity(ClientLevel world, GameProfile profile) {
      super(world, profile);
      this.setHealth(20.0F);
   }

   public boolean isPushable() {
      return false;
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public boolean canCollideWith(Entity other) {
      return false;
   }

   public void knockback(double strength, double x, double z) {
   }

   public ItemStack getItemBySlot(EquipmentSlot slot) {
      return slot == EquipmentSlot.OFFHAND ? new ItemStack(Items.TOTEM_OF_UNDYING) : super.getItemBySlot(slot);
   }
}
