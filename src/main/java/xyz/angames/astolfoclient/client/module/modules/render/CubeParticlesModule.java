package xyz.angames.astolfoclient.client.module.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class CubeParticlesModule
extends Module {
    private static final Identifier BLOOM_TEXTURE = Identifier.of((String)"astolfoclient", (String)"textures/effects/bloom.png");
    public final NumberSetting maxAmount = new NumberSetting("Amount", 60.0, 10.0, 200.0, 5.0);
    public final NumberSetting baseSize = new NumberSetting("Size", 0.25, 0.05, 1.0, 0.01);
    public final NumberSetting speedMultiplier = new NumberSetting("Speed", 1.0, 0.1, 3.0, 0.1);
    public final NumberSetting spawnRadius = new NumberSetting("Spawn Radius", 16.0, 4.0, 32.0, 1.0);
    public final ModeSetting colorMode = new ModeSetting("Color", "Theme", "Theme", "Rainbow", "White");
    public final BooleanSetting bloomGlow = new BooleanSetting("Glow Bloom", true);
    public final NumberSetting bloomScale = new NumberSetting("Bloom Scale", 3.2, 1.0, 8.0, 0.1){

        @Override
        public boolean isVisible() {
            return CubeParticlesModule.this.bloomGlow.get();
        }
    };
    public final BooleanSetting filledFaces = new BooleanSetting("Filled Faces", true);
    public final NumberSetting fillAlpha = new NumberSetting("Fill Alpha", 0.2, 0.02, 1.0, 0.02){

        @Override
        public boolean isVisible() {
            return CubeParticlesModule.this.filledFaces.get();
        }
    };
    public final BooleanSetting wireframe = new BooleanSetting("Wireframe", true);
    public final BooleanSetting internalCore = new BooleanSetting("Internal Core", true);
    public final BooleanSetting physics = new BooleanSetting("Physics Drift", true);
    private final List<CubeParticle> particles = new ArrayList<CubeParticle>();

    public CubeParticlesModule() {
        super("CubeParticles", "Renders glowing ambient 3D geometric cubes with bloom halos around you", Module.Category.RENDER);
    }

    @Override
    public void onDisable() {
        this.particles.clear();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            this.particles.clear();
            return;
        }
        long now = System.currentTimeMillis();
        float speedMul = this.speedMultiplier.getFloat();
        boolean usePhysics = this.physics.get();
        Iterator<CubeParticle> it = this.particles.iterator();
        while (it.hasNext()) {
            CubeParticle p = it.next();
            p.tick(speedMul, usePhysics);
            if (!p.isDead(now)) continue;
            it.remove();
        }
        int targetCount = (int)this.maxAmount.get();
        double radius = this.spawnRadius.get();
        Vec3d playerPos = mc.player.getEntityPos();
        int spawnPerTick = Math.min(2, Math.max(1, (targetCount - this.particles.size()) / 12 + 1));
        for (int i = 0; i < spawnPerTick && this.particles.size() < targetCount; ++i) {
            double angle = Math.random() * Math.PI * 2.0;
            double dist = 2.5 + Math.random() * (radius - 2.5);
            double height = (Math.random() - 0.3) * 6.0;
            double spawnX = playerPos.x + Math.cos(angle) * dist;
            double spawnY = playerPos.y + height;
            double spawnZ = playerPos.z + Math.sin(angle) * dist;
            Vec3d spawnPos = new Vec3d(spawnX, spawnY, spawnZ);
            Vec3d initialMotion = new Vec3d((Math.random() - 0.5) * 0.015, 0.003 + Math.random() * 0.01, (Math.random() - 0.5) * 0.015);
            Vec3d initialRot = new Vec3d(Math.random() * Math.PI * 2.0, Math.random() * Math.PI * 2.0, Math.random() * Math.PI * 2.0);
            Vec3d rotSpeed = new Vec3d(this.randomInRange(-0.03, 0.03), this.randomInRange(-0.03, 0.03), this.randomInRange(-0.03, 0.03));
            long lifespan = (long)this.randomInRange(4000.0, 7500.0);
            float scaleVar = (float)this.randomInRange(0.85, 1.15);
            this.particles.add(new CubeParticle(spawnPos, initialMotion, initialRot, rotSpeed, lifespan, scaleVar));
        }
    }

    public void onRender3D(WorldRenderContext context) {
        Color particleColor;
        float particleSize;
        double rotZ;
        double z;
        double y;
        double x;
        float scaleFactor;
        float alpha;
        if (this.particles.isEmpty()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        Camera camera = context.gameRenderer().getCamera();
        Vec3d cameraPos = camera.getCameraPos();
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
        MatrixStack ms = context.matrices();
        long now = System.currentTimeMillis();
        float baseSizeVal = this.baseSize.getFloat();
        boolean renderBloom = this.bloomGlow.get();
        float bloomScaleVal = this.bloomScale.getFloat();
        boolean renderFaces = this.filledFaces.get();
        float faceAlphaVal = this.fillAlpha.getFloat();
        boolean renderWire = this.wireframe.get();
        boolean renderCore = this.internalCore.get();
        if (renderBloom) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.disableCull();
            var bloomTexture = mc.getTextureManager().getTexture(BLOOM_TEXTURE);
            RenderSystem.setShaderTexture(0, bloomTexture.getGlTextureView(), com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().get(com.mojang.blaze3d.textures.FilterMode.LINEAR));
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder bloomBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            for (CubeParticle p : this.particles) {
                alpha = p.getAlpha(now);
                scaleFactor = p.getScaleFactor(now);
                if (alpha <= 0.005f || scaleFactor <= 0.005f) continue;
                x = MathHelper.lerp((double)tickDelta, (double)p.prevPos.x, (double)p.pos.x);
                y = MathHelper.lerp((double)tickDelta, (double)p.prevPos.y, (double)p.pos.y);
                z = MathHelper.lerp((double)tickDelta, (double)p.prevPos.z, (double)p.pos.z);
                float particleSize2 = baseSizeVal * p.scaleMultiplier * scaleFactor;
                float quadSize = particleSize2 * bloomScaleVal;
                Color particleColor2 = this.getColor(p.creationTime);
                int color = this.withAlpha(particleColor2, alpha * 0.45f);
                ms.push();
                ms.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
                ms.multiply(camera.getRotation());
                this.drawBillboardQuad(ms, bloomBuffer, -quadSize * 0.5f, -quadSize * 0.5f, quadSize, quadSize, color);
                ms.pop();
            }
            BuiltBuffer builtBloom = bloomBuffer.endNullable();
            if (builtBloom != null) {
                BufferRenderer.drawWithGlobalProgram((BuiltBuffer)builtBloom);
            }
        }
        if (renderFaces) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.disableCull();
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder faceBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (CubeParticle p : this.particles) {
                alpha = p.getAlpha(now);
                scaleFactor = p.getScaleFactor(now);
                if (alpha <= 0.005f || scaleFactor <= 0.005f) continue;
                x = MathHelper.lerp((double)tickDelta, (double)p.prevPos.x, (double)p.pos.x);
                y = MathHelper.lerp((double)tickDelta, (double)p.prevPos.y, (double)p.pos.y);
                z = MathHelper.lerp((double)tickDelta, (double)p.prevPos.z, (double)p.pos.z);
                double rotX = MathHelper.lerp((double)tickDelta, (double)p.prevRot.x, (double)p.rot.x);
                double rotY = MathHelper.lerp((double)tickDelta, (double)p.prevRot.y, (double)p.rot.y);
                rotZ = MathHelper.lerp((double)tickDelta, (double)p.prevRot.z, (double)p.rot.z);
                particleSize = baseSizeVal * p.scaleMultiplier * scaleFactor;
                particleColor = this.getColor(p.creationTime);
                float effectiveAlpha = alpha * faceAlphaVal;
                ms.push();
                ms.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
                ms.multiply(new Quaternionf().rotationXYZ((float)rotX, (float)rotY, (float)rotZ));
                ms.scale(particleSize, particleSize, particleSize);
                this.renderUnitCubeFaces(ms, faceBuffer, particleColor, effectiveAlpha);
                ms.pop();
            }
            BuiltBuffer builtFaces = faceBuffer.endNullable();
            if (builtFaces != null) {
                BufferRenderer.drawWithGlobalProgram((BuiltBuffer)builtFaces);
            }
        }
        if (renderWire || renderCore) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.disableCull();
            RenderSystem.lineWidth((float)2.0f);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder lineBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (CubeParticle p : this.particles) {
                alpha = p.getAlpha(now);
                scaleFactor = p.getScaleFactor(now);
                if (alpha <= 0.005f || scaleFactor <= 0.005f) continue;
                x = MathHelper.lerp((double)tickDelta, (double)p.prevPos.x, (double)p.pos.x);
                y = MathHelper.lerp((double)tickDelta, (double)p.prevPos.y, (double)p.pos.y);
                z = MathHelper.lerp((double)tickDelta, (double)p.prevPos.z, (double)p.pos.z);
                double rotX = MathHelper.lerp((double)tickDelta, (double)p.prevRot.x, (double)p.rot.x);
                double rotY = MathHelper.lerp((double)tickDelta, (double)p.prevRot.y, (double)p.rot.y);
                rotZ = MathHelper.lerp((double)tickDelta, (double)p.prevRot.z, (double)p.rot.z);
                particleSize = baseSizeVal * p.scaleMultiplier * scaleFactor;
                particleColor = this.getColor(p.creationTime);
                ms.push();
                ms.translate(x - cameraPos.x, y - cameraPos.y, z - cameraPos.z);
                ms.multiply(new Quaternionf().rotationXYZ((float)rotX, (float)rotY, (float)rotZ));
                ms.scale(particleSize, particleSize, particleSize);
                if (renderCore) {
                    int diagColor = this.withAlpha(particleColor, alpha * 0.35f);
                    this.renderUnitInternalDiagonals(ms, lineBuffer, diagColor);
                }
                if (renderWire) {
                    int wireColor = this.withAlpha(particleColor, alpha * 0.85f);
                    this.renderUnitOutlinedBox(ms, lineBuffer, wireColor);
                }
                ms.pop();
            }
            BuiltBuffer builtLines = lineBuffer.endNullable();
            if (builtLines != null) {
                BufferRenderer.drawWithGlobalProgram((BuiltBuffer)builtLines);
            }
            RenderSystem.lineWidth((float)1.0f);
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private Color getColor(long timeOffset) {
        String mode = this.colorMode.get();
        if (mode.equalsIgnoreCase("Rainbow")) {
            float hue = (float)((System.currentTimeMillis() + timeOffset) % 4000L) / 4000.0f;
            return Color.getHSBColor(hue, 0.75f, 1.0f);
        }
        if (mode.equalsIgnoreCase("White")) {
            return Color.WHITE;
        }
        return new Color(ThemeManager.getThemedColor(timeOffset));
    }

    private void drawBillboardQuad(MatrixStack ms, BufferBuilder builder, float x, float y, float w, float h, int color) {
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        Matrix4f m = ms.peek().getPositionMatrix();
        builder.vertex(m, x, y + h, 0.0f).texture(0.0f, 1.0f).color(r, g, b, a);
        builder.vertex(m, x + w, y + h, 0.0f).texture(1.0f, 1.0f).color(r, g, b, a);
        builder.vertex(m, x + w, y, 0.0f).texture(1.0f, 0.0f).color(r, g, b, a);
        builder.vertex(m, x, y, 0.0f).texture(0.0f, 0.0f).color(r, g, b, a);
    }

    private void renderUnitCubeFaces(MatrixStack ms, BufferBuilder builder, Color baseColor, float alpha) {
        Matrix4f m = ms.peek().getPositionMatrix();
        int topColor = this.withAlpha(this.shadeColor(baseColor, 1.0f), alpha);
        int botColor = this.withAlpha(this.shadeColor(baseColor, 0.55f), alpha);
        int frontColor = this.withAlpha(this.shadeColor(baseColor, 0.88f), alpha);
        int backColor = this.withAlpha(this.shadeColor(baseColor, 0.7f), alpha);
        int leftColor = this.withAlpha(this.shadeColor(baseColor, 0.78f), alpha);
        int rightColor = this.withAlpha(this.shadeColor(baseColor, 0.82f), alpha);
        this.renderQuad(builder, m, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, topColor);
        this.renderQuad(builder, m, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, botColor);
        this.renderQuad(builder, m, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, frontColor);
        this.renderQuad(builder, m, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, backColor);
        this.renderQuad(builder, m, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, leftColor);
        this.renderQuad(builder, m, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, rightColor);
    }

    private void renderQuad(BufferBuilder builder, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int color) {
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        builder.vertex(m, x1, y1, z1).color(r, g, b, a);
        builder.vertex(m, x2, y2, z2).color(r, g, b, a);
        builder.vertex(m, x3, y3, z3).color(r, g, b, a);
        builder.vertex(m, x4, y4, z4).color(r, g, b, a);
    }

    private Color shadeColor(Color c, float factor) {
        return new Color(Math.min(255, (int)((float)c.getRed() * factor)), Math.min(255, (int)((float)c.getGreen() * factor)), Math.min(255, (int)((float)c.getBlue() * factor)));
    }

    private void renderUnitInternalDiagonals(MatrixStack ms, BufferBuilder builder, int color) {
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        Matrix4f m = ms.peek().getPositionMatrix();
        builder.vertex(m, -0.5f, -0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, 0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, -0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, 0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, -0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, 0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, -0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, 0.5f, -0.5f).color(r, g, b, a);
    }

    private void renderUnitOutlinedBox(MatrixStack ms, BufferBuilder builder, int color) {
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        float a = (float)(color >> 24 & 0xFF) / 255.0f;
        Matrix4f m = ms.peek().getPositionMatrix();
        builder.vertex(m, -0.5f, -0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, -0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, -0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, -0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, -0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, -0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, -0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, -0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, 0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, 0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, 0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, 0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, 0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, 0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, 0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, 0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, -0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, 0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, -0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, 0.5f, -0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, -0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, 0.5f, 0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, -0.5f, 0.5f).color(r, g, b, a);
        builder.vertex(m, -0.5f, 0.5f, 0.5f).color(r, g, b, a);
    }

    private int withAlpha(Color baseColor, float alpha) {
        int a = Math.max(0, Math.min(255, (int)(alpha * 255.0f)));
        return a << 24 | baseColor.getRed() << 16 | baseColor.getGreen() << 8 | baseColor.getBlue();
    }

    private double randomInRange(double min, double max) {
        return min + Math.random() * (max - min);
    }

    @Environment(value=EnvType.CLIENT)
    private static class CubeParticle {
        Vec3d prevPos;
        Vec3d pos;
        Vec3d motion;
        Vec3d prevRot;
        Vec3d rot;
        Vec3d rotSpeed;
        final long creationTime;
        final long lifespan;
        final long fadeIn = 750L;
        final long fadeOut = 950L;
        final float scaleMultiplier;

        CubeParticle(Vec3d pos, Vec3d motion, Vec3d rot, Vec3d rotSpeed, long lifespan, float scaleMultiplier) {
            this.pos = pos;
            this.prevPos = pos;
            this.motion = motion;
            this.rot = rot;
            this.prevRot = rot;
            this.rotSpeed = rotSpeed;
            this.lifespan = lifespan;
            this.scaleMultiplier = scaleMultiplier;
            this.creationTime = System.currentTimeMillis();
        }

        void tick(float speedMul, boolean usePhysics) {
            this.prevPos = this.pos;
            this.prevRot = this.rot;
            this.pos = this.pos.add(this.motion.multiply((double)speedMul));
            this.rot = this.rot.add(this.rotSpeed.multiply((double)speedMul));
            if (usePhysics) {
                double swayX = Math.sin((double)(System.currentTimeMillis() + this.creationTime) * 0.002) * 6.0E-4;
                double swayZ = Math.cos((double)(System.currentTimeMillis() + this.creationTime) * 0.002) * 6.0E-4;
                this.motion = new Vec3d(this.motion.x * 0.98 + swayX, this.motion.y * 0.98 + 3.0E-4, this.motion.z * 0.98 + swayZ);
                this.rotSpeed = this.rotSpeed.multiply(0.995);
            }
        }

        float getAlpha(long now) {
            long elapsed = now - this.creationTime;
            if (elapsed <= 0L) {
                return 0.0f;
            }
            if (elapsed >= this.lifespan) {
                return 0.0f;
            }
            if (elapsed < 750L) {
                float t = (float)elapsed / 750.0f;
                return (float)(1.0 - Math.cos((double)t * Math.PI * 0.5));
            }
            long remaining = this.lifespan - elapsed;
            if (remaining < 950L) {
                float t = (float)remaining / 950.0f;
                return (float)(1.0 - Math.cos((double)t * Math.PI * 0.5));
            }
            return 1.0f;
        }

        float getScaleFactor(long now) {
            long elapsed = now - this.creationTime;
            if (elapsed <= 0L) {
                return 0.0f;
            }
            if (elapsed >= this.lifespan) {
                return 0.0f;
            }
            if (elapsed < 750L) {
                float t = (float)elapsed / 750.0f;
                float backT = t - 1.0f;
                float s = 1.3f;
                return Math.max(0.0f, backT * backT * ((s + 1.0f) * backT + s) + 1.0f);
            }
            long remaining = this.lifespan - elapsed;
            if (remaining < 950L) {
                float t = (float)remaining / 950.0f;
                return Math.max(0.0f, t * t * (3.0f - 2.0f * t));
            }
            return 1.0f + (float)Math.sin((double)(now + this.creationTime) * 0.0025) * 0.035f;
        }

        boolean isDead(long now) {
            return now - this.creationTime >= this.lifespan;
        }
    }
}

