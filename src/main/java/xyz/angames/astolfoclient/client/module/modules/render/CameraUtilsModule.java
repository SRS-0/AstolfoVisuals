package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.platform.InputConstants;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.KeybindSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(EnvType.CLIENT)
public class CameraUtilsModule extends Module {
   public static CameraUtilsModule INSTANCE;
   public final KeybindSetting zoomKey = new KeybindSetting("Zoom Key", 67);
   public final NumberSetting zoomFov = new NumberSetting("Zoom FOV", 30.0, 5.0, 100.0, 1.0);
   public final NumberSetting zoomSpeed = new NumberSetting("Zoom Speed", 15.0, 1.0, 50.0, 1.0);
   public final BooleanSetting smoothMouse = new BooleanSetting("Smooth Mouse", true);
   private float zoomProgress = 0.0F;
   private long lastZoomTimeNs = -1L;
   private boolean originalSmoothCamera = false;
   private boolean zoomActive = false;

   public CameraUtilsModule() {
      super("CameraUtils", "Smooth zoom with customizable key, FOV and speed", Module.Category.RENDER);
      INSTANCE = this;
      this.addSettings(this.zoomKey, this.zoomFov, this.zoomSpeed, this.smoothMouse);
   }

   public static CameraUtilsModule getInstance() {
      if (INSTANCE != null) {
         return INSTANCE;
      } else if ((AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("CameraUtils") : null) instanceof CameraUtilsModule cm
         )
       {
         INSTANCE = cm;
         return cm;
      } else {
         return null;
      }
   }

   @Override
   public void onEnable() {
      this.zoomProgress = 0.0F;
      this.lastZoomTimeNs = -1L;
      this.zoomActive = false;
   }

   @Override
   public void onDisable() {
      this.zoomProgress = 0.0F;
      this.resetSmoothCamera();
   }

   private void resetSmoothCamera() {
      if (this.zoomActive) {
         this.zoomActive = false;
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.options != null) {
            mc.options.smoothCamera = this.originalSmoothCamera;
         }
      }
   }

   public double getAnimatedFov(double baseFov) {
      if (!this.isEnabled()) {
         if (this.zoomActive) {
            this.resetSmoothCamera();
         }

         this.zoomProgress = 0.0F;
         return baseFov;
      } else {
         Minecraft mc = Minecraft.getInstance();
         boolean isKeyDown = false;
         if (mc != null && mc.gui.screen() == null && this.zoomKey.getKey() != -1 && this.zoomKey.getKey() != 0) {
            isKeyDown = InputConstants.isKeyDown(xyz.angames.astolfoclient.client.compat.KeyMap.glfwToSdl(this.zoomKey.getKey()));
         }

         if (this.smoothMouse.get() && mc != null && mc.options != null) {
            if (isKeyDown && !this.zoomActive) {
               this.originalSmoothCamera = mc.options.smoothCamera;
               mc.options.smoothCamera = true;
               this.zoomActive = true;
            } else if (!isKeyDown && this.zoomActive) {
               mc.options.smoothCamera = this.originalSmoothCamera;
               this.zoomActive = false;
            }
         } else if (this.zoomActive) {
            this.resetSmoothCamera();
         }

         long nowNs = System.nanoTime();
         if (this.lastZoomTimeNs == -1L) {
            this.lastZoomTimeNs = nowNs;
         }

         double deltaSeconds = (nowNs - this.lastZoomTimeNs) / 1.0E9;
         this.lastZoomTimeNs = nowNs;
         if (deltaSeconds > 0.1) {
            deltaSeconds = 0.1;
         }

         float speed = (float)this.zoomSpeed.get();
         float targetProgress = isKeyDown ? 1.0F : 0.0F;
         this.zoomProgress = this.zoomProgress + (targetProgress - this.zoomProgress) * (float)(1.0 - Math.exp(-speed * deltaSeconds));
         if (this.zoomProgress < 0.001F) {
            this.zoomProgress = 0.0F;
            return baseFov;
         }

         if (this.zoomProgress > 0.999F && isKeyDown) {
            this.zoomProgress = 1.0F;
         }

         double targetFov = this.zoomFov.get();
         return Mth.lerp(this.zoomProgress, baseFov, targetFov);
      }
   }

   public float getZoomProgress() {
      return this.zoomProgress;
   }
}
