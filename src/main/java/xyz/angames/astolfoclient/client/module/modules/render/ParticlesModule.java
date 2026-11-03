package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class ParticlesModule
extends Module {
    public final BooleanSetting hits = new BooleanSetting("Hit Particles", true);
    public final EnumSetting<ParticleType> type = new EnumSetting<ParticleType>("Hit Type", ParticleType.STARS){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.hits.get();
        }
    };
    public final NumberSetting amount = new NumberSetting("Hit Amount", 5.0, 1.0, 50.0, 1.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.hits.get();
        }
    };
    public final NumberSetting lifespan = new NumberSetting("Hit Life (ms)", 5000.0, 500.0, 10000.0, 100.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.hits.get();
        }
    };
    public final BooleanSetting totemPop = new BooleanSetting("Totem Pop", true);
    public final EnumSetting<ParticleType> totemType = new EnumSetting<ParticleType>("Totem Type", ParticleType.STARS){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.totemPop.get();
        }
    };
    public final ModeSetting totemAnimation = new ModeSetting("Totem Animation", "Explosion", new String[]{"Explosion", "Sphere", "Spiral", "Fountain", "Shockwave"}){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.totemPop.get();
        }
    };
    public final NumberSetting totemAmount = new NumberSetting("Totem Amount", 40.0, 5.0, 200.0, 5.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.totemPop.get();
        }
    };
    public final NumberSetting totemLifespan = new NumberSetting("Totem Life (ms)", 2500.0, 200.0, 10000.0, 100.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.totemPop.get();
        }
    };
    public final ModeSetting totemColor = new ModeSetting("Totem Color", "Vanilla", new String[]{"Vanilla", "Theme"}){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.totemPop.get();
        }
    };
    public final BooleanSetting totemPhysics = new BooleanSetting("Totem Physics", true){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.totemPop.get();
        }
    };
    public final BooleanSetting arrows = new BooleanSetting("Arrow Trails", true);
    public final EnumSetting<ParticleType> arrowType = new EnumSetting<ParticleType>("Arrow Type", ParticleType.STARS){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.arrows.get();
        }
    };
    public final NumberSetting arrowAmount = new NumberSetting("Arrow Amount", 1.0, 1.0, 10.0, 1.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.arrows.get();
        }
    };
    public final NumberSetting arrowLifespan = new NumberSetting("Arrow Life (ms)", 1500.0, 100.0, 5000.0, 100.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.arrows.get();
        }
    };
    public final BooleanSetting pearls = new BooleanSetting("Pearl Trails", true);
    public final EnumSetting<ParticleType> pearlType = new EnumSetting<ParticleType>("Pearl Type", ParticleType.BUBBLES){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.pearls.get();
        }
    };
    public final NumberSetting pearlAmount = new NumberSetting("Pearl Amount", 2.0, 1.0, 10.0, 1.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.pearls.get();
        }
    };
    public final NumberSetting pearlLifespan = new NumberSetting("Pearl Life (ms)", 2000.0, 100.0, 5000.0, 100.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.pearls.get();
        }
    };
    public final BooleanSetting tridents = new BooleanSetting("Trident Trails", true);
    public final EnumSetting<ParticleType> tridentType = new EnumSetting<ParticleType>("Trident Type", ParticleType.BUBBLES){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.tridents.get();
        }
    };
    public final NumberSetting tridentAmount = new NumberSetting("Trident Amount", 2.0, 1.0, 10.0, 1.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.tridents.get();
        }
    };
    public final NumberSetting tridentLifespan = new NumberSetting("Trident Life (ms)", 2000.0, 100.0, 5000.0, 100.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.tridents.get();
        }
    };
    public final BooleanSetting items = new BooleanSetting("Item Trails", false);
    public final EnumSetting<ParticleType> itemType = new EnumSetting<ParticleType>("Item Type", ParticleType.DOLLARS){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.items.get();
        }
    };
    public final NumberSetting itemAmount = new NumberSetting("Item Amount", 1.0, 1.0, 10.0, 1.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.items.get();
        }
    };
    public final NumberSetting itemLifespan = new NumberSetting("Item Life (ms)", 3000.0, 100.0, 5000.0, 100.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.items.get();
        }
    };
    public final BooleanSetting walk = new BooleanSetting("Walk Particles", false);
    public final EnumSetting<ParticleType> walkType = new EnumSetting<ParticleType>("Walk Type", ParticleType.STARS){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.walk.get();
        }
    };
    public final NumberSetting walkAmount = new NumberSetting("Walk Amount", 1.0, 1.0, 10.0, 1.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.walk.get();
        }
    };
    public final NumberSetting walkLifespan = new NumberSetting("Walk Life (ms)", 1000.0, 100.0, 5000.0, 100.0){

        @Override
        public boolean isVisible() {
            return ParticlesModule.this.walk.get();
        }
    };

    public ParticlesModule() {
        super("Particles", Module.Category.RENDER);
        this.addSetting(this.hits);
        this.addSetting(this.type);
        this.addSetting(this.amount);
        this.addSetting(this.lifespan);
        this.addSetting(this.totemPop);
        this.addSetting(this.totemType);
        this.addSetting(this.totemAnimation);
        this.addSetting(this.totemAmount);
        this.addSetting(this.totemLifespan);
        this.addSetting(this.totemColor);
        this.addSetting(this.totemPhysics);
        this.addSetting(this.arrows);
        this.addSetting(this.arrowType);
        this.addSetting(this.arrowAmount);
        this.addSetting(this.arrowLifespan);
        this.addSetting(this.pearls);
        this.addSetting(this.pearlType);
        this.addSetting(this.pearlAmount);
        this.addSetting(this.pearlLifespan);
        this.addSetting(this.tridents);
        this.addSetting(this.tridentType);
        this.addSetting(this.tridentAmount);
        this.addSetting(this.tridentLifespan);
        this.addSetting(this.items);
        this.addSetting(this.itemType);
        this.addSetting(this.itemAmount);
        this.addSetting(this.itemLifespan);
        this.addSetting(this.walk);
        this.addSetting(this.walkType);
        this.addSetting(this.walkAmount);
        this.addSetting(this.walkLifespan);
    }

    @Environment(value=EnvType.CLIENT)
    public static enum ParticleType {
        BUBBLES,
        STARS,
        DOLLARS,
        HEART,
        SMOLESTAR;

    }
}

