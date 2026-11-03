package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(value=EnvType.CLIENT)
@Mixin(value={PlayerMoveC2SPacket.class})
public interface PlayerMoveC2SPacketAccessor {
    @Accessor(value="horizontalCollision")
    public boolean getHorizontalCollision();

    @Accessor(value="onGround")
    @Mutable
    public void setOnGround(boolean var1);

    @Accessor(value="x")
    @Mutable
    public void setX(double var1);

    @Accessor(value="y")
    @Mutable
    public void setY(double var1);

    @Accessor(value="z")
    @Mutable
    public void setZ(double var1);

    @Accessor(value="yaw")
    @Mutable
    public void setYaw(float var1);

    @Accessor(value="pitch")
    @Mutable
    public void setPitch(float var1);

    @Accessor(value="changePosition")
    @Mutable
    public void setChangePosition(boolean var1);

    @Accessor(value="changeLook")
    @Mutable
    public void setChangeLook(boolean var1);
}

