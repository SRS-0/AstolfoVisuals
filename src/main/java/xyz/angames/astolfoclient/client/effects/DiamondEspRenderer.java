package xyz.angames.astolfoclient.client.effects;
import net.minecraft.client.MinecraftClient;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.effects.DiamondEspManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class DiamondEspRenderer {
    private static final Identifier BLOOM_TEXTURE = Identifier.of((String)"astolfoclient", (String)"textures/effects/bloom.png");
    private final DiamondEspManager manager;

    public DiamondEspRenderer(DiamondEspManager manager) {
        this.manager = manager;
    }

    public void render(WorldRenderContext context) {
        Map<Entity, DiamondEspManager.DiamondEffect> allEffects;
        Module targetEspModule = AstolfoclientClient.moduleManager.getModuleByName("TargetESP");
        if (targetEspModule == null || !targetEspModule.isEnabled()) {
            return;
        }
        if (targetEspModule instanceof TargetEspModule) {
            TargetEspModule tem = (TargetEspModule)targetEspModule;
            if (!tem.mode.is("Diamond")) {
                return;
            }
        }
        if ((allEffects = this.manager.getEffects()).isEmpty()) {
            return;
        }
        ArrayList<DiamondEspManager.DiamondEffect> validEffects = new ArrayList<DiamondEspManager.DiamondEffect>(allEffects.values());
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
        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
        for (DiamondEspManager.DiamondEffect effect : validEffects) {
            float baseAlpha;
            Entity target = effect.target;
            if (target == null || !target.isAlive() || TargetUtils.isInvisible(target)) continue;
            long timeSinceStart = currentTime - effect.startTime;
            long timeSinceHit = currentTime - effect.lastHitTime;
            if (timeSinceHit > 450L) continue;
            float fastTime = (float)timeSinceStart / 1000.0f;
            float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
            double tX = target.lastRenderX + (target.getX() - target.lastRenderX) * (double)tickDelta;
            double tY = target.lastRenderY + (target.getY() - target.lastRenderY) * (double)tickDelta;
            double tZ = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * (double)tickDelta;
            float entityCenterY = (float)(tY + (double)(target.getHeight() * 0.5f));
            float arriveProgress = Math.min(1.0f, (float)timeSinceStart / 400.0f);
            float arriveEase = this.easeOutCubic(arriveProgress);
            float scatterProgress = Math.max(0.0f, ((float)timeSinceHit - 360.0f) / 90.0f);
            float scatterEase = this.easeInQuad(Math.min(1.0f, scatterProgress));
            float f = baseAlpha = arriveProgress < 1.0f ? arriveEase : 1.0f - scatterEase;
            if (baseAlpha <= 0.01f) continue;
            long timeSinceAttack = currentTime - effect.lastAttackTime;
            float hitColorFactor = Math.max(0.0f, 1.0f - (float)timeSinceAttack / 350.0f);
            int rgb = ThemeManager.getThemedColor(0L);
            Color themeColor = new Color(rgb);
            float themeR = (float)themeColor.getRed() / 255.0f;
            float themeG = (float)themeColor.getGreen() / 255.0f;
            float themeB = (float)themeColor.getBlue() / 255.0f;
            float r = hitColorFactor * 1.0f + (1.0f - hitColorFactor) * themeR;
            float g = hitColorFactor * 0.0f + (1.0f - hitColorFactor) * themeG;
            float b = hitColorFactor * 0.0f + (1.0f - hitColorFactor) * themeB;
            float cr = 1.0f;
            float cg = 1.0f - hitColorFactor;
            float cb = 1.0f - hitColorFactor;
            int numDiamonds = 18;
            float baseRadius = target.getWidth() * 1.1f;
            MatrixStack matrices = context.matrices();
            RenderSystem.setShaderTexture((int)0, (Identifier)BLOOM_TEXTURE);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder bbBloom = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (int i = 0; i < numDiamonds; ++i) {
                float[] pos = this.calculateDiamondPosition(i, numDiamonds, fastTime, baseRadius, target.getHeight(), arriveEase, scatterEase);
                matrices.push();
                matrices.translate(tX + (double)pos[0] - cameraPos.x, (double)(entityCenterY + pos[1]) - cameraPos.y, tZ + (double)pos[2] - cameraPos.z);
                matrices.multiply(context.gameRenderer().getCamera().getRotation());
                this.drawBloom(matrices, bbBloom, 0.35f, r, g, b, baseAlpha * 0.6f * pos[3]);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)bbBloom.end());
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder bbSolid = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < numDiamonds; ++i) {
                float[] pos = this.calculateDiamondPosition(i, numDiamonds, fastTime, baseRadius, target.getHeight(), arriveEase, scatterEase);
                matrices.push();
                matrices.translate(tX + (double)pos[0] - cameraPos.x, (double)(entityCenterY + pos[1]) - cameraPos.y, tZ + (double)pos[2] - cameraPos.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(fastTime * 60.0f + (float)i * 15.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(fastTime * 40.0f + (float)i * 10.0f));
                this.draw3DDiamond(matrices, bbSolid, 0.08f, r, g, b, baseAlpha * 0.8f * pos[3]);
                this.draw3DDiamond(matrices, bbSolid, 0.07f, cr, cg, cb, baseAlpha * pos[3]);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)bbSolid.end());
            RenderSystem.lineWidth((float)1.5f);
            BufferBuilder bbLines = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < numDiamonds; ++i) {
                float[] pos = this.calculateDiamondPosition(i, numDiamonds, fastTime, baseRadius, target.getHeight(), arriveEase, scatterEase);
                matrices.push();
                matrices.translate(tX + (double)pos[0] - cameraPos.x, (double)(entityCenterY + pos[1]) - cameraPos.y, tZ + (double)pos[2] - cameraPos.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(fastTime * 60.0f + (float)i * 15.0f));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(fastTime * 40.0f + (float)i * 10.0f));
                this.draw3DDiamondLines(matrices, bbLines, 0.08f, r, g, b, baseAlpha * pos[3]);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)bbLines.end());
            RenderSystem.lineWidth((float)1.0f);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private float[] calculateDiamondPosition(int i, int total, float fastTime, float baseRadius, float targetHeight, float arriveEase, float scatterEase) {
        float loops = 1.5f;
        float orbitSpeed = 0.8f;
        boolean isSecondSpiral = i % 2 == 1;
        int totalPerSpiral = total / 2;
        int indexInSpiral = i / 2;
        float progress = totalPerSpiral > 1 ? (float)indexInSpiral / (float)(totalPerSpiral - 1) : 0.5f;
        float height = this.lerp(-targetHeight * 0.5f, targetHeight * 0.7f, progress);
        float helixAngle = progress * loops * 2.0f * (float)Math.PI;
        float spiralOffset = isSecondSpiral ? (float)Math.PI : 0.0f;
        float angle = helixAngle + spiralOffset + fastTime * orbitSpeed;
        float rad = baseRadius;
        float alphaMultiplier = 1.0f;
        if (arriveEase < 1.0f) {
            rad = baseRadius + (1.0f - arriveEase) * 3.0f;
            height += (1.0f - arriveEase) * 2.0f;
            alphaMultiplier = arriveEase;
        } else if (scatterEase > 0.0f) {
            rad = baseRadius + scatterEase * 3.0f;
            height += scatterEase * 2.0f;
            alphaMultiplier = 1.0f - scatterEase;
        }
        float dx = (float)Math.cos(angle) * rad;
        float dz = (float)Math.sin(angle) * rad;
        return new float[]{dx, height, dz, alphaMultiplier};
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float easeOutCubic(float t) {
        return 1.0f - (float)Math.pow(1.0f - t, 3.0);
    }

    private float easeInQuad(float t) {
        return t * t;
    }

    private void drawBloom(MatrixStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.peek().getPositionMatrix();
        buffer.vertex(m, -size, size, 0.0f).texture(0.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(m, size, size, 0.0f).texture(1.0f, 1.0f).color(r, g, b, a);
        buffer.vertex(m, size, -size, 0.0f).texture(1.0f, 0.0f).color(r, g, b, a);
        buffer.vertex(m, -size, -size, 0.0f).texture(0.0f, 0.0f).color(r, g, b, a);
    }

    private void draw3DDiamond(MatrixStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.peek().getPositionMatrix();
        float h = size * 1.8f;
        float w = size * 0.7f;
        this.drawTri(m, buffer, 0.0f, h, 0.0f, -w, 0.0f, -w, w, 0.0f, -w, r, g, b, a);
        this.drawTri(m, buffer, 0.0f, h, 0.0f, w, 0.0f, -w, w, 0.0f, w, r, g, b, a);
        this.drawTri(m, buffer, 0.0f, h, 0.0f, w, 0.0f, w, -w, 0.0f, w, r, g, b, a);
        this.drawTri(m, buffer, 0.0f, h, 0.0f, -w, 0.0f, w, -w, 0.0f, -w, r, g, b, a);
        this.drawTri(m, buffer, 0.0f, -h, 0.0f, w, 0.0f, -w, -w, 0.0f, -w, r, g, b, a);
        this.drawTri(m, buffer, 0.0f, -h, 0.0f, w, 0.0f, w, w, 0.0f, -w, r, g, b, a);
        this.drawTri(m, buffer, 0.0f, -h, 0.0f, -w, 0.0f, w, w, 0.0f, w, r, g, b, a);
        this.drawTri(m, buffer, 0.0f, -h, 0.0f, -w, 0.0f, -w, -w, 0.0f, w, r, g, b, a);
    }

    private void drawTri(Matrix4f m, BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float bl, float a) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);
        b.vertex(m, x3, y3, z3).color(r, g, bl, a);
    }

    private void draw3DDiamondLines(MatrixStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.peek().getPositionMatrix();
        float h = size * 1.8f;
        float w = size * 0.7f;
        this.drawLine(m, buffer, -w, 0.0f, -w, w, 0.0f, -w, r, g, b, a);
        this.drawLine(m, buffer, w, 0.0f, -w, w, 0.0f, w, r, g, b, a);
        this.drawLine(m, buffer, w, 0.0f, w, -w, 0.0f, w, r, g, b, a);
        this.drawLine(m, buffer, -w, 0.0f, w, -w, 0.0f, -w, r, g, b, a);
        this.drawLine(m, buffer, 0.0f, h, 0.0f, -w, 0.0f, -w, r, g, b, a);
        this.drawLine(m, buffer, 0.0f, h, 0.0f, w, 0.0f, -w, r, g, b, a);
        this.drawLine(m, buffer, 0.0f, h, 0.0f, w, 0.0f, w, r, g, b, a);
        this.drawLine(m, buffer, 0.0f, h, 0.0f, -w, 0.0f, w, r, g, b, a);
        this.drawLine(m, buffer, 0.0f, -h, 0.0f, -w, 0.0f, -w, r, g, b, a);
        this.drawLine(m, buffer, 0.0f, -h, 0.0f, w, 0.0f, -w, r, g, b, a);
        this.drawLine(m, buffer, 0.0f, -h, 0.0f, w, 0.0f, w, r, g, b, a);
        this.drawLine(m, buffer, 0.0f, -h, 0.0f, -w, 0.0f, w, r, g, b, a);
    }

    private void drawLine(Matrix4f m, BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float bl, float a) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);
    }
}

