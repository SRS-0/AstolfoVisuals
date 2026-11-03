package xyz.angames.astolfoclient.client.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@Environment(value=EnvType.CLIENT)
public class FakePlayerEntity
extends OtherClientPlayerEntity {
    public static FakePlayerEntity instance = null;

    public FakePlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
        this.setHealth(20.0f);
    }

    public boolean isPushable() {
        return false;
    }

    public boolean isCollidable() {
        return false;
    }

    public boolean collidesWith(Entity other) {
        return false;
    }

    public void takeKnockback(double strength, double x, double z) {
    }

    public ItemStack getEquippedStack(EquipmentSlot slot) {
        if (slot == EquipmentSlot.OFFHAND) {
            return new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING);
        }
        return super.getEquippedStack(slot);
    }
}

