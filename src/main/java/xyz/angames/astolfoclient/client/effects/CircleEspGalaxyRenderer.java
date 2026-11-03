package xyz.angames.astolfoclient.client.effects;
import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.effects.CircleEspManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class CircleEspGalaxyRenderer {
    private static final Identifier BLOOM_TEXTURE = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/bloom.png");
    private final CircleEspManager manager;

    public CircleEspGalaxyRenderer(CircleEspManager manager) {
        this.manager = manager;
    }

    public void render(LevelRenderContext context, List<CircleEspManager.CircleEspEffect> validEffects) {
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
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.ONE);
        Tesselator tessellator = Tesselator.getInstance();
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
            float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            double tX = target.xOld + (target.getX() - target.xOld) * (double)tickDelta;
            double tY = target.yOld + (target.getY() - target.yOld) * (double)tickDelta;
            double tZ = target.zOld + (target.getZ() - target.zOld) * (double)tickDelta;
            float height = target.getBbHeight();
            float radius = target.getBbWidth() * 0.8f;
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
            PoseStack matrices = context.poseStack();
            matrices.pushPose();
            matrices.translate(tX - context.gameRenderer().mainCamera().position().x, tY - context.gameRenderer().mainCamera().position().y, tZ - context.gameRenderer().mainCamera().position().z);
            matrices.mulPose(Axis.YP.rotationDegrees(animTime * 45.0f));
            RenderSystem.setShaderTexture((int)0, (Identifier)BLOOM_TEXTURE);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder bbBloom = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            this.drawHorizontalBloom(matrices, bbBloom, scanY, radius * 2.5f, r, g, b, alpha * 0.5f);
            BufferRenderer.drawWithGlobalProgram((MeshData)bbBloom.buildOrThrow());
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder bbTail = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
            this.drawGradientCylinder(matrices, bbTail, radius, scanY, scanY + tailOffset, r, g, b, alpha * 0.45f, 0.0f);
            BufferRenderer.drawWithGlobalProgram((MeshData)bbTail.buildOrThrow());
            RenderSystem.lineWidth((float)2.5f);
            BufferBuilder bbRing = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            this.drawCrispRing(matrices, bbRing, radius, scanY, r, g, b, alpha * 0.9f);
            BufferRenderer.drawWithGlobalProgram((MeshData)bbRing.buildOrThrow());
            RenderSystem.lineWidth((float)1.0f);
            matrices.popPose();
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawHorizontalBloom(PoseStack stack, BufferBuilder buffer, float y, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.last().pose();
        buffer.addVertex(m, -size, y, -size).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, -size, y, size).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, size, y, size).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, size, y, -size).setUv(1.0f, 0.0f).setColor(r, g, b, a);
    }

    private void drawGradientCylinder(PoseStack stack, BufferBuilder buffer, float radius, float yStart, float yEnd, float r, float g, float b, float aStart, float aEnd) {
        Matrix4f m = stack.last().pose();
        int segments = 40;
        for (int i = 0; i < segments; ++i) {
            float angle1 = (float)((double)i * Math.PI * 2.0 / (double)segments);
            float angle2 = (float)((double)(i + 1) * Math.PI * 2.0 / (double)segments);
            float x1 = (float)Math.cos(angle1) * radius;
            float z1 = (float)Math.sin(angle1) * radius;
            float x2 = (float)Math.cos(angle2) * radius;
            float z2 = (float)Math.sin(angle2) * radius;
            buffer.addVertex(m, x1, yStart, z1).setColor(r, g, b, aStart);
            buffer.addVertex(m, x1, yEnd, z1).setColor(r, g, b, aEnd);
            buffer.addVertex(m, x2, yEnd, z2).setColor(r, g, b, aEnd);
            buffer.addVertex(m, x2, yStart, z2).setColor(r, g, b, aStart);
        }
    }

    private void drawCrispRing(PoseStack stack, BufferBuilder buffer, float radius, float y, float r, float g, float b, float a) {
        Matrix4f m = stack.last().pose();
        int segments = 40;
        for (int i = 0; i <= segments; ++i) {
            float angle = (float)((double)i * Math.PI * 2.0 / (double)segments);
            float x = (float)Math.cos(angle) * radius;
            float z = (float)Math.sin(angle) * radius;
            buffer.addVertex(m, x, y, z).setColor(r, g, b, a);
        }
    }
}

