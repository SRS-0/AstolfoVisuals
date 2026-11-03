package xyz.angames.astolfoclient.client.effects;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
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
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.BabyPlayerModule;

@Environment(value=EnvType.CLIENT)
public class TrailsRenderer {
    private final Minecraft client = Minecraft.getInstance();
    private final List<TrailPoint> points = new ArrayList<TrailPoint>();
    private static final long TRAIL_LIFESPAN = 850L;

    public void render(LevelRenderContext context) {
        float midY;
        float heightScale;
        float alpha;
        float reverseProgress;
        float progress;
        long age;
        float targetHeight;
        Module module = AstolfoclientClient.moduleManager.getModuleByName("Trails");
        if (module == null || !module.isEnabled() || this.client.options.getCameraType().isFirstPerson()) {
            this.points.clear();
            return;
        }
        LocalPlayer player = this.client.player;
        if (player == null) {
            return;
        }
        float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        double x = Mth.lerp((double)tickDelta, (double)player.xOld, (double)player.getX());
        double y = Mth.lerp((double)tickDelta, (double)player.yOld, (double)player.getY());
        double z = Mth.lerp((double)tickDelta, (double)player.zOld, (double)player.getZ());
        Vec3 currentPos = new Vec3(x, y, z);
        boolean isBaby = false;
        Module babyModule = AstolfoclientClient.moduleManager.getModuleByName("BabyPlayer");
        if (babyModule instanceof BabyPlayerModule) {
            BabyPlayerModule bpm = (BabyPlayerModule)babyModule;
            isBaby = bpm.isEnabled() && bpm.self.get();
        }
        float f = targetHeight = isBaby ? 0.9f : player.getBbHeight();
        if (this.points.isEmpty() || this.points.get((int)(this.points.size() - 1)).pos.distanceToSqr(currentPos) > 0.001) {
            this.points.add(new TrailPoint(currentPos, targetHeight, System.currentTimeMillis()));
        }
        long currentTime = System.currentTimeMillis();
        this.points.removeIf(p -> currentTime - p.timeCreated > 850L);
        if (this.points.size() < 2) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.ONE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        Tesselator tessellator = Tesselator.getInstance();
        PoseStack matrices = context.poseStack();
        Matrix4f matrix = matrices.last().pose();
        Vec3 cameraPos = context.gameRenderer().mainCamera().position();
        BufferBuilder buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int index = 0;
        for (TrailPoint point : this.points) {
            age = currentTime - point.timeCreated;
            progress = Mth.clamp((float)((float)age / 850.0f), (float)0.0f, (float)1.0f);
            reverseProgress = 1.0f - progress;
            alpha = (float)Math.pow(reverseProgress, 2.5);
            float bodyAlpha = alpha * 0.45f;
            float heightScale2 = reverseProgress * reverseProgress;
            float midY2 = point.height / 2.0f;
            float topY = midY2 + midY2 * heightScale2;
            float botY = midY2 - midY2 * heightScale2;
            Color c = new Color(ThemeManager.getThemedColor(index * 20));
            float r = (float)c.getRed() / 255.0f;
            float g = (float)c.getGreen() / 255.0f;
            float b = (float)c.getBlue() / 255.0f;
            double renderX = point.pos.x - cameraPos.x;
            double renderY = point.pos.y - cameraPos.y;
            double renderZ = point.pos.z - cameraPos.z;
            buffer.addVertex(matrix, (float)renderX, (float)(renderY + (double)topY), (float)renderZ).setColor(r, g, b, bodyAlpha);
            buffer.addVertex(matrix, (float)renderX, (float)(renderY + (double)botY), (float)renderZ).setColor(r, g, b, bodyAlpha);
            ++index;
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
        RenderSystem.lineWidth((float)2.5f);
        buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        index = 0;
        for (TrailPoint point : this.points) {
            age = currentTime - point.timeCreated;
            progress = Mth.clamp((float)((float)age / 850.0f), (float)0.0f, (float)1.0f);
            reverseProgress = 1.0f - progress;
            alpha = (float)Math.pow(reverseProgress, 2.0);
            heightScale = reverseProgress * reverseProgress;
            midY = point.height / 2.0f;
            float topY = midY + midY * heightScale;
            Color c = new Color(ThemeManager.getThemedColor(index * 20));
            double renderX = point.pos.x - cameraPos.x;
            double renderY = point.pos.y - cameraPos.y;
            double renderZ = point.pos.z - cameraPos.z;
            buffer.addVertex(matrix, (float)renderX, (float)(renderY + (double)topY), (float)renderZ).setColor((float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f, alpha * 0.8f);
            ++index;
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
        buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        index = 0;
        for (TrailPoint point : this.points) {
            age = currentTime - point.timeCreated;
            progress = Mth.clamp((float)((float)age / 850.0f), (float)0.0f, (float)1.0f);
            reverseProgress = 1.0f - progress;
            alpha = (float)Math.pow(reverseProgress, 2.0);
            heightScale = reverseProgress * reverseProgress;
            midY = point.height / 2.0f;
            float botY = midY - midY * heightScale;
            Color c = new Color(ThemeManager.getThemedColor(index * 20));
            double renderX = point.pos.x - cameraPos.x;
            double renderY = point.pos.y - cameraPos.y;
            double renderZ = point.pos.z - cameraPos.z;
            buffer.addVertex(matrix, (float)renderX, (float)(renderY + (double)botY), (float)renderZ).setColor((float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f, alpha * 0.8f);
            ++index;
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Environment(value=EnvType.CLIENT)
    private static class TrailPoint {
        final Vec3 pos;
        final float height;
        final long timeCreated;

        public TrailPoint(Vec3 pos, float height, long timeCreated) {
            this.pos = pos;
            this.height = height;
            this.timeCreated = timeCreated;
        }
    }
}

