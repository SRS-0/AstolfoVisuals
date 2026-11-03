package xyz.angames.astolfoclient.client.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

@Environment(value=EnvType.CLIENT)
public class FakePlayerEntity
extends RemotePlayer {
    public static FakePlayerEntity instance = null;

    public FakePlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
        this.setHealth(20.0f);
    }

    public boolean isPushable() {
        return false;
    }

    public boolean isCollidable() {
        return false;
    }

    public boolean canCollideWith(Entity other) {
        return false;
    }

    public void knockback(double strength, double x, double z) {
    }

    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.OFFHAND) {
            return new ItemStack((ItemLike)Items.TOTEM_OF_UNDYING);
        }
        return super.getItemBySlot(slot);
    }
}

