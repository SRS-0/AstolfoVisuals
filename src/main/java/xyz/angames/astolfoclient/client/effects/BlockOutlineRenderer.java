package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
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
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.modules.render.BlockOutlineModule;

@Environment(value=EnvType.CLIENT)
public class BlockOutlineRenderer {
    private RenderBox currentBox = null;
    private BlockPos lastPos = null;
    private long lastRenderTime = System.currentTimeMillis();
    private float fadeAlpha = 0.0f;

    public void render(LevelRenderContext context) {
        BlockHitResult blockHit;
        BlockPos pos;
        BlockState state;
        VoxelShape shape;
        HitResult hit;
        boolean hasBlockTarget;
        BlockOutlineModule module = (BlockOutlineModule)AstolfoclientClient.moduleManager.getModuleByName("BlockOutline");
        if (module == null || !module.isEnabled()) {
            this.fadeAlpha = 0.0f;
            this.currentBox = null;
            this.lastPos = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        float deltaTime = (float)(now - this.lastRenderTime) / 1000.0f;
        this.lastRenderTime = now;
        if (deltaTime > 0.1f) {
            deltaTime = 0.1f;
        }
        if (deltaTime < 0.001f) {
            deltaTime = 0.001f;
        }
        boolean bl = hasBlockTarget = (hit = mc.hitResult) != null && hit.getType() == HitResult.Type.BLOCK;
        if (module.fadeEffect.get()) {
            float fadeRate = (float)module.fadeSpeed.get();
            this.fadeAlpha = hasBlockTarget ? Math.min(1.0f, this.fadeAlpha + deltaTime * fadeRate) : Math.max(0.0f, this.fadeAlpha - deltaTime * fadeRate);
        } else {
            float f = this.fadeAlpha = hasBlockTarget ? 1.0f : 0.0f;
        }
        if (this.fadeAlpha <= 0.001f) {
            this.currentBox = null;
            this.lastPos = null;
            return;
        }
        if (hasBlockTarget && !(shape = (state = mc.level.getBlockState(pos = (blockHit = (BlockHitResult)hit).getBlockPos())).getShape((BlockGetter)mc.level, pos)).isEmpty()) {
            AABB targetBox = shape.bounds().move(pos).inflate(0.002);
            RenderBox targetRenderBox = new RenderBox(targetBox.minX, targetBox.minY, targetBox.minZ, targetBox.maxX, targetBox.maxY, targetBox.maxZ);
            float morphSpeed = (float)module.animSpeed.get();
            float lerpAmount = Math.min(1.0f, deltaTime * morphSpeed);
            this.currentBox = this.currentBox == null || !module.smoothAnim.get() || this.lastPos != null && pos.distSqr((Vec3i)this.lastPos) > 64.0 ? targetRenderBox : this.currentBox.lerp(targetRenderBox, lerpAmount);
            this.lastPos = pos;
        }
        if (this.currentBox == null) {
            return;
        }
        Camera camera = context.gameRenderer().mainCamera();
        PoseStack matrices = context.poseStack();
        matrices.pushPose();
        matrices.translate(-camera.position().x, -camera.position().y, -camera.position().z);
        Color themeColor = new Color(ThemeManager.getThemedColor(0L));
        float r = (float)themeColor.getRed() / 255.0f;
        float g = (float)themeColor.getGreen() / 255.0f;
        float b = (float)themeColor.getBlue() / 255.0f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        if (module.onlyVisible.get()) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        Tesselator tessellator = Tesselator.getInstance();
        if (module.shaderFill.get()) {
            ShaderProgram shader = RenderSystem.setShader((ShaderProgramKey)AstolfoclientClient.BLOCK_OUTLINE_SHADER);
            if (shader != null) {
                float timeSecs = (float)(System.currentTimeMillis() % 1000000L) / 1000.0f;
                if (shader.getUniform("uTime") != null) {
                    shader.getUniform("uTime").set(timeSecs);
                }
                int color1 = ThemeManager.getThemedColor(0L);
                float r1 = (float)(color1 >> 16 & 0xFF) / 255.0f;
                float g1 = (float)(color1 >> 8 & 0xFF) / 255.0f;
                float b1 = (float)(color1 & 0xFF) / 255.0f;
                int color2 = ThemeManager.getThemedColor(1000L);
                float r2 = (float)(color2 >> 16 & 0xFF) / 255.0f;
                float g2 = (float)(color2 >> 8 & 0xFF) / 255.0f;
                float b2 = (float)(color2 & 0xFF) / 255.0f;
                if (shader.getUniform("uColor1") != null) {
                    shader.getUniform("uColor1").set(r1, g1, b1);
                }
                if (shader.getUniform("uColor2") != null) {
                    shader.getUniform("uColor2").set(r2, g2, b2);
                }
                if (shader.getUniform("uBlockCenter") != null) {
                    double centerX = (this.currentBox.minX + this.currentBox.maxX) / 2.0;
                    double centerY = (this.currentBox.minY + this.currentBox.maxY) / 2.0;
                    double centerZ = (this.currentBox.minZ + this.currentBox.maxZ) / 2.0;
                    var center = matrices.last().pose().transformPosition(new org.joml.Vector3f((float)centerX, (float)centerY, (float)centerZ));
                    shader.getUniform("uBlockCenter").set(center.x, center.y, center.z);
                }
                if (shader.getUniform("uGlowIntensity") != null) {
                    shader.getUniform("uGlowIntensity").set((float)module.glowIntensity.get());
                }
                if (shader.getUniform("uPulseSpeed") != null) {
                    shader.getUniform("uPulseSpeed").set((float)module.pulseSpeed.get());
                }
                if (shader.getUniform("uPulseWidth") != null) {
                    shader.getUniform("uPulseWidth").set((float)module.pulseWidth.get());
                }
                if (shader.getUniform("uDistortion") != null) {
                    shader.getUniform("uDistortion").set(module.distortion.get() ? 1.0f : 0.0f);
                }
                if (shader.getUniform("uChromatic") != null) {
                    shader.getUniform("uChromatic").set(module.chromatic.get() ? 1.0f : 0.0f);
                }
                if (shader.getUniform("uFadeAlpha") != null) {
                    shader.getUniform("uFadeAlpha").set(this.fadeAlpha);
                }
                if (shader.getUniform("uFillAlpha") != null) {
                    shader.getUniform("uFillAlpha").set((float)module.fillAlpha.get());
                }
                if (shader.getUniform("uMode") != null) {
                    int modeIdx = 0;
                    modeIdx = module.mode.is("Pulse Wave") ? 0 : (module.mode.is("Cosmos") ? 1 : (module.mode.is("Neon Glow") ? 2 : (module.mode.is("Rainbow Wave") ? 3 : (module.mode.is("Cyber Grid") ? 4 : 5))));
                    shader.getUniform("uMode").set(modeIdx);
                }
            }
            BufferBuilder buffer = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            this.drawBoxFaces(matrices, buffer, this.currentBox, r, g, b, 1.0f);
            MeshData builtBuffer = buffer.build();
            if (builtBuffer != null) {
                BufferRenderer.drawWithGlobalProgram((MeshData)builtBuffer);
            }
        }
        if (module.outline.get()) {
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth((float)((float)module.lineWidth.get()));
            float finalOutlineAlpha = (float)module.outlineAlpha.get() * this.fadeAlpha;
            if (finalOutlineAlpha > 0.01f) {
                BufferBuilder lineBuffer = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
                this.drawBoxOutline(matrices, lineBuffer, this.currentBox, r, g, b, finalOutlineAlpha);
                MeshData builtLines = lineBuffer.build();
                if (builtLines != null) {
                    BufferRenderer.drawWithGlobalProgram((MeshData)builtLines);
                }
            }
            RenderSystem.lineWidth((float)1.0f);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.popPose();
    }

    private void drawBoxFaces(PoseStack matrices, BufferBuilder buffer, RenderBox box, float r, float g, float b, float a) {
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        Matrix4f m = matrices.last().pose();
        buffer.addVertex(m, minX, minY, maxZ).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, maxZ).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, minZ).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, minZ).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, minZ).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, minZ).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, maxZ).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, maxZ).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, minZ).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, minZ).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, minZ).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, minZ).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, maxZ).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, maxZ).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, maxZ).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, maxZ).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, minZ).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, maxZ).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, maxZ).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, minZ).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, maxZ).setUv(0.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, minZ).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, minZ).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, maxZ).setUv(0.0f, 1.0f).setColor(r, g, b, a);
    }

    private void drawBoxOutline(PoseStack matrices, BufferBuilder buffer, RenderBox box, float r, float g, float b, float a) {
        float minX = (float)box.minX;
        float minY = (float)box.minY;
        float minZ = (float)box.minZ;
        float maxX = (float)box.maxX;
        float maxY = (float)box.maxY;
        float maxZ = (float)box.maxZ;
        Matrix4f m = matrices.last().pose();
        buffer.addVertex(m, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, minZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, maxX, maxY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, minY, maxZ).setColor(r, g, b, a);
        buffer.addVertex(m, minX, maxY, maxZ).setColor(r, g, b, a);
    }

    @Environment(value=EnvType.CLIENT)
    private static class RenderBox {
        public double minX;
        public double minY;
        public double minZ;
        public double maxX;
        public double maxY;
        public double maxZ;

        public RenderBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public RenderBox lerp(RenderBox target, float delta) {
            return new RenderBox(this.minX + (target.minX - this.minX) * (double)delta, this.minY + (target.minY - this.minY) * (double)delta, this.minZ + (target.minZ - this.minZ) * (double)delta, this.maxX + (target.maxX - this.maxX) * (double)delta, this.maxY + (target.maxY - this.maxY) * (double)delta, this.maxZ + (target.maxZ - this.maxZ) * (double)delta);
        }

        public RenderBox scale(float factor) {
            double centerX = (this.minX + this.maxX) / 2.0;
            double centerY = (this.minY + this.maxY) / 2.0;
            double centerZ = (this.minZ + this.maxZ) / 2.0;
            double halfSizeX = (this.maxX - this.minX) / 2.0 * (double)factor;
            double halfSizeY = (this.maxY - this.minY) / 2.0 * (double)factor;
            double halfSizeZ = (this.maxZ - this.minZ) / 2.0 * (double)factor;
            return new RenderBox(centerX - halfSizeX, centerY - halfSizeY, centerZ - halfSizeZ, centerX + halfSizeX, centerY + halfSizeY, centerZ + halfSizeZ);
        }

        public double getVolume() {
            return (this.maxX - this.minX) * (this.maxY - this.minY) * (this.maxZ - this.minZ);
        }
    }
}

