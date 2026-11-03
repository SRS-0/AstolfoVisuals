package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.protection.ClientProtectionManager;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ClientConnection.class})
public abstract class ClientConnectionMixin {
    @Inject(method={"handlePacket"}, at={@At(value="HEAD")}, cancellable=true)
    private static void receivePackets(Packet<?> packet, PacketListener listener, CallbackInfo callbackInfo) {
        if (ClientProtectionManager.getInstance().isMaliciousPacket(packet)) {
            callbackInfo.cancel();
            return;
        }
        if (packet instanceof BundleS2CPacket) {
            BundleS2CPacket bundlePacket = (BundleS2CPacket)packet;
            for (Packet innerPacket : bundlePacket.getPackets()) {
                if (!ClientProtectionManager.getInstance().isMaliciousPacket(innerPacket)) continue;
                callbackInfo.cancel();
                return;
            }
        }
    }
}

