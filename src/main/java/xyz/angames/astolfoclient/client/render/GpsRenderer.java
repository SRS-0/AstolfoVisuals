package xyz.angames.astolfoclient.client.render;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.manager.GpsManager;

@Environment(value=EnvType.CLIENT)
public class GpsRenderer {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Identifier GPS_ICON = Identifier.of((String)"astolfoclient", (String)"textures/icons/gps.png");
    private String cachedDistanceText = "";
    private int lastDistanceInt = -1;

    public void render(WorldRenderContext context) {
        double renderZ;
        double renderYFinal;
        double renderX;
        GpsManager manager = GpsManager.getInstance();
        if (!manager.isActive()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
        double targetX = manager.getTargetX();
        double targetZ = manager.getTargetZ();
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
        double playerX = MathHelper.lerp((double)tickDelta, (double)client.player.lastX, (double)client.player.getX());
        double playerY = MathHelper.lerp((double)tickDelta, (double)client.player.lastY, (double)client.player.getY());
        double playerZ = MathHelper.lerp((double)tickDelta, (double)client.player.lastZ, (double)client.player.getZ());
        double targetY = playerY + 2.0;
        double distSq = client.player.squaredDistanceTo(targetX, client.player.getY(), targetZ);
        double realDistance = Math.sqrt(distSq);
        int currentDistanceInt = (int)realDistance;
        if (currentDistanceInt != this.lastDistanceInt) {
            this.cachedDistanceText = String.format("%.0fm", realDistance);
            this.lastDistanceInt = currentDistanceInt;
        }
        double renderDist = 10.0;
        double dx = targetX - playerX;
        double dy = targetY - playerY;
        double dz = targetZ - playerZ;
        double distance3D = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double dirX = dx / distance3D;
        double dirY = dy / distance3D;
        double dirZ = dz / distance3D;
        if (realDistance < renderDist) {
            renderX = targetX;
            renderYFinal = targetY;
            renderZ = targetZ;
        } else {
            renderX = playerX + dirX * renderDist;
            renderYFinal = playerY + dirY * renderDist;
            renderZ = playerZ + dirZ * renderDist;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        MatrixStack matrixStack = context.matrices();
        matrixStack.push();
        matrixStack.translate(renderX - cameraPos.x, renderYFinal - cameraPos.y, renderZ - cameraPos.z);
        matrixStack.multiply(context.gameRenderer().getCamera().getRotation());
        matrixStack.scale(1.0f, -1.0f, 1.0f);
        float fixedScale = 0.04f;
        matrixStack.scale(fixedScale, fixedScale, fixedScale);
        RenderSystem.setShaderTexture((int)0, (Identifier)GPS_ICON);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        float size = 40.0f;
        float halfSize = size / 2.0f;
        float shadowOffset = 0.8f;
        matrixStack.push();
        matrixStack.translate(shadowOffset, shadowOffset, 0.05f);
        Matrix4f shadowMatrix = matrixStack.peek().getPositionMatrix();
        BufferBuilder shadowBuffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        shadowBuffer.vertex(shadowMatrix, -halfSize, -halfSize, 0.0f).texture(0.0f, 0.0f).color(0.0f, 0.0f, 0.0f, 1.0f);
        shadowBuffer.vertex(shadowMatrix, -halfSize, halfSize, 0.0f).texture(0.0f, 1.0f).color(0.0f, 0.0f, 0.0f, 1.0f);
        shadowBuffer.vertex(shadowMatrix, halfSize, halfSize, 0.0f).texture(1.0f, 1.0f).color(0.0f, 0.0f, 0.0f, 1.0f);
        shadowBuffer.vertex(shadowMatrix, halfSize, -halfSize, 0.0f).texture(1.0f, 0.0f).color(0.0f, 0.0f, 0.0f, 1.0f);
        BufferRenderer.draw((BuiltBuffer)shadowBuffer.end());
        matrixStack.pop();
        matrixStack.push();
        matrixStack.translate(0.0f, 0.0f, -0.1f);
        Matrix4f iconMatrix = matrixStack.peek().getPositionMatrix();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(iconMatrix, -halfSize, -halfSize, 0.0f).texture(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(iconMatrix, -halfSize, halfSize, 0.0f).texture(0.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(iconMatrix, halfSize, halfSize, 0.0f).texture(1.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.vertex(iconMatrix, halfSize, -halfSize, 0.0f).texture(1.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f);
        BufferRenderer.draw((BuiltBuffer)buffer.end());
        matrixStack.pop();
        float padding = 5.0f;
        matrixStack.translate(0.0f, halfSize + padding, 0.0f);
        float textScale = 0.6f;
        float textWidth = (float)this.cachedDistanceText.length() * 4.5f;
        matrixStack.translate((double)(-(textWidth * textScale)) / 2.0, 0.0, 0.0);
        matrixStack.translate(shadowOffset, shadowOffset, 0.05f);
        this.renderText(matrixStack, this.cachedDistanceText, Color.BLACK, textScale);
        matrixStack.translate(-shadowOffset, -shadowOffset, -0.1f);
        this.renderText(matrixStack, this.cachedDistanceText, Color.WHITE, textScale);
        matrixStack.pop();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderText(MatrixStack stack, String text, Color color, float scale) {
        Matrix4f mat = stack.peek().getPositionMatrix();
        ((BuiltText)Builder.text().font((MsdfFont)BIKO_FONT.get()).text(text).color(color).size(20.0f * scale).build()).render(mat, 0.0f, 0.0f);
    }
}

