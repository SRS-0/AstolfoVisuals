package xyz.angames.astolfoclient.client.effects;
import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import xyz.angames.astolfoclient.client.render.compat.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.effects.CircleEspManager;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class CircleEspGhostRenderer {
    public void render(LevelRenderContext context, List<CircleEspManager.CircleEspEffect> effects) {
        if (effects.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc((int)515);
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.ONE);
        Tesselator tessellator = Tesselator.getInstance();
        long currentTime = System.currentTimeMillis();
        PoseStack matrices = context.poseStack();
        double camX = context.gameRenderer().mainCamera().position().x;
        double camY = context.gameRenderer().mainCamera().position().y;
        double camZ = context.gameRenderer().mainCamera().position().z;
        Quaternionf cameraRot = context.gameRenderer().mainCamera().rotation();
        for (CircleEspManager.CircleEspEffect effect : effects) {
            long timeSinceHit;
            float fadeProgress;
            float baseAlpha;
            Entity target = effect.target;
            if (!target.isAlive() || TargetUtils.isInvisible(target) || (baseAlpha = 1.0f - Math.max(0.0f, ((fadeProgress = Mth.clamp((float)((float)(timeSinceHit = currentTime - effect.lastHitTime) / 450.0f), (float)0.0f, (float)1.0f)) - 0.5f) * 2.0f)) <= 0.05f) continue;
            float animTime = (float)(currentTime - effect.startTime) / 1000.0f;
            float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            double tX = Mth.lerp((double)tickDelta, (double)target.xOld, (double)target.getX()) - camX;
            double tY = Mth.lerp((double)tickDelta, (double)target.yOld, (double)target.getY()) - camY;
            double tZ = Mth.lerp((double)tickDelta, (double)target.zOld, (double)target.getZ()) - camZ;
            float height = target.getBbHeight();
            float radius = target.getBbWidth() / 2.0f + 0.1f;
            if (timeSinceHit > 400L) {
                float endProgress = (float)(timeSinceHit - 400L) / 200.0f;
                radius *= Math.max(0.0f, 1.0f - endProgress);
            }
            float speedY = 3.5f;
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            float mainY = (float)((Math.sin(animTime * speedY) + 1.0) / 2.0) * height;
            Color headColor = new Color(ThemeManager.getThemedColor(0L));
            float rH = (float)headColor.getRed() / 255.0f;
            float gH = (float)headColor.getGreen() / 255.0f;
            float bH = (float)headColor.getBlue() / 255.0f;
            for (int angle = 0; angle < 360; angle += 10) {
                double rad = Math.toRadians(angle);
                float pX = (float)Math.cos(rad) * radius;
                float pZ = (float)Math.sin(rad) * radius;
                matrices.pushPose();
                matrices.translate(tX + (double)pX, tY + (double)mainY, tZ + (double)pZ);
                matrices.mulPose(cameraRot);
                float scale = 0.12f;
                matrices.scale(scale, scale, scale);
                this.drawBatchedGlowingDot(matrices.last().pose(), buffer, rH, gH, bH, baseAlpha);
                matrices.popPose();
            }
            int tailSteps = 22;
            for (int i = 1; i <= tailSteps; ++i) {
                float tHist = animTime - (float)i * 0.025f;
                if (tHist < 0.0f) continue;
                float histY = (float)((Math.sin(tHist * speedY) + 1.0) / 2.0) * height;
                float tailProgress = (float)i / (float)tailSteps;
                float alphaMultiplier = (float)Math.pow(1.0 - (double)tailProgress, 2.0);
                float tailAlpha = baseAlpha * alphaMultiplier * 0.6f;
                float tailRadius = radius * (1.0f - tailProgress * 0.15f);
                Color tColor = new Color(ThemeManager.getThemedColor(i * 15));
                float rT = (float)tColor.getRed() / 255.0f;
                float gT = (float)tColor.getGreen() / 255.0f;
                float bT = (float)tColor.getBlue() / 255.0f;
                for (int angle = 0; angle < 360; angle += 12) {
                    double rad = Math.toRadians(angle);
                    float pX = (float)Math.cos(rad) * tailRadius;
                    float pZ = (float)Math.sin(rad) * tailRadius;
                    matrices.pushPose();
                    matrices.translate(tX + (double)pX, tY + (double)histY, tZ + (double)pZ);
                    matrices.mulPose(cameraRot);
                    float scale = 0.12f * (1.0f - tailProgress * 0.5f);
                    matrices.scale(scale, scale, scale);
                    this.drawBatchedGlowingDot(matrices.last().pose(), buffer, rT, gT, bT, tailAlpha);
                    matrices.popPose();
                }
            }
            BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawBatchedGlowingDot(Matrix4f matrix, BufferBuilder buffer, float r, float g, float b, float alpha) {
        float coreR = r * 0.15f + 0.85f;
        float coreG = g * 0.15f + 0.85f;
        float coreB = b * 0.15f + 0.85f;
        for (int i = 0; i < 360; i += 30) {
            double rad1 = Math.toRadians(i);
            double rad2 = Math.toRadians(i + 30);
            float px1 = (float)Math.cos(rad1);
            float py1 = (float)Math.sin(rad1);
            float px2 = (float)Math.cos(rad2);
            float py2 = (float)Math.sin(rad2);
            buffer.addVertex(matrix, 0.0f, 0.0f, 0.0f).setColor(coreR, coreG, coreB, alpha);
            buffer.addVertex(matrix, px1, py1, 0.0f).setColor(r, g, b, 0.0f);
            buffer.addVertex(matrix, px2, py2, 0.0f).setColor(r, g, b, 0.0f);
        }
    }
}

