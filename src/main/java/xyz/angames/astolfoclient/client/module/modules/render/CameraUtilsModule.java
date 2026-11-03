package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.KeybindSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class CameraUtilsModule
extends Module {
    public static CameraUtilsModule INSTANCE;
    public final KeybindSetting zoomKey = new KeybindSetting("Zoom Key", 67);
    public final NumberSetting zoomFov = new NumberSetting("Zoom FOV", 30.0, 5.0, 100.0, 1.0);
    public final NumberSetting zoomSpeed = new NumberSetting("Zoom Speed", 15.0, 1.0, 50.0, 1.0);
    public final BooleanSetting smoothMouse = new BooleanSetting("Smooth Mouse", true);
    private float zoomProgress = 0.0f;
    private long lastZoomTimeNs = -1L;
    private boolean originalSmoothCamera = false;
    private boolean zoomActive = false;

    public CameraUtilsModule() {
        super("CameraUtils", "Smooth zoom with customizable key, FOV and speed", Module.Category.RENDER);
        INSTANCE = this;
        this.addSettings(this.zoomKey, this.zoomFov, this.zoomSpeed, this.smoothMouse);
    }

    public static CameraUtilsModule getInstance() {
        Module mod;
        if (INSTANCE != null) {
            return INSTANCE;
        }
        Module module = mod = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("CameraUtils") : null;
        if (mod instanceof CameraUtilsModule) {
            CameraUtilsModule cm;
            INSTANCE = cm = (CameraUtilsModule)mod;
            return cm;
        }
        return null;
    }

    @Override
    public void onEnable() {
        this.zoomProgress = 0.0f;
        this.lastZoomTimeNs = -1L;
        this.zoomActive = false;
    }

    @Override
    public void onDisable() {
        this.zoomProgress = 0.0f;
        this.resetSmoothCamera();
    }

    private void resetSmoothCamera() {
        if (this.zoomActive) {
            this.zoomActive = false;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.options != null) {
                mc.options.smoothCameraEnabled = this.originalSmoothCamera;
            }
        }
    }

    public double getAnimatedFov(double baseFov) {
        if (!this.isEnabled()) {
            if (this.zoomActive) {
                this.resetSmoothCamera();
            }
            this.zoomProgress = 0.0f;
            return baseFov;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isKeyDown = false;
        if (mc != null && mc.currentScreen == null && this.zoomKey.getKey() != -1 && this.zoomKey.getKey() != 0) {
            isKeyDown = InputUtil.isKeyPressed(mc.getWindow(), (int)this.zoomKey.getKey());
        }
        if (this.smoothMouse.get() && mc != null && mc.options != null) {
            if (isKeyDown && !this.zoomActive) {
                this.originalSmoothCamera = mc.options.smoothCameraEnabled;
                mc.options.smoothCameraEnabled = true;
                this.zoomActive = true;
            } else if (!isKeyDown && this.zoomActive) {
                mc.options.smoothCameraEnabled = this.originalSmoothCamera;
                this.zoomActive = false;
            }
        } else if (this.zoomActive) {
            this.resetSmoothCamera();
        }
        long nowNs = System.nanoTime();
        if (this.lastZoomTimeNs == -1L) {
            this.lastZoomTimeNs = nowNs;
        }
        double deltaSeconds = (double)(nowNs - this.lastZoomTimeNs) / 1.0E9;
        this.lastZoomTimeNs = nowNs;
        if (deltaSeconds > 0.1) {
            deltaSeconds = 0.1;
        }
        float speed = (float)this.zoomSpeed.get();
        float targetProgress = isKeyDown ? 1.0f : 0.0f;
        this.zoomProgress += (targetProgress - this.zoomProgress) * (float)(1.0 - Math.exp((double)(-speed) * deltaSeconds));
        if (this.zoomProgress < 0.001f) {
            this.zoomProgress = 0.0f;
            return baseFov;
        }
        if (this.zoomProgress > 0.999f && isKeyDown) {
            this.zoomProgress = 1.0f;
        }
        double targetFov = this.zoomFov.get();
        return MathHelper.lerp((double)this.zoomProgress, (double)baseFov, (double)targetFov);
    }

    public float getZoomProgress() {
        return this.zoomProgress;
    }
}

