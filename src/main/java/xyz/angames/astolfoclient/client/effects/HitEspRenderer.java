package xyz.angames.astolfoclient.client.effects;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import xyz.angames.astolfoclient.client.render.compat.Tesselator;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.effects.HitEspEffect;
import xyz.angames.astolfoclient.client.effects.HitEspManager;
import xyz.angames.astolfoclient.client.module.modules.HitEspModule;

@Environment(value=EnvType.CLIENT)
public class HitEspRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/hit_effect.png");
    private final HitEspManager manager;

    public HitEspRenderer(HitEspManager manager) {
        this.manager = manager;
    }

    public void render(LevelRenderContext context) {
        HitEspModule hitEspModule = (HitEspModule)AstolfoclientClient.moduleManager.getModuleByName("HitESP");
        if (hitEspModule == null || !hitEspModule.isEnabled()) {
            return;
        }
        List<HitEspEffect> validEffects = this.manager.getEffects();
        if (validEffects.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShaderTexture((int)0, (Identifier)TEXTURE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        long currentTime = System.currentTimeMillis();
        double tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
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
            PoseStack matrixStack = context.poseStack();
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
                matrixStack.pushPose();
                matrixStack.translate(effect.position.x - context.gameRenderer().mainCamera().position().x, effect.position.y - context.gameRenderer().mainCamera().position().y, effect.position.z - context.gameRenderer().mainCamera().position().z);
                matrixStack.rotate(effect.orientation);
                matrixStack.rotate(Axis.ZP.rotation(textureRotation));
                matrixStack.scale(scale, scale, scale);
                Matrix4f matrix = matrixStack.last().pose();
                buffer.addVertex(matrix, -0.5f, -0.5f, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, 0.5f, -0.5f, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, 0.5f, 0.5f, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, -0.5f, 0.5f, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, alpha);
                matrixStack.popPose();
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
                double renderX = Mth.lerp((double)tickDelta, (double)shard.prevPos.x, (double)shard.pos.x);
                double renderY = Mth.lerp((double)tickDelta, (double)shard.prevPos.y, (double)shard.pos.y);
                double renderZ = Mth.lerp((double)tickDelta, (double)shard.prevPos.z, (double)shard.pos.z);
                float renderRotX = (float)Mth.lerp((double)tickDelta, (double)shard.prevRotX, (double)shard.rotX);
                float renderRotY = (float)Mth.lerp((double)tickDelta, (double)shard.prevRotY, (double)shard.rotY);
                float renderRotZ = (float)Mth.lerp((double)tickDelta, (double)shard.prevRotZ, (double)shard.rotZ);
                matrixStack.pushPose();
                matrixStack.translate(renderX - context.gameRenderer().mainCamera().position().x, renderY - context.gameRenderer().mainCamera().position().y, renderZ - context.gameRenderer().mainCamera().position().z);
                matrixStack.rotate(Axis.XP.rotationDegrees(renderRotX));
                matrixStack.rotate(Axis.YP.rotationDegrees(renderRotY));
                matrixStack.rotate(Axis.ZP.rotationDegrees(renderRotZ));
                matrixStack.scale(shardScale, shardScale, shardScale);
                Matrix4f matrix = matrixStack.last().pose();
                buffer.addVertex(matrix, -0.5f, -0.5f, 0.0f).setUv(shard.u1, shard.v2).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, 0.5f, -0.5f, 0.0f).setUv(shard.u2, shard.v2).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, 0.5f, 0.5f, 0.0f).setUv(shard.u2, shard.v1).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, -0.5f, 0.5f, 0.0f).setUv(shard.u1, shard.v1).setColor(r, g, b, alpha);
                matrixStack.popPose();
                drewAnything = true;
            }
        }
        if (!drewAnything) {
            Matrix4f dummy = context.poseStack().last().pose();
            buffer.addVertex(dummy, 0.0f, 0.0f, 0.0f).setUv(0.0f, 0.0f).setColor(0, 0, 0, 0);
            buffer.addVertex(dummy, 0.0f, 0.0f, 0.0f).setUv(0.0f, 0.0f).setColor(0, 0, 0, 0);
            buffer.addVertex(dummy, 0.0f, 0.0f, 0.0f).setUv(0.0f, 0.0f).setColor(0, 0, 0, 0);
            buffer.addVertex(dummy, 0.0f, 0.0f, 0.0f).setUv(0.0f, 0.0f).setColor(0, 0, 0, 0);
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}

