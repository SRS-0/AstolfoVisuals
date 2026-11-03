package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import xyz.angames.astolfoclient.client.render.compat.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.effects.DamageIndicatorManager;
import xyz.angames.astolfoclient.client.module.modules.render.DamageIndicatorModule;

@Environment(value=EnvType.CLIENT)
public class DamageIndicatorRenderer {
    private final DamageIndicatorManager manager;

    public DamageIndicatorRenderer(DamageIndicatorManager manager) {
        this.manager = manager;
    }

    public void render(LevelRenderContext context) {
        DamageIndicatorModule module = (DamageIndicatorModule)AstolfoclientClient.moduleManager.getModuleByName("DamageIndicators");
        if (module == null || !module.isEnabled()) {
            return;
        }
        if (this.manager.getParticles().isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Font textRenderer = client.font;
        Camera camera = context.gameRenderer().mainCamera();
        PoseStack matrices = context.poseStack();
        double camX = camera.position().x;
        double camY = camera.position().y;
        double camZ = camera.position().z;
        float baseScale = (float)module.scale.get() * 0.02f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        for (DamageIndicatorManager.DamageParticle p : this.manager.getParticles()) {
            float t;
            float c1;
            float c3;
            float alpha = 1.0f;
            int fadeTime = 12;
            if (p.maxAge - p.age <= fadeTime) {
                float fadeProgress = (float)(p.maxAge - p.age) / (float)fadeTime;
                alpha = fadeProgress * fadeProgress * (3.0f - 2.0f * fadeProgress);
            }
            float popScale = 1.0f;
            float popDuration = 6.0f;
            if ((float)p.age < popDuration && (popScale = 1.0f + (c3 = (c1 = 1.70158f) + 1.0f) * (float)Math.pow((t = (float)p.age / popDuration) - 1.0f, 3.0) + c1 * (float)Math.pow(t - 1.0f, 2.0)) < 0.0f) {
                popScale = 0.0f;
            }
            int color = p.isCrit ? -65536 : -22016;
            int alphaHex = (int)(alpha * 255.0f) << 24;
            color = color & 0xFFFFFF | alphaHex;
            matrices.pushPose();
            matrices.translate(p.x - camX, p.y - camY, p.z - camZ);
            matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
            matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
            float currentScale = baseScale * (p.isCrit ? 1.3f : 1.0f) * popScale;
            matrices.scale(-currentScale, -currentScale, currentScale);
            Matrix4f positionMatrix = matrices.last().pose();
            MutableComponent text = Component.literal((String)p.text);
            float xOffset = (float)(-textRenderer.width((FormattedText)text)) / 2.0f;
            context.submitNodeCollector().submitText(matrices,xOffset,0.0f,text.getVisualOrderText(),true,Font.DisplayMode.SEE_THROUGH,0xF000F0,color,0,0);
            matrices.popPose();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
    }
}

