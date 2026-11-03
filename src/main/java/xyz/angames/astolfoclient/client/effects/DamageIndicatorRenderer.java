package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
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

    public void render(WorldRenderContext context) {
        DamageIndicatorModule module = (DamageIndicatorModule)AstolfoclientClient.moduleManager.getModuleByName("DamageIndicators");
        if (module == null || !module.isEnabled()) {
            return;
        }
        if (this.manager.getParticles().isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        Camera camera = context.gameRenderer().getCamera();
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider.Immediate vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
        double camX = camera.getCameraPos().x;
        double camY = camera.getCameraPos().y;
        double camZ = camera.getCameraPos().z;
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
            matrices.push();
            matrices.translate(p.x - camX, p.y - camY, p.z - camZ);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            float currentScale = baseScale * (p.isCrit ? 1.3f : 1.0f) * popScale;
            matrices.scale(-currentScale, -currentScale, currentScale);
            Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
            MutableText text = Text.literal((String)p.text);
            float xOffset = (float)(-textRenderer.getWidth((StringVisitable)text)) / 2.0f;
            textRenderer.draw((Text)text, xOffset, 0.0f, color, true, positionMatrix, (VertexConsumerProvider)vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            matrices.pop();
        }
        vertexConsumers.draw();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
    }
}

