package xyz.angames.astolfoclient.client.effects;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.BabyPlayerModule;

@Environment(value=EnvType.CLIENT)
public class TrailsRenderer {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final List<TrailPoint> points = new ArrayList<TrailPoint>();
    private static final long TRAIL_LIFESPAN = 850L;

    public void render(WorldRenderContext context) {
        float midY;
        float heightScale;
        float alpha;
        float reverseProgress;
        float progress;
        long age;
        float targetHeight;
        Module module = AstolfoclientClient.moduleManager.getModuleByName("Trails");
        if (module == null || !module.isEnabled() || this.client.options.getPerspective().isFirstPerson()) {
            this.points.clear();
            return;
        }
        ClientPlayerEntity player = this.client.player;
        if (player == null) {
            return;
        }
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
        double x = MathHelper.lerp((double)tickDelta, (double)player.lastRenderX, (double)player.getX());
        double y = MathHelper.lerp((double)tickDelta, (double)player.lastRenderY, (double)player.getY());
        double z = MathHelper.lerp((double)tickDelta, (double)player.lastRenderZ, (double)player.getZ());
        Vec3d currentPos = new Vec3d(x, y, z);
        boolean isBaby = false;
        Module babyModule = AstolfoclientClient.moduleManager.getModuleByName("BabyPlayer");
        if (babyModule instanceof BabyPlayerModule) {
            BabyPlayerModule bpm = (BabyPlayerModule)babyModule;
            isBaby = bpm.isEnabled() && bpm.self.get();
        }
        float f = targetHeight = isBaby ? 0.9f : player.getHeight();
        if (this.points.isEmpty() || this.points.get((int)(this.points.size() - 1)).pos.squaredDistanceTo(currentPos) > 0.001) {
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
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        MatrixStack matrices = context.matrices();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        int index = 0;
        for (TrailPoint point : this.points) {
            age = currentTime - point.timeCreated;
            progress = MathHelper.clamp((float)((float)age / 850.0f), (float)0.0f, (float)1.0f);
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
            buffer.vertex(matrix, (float)renderX, (float)(renderY + (double)topY), (float)renderZ).color(r, g, b, bodyAlpha);
            buffer.vertex(matrix, (float)renderX, (float)(renderY + (double)botY), (float)renderZ).color(r, g, b, bodyAlpha);
            ++index;
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.lineWidth((float)2.5f);
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        index = 0;
        for (TrailPoint point : this.points) {
            age = currentTime - point.timeCreated;
            progress = MathHelper.clamp((float)((float)age / 850.0f), (float)0.0f, (float)1.0f);
            reverseProgress = 1.0f - progress;
            alpha = (float)Math.pow(reverseProgress, 2.0);
            heightScale = reverseProgress * reverseProgress;
            midY = point.height / 2.0f;
            float topY = midY + midY * heightScale;
            Color c = new Color(ThemeManager.getThemedColor(index * 20));
            double renderX = point.pos.x - cameraPos.x;
            double renderY = point.pos.y - cameraPos.y;
            double renderZ = point.pos.z - cameraPos.z;
            buffer.vertex(matrix, (float)renderX, (float)(renderY + (double)topY), (float)renderZ).color((float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f, alpha * 0.8f);
            ++index;
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        index = 0;
        for (TrailPoint point : this.points) {
            age = currentTime - point.timeCreated;
            progress = MathHelper.clamp((float)((float)age / 850.0f), (float)0.0f, (float)1.0f);
            reverseProgress = 1.0f - progress;
            alpha = (float)Math.pow(reverseProgress, 2.0);
            heightScale = reverseProgress * reverseProgress;
            midY = point.height / 2.0f;
            float botY = midY - midY * heightScale;
            Color c = new Color(ThemeManager.getThemedColor(index * 20));
            double renderX = point.pos.x - cameraPos.x;
            double renderY = point.pos.y - cameraPos.y;
            double renderZ = point.pos.z - cameraPos.z;
            buffer.vertex(matrix, (float)renderX, (float)(renderY + (double)botY), (float)renderZ).color((float)c.getRed() / 255.0f, (float)c.getGreen() / 255.0f, (float)c.getBlue() / 255.0f, alpha * 0.8f);
            ++index;
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Environment(value=EnvType.CLIENT)
    private static class TrailPoint {
        final Vec3d pos;
        final float height;
        final long timeCreated;

        public TrailPoint(Vec3d pos, float height, long timeCreated) {
            this.pos = pos;
            this.height = height;
            this.timeCreated = timeCreated;
        }
    }
}

