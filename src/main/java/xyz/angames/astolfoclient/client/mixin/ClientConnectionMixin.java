package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.protection.ClientProtectionManager;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Connection.class})
public abstract class ClientConnectionMixin {
    @Inject(method={"genericsFtw"}, at={@At(value="HEAD")}, cancellable=true)
    private static void receivePackets(Packet<?> packet, PacketListener listener, CallbackInfo callbackInfo) {
        if (ClientProtectionManager.getInstance().isMaliciousPacket(packet)) {
            callbackInfo.cancel();
            return;
        }
        if (packet instanceof ClientboundBundlePacket) {
            ClientboundBundlePacket bundlePacket = (ClientboundBundlePacket)packet;
            for (Packet innerPacket : bundlePacket.subPackets()) {
                if (!ClientProtectionManager.getInstance().isMaliciousPacket(innerPacket)) continue;
                callbackInfo.cancel();
                return;
            }
        }
    }
}

