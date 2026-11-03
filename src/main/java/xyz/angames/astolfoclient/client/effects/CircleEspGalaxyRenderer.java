package xyz.angames.astolfoclient.client.effects;
import net.minecraft.client.MinecraftClient;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
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
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.effects.CircleEspManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class CircleEspGalaxyRenderer {
    private static final Identifier BLOOM_TEXTURE = Identifier.of((String)"astolfoclient", (String)"textures/effects/bloom.png");
    private final CircleEspManager manager;

    public CircleEspGalaxyRenderer(CircleEspManager manager) {
        this.manager = manager;
    }

    public void render(WorldRenderContext context, List<CircleEspManager.CircleEspEffect> validEffects) {
        Module module = AstolfoclientClient.moduleManager.getModuleByName("TargetESP");
        if (module == null || !module.isEnabled()) {
            return;
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
        Tessellator tessellator = Tessellator.getInstance();
        long currentTime = System.currentTimeMillis();
        int rgb = ThemeManager.getThemedColor(0L);
        Color c = new Color(rgb);
        float r = (float)c.getRed() / 255.0f;
        float g = (float)c.getGreen() / 255.0f;
        float b = (float)c.getBlue() / 255.0f;
        for (CircleEspManager.CircleEspEffect effect : validEffects) {
            long timeSinceHit;
            float fadeProgress;
            float alpha;
            Entity target = effect.target;
            if (!target.isAlive() || TargetUtils.isInvisible(target) || (alpha = 1.0f - Math.max(0.0f, ((fadeProgress = (float)(timeSinceHit = currentTime - effect.lastHitTime) / 450.0f) - 0.5f) * 2.0f)) <= 0.05f) continue;
            float animTime = (float)(currentTime - effect.startTime) / 1000.0f;
            float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
            double tX = target.lastRenderX + (target.getX() - target.lastRenderX) * (double)tickDelta;
            double tY = target.lastRenderY + (target.getY() - target.lastRenderY) * (double)tickDelta;
            double tZ = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * (double)tickDelta;
            float height = target.getHeight();
            float radius = target.getWidth() * 0.8f;
            if (timeSinceHit > 400L) {
                float endProgress = (float)(timeSinceHit - 400L) / 200.0f;
                radius *= Math.max(0.0f, 1.0f - endProgress);
            }
            float scanSpeed = 3.0f;
            float phase = (float)Math.sin(animTime * scanSpeed);
            float scanY = (phase + 1.0f) / 2.0f * height;
            float velocity = (float)Math.cos(animTime * scanSpeed);
            float maxTailLength = height * 0.4f;
            float tailOffset = -velocity * maxTailLength;
            MatrixStack matrices = context.matrices();
            matrices.push();
            matrices.translate(tX - context.gameRenderer().getCamera().getCameraPos().x, tY - context.gameRenderer().getCamera().getCameraPos().y, tZ - context.gameRenderer().getCamera().getCameraPos().z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(animTime * 45.0f));
            RenderSystem.setShaderTexture((int)0, (Identifier)BLOOM_TEXTURE);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder bbBloom = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            this.drawHorizontalBloom(matrices, bbBloom, scanY, radius * 2.5f, r, g, b, alpha * 0.5f);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)bbBloom.end());
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder bbTail = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            this.drawGradientCylinder(matrices, bbTail, radius, scanY, scanY + tailOffset, r, g, b, alpha * 0.45f, 0.0f);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)bbTail.end());
            RenderSystem.lineWidth((float)2.5f);
            BufferBuilder bbRing = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            this.drawCrispRing(matrices, bbRing, radius, scanY, r, g, b, alpha * 0.9f);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)bbRing.end());
            RenderSystem.lineWidth((float)1.0f);
            matrices.pop();
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawHorizontalBloom(MatrixStack stack, BufferBuilder buffer, float y, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.peek().getPositionMatrix();
        buffer.vertex(m, -size, y, -size).texture(0.0f, 0.0f).color(r, g, b, a);
        buffer.vertex(m, -size, y, size).texture(0.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(m, size, y, size).texture(1.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(m, size, y, -size).texture(1.0f, 0.0f).color(r, g, b, a);
    }

    private void drawGradientCylinder(MatrixStack stack, BufferBuilder buffer, float radius, float yStart, float yEnd, float r, float g, float b, float aStart, float aEnd) {
        Matrix4f m = stack.peek().getPositionMatrix();
        int segments = 40;
        for (int i = 0; i < segments; ++i) {
            float angle1 = (float)((double)i * Math.PI * 2.0 / (double)segments);
            float angle2 = (float)((double)(i + 1) * Math.PI * 2.0 / (double)segments);
            float x1 = (float)Math.cos(angle1) * radius;
            float z1 = (float)Math.sin(angle1) * radius;
            float x2 = (float)Math.cos(angle2) * radius;
            float z2 = (float)Math.sin(angle2) * radius;
            buffer.vertex(m, x1, yStart, z1).color(r, g, b, aStart);
            buffer.vertex(m, x1, yEnd, z1).color(r, g, b, aEnd);
            buffer.vertex(m, x2, yEnd, z2).color(r, g, b, aEnd);
            buffer.vertex(m, x2, yStart, z2).color(r, g, b, aStart);
        }
    }

    private void drawCrispRing(MatrixStack stack, BufferBuilder buffer, float radius, float y, float r, float g, float b, float a) {
        Matrix4f m = stack.peek().getPositionMatrix();
        int segments = 40;
        for (int i = 0; i <= segments; ++i) {
            float angle = (float)((double)i * Math.PI * 2.0 / (double)segments);
            float x = (float)Math.cos(angle) * radius;
            float z = (float)Math.sin(angle) * radius;
            buffer.vertex(m, x, y, z).color(r, g, b, a);
        }
    }
}

