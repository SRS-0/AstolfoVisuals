package xyz.angames.astolfoclient.client.protection;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
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
        Minecraft mc;
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
        if ((mc = Minecraft.getInstance()) != null && mc.gui.hud != null && mc.gui.hud.getChat() != null) {
            mc.gui.hud.getChat().addClientSystemMessage((Component)Component.literal((String)("\u00a7c[Protection] \u00a7fBlocked crash exploit: \u00a7e" + title + " \u00a77(" + details + ")")));
        }
    }

    public boolean isMaliciousPacket(Packet<?> packet) {
        ClientboundEntityPositionSyncPacket entitySync;
        PositionMoveRotation values;
        ClientboundTeleportEntityPacket entityPos;
        ClientboundMoveVehiclePacket vehicleMove;
        Vec3 pos;
        Vec3 pos2;
        ClientboundPlayerPositionPacket teleport;
        PositionMoveRotation change;
        if (packet == null) {
            return false;
        }
        if (packet instanceof ClientboundExplodePacket) {
            Vec3 kb;
            ClientboundExplodePacket explosion = (ClientboundExplodePacket)packet;
            Vec3 center = explosion.center();
            if (center == null || ClientProtectionManager.isInvalidDouble(center.x) || ClientProtectionManager.isInvalidDouble(center.y) || ClientProtectionManager.isInvalidDouble(center.z) || Math.abs(center.x) > 3.0E7 || Math.abs(center.y) > 3.0E7 || Math.abs(center.z) > 3.0E7) {
                this.onCrashBlocked("Explosion Crash", "Center coordinates outside world boundary");
                return true;
            }
            if (explosion.playerKnockback().isPresent() && ((kb = (Vec3)explosion.playerKnockback().get()) == null || ClientProtectionManager.isInvalidDouble(kb.x) || ClientProtectionManager.isInvalidDouble(kb.y) || ClientProtectionManager.isInvalidDouble(kb.z) || Math.abs(kb.x) > 1000000.0 || Math.abs(kb.y) > 1000000.0 || Math.abs(kb.z) > 1000000.0)) {
                this.onCrashBlocked("Explosion Crash", "Malformed knockback vector");
                return true;
            }
        }
        if (packet instanceof ClientboundLevelParticlesPacket) {
            ClientboundLevelParticlesPacket particle = (ClientboundLevelParticlesPacket)packet;
            if (ClientProtectionManager.isInvalidDouble(particle.getX()) || ClientProtectionManager.isInvalidDouble(particle.getY()) || ClientProtectionManager.isInvalidDouble(particle.getZ()) || Math.abs(particle.getX()) > 3.0E7 || Math.abs(particle.getY()) > 3.0E7 || Math.abs(particle.getZ()) > 3.0E7) {
                this.onCrashBlocked("Particle Exploit", "Coordinates out of bounds");
                return true;
            }
            if (particle.getCount() > 1000 || particle.getCount() < 0) {
                this.onCrashBlocked("Particle Exploit", "Invalid count: " + particle.getCount());
                return true;
            }
            if (Float.isNaN(particle.getMaxSpeed()) || Float.isInfinite(particle.getMaxSpeed()) || Math.abs(particle.getMaxSpeed()) > 1000.0f) {
                this.onCrashBlocked("Particle Exploit", "Malformed particle speed");
                return true;
            }
            if (Float.isNaN(particle.getXDist()) || Float.isNaN(particle.getYDist()) || Float.isNaN(particle.getZDist()) || Math.abs(particle.getXDist()) > 1000.0f || Math.abs(particle.getYDist()) > 1000.0f || Math.abs(particle.getZDist()) > 1000.0f) {
                this.onCrashBlocked("Particle Exploit", "Malformed particle offset");
                return true;
            }
        }
        if (packet instanceof ClientboundUpdateAdvancementsPacket) {
            if (this.pendingAdvancements.size() >= 1600) {
                this.pendingAdvancements.poll();
                this.onCrashBlocked("Advancement Flood", "Queue exceeded limit (1600)");
            }
            this.pendingAdvancements.add(packet);
            this.lastAdvancementPacketTime = System.currentTimeMillis();
            return true;
        }
        if (packet instanceof ClientboundSetEntityMotionPacket) {
            ClientboundSetEntityMotionPacket vel = (ClientboundSetEntityMotionPacket)packet;
            double vx = Math.abs(vel.movement().x);
            double vy = Math.abs(vel.movement().y);
            double vz = Math.abs(vel.movement().z);
            if (ClientProtectionManager.isInvalidDouble(vx) || ClientProtectionManager.isInvalidDouble(vy) || ClientProtectionManager.isInvalidDouble(vz) || vx > 100000.0 || vy > 100000.0 || vz > 100000.0) {
                this.onCrashBlocked("Velocity Exploit", "Extreme entity velocity values");
                return true;
            }
        }
        if (packet instanceof ClientboundSoundPacket) {
            ClientboundSoundPacket sound = (ClientboundSoundPacket)packet;
            if (ClientProtectionManager.isInvalidDouble(sound.getX()) || ClientProtectionManager.isInvalidDouble(sound.getY()) || ClientProtectionManager.isInvalidDouble(sound.getZ()) || Math.abs(sound.getX()) > 3.0E7 || Math.abs(sound.getY()) > 3.0E7 || Math.abs(sound.getZ()) > 3.0E7) {
                this.onCrashBlocked("Sound Exploit", "Coordinates out of bounds");
                return true;
            }
            if (Float.isNaN(sound.getVolume()) || Float.isNaN(sound.getPitch()) || sound.getVolume() < 0.0f || sound.getVolume() > 100.0f || sound.getPitch() < 0.0f || sound.getPitch() > 100.0f) {
                this.onCrashBlocked("Sound Exploit", "Invalid sound volume/pitch");
                return true;
            }
        }
        if (packet instanceof ClientboundAddEntityPacket) {
            ClientboundAddEntityPacket spawn = (ClientboundAddEntityPacket)packet;
            if (ClientProtectionManager.isInvalidDouble(spawn.getX()) || ClientProtectionManager.isInvalidDouble(spawn.getY()) || ClientProtectionManager.isInvalidDouble(spawn.getZ()) || Math.abs(spawn.getX()) > 3.0E7 || Math.abs(spawn.getY()) > 3.0E7 || Math.abs(spawn.getZ()) > 3.0E7) {
                this.onCrashBlocked("Spawn Exploit", "Entity coordinates out of bounds");
                return true;
            }
            if (ClientProtectionManager.isInvalidDouble(spawn.getMovement().x) || ClientProtectionManager.isInvalidDouble(spawn.getMovement().y) || ClientProtectionManager.isInvalidDouble(spawn.getMovement().z) || Math.abs(spawn.getMovement().x) > 100000.0 || Math.abs(spawn.getMovement().y) > 100000.0 || Math.abs(spawn.getMovement().z) > 100000.0) {
                this.onCrashBlocked("Spawn Exploit", "Entity spawned with extreme velocity");
                return true;
            }
        }
        if (packet instanceof ClientboundPlayerPositionPacket && (change = (teleport = (ClientboundPlayerPositionPacket)packet).change()) != null) {
            pos2 = change.position();
            Vec3 delta = change.deltaMovement();
            if (pos2 == null || ClientProtectionManager.isInvalidDouble(pos2.x) || ClientProtectionManager.isInvalidDouble(pos2.y) || ClientProtectionManager.isInvalidDouble(pos2.z) || Math.abs(pos2.x) > 3.0E7 || Math.abs(pos2.y) > 3.0E7 || Math.abs(pos2.z) > 3.0E7 || delta != null && (ClientProtectionManager.isInvalidDouble(delta.x) || ClientProtectionManager.isInvalidDouble(delta.y) || ClientProtectionManager.isInvalidDouble(delta.z) || Math.abs(delta.x) > 1000000.0 || Math.abs(delta.y) > 1000000.0 || Math.abs(delta.z) > 1000000.0) || Float.isNaN(change.yRot()) || Float.isInfinite(change.yRot()) || Float.isNaN(change.xRot()) || Float.isInfinite(change.xRot())) {
                String coordsStr = pos2 != null ? String.format("X: %.1f, Y: %.1f, Z: %.1f", pos2.x, pos2.y, pos2.z) : "null";
                this.onCrashBlocked("Teleport Crash", "Invalid Coordinates (" + coordsStr + ")");
                try {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc != null && mc.getConnection() != null) {
                        mc.getConnection().send((Packet)new ServerboundAcceptTeleportationPacket(teleport.id()));
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
                return true;
            }
        }
        if (packet instanceof ClientboundMoveVehiclePacket && ((pos = (vehicleMove = (ClientboundMoveVehiclePacket)packet).position()) == null || ClientProtectionManager.isInvalidDouble(pos.x) || ClientProtectionManager.isInvalidDouble(pos.y) || ClientProtectionManager.isInvalidDouble(pos.z) || Math.abs(pos.x) > 3.0E7 || Math.abs(pos.y) > 3.0E7 || Math.abs(pos.z) > 3.0E7 || Float.isNaN(vehicleMove.yRot()) || Float.isInfinite(vehicleMove.yRot()) || Float.isNaN(vehicleMove.xRot()) || Float.isInfinite(vehicleMove.xRot()))) {
            this.onCrashBlocked("Vehicle Crash", "Invalid vehicle coordinates or rotation");
            return true;
        }
        if (packet instanceof ClientboundTeleportEntityPacket && (change = (entityPos = (ClientboundTeleportEntityPacket)packet).change()) != null && ((pos2 = change.position()) == null || ClientProtectionManager.isInvalidDouble(pos2.x) || ClientProtectionManager.isInvalidDouble(pos2.y) || ClientProtectionManager.isInvalidDouble(pos2.z) || Math.abs(pos2.x) > 3.0E7 || Math.abs(pos2.y) > 3.0E7 || Math.abs(pos2.z) > 3.0E7)) {
            this.onCrashBlocked("Entity Position Exploit", "Invalid coordinates for entity");
            return true;
        }
        if (packet instanceof ClientboundEntityPositionSyncPacket && (values = (entitySync = (ClientboundEntityPositionSyncPacket)packet).values()) != null && ((pos2 = values.position()) == null || ClientProtectionManager.isInvalidDouble(pos2.x) || ClientProtectionManager.isInvalidDouble(pos2.y) || ClientProtectionManager.isInvalidDouble(pos2.z) || Math.abs(pos2.x) > 3.0E7 || Math.abs(pos2.y) > 3.0E7 || Math.abs(pos2.z) > 3.0E7)) {
            this.onCrashBlocked("Entity Sync Exploit", "Invalid sync coordinates for entity");
            return true;
        }
        return false;
    }

    public void tick() {
        Minecraft mc;
        for (CrashAlert alert : this.activeAlerts) {
            if (!alert.isExpired()) continue;
            this.activeAlerts.remove(alert);
        }
        if (!this.pendingAdvancements.isEmpty() && System.currentTimeMillis() - this.lastAdvancementPacketTime >= 3000L && (mc = Minecraft.getInstance()).getConnection() != null) {
            while (!this.pendingAdvancements.isEmpty()) {
                Packet<?> p = this.pendingAdvancements.poll();
                if (p == null) continue;
                try {
                    ((Packet)p).handle(mc.getConnection());
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

