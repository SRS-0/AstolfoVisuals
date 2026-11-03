package xyz.angames.astolfoclient.client.effects;
import net.minecraft.client.MinecraftClient;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.util.List;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.effects.HitEspEffect;
import xyz.angames.astolfoclient.client.effects.HitEspManager;
import xyz.angames.astolfoclient.client.module.modules.HitEspModule;

@Environment(value=EnvType.CLIENT)
public class HitEspRenderer {
    private static final Identifier TEXTURE = Identifier.of((String)"astolfoclient", (String)"textures/effects/hit_effect.png");
    private final HitEspManager manager;

    public HitEspRenderer(HitEspManager manager) {
        this.manager = manager;
    }

    public void render(WorldRenderContext context) {
        HitEspModule hitEspModule = (HitEspModule)AstolfoclientClient.moduleManager.getModuleByName("HitESP");
        if (hitEspModule == null || !hitEspModule.isEnabled()) {
            return;
        }
        List<HitEspEffect> validEffects = this.manager.getEffects();
        if (validEffects.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShaderTexture((int)0, (Identifier)TEXTURE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        long currentTime = System.currentTimeMillis();
        double tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
        int rgb = ThemeManager.getThemedColor(0L);
        float r = (float)(rgb >> 16 & 0xFF) / 255.0f;
        float g = (float)(rgb >> 8 & 0xFF) / 255.0f;
        float b = (float)(rgb & 0xFF) / 255.0f;
        float a = 1.0f;
        float baseScale = hitEspModule.size.getFloat();
        float rotSpeed = hitEspModule.rotationSpeed.getFloat();
        float spawnAnimDuration = hitEspModule.spawnAnimDuration.getFloat();
        float leaveTime = hitEspModule.leaveTime.getFloat();
        float fadeTime = hitEspModule.fadeTime.getFloat();
        boolean doExplosion = hitEspModule.doExplosion.get();
        boolean drewAnything = false;
        for (HitEspEffect effect : validEffects) {
            MatrixStack matrixStack = context.matrices();
            long age = currentTime - effect.creationTime;
            if (!effect.isShattered) {
                float textureRotation = (float)age * rotSpeed * effect.rotationDirection;
                float scale = baseScale;
                float alpha = 1.0f;
                if ((float)age < spawnAnimDuration) {
                    float p = (float)age / spawnAnimDuration;
                    float ease = (float)(Math.sin(-20.420352248333657 * ((double)p + 1.0)) * Math.pow(2.0, -10.0 * (double)p) + 1.0);
                    scale = baseScale * ease;
                    alpha = Math.min(1.0f, p * 2.5f);
                }
                if (!doExplosion && (float)age >= leaveTime) {
                    float fadeProgress = ((float)age - leaveTime) / fadeTime;
                    alpha = Math.max(0.0f, 1.0f - fadeProgress);
                }
                if (!((alpha *= a) > 0.01f)) continue;
                matrixStack.push();
                matrixStack.translate(effect.position.x - context.gameRenderer().getCamera().getCameraPos().x, effect.position.y - context.gameRenderer().getCamera().getCameraPos().y, effect.position.z - context.gameRenderer().getCamera().getCameraPos().z);
                matrixStack.multiply(effect.orientation);
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(textureRotation));
                matrixStack.scale(scale, scale, scale);
                Matrix4f matrix = matrixStack.peek().getPositionMatrix();
                buffer.vertex(matrix, -0.5f, -0.5f, 0.0f).texture(0.0f, 1.0f).color(r, g, b, alpha);
                buffer.vertex(matrix, 0.5f, -0.5f, 0.0f).texture(1.0f, 1.0f).color(r, g, b, alpha);
                buffer.vertex(matrix, 0.5f, 0.5f, 0.0f).texture(1.0f, 0.0f).color(r, g, b, alpha);
                buffer.vertex(matrix, -0.5f, 0.5f, 0.0f).texture(0.0f, 0.0f).color(r, g, b, alpha);
                matrixStack.pop();
                drewAnything = true;
                continue;
            }
            float groundLifespan = hitEspModule.groundLifespan.getFloat();
            boolean shrinkOnGround = hitEspModule.shrinkOnGround.get();
            float gridSize = hitEspModule.gridSize.getFloat();
            float baseShardScale = baseScale / gridSize * 0.93f;
            for (HitEspEffect.Shard shard : effect.shards) {
                float alpha = 1.0f;
                float shardScale = baseShardScale;
                if (shard.onGround) {
                    long timeOnGround = currentTime - shard.groundHitTime;
                    float deathProgress = (float)timeOnGround / groundLifespan;
                    alpha = 1.0f - deathProgress;
                    if (alpha < 0.0f) {
                        alpha = 0.0f;
                    }
                    if (shrinkOnGround) {
                        float shrinkEase = 1.0f - (float)Math.pow(deathProgress, 2.0);
                        shardScale *= Math.max(0.0f, shrinkEase);
                    }
                }
                if (!((alpha *= a) > 0.01f) || !(shardScale > 0.01f)) continue;
                double renderX = MathHelper.lerp((double)tickDelta, (double)shard.prevPos.x, (double)shard.pos.x);
                double renderY = MathHelper.lerp((double)tickDelta, (double)shard.prevPos.y, (double)shard.pos.y);
                double renderZ = MathHelper.lerp((double)tickDelta, (double)shard.prevPos.z, (double)shard.pos.z);
                float renderRotX = (float)MathHelper.lerp((double)tickDelta, (double)shard.prevRotX, (double)shard.rotX);
                float renderRotY = (float)MathHelper.lerp((double)tickDelta, (double)shard.prevRotY, (double)shard.rotY);
                float renderRotZ = (float)MathHelper.lerp((double)tickDelta, (double)shard.prevRotZ, (double)shard.rotZ);
                matrixStack.push();
                matrixStack.translate(renderX - context.gameRenderer().getCamera().getCameraPos().x, renderY - context.gameRenderer().getCamera().getCameraPos().y, renderZ - context.gameRenderer().getCamera().getCameraPos().z);
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(renderRotX));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderRotY));
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(renderRotZ));
                matrixStack.scale(shardScale, shardScale, shardScale);
                Matrix4f matrix = matrixStack.peek().getPositionMatrix();
                buffer.vertex(matrix, -0.5f, -0.5f, 0.0f).texture(shard.u1, shard.v2).color(r, g, b, alpha);
                buffer.vertex(matrix, 0.5f, -0.5f, 0.0f).texture(shard.u2, shard.v2).color(r, g, b, alpha);
                buffer.vertex(matrix, 0.5f, 0.5f, 0.0f).texture(shard.u2, shard.v1).color(r, g, b, alpha);
                buffer.vertex(matrix, -0.5f, 0.5f, 0.0f).texture(shard.u1, shard.v1).color(r, g, b, alpha);
                matrixStack.pop();
                drewAnything = true;
            }
        }
        if (!drewAnything) {
            Matrix4f dummy = context.matrices().peek().getPositionMatrix();
            buffer.vertex(dummy, 0.0f, 0.0f, 0.0f).texture(0.0f, 0.0f).color(0, 0, 0, 0);
            buffer.vertex(dummy, 0.0f, 0.0f, 0.0f).texture(0.0f, 0.0f).color(0, 0, 0, 0);
            buffer.vertex(dummy, 0.0f, 0.0f, 0.0f).texture(0.0f, 0.0f).color(0, 0, 0, 0);
            buffer.vertex(dummy, 0.0f, 0.0f, 0.0f).texture(0.0f, 0.0f).color(0, 0, 0, 0);
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}

