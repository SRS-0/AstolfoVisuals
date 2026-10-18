package xyz.angames.astolfoclient.client.module.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(EnvType.CLIENT)
public class TargetEspModule extends Module {
   public final ModeSetting mode = new ModeSetting("Mode", "Cube", "Cube", "Circle", "Ghost", "Diamond");
   public final ModeSetting cubeTexture = new ModeSetting("Texture", "Classic", "Classic", "Rounded", "Frame", "Jeka", "Vegas") {
      @Override
      public boolean isVisible() {
         return TargetEspModule.this.mode.is("Cube");
      }
   };
   public final ModeSetting circleMode = new ModeSetting("Circle Mode", "Jello", "Jello", "Mimbran") {
      @Override
      public boolean isVisible() {
         return TargetEspModule.this.mode.is("Circle");
      }
   };
   public final BooleanSetting distortion = new BooleanSetting("Distortion", true);
   public final NumberSetting distortionStrength = new NumberSetting("Strength", 1.0, 0.2, 2.5, 0.1) {
      @Override
      public boolean isVisible() {
         return TargetEspModule.this.distortion.get();
      }
   };
   public final BooleanSetting chromatic = new BooleanSetting("Chromatic", true) {
      @Override
      public boolean isVisible() {
         return TargetEspModule.this.distortion.get();
      }
   };

   public TargetEspModule() {
      super("TargetESP", Module.Category.RENDER);
      this.addSetting(this.mode);
      this.addSetting(this.cubeTexture);
      this.addSetting(this.circleMode);
      this.addSetting(this.distortion);
      this.addSetting(this.distortionStrength);
      this.addSetting(this.chromatic);
   }

   public static void addTargetEffect(Entity target) {
      if (target != null) {
         if (AstolfoclientClient.moduleManager.getModuleByName("TargetESP") instanceof TargetEspModule tem && tem.isEnabled()) {
            String m = tem.mode.get();
            if ("Cube".equals(m) && AstolfoclientClient.targetEspManager != null) {
               AstolfoclientClient.targetEspManager.addEffect(target);
            } else if ("Circle".equals(m) && AstolfoclientClient.circleEspManager != null) {
               AstolfoclientClient.circleEspManager.addEffect(target);
            } else if ("Ghost".equals(m) && AstolfoclientClient.ghostEspManager != null) {
               AstolfoclientClient.ghostEspManager.addEffect(target);
            } else if ("Diamond".equals(m) && AstolfoclientClient.diamondEspManager != null) {
               AstolfoclientClient.diamondEspManager.addEffect(target);
            }
         }
      }
   }

   public static void addTargetAttack(Entity target) {
      if (target != null) {
         if (AstolfoclientClient.moduleManager.getModuleByName("TargetESP") instanceof TargetEspModule tem && tem.isEnabled()) {
            String m = tem.mode.get();
            if ("Ghost".equals(m) && AstolfoclientClient.ghostEspManager != null) {
               AstolfoclientClient.ghostEspManager.addAttack(target);
            } else if ("Diamond".equals(m) && AstolfoclientClient.diamondEspManager != null) {
               AstolfoclientClient.diamondEspManager.addAttack(target);
            }
         }
      }
   }
}
