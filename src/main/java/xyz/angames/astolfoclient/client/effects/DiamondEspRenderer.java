package xyz.angames.astolfoclient.client.effects;
import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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
import xyz.angames.astolfoclient.client.effects.DiamondEspManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class DiamondEspRenderer {
    private static final Identifier BLOOM_TEXTURE = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/bloom.png");
    private final DiamondEspManager manager;

    public DiamondEspRenderer(DiamondEspManager manager) {
        this.manager = manager;
    }

    public void render(LevelRenderContext context) {
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
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.ONE);
        Tesselator tessellator = Tesselator.getInstance();
        long currentTime = System.currentTimeMillis();
        Vec3 cameraPos = context.gameRenderer().mainCamera().position();
        for (DiamondEspManager.DiamondEffect effect : validEffects) {
            float baseAlpha;
            Entity target = effect.target;
            if (target == null || !target.isAlive() || TargetUtils.isInvisible(target)) continue;
            long timeSinceStart = currentTime - effect.startTime;
            long timeSinceHit = currentTime - effect.lastHitTime;
            if (timeSinceHit > 450L) continue;
            float fastTime = (float)timeSinceStart / 1000.0f;
            float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            double tX = target.xOld + (target.getX() - target.xOld) * (double)tickDelta;
            double tY = target.yOld + (target.getY() - target.yOld) * (double)tickDelta;
            double tZ = target.zOld + (target.getZ() - target.zOld) * (double)tickDelta;
            float entityCenterY = (float)(tY + (double)(target.getBbHeight() * 0.5f));
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
            float baseRadius = target.getBbWidth() * 1.1f;
            PoseStack matrices = context.poseStack();
            RenderSystem.setShaderTexture((int)0, (Identifier)BLOOM_TEXTURE);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder bbBloom = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int i = 0; i < numDiamonds; ++i) {
                float[] pos = this.calculateDiamondPosition(i, numDiamonds, fastTime, baseRadius, target.getBbHeight(), arriveEase, scatterEase);
                matrices.pushPose();
                matrices.translate(tX + (double)pos[0] - cameraPos.x, (double)(entityCenterY + pos[1]) - cameraPos.y, tZ + (double)pos[2] - cameraPos.z);
                matrices.mulPose(context.gameRenderer().mainCamera().rotation());
                this.drawBloom(matrices, bbBloom, 0.35f, r, g, b, baseAlpha * 0.6f * pos[3]);
                matrices.popPose();
            }
            BufferRenderer.drawWithGlobalProgram((MeshData)bbBloom.buildOrThrow());
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder bbSolid = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < numDiamonds; ++i) {
                float[] pos = this.calculateDiamondPosition(i, numDiamonds, fastTime, baseRadius, target.getBbHeight(), arriveEase, scatterEase);
                matrices.pushPose();
                matrices.translate(tX + (double)pos[0] - cameraPos.x, (double)(entityCenterY + pos[1]) - cameraPos.y, tZ + (double)pos[2] - cameraPos.z);
                matrices.mulPose(Axis.YP.rotationDegrees(fastTime * 60.0f + (float)i * 15.0f));
                matrices.mulPose(Axis.XP.rotationDegrees(fastTime * 40.0f + (float)i * 10.0f));
                this.draw3DDiamond(matrices, bbSolid, 0.08f, r, g, b, baseAlpha * 0.8f * pos[3]);
                this.draw3DDiamond(matrices, bbSolid, 0.07f, cr, cg, cb, baseAlpha * pos[3]);
                matrices.popPose();
            }
            BufferRenderer.drawWithGlobalProgram((MeshData)bbSolid.buildOrThrow());
            RenderSystem.lineWidth((float)1.5f);
            BufferBuilder bbLines = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < numDiamonds; ++i) {
                float[] pos = this.calculateDiamondPosition(i, numDiamonds, fastTime, baseRadius, target.getBbHeight(), arriveEase, scatterEase);
                matrices.pushPose();
                matrices.translate(tX + (double)pos[0] - cameraPos.x, (double)(entityCenterY + pos[1]) - cameraPos.y, tZ + (double)pos[2] - cameraPos.z);
                matrices.mulPose(Axis.YP.rotationDegrees(fastTime * 60.0f + (float)i * 15.0f));
                matrices.mulPose(Axis.XP.rotationDegrees(fastTime * 40.0f + (float)i * 10.0f));
                this.draw3DDiamondLines(matrices, bbLines, 0.08f, r, g, b, baseAlpha * pos[3]);
                matrices.popPose();
            }
            BufferRenderer.drawWithGlobalProgram((MeshData)bbLines.buildOrThrow());
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

    private void drawBloom(PoseStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.last().pose();
        buffer.addVertex(m, -size, size, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, size, size, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, size, -size, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, -size, -size, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, a);
    }

    private void draw3DDiamond(PoseStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.last().pose();
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
        b.addVertex(m, x1, y1, z1).setColor(r, g, bl, a);
        b.addVertex(m, x2, y2, z2).setColor(r, g, bl, a);
        b.addVertex(m, x3, y3, z3).setColor(r, g, bl, a);
    }

    private void draw3DDiamondLines(PoseStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.last().pose();
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
        b.addVertex(m, x1, y1, z1).setColor(r, g, bl, a);
        b.addVertex(m, x2, y2, z2).setColor(r, g, bl, a);
    }
}

