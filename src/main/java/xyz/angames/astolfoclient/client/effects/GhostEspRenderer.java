package xyz.angames.astolfoclient.client.effects;
import net.minecraft.client.MinecraftClient;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.util.ArrayList;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.effects.GhostEspEffect;
import xyz.angames.astolfoclient.client.effects.GhostEspManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class GhostEspRenderer {
    private final GhostEspManager manager;

    public GhostEspRenderer(GhostEspManager manager) {
        this.manager = manager;
    }

    public void render(WorldRenderContext context) {
        Map<Entity, GhostEspEffect> allEffects;
        Module targetEspModule = AstolfoclientClient.moduleManager.getModuleByName("TargetESP");
        if (targetEspModule == null || !targetEspModule.isEnabled()) {
            return;
        }
        if (targetEspModule instanceof TargetEspModule) {
            TargetEspModule tem = (TargetEspModule)targetEspModule;
            if (!tem.mode.is("Ghost")) {
                return;
            }
        }
        if ((allEffects = this.manager.getEffects()).isEmpty()) {
            return;
        }
        ArrayList<GhostEspEffect> validEffects = new ArrayList<GhostEspEffect>();
        long currentTime = System.currentTimeMillis();
        for (GhostEspEffect effect : allEffects.values()) {
            long age;
            if (effect.target == null || !effect.target.isAlive() || TargetUtils.isInvisible(effect.target) || (age = currentTime - effect.lastHitTime) > 450L) continue;
            validEffects.add(effect);
        }
        if (validEffects.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc((int)515);
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        double safeTime = currentTime % 1000000L;
        float speed = 0.006f;
        int trailLength = 22;
        float baseDistanceMultiplier = 1.05f;
        double spacingDegrees = 120.0;
        float trailSegmentSpacing = 10.0f;
        for (GhostEspEffect effect : validEffects) {
            long age;
            float lifeProgress;
            float baseAlpha;
            Entity target = effect.target;
            if (target == null || !target.isAlive() || TargetUtils.isInvisible(target) || (baseAlpha = 1.0f - Math.max(0.0f, ((lifeProgress = (float)(age = currentTime - effect.lastHitTime) / 450.0f) - 0.5f) * 2.0f)) <= 0.05f) continue;
            int rgb = ThemeManager.getThemedColor(0L);
            float themeR = (float)(rgb >> 16 & 0xFF) / 255.0f;
            float themeG = (float)(rgb >> 8 & 0xFF) / 255.0f;
            float themeB = (float)(rgb & 0xFF) / 255.0f;
            long ageSinceAttack = currentTime - effect.lastAttackTime;
            float hitColorFactor = Math.max(0.0f, 1.0f - (float)ageSinceAttack / 350.0f);
            float r = hitColorFactor * 1.0f + (1.0f - hitColorFactor) * themeR;
            float g = hitColorFactor * 0.0f + (1.0f - hitColorFactor) * themeG;
            float b = hitColorFactor * 0.0f + (1.0f - hitColorFactor) * themeB;
            float cr = 1.0f;
            float cg = 1.0f - hitColorFactor;
            float cb = 1.0f - hitColorFactor;
            float baseRadius = target.getWidth() * baseDistanceMultiplier;
            if (age > 400L) {
                float endProgress = (float)(age - 400L) / 200.0f;
                baseRadius *= Math.max(0.0f, 1.0f - endProgress);
            }
            float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
            double tX = target.lastRenderX + (target.getX() - target.lastRenderX) * (double)tickDelta;
            double tY = target.lastRenderY + (target.getY() - target.lastRenderY) * (double)tickDelta;
            double tZ = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * (double)tickDelta;
            double camX = context.gameRenderer().getCamera().getCameraPos().x;
            double camY = context.gameRenderer().getCamera().getCameraPos().y;
            double camZ = context.gameRenderer().getCamera().getCameraPos().z;
            for (int j = 0; j < 3; ++j) {
                float breathing = (float)Math.sin(safeTime * 0.002 + (double)j * 1.5);
                float currentRadius = baseRadius + breathing * 0.2f;
                for (int t = 0; t < trailLength; ++t) {
                    float decay = 1.0f / (float)trailLength;
                    float scale = (1.0f - (float)t * decay) * 0.2f;
                    float alpha = baseAlpha * (1.0f - (float)t * decay);
                    if (scale <= 0.01f || alpha <= 0.02f) continue;
                    float timeOffset = (float)t * trailSegmentSpacing;
                    float histSafeTime = (float)(safeTime - (double)timeOffset);
                    float angle = histSafeTime * speed + (float)((double)j * Math.toRadians(spacingDegrees));
                    float localX = (float)Math.cos(angle) * currentRadius;
                    float localY = 0.0f;
                    float localZ = (float)Math.sin(angle) * currentRadius;
                    float histAnimTime = histSafeTime / 1000.0f;
                    float scanSpeed = 2.0f;
                    float phase = (float)Math.sin(histAnimTime * scanSpeed);
                    float scanY = (phase + 1.0f) / 2.0f * target.getHeight();
                    MatrixStack matrices = context.matrices();
                    matrices.push();
                    matrices.translate(tX + (double)localX - camX, tY + (double)scanY + (double)localY - camY, tZ + (double)localZ - camZ);
                    matrices.multiply(context.gameRenderer().getCamera().getRotation());
                    matrices.scale(scale, scale, scale);
                    BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
                    this.drawGlowingDot(matrices.peek().getPositionMatrix(), buffer, r, g, b, cr, cg, cb, alpha);
                    BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
                    matrices.pop();
                }
            }
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawGlowingDot(Matrix4f matrix, BufferBuilder buffer, float r, float g, float b, float cr, float cg, float cb, float alpha) {
        buffer.vertex(matrix, 0.0f, 0.0f, 0.0f).color(cr, cg, cb, alpha);
        for (int i = 0; i <= 360; i += 20) {
            double rad = Math.toRadians(i);
            float px = (float)Math.cos(rad);
            float py = (float)Math.sin(rad);
            buffer.vertex(matrix, px, py, 0.0f).color(r, g, b, 0.0f);
        }
    }
}

