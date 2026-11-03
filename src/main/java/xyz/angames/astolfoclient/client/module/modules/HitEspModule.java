package xyz.angames.astolfoclient.client.module.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class HitEspModule
extends Module {
    public final NumberSetting size = new NumberSetting("Size", 1.5, 0.1, 5.0, 0.1);
    public final NumberSetting leaveTime = new NumberSetting("Leave Time", 350.0, 50.0, 2000.0, 50.0);
    public final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 0.004, 0.0, 0.05, 0.001);
    public final NumberSetting spawnAnimDuration = new NumberSetting("Spawn Duration", 400.0, 50.0, 2000.0, 50.0);
    public final BooleanSetting doExplosion = new BooleanSetting("Do Explosion", true);
    public final NumberSetting fadeTime = new NumberSetting("Fade Time", 300.0, 50.0, 2000.0, 50.0){

        @Override
        public boolean isVisible() {
            return !HitEspModule.this.doExplosion.get();
        }
    };
    public final NumberSetting gridSize = new NumberSetting("Grid Size", 4.0, 2.0, 10.0, 1.0){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get();
        }
    };
    public final NumberSetting explosionStrength = new NumberSetting("Explos Strength", 1.0, 0.1, 5.0, 0.1){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get();
        }
    };
    public final NumberSetting explosionRadius = new NumberSetting("Explos Radius", 0.0, 0.0, 2.0, 0.1){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get();
        }
    };
    public final NumberSetting gravity = new NumberSetting("Gravity", 0.05, -0.2, 0.5, 0.01){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get();
        }
    };
    public final NumberSetting friction = new NumberSetting("Friction", 0.92, 0.5, 1.0, 0.01){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get();
        }
    };
    public final NumberSetting groundLifespan = new NumberSetting("Ground Lifespan", 1000.0, 100.0, 5000.0, 100.0){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get();
        }
    };
    public final NumberSetting spinSpeed = new NumberSetting("Spin Speed", 1.0, 0.0, 5.0, 0.1){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get();
        }
    };
    public final BooleanSetting shrinkOnGround = new BooleanSetting("Shrink On Ground", true){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get();
        }
    };
    public final BooleanSetting bounce = new BooleanSetting("Bounce", false){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get();
        }
    };
    public final NumberSetting bounceFactor = new NumberSetting("Bounce Factor", 0.5, 0.1, 0.9, 0.05){

        @Override
        public boolean isVisible() {
            return HitEspModule.this.doExplosion.get() && HitEspModule.this.bounce.get();
        }
    };

    public HitEspModule() {
        super("HitESP", Module.Category.RENDER);
        this.addSettings(this.size, this.leaveTime, this.rotationSpeed, this.spawnAnimDuration, this.doExplosion, this.fadeTime, this.gridSize, this.explosionStrength, this.explosionRadius, this.gravity, this.friction, this.groundLifespan, this.spinSpeed, this.shrinkOnGround, this.bounce, this.bounceFactor);
    }
}

