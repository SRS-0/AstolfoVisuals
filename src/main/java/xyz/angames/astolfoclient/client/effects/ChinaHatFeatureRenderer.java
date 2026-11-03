package xyz.angames.astolfoclient.client.effects;

import com.mojang.renderpearl.backend.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgram;
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
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
public class ChinaHatFeatureRenderer
extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static Player currentlyRenderingPlayer;
    private static final float RADIUS = 0.65f;
    private static final float HEIGHT = 0.28f;
    private static final int SEGMENTS = 40;
    private static final Identifier BLOOM_TEXTURE;
    private float spinAngle = 0.0f;
    private long lastRenderTime = 0L;

    public ChinaHatFeatureRenderer(RenderLayerParent<AvatarRenderState, PlayerModel> context) {
        super(context);
    }

    public void submit(PoseStack matrices, net.minecraft.client.renderer.SubmitNodeCollector queue, int light, AvatarRenderState state, float limbAngle, float limbDistance) {

        Minecraft mc = Minecraft.getInstance();
        Module module = AstolfoclientClient.moduleManager.getModuleByName("ChinaHat");
        if (module == null || !module.isEnabled() || mc.player == null) {
            return;
        }
        if (state.id != mc.player.getId()) {
            return;
        }
        if (state.isInvisible) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.lastRenderTime != 0L) {
            this.spinAngle += (float)(now - this.lastRenderTime) / 1000.0f * 80.0f;
            if (this.spinAngle > 360.0f) {
                this.spinAngle -= 360.0f;
            }
        }
        this.lastRenderTime = now;
        matrices.pushPose();
        ((PlayerModel)this.getParentModel()).head.translateAndRotate(matrices);
        matrices.translate(0.0f, -0.4f, 0.0f);
        matrices.scale(1.0f, -1.0f, 1.0f);
        matrices.rotate(Axis.YP.rotationDegrees(this.spinAngle));
        var pose = new Matrix4f(matrices.last().pose());
        xyz.angames.astolfoclient.client.render.compat.EffectSubmit.submit(queue, () -> this.drawHat(pose, now));
        matrices.popPose();
    }

    private void drawHat(Matrix4f matrix, long now) {
        int i;
        Tesselator tessellator = Tesselator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.blendFuncSeparate((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE_MINUS_SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ZERO);
        ShaderProgram shader = RenderSystem.setShader((ShaderProgramKey)AstolfoclientClient.CHINA_HAT_SHADER);
        if (shader != null) {
            float timeSecs = (float)(System.currentTimeMillis() % 1000000L) / 1000.0f;
            if (shader.getUniform("uTime") != null) {
                shader.getUniform("uTime").set(timeSecs);
            }
            if (shader.getUniform("uResolution") != null) {
                shader.getUniform("uResolution").set(1.0f, 1.0f);
            }
            int themeRgb = ThemeManager.getThemedColor((int)(now / 10L));
            float tr = (float)(themeRgb >> 16 & 0xFF) / 255.0f;
            float tg = (float)(themeRgb >> 8 & 0xFF) / 255.0f;
            float tb = (float)(themeRgb & 0xFF) / 255.0f;
            if (shader.getUniform("uThemeColor") != null) {
                shader.getUniform("uThemeColor").set(tr, tg, tb);
            }
        }
        Color cInner = new Color(ThemeManager.getThemedColor((int)(now / 10L)));
        Color cOuter = new Color(ThemeManager.getThemedColor((int)(now / 10L) + 60));
        int optimizedLayers = 16;
        BufferBuilder bufBody = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int layer = 1; layer < optimizedLayers; ++layer) {
            float t0 = (float)(layer - 1) / (float)(optimizedLayers - 1);
            float t1 = (float)layer / (float)(optimizedLayers - 1);
            float r0 = 0.65f * t0;
            float h0 = 0.28f * (1.0f - t0);
            float r1 = 0.65f * t1;
            float h1 = 0.28f * (1.0f - t1);
            Color c0 = this.lerpColor(cInner, cOuter, this.smoothstep(t0));
            Color c1 = this.lerpColor(cInner, cOuter, this.smoothstep(t1));
            float a0 = 1.0f;
            float a1 = 1.0f;
            for (int i2 = 0; i2 < 40; ++i2) {
                double ang0 = Math.PI * 2 * (double)i2 / 40.0;
                double ang1 = Math.PI * 2 * (double)(i2 + 1) / 40.0;
                float st = (float)i2 / 40.0f;
                Color sc0 = this.lerpColor(c0, cOuter, st * 0.25f);
                Color sc1 = this.lerpColor(c1, cOuter, st * 0.25f);
                float u0_0 = (float)Math.cos(ang0) * t0 * 0.5f + 0.5f;
                float v0_0 = (float)Math.sin(ang0) * t0 * 0.5f + 0.5f;
                float u1_0 = (float)Math.cos(ang1) * t0 * 0.5f + 0.5f;
                float v1_0 = (float)Math.sin(ang1) * t0 * 0.5f + 0.5f;
                float u1_1 = (float)Math.cos(ang1) * t1 * 0.5f + 0.5f;
                float v1_1 = (float)Math.sin(ang1) * t1 * 0.5f + 0.5f;
                float u0_1 = (float)Math.cos(ang0) * t1 * 0.5f + 0.5f;
                float v0_1 = (float)Math.sin(ang0) * t1 * 0.5f + 0.5f;
                bufBody.addVertex(matrix, (float)Math.cos(ang0) * r0, h0, (float)Math.sin(ang0) * r0).setUv(u0_0, v0_0).setColor((float)sc0.getRed() / 255.0f, (float)sc0.getGreen() / 255.0f, (float)sc0.getBlue() / 255.0f, a0);
                bufBody.addVertex(matrix, (float)Math.cos(ang1) * r0, h0, (float)Math.sin(ang1) * r0).setUv(u1_0, v1_0).setColor((float)sc0.getRed() / 255.0f, (float)sc0.getGreen() / 255.0f, (float)sc0.getBlue() / 255.0f, a0);
                bufBody.addVertex(matrix, (float)Math.cos(ang1) * r1, h1, (float)Math.sin(ang1) * r1).setUv(u1_1, v1_1).setColor((float)sc1.getRed() / 255.0f, (float)sc1.getGreen() / 255.0f, (float)sc1.getBlue() / 255.0f, a1);
                bufBody.addVertex(matrix, (float)Math.cos(ang0) * r1, h1, (float)Math.sin(ang0) * r1).setUv(u0_1, v0_1).setColor((float)sc1.getRed() / 255.0f, (float)sc1.getGreen() / 255.0f, (float)sc1.getBlue() / 255.0f, a1);
            }
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)bufBody.buildOrThrow());
        float r = (float)cOuter.getRed() / 255.0f;
        float g = (float)cOuter.getGreen() / 255.0f;
        float b = (float)cOuter.getBlue() / 255.0f;
        float rimW = 0.035f;
        float innerR = 0.65f - rimW;
        float innerH = 0.28f * (rimW / 0.65f);
        float innerT = (0.65f - rimW) / 0.65f;
        BufferBuilder bufRim = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (i = 0; i < 40; ++i) {
            double a0 = Math.PI * 2 * (double)i / 40.0;
            double a1 = Math.PI * 2 * (double)(i + 1) / 40.0;
            float u0_in = (float)Math.cos(a0) * innerT * 0.5f + 0.5f;
            float v0_in = (float)Math.sin(a0) * innerT * 0.5f + 0.5f;
            float u1_in = (float)Math.cos(a1) * innerT * 0.5f + 0.5f;
            float v1_in = (float)Math.sin(a1) * innerT * 0.5f + 0.5f;
            float u1_out = (float)Math.cos(a1) * 0.5f + 0.5f;
            float v1_out = (float)Math.sin(a1) * 0.5f + 0.5f;
            float u0_out = (float)Math.cos(a0) * 0.5f + 0.5f;
            float v0_out = (float)Math.sin(a0) * 0.5f + 0.5f;
            bufRim.addVertex(matrix, (float)Math.cos(a0) * innerR, innerH, (float)Math.sin(a0) * innerR).setUv(u0_in, v0_in).setColor(r, g, b, 1.0f);
            bufRim.addVertex(matrix, (float)Math.cos(a1) * innerR, innerH, (float)Math.sin(a1) * innerR).setUv(u1_in, v1_in).setColor(r, g, b, 1.0f);
            bufRim.addVertex(matrix, (float)Math.cos(a1) * 0.65f, 0.0f, (float)Math.sin(a1) * 0.65f).setUv(u1_out, v1_out).setColor(r, g, b, 1.0f);
            bufRim.addVertex(matrix, (float)Math.cos(a0) * 0.65f, 0.0f, (float)Math.sin(a0) * 0.65f).setUv(u0_out, v0_out).setColor(r, g, b, 1.0f);
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)bufRim.buildOrThrow());
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        float gr = (float)cOuter.getRed() / 255.0f;
        float gg = (float)cOuter.getGreen() / 255.0f;
        float gb = (float)cOuter.getBlue() / 255.0f;
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder glowRimBuf = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float glowInnerR = 0.598f;
        float glowOuterR = 0.68899995f;
        float innerAlpha = 0.7f;
        float outerAlpha = 0.0f;
        for (i = 0; i < 40; ++i) {
            double a0 = Math.PI * 2 * (double)i / 40.0;
            double a1 = Math.PI * 2 * (double)(i + 1) / 40.0;
            float x0_in = (float)Math.cos(a0) * glowInnerR;
            float z0_in = (float)Math.sin(a0) * glowInnerR;
            float x1_in = (float)Math.cos(a1) * glowInnerR;
            float z1_in = (float)Math.sin(a1) * glowInnerR;
            float x0_out = (float)Math.cos(a0) * glowOuterR;
            float z0_out = (float)Math.sin(a0) * glowOuterR;
            float x1_out = (float)Math.cos(a1) * glowOuterR;
            float z1_out = (float)Math.sin(a1) * glowOuterR;
            glowRimBuf.addVertex(matrix, x0_in, 0.005f, z0_in).setColor(gr, gg, gb, innerAlpha);
            glowRimBuf.addVertex(matrix, x1_in, 0.005f, z1_in).setColor(gr, gg, gb, innerAlpha);
            glowRimBuf.addVertex(matrix, x1_out, 0.001f, z1_out).setColor(gr, gg, gb, outerAlpha);
            glowRimBuf.addVertex(matrix, x0_out, 0.001f, z0_out).setColor(gr, gg, gb, outerAlpha);
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)glowRimBuf.buildOrThrow());
        RenderSystem.setShaderTexture((int)0, (Identifier)BLOOM_TEXTURE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder bloomBaseBuf = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float bSize = 0.663f;
        bloomBaseBuf.addVertex(matrix, -bSize, 0.005f, bSize).setUv(0.0f, 1.0f).setColor(gr, gg, gb, 0.4f);
        bloomBaseBuf.addVertex(matrix, bSize, 0.005f, bSize).setUv(1.0f, 1.0f).setColor(gr, gg, gb, 0.4f);
        bloomBaseBuf.addVertex(matrix, bSize, 0.005f, -bSize).setUv(1.0f, 0.0f).setColor(gr, gg, gb, 0.4f);
        bloomBaseBuf.addVertex(matrix, -bSize, 0.005f, -bSize).setUv(0.0f, 0.0f).setColor(gr, gg, gb, 0.4f);
        BufferRenderer.drawWithGlobalProgram((MeshData)bloomBaseBuf.buildOrThrow());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private Color lerpColor(Color a, Color b, float t) {
        t = Mth.clamp((float)t, (float)0.0f, (float)1.0f);
        return new Color((int)((float)a.getRed() + (float)(b.getRed() - a.getRed()) * t), (int)((float)a.getGreen() + (float)(b.getGreen() - a.getGreen()) * t), (int)((float)a.getBlue() + (float)(b.getBlue() - a.getBlue()) * t));
    }

    private float smoothstep(float t) {
        t = Mth.clamp((float)t, (float)0.0f, (float)1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    static {
        BLOOM_TEXTURE = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/dashtrail/dashbloom.png");
    }
}

