package xyz.angames.astolfoclient.client.protection;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityPosition;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(value=EnvType.CLIENT)
public class ClientProtectionManager {
    private static final ClientProtectionManager INSTANCE = new ClientProtectionManager();
    private final List<CrashAlert> activeAlerts = new CopyOnWriteArrayList<CrashAlert>();
    private long lastSoundTime = 0L;
    private static final int ADVANCEMENT_QUEUE_LIMIT = 1600;
    private static final long ADVANCEMENT_QUIET_PERIOD_MS = 3000L;
    private final Queue<Packet<?>> pendingAdvancements = new ConcurrentLinkedQueue();
    private volatile long lastAdvancementPacketTime = 0L;

    public static ClientProtectionManager getInstance() {
        return INSTANCE;
    }

    public static void init() {
    }

    public List<CrashAlert> getActiveAlerts() {
        return this.activeAlerts;
    }

    public void onCrashBlocked(String title, String details) {
        MinecraftClient mc;
        long now = System.currentTimeMillis();
        if (now - this.lastSoundTime > 400L) {
            this.lastSoundTime = now;
            ModSounds.playCrashDetectionSound();
        }
        boolean hasDuplicate = false;
        for (CrashAlert alert : this.activeAlerts) {
            if (!alert.title.equalsIgnoreCase(title) || now - alert.timestamp >= 1500L) continue;
            hasDuplicate = true;
            break;
        }
        if (!hasDuplicate) {
            this.activeAlerts.add(new CrashAlert(title, details, now));
            while (this.activeAlerts.size() > 3) {
                this.activeAlerts.remove(0);
            }
        }
        if ((mc = MinecraftClient.getInstance()) != null && mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
            mc.inGameHud.getChatHud().addMessage((Text)Text.literal((String)("\u00a7c[Protection] \u00a7fBlocked crash exploit: \u00a7e" + title + " \u00a77(" + details + ")")));
        }
    }

    public boolean isMaliciousPacket(Packet<?> packet) {
        EntityPositionSyncS2CPacket entitySync;
        EntityPosition values;
        EntityPositionS2CPacket entityPos;
        VehicleMoveS2CPacket vehicleMove;
        Vec3d pos;
        Vec3d pos2;
        PlayerPositionLookS2CPacket teleport;
        EntityPosition change;
        if (packet == null) {
            return false;
        }
        if (packet instanceof ExplosionS2CPacket) {
            Vec3d kb;
            ExplosionS2CPacket explosion = (ExplosionS2CPacket)packet;
            Vec3d center = explosion.center();
            if (center == null || ClientProtectionManager.isInvalidDouble(center.x) || ClientProtectionManager.isInvalidDouble(center.y) || ClientProtectionManager.isInvalidDouble(center.z) || Math.abs(center.x) > 3.0E7 || Math.abs(center.y) > 3.0E7 || Math.abs(center.z) > 3.0E7) {
                this.onCrashBlocked("Explosion Crash", "Center coordinates outside world boundary");
                return true;
            }
            if (explosion.playerKnockback().isPresent() && ((kb = (Vec3d)explosion.playerKnockback().get()) == null || ClientProtectionManager.isInvalidDouble(kb.x) || ClientProtectionManager.isInvalidDouble(kb.y) || ClientProtectionManager.isInvalidDouble(kb.z) || Math.abs(kb.x) > 1000000.0 || Math.abs(kb.y) > 1000000.0 || Math.abs(kb.z) > 1000000.0)) {
                this.onCrashBlocked("Explosion Crash", "Malformed knockback vector");
                return true;
            }
        }
        if (packet instanceof ParticleS2CPacket) {
            ParticleS2CPacket particle = (ParticleS2CPacket)packet;
            if (ClientProtectionManager.isInvalidDouble(particle.getX()) || ClientProtectionManager.isInvalidDouble(particle.getY()) || ClientProtectionManager.isInvalidDouble(particle.getZ()) || Math.abs(particle.getX()) > 3.0E7 || Math.abs(particle.getY()) > 3.0E7 || Math.abs(particle.getZ()) > 3.0E7) {
                this.onCrashBlocked("Particle Exploit", "Coordinates out of bounds");
                return true;
            }
            if (particle.getCount() > 1000 || particle.getCount() < 0) {
                this.onCrashBlocked("Particle Exploit", "Invalid count: " + particle.getCount());
                return true;
            }
            if (Float.isNaN(particle.getSpeed()) || Float.isInfinite(particle.getSpeed()) || Math.abs(particle.getSpeed()) > 1000.0f) {
                this.onCrashBlocked("Particle Exploit", "Malformed particle speed");
                return true;
            }
            if (Float.isNaN(particle.getOffsetX()) || Float.isNaN(particle.getOffsetY()) || Float.isNaN(particle.getOffsetZ()) || Math.abs(particle.getOffsetX()) > 1000.0f || Math.abs(particle.getOffsetY()) > 1000.0f || Math.abs(particle.getOffsetZ()) > 1000.0f) {
                this.onCrashBlocked("Particle Exploit", "Malformed particle offset");
                return true;
            }
        }
        if (packet instanceof AdvancementUpdateS2CPacket) {
            if (this.pendingAdvancements.size() >= 1600) {
                this.pendingAdvancements.poll();
                this.onCrashBlocked("Advancement Flood", "Queue exceeded limit (1600)");
            }
            this.pendingAdvancements.add(packet);
            this.lastAdvancementPacketTime = System.currentTimeMillis();
            return true;
        }
        if (packet instanceof EntityVelocityUpdateS2CPacket) {
            EntityVelocityUpdateS2CPacket vel = (EntityVelocityUpdateS2CPacket)packet;
            double vx = Math.abs(vel.getVelocity().x);
            double vy = Math.abs(vel.getVelocity().y);
            double vz = Math.abs(vel.getVelocity().z);
            if (ClientProtectionManager.isInvalidDouble(vx) || ClientProtectionManager.isInvalidDouble(vy) || ClientProtectionManager.isInvalidDouble(vz) || vx > 100000.0 || vy > 100000.0 || vz > 100000.0) {
                this.onCrashBlocked("Velocity Exploit", "Extreme entity velocity values");
                return true;
            }
        }
        if (packet instanceof PlaySoundS2CPacket) {
            PlaySoundS2CPacket sound = (PlaySoundS2CPacket)packet;
            if (ClientProtectionManager.isInvalidDouble(sound.getX()) || ClientProtectionManager.isInvalidDouble(sound.getY()) || ClientProtectionManager.isInvalidDouble(sound.getZ()) || Math.abs(sound.getX()) > 3.0E7 || Math.abs(sound.getY()) > 3.0E7 || Math.abs(sound.getZ()) > 3.0E7) {
                this.onCrashBlocked("Sound Exploit", "Coordinates out of bounds");
                return true;
            }
            if (Float.isNaN(sound.getVolume()) || Float.isNaN(sound.getPitch()) || sound.getVolume() < 0.0f || sound.getVolume() > 100.0f || sound.getPitch() < 0.0f || sound.getPitch() > 100.0f) {
                this.onCrashBlocked("Sound Exploit", "Invalid sound volume/pitch");
                return true;
            }
        }
        if (packet instanceof EntitySpawnS2CPacket) {
            EntitySpawnS2CPacket spawn = (EntitySpawnS2CPacket)packet;
            if (ClientProtectionManager.isInvalidDouble(spawn.getX()) || ClientProtectionManager.isInvalidDouble(spawn.getY()) || ClientProtectionManager.isInvalidDouble(spawn.getZ()) || Math.abs(spawn.getX()) > 3.0E7 || Math.abs(spawn.getY()) > 3.0E7 || Math.abs(spawn.getZ()) > 3.0E7) {
                this.onCrashBlocked("Spawn Exploit", "Entity coordinates out of bounds");
                return true;
            }
            if (ClientProtectionManager.isInvalidDouble(spawn.getVelocity().x) || ClientProtectionManager.isInvalidDouble(spawn.getVelocity().y) || ClientProtectionManager.isInvalidDouble(spawn.getVelocity().z) || Math.abs(spawn.getVelocity().x) > 100000.0 || Math.abs(spawn.getVelocity().y) > 100000.0 || Math.abs(spawn.getVelocity().z) > 100000.0) {
                this.onCrashBlocked("Spawn Exploit", "Entity spawned with extreme velocity");
                return true;
            }
        }
        if (packet instanceof PlayerPositionLookS2CPacket && (change = (teleport = (PlayerPositionLookS2CPacket)packet).change()) != null) {
            pos2 = change.position();
            Vec3d delta = change.deltaMovement();
            if (pos2 == null || ClientProtectionManager.isInvalidDouble(pos2.x) || ClientProtectionManager.isInvalidDouble(pos2.y) || ClientProtectionManager.isInvalidDouble(pos2.z) || Math.abs(pos2.x) > 3.0E7 || Math.abs(pos2.y) > 3.0E7 || Math.abs(pos2.z) > 3.0E7 || delta != null && (ClientProtectionManager.isInvalidDouble(delta.x) || ClientProtectionManager.isInvalidDouble(delta.y) || ClientProtectionManager.isInvalidDouble(delta.z) || Math.abs(delta.x) > 1000000.0 || Math.abs(delta.y) > 1000000.0 || Math.abs(delta.z) > 1000000.0) || Float.isNaN(change.yaw()) || Float.isInfinite(change.yaw()) || Float.isNaN(change.pitch()) || Float.isInfinite(change.pitch())) {
                String coordsStr = pos2 != null ? String.format("X: %.1f, Y: %.1f, Z: %.1f", pos2.x, pos2.y, pos2.z) : "null";
                this.onCrashBlocked("Teleport Crash", "Invalid Coordinates (" + coordsStr + ")");
                try {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null && mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().sendPacket((Packet)new TeleportConfirmC2SPacket(teleport.teleportId()));
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                return true;
            }
        }
        if (packet instanceof VehicleMoveS2CPacket && ((pos = (vehicleMove = (VehicleMoveS2CPacket)packet).position()) == null || ClientProtectionManager.isInvalidDouble(pos.x) || ClientProtectionManager.isInvalidDouble(pos.y) || ClientProtectionManager.isInvalidDouble(pos.z) || Math.abs(pos.x) > 3.0E7 || Math.abs(pos.y) > 3.0E7 || Math.abs(pos.z) > 3.0E7 || Float.isNaN(vehicleMove.yaw()) || Float.isInfinite(vehicleMove.yaw()) || Float.isNaN(vehicleMove.pitch()) || Float.isInfinite(vehicleMove.pitch()))) {
            this.onCrashBlocked("Vehicle Crash", "Invalid vehicle coordinates or rotation");
            return true;
        }
        if (packet instanceof EntityPositionS2CPacket && (change = (entityPos = (EntityPositionS2CPacket)packet).change()) != null && ((pos2 = change.position()) == null || ClientProtectionManager.isInvalidDouble(pos2.x) || ClientProtectionManager.isInvalidDouble(pos2.y) || ClientProtectionManager.isInvalidDouble(pos2.z) || Math.abs(pos2.x) > 3.0E7 || Math.abs(pos2.y) > 3.0E7 || Math.abs(pos2.z) > 3.0E7)) {
            this.onCrashBlocked("Entity Position Exploit", "Invalid coordinates for entity");
            return true;
        }
        if (packet instanceof EntityPositionSyncS2CPacket && (values = (entitySync = (EntityPositionSyncS2CPacket)packet).values()) != null && ((pos2 = values.position()) == null || ClientProtectionManager.isInvalidDouble(pos2.x) || ClientProtectionManager.isInvalidDouble(pos2.y) || ClientProtectionManager.isInvalidDouble(pos2.z) || Math.abs(pos2.x) > 3.0E7 || Math.abs(pos2.y) > 3.0E7 || Math.abs(pos2.z) > 3.0E7)) {
            this.onCrashBlocked("Entity Sync Exploit", "Invalid sync coordinates for entity");
            return true;
        }
        return false;
    }

    public void tick() {
        MinecraftClient mc;
        for (CrashAlert alert : this.activeAlerts) {
            if (!alert.isExpired()) continue;
            this.activeAlerts.remove(alert);
        }
        if (!this.pendingAdvancements.isEmpty() && System.currentTimeMillis() - this.lastAdvancementPacketTime >= 3000L && (mc = MinecraftClient.getInstance()).getNetworkHandler() != null) {
            while (!this.pendingAdvancements.isEmpty()) {
                Packet<?> p = this.pendingAdvancements.poll();
                if (p == null) continue;
                try {
                    ((Packet)p).apply(mc.getNetworkHandler());
                }
                catch (Exception exception) {}
            }
        }
    }

    private static boolean isInvalidDouble(double val) {
        return Double.isNaN(val) || Double.isInfinite(val);
    }

    @Environment(value=EnvType.CLIENT)
    public static class CrashAlert {
        public final String title;
        public final String details;
        public final long timestamp;
        public float anim = 0.0f;

        public CrashAlert(String title, String details, long timestamp) {
            this.title = title;
            this.details = details;
            this.timestamp = timestamp;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > 4500L;
        }
    }
}

