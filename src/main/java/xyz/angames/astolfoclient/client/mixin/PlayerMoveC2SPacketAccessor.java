package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ServerboundMovePlayerPacket.class})
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

    @Accessor(value="yRot")
    @Mutable
    public void setYaw(float var1);

    @Accessor(value="xRot")
    @Mutable
    public void setPitch(float var1);

    @Accessor(value="hasPos")
    @Mutable
    public void setChangePosition(boolean var1);

    @Accessor(value="hasRot")
    @Mutable
    public void setChangeLook(boolean var1);
}

