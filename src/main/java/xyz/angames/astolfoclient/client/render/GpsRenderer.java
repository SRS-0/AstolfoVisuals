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
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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
import com.mojang.renderpearl.api.vertex.VertexFormat;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.manager.GpsManager;

@Environment(value=EnvType.CLIENT)
public class GpsRenderer {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private static final Identifier GPS_ICON = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/icons/gps.png");
    private String cachedDistanceText = "";
    private int lastDistanceInt = -1;

    public void render(LevelRenderContext context) {
        double renderZ;
        double renderYFinal;
        double renderX;
        GpsManager manager = GpsManager.getInstance();
        if (!manager.isActive()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        Vec3 cameraPos = context.gameRenderer().mainCamera().position();
        double targetX = manager.getTargetX();
        double targetZ = manager.getTargetZ();
        float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        double playerX = Mth.lerp((double)tickDelta, (double)client.player.xo, (double)client.player.getX());
        double playerY = Mth.lerp((double)tickDelta, (double)client.player.yo, (double)client.player.getY());
        double playerZ = Mth.lerp((double)tickDelta, (double)client.player.zo, (double)client.player.getZ());
        double targetY = playerY + 2.0;
        double distSq = client.player.distanceToSqr(targetX, client.player.getY(), targetZ);
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
        PoseStack matrixStack = context.poseStack();
        matrixStack.pushPose();
        matrixStack.translate(renderX - cameraPos.x, renderYFinal - cameraPos.y, renderZ - cameraPos.z);
        matrixStack.rotate(context.gameRenderer().mainCamera().rotation());
        matrixStack.scale(1.0f, -1.0f, 1.0f);
        float fixedScale = 0.04f;
        matrixStack.scale(fixedScale, fixedScale, fixedScale);
        RenderSystem.setShaderTexture((int)0, (Identifier)GPS_ICON);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        Tesselator tessellator = Tesselator.getInstance();
        float size = 40.0f;
        float halfSize = size / 2.0f;
        float shadowOffset = 0.8f;
        matrixStack.pushPose();
        matrixStack.translate(shadowOffset, shadowOffset, 0.05f);
        Matrix4f shadowMatrix = matrixStack.last().pose();
        BufferBuilder shadowBuffer = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        shadowBuffer.addVertex(shadowMatrix, -halfSize, -halfSize, 0.0f).setUv(0.0f, 0.0f).setColor(0.0f, 0.0f, 0.0f, 1.0f);
        shadowBuffer.addVertex(shadowMatrix, -halfSize, halfSize, 0.0f).setUv(0.0f, 1.0f).setColor(0.0f, 0.0f, 0.0f, 1.0f);
        shadowBuffer.addVertex(shadowMatrix, halfSize, halfSize, 0.0f).setUv(1.0f, 1.0f).setColor(0.0f, 0.0f, 0.0f, 1.0f);
        shadowBuffer.addVertex(shadowMatrix, halfSize, -halfSize, 0.0f).setUv(1.0f, 0.0f).setColor(0.0f, 0.0f, 0.0f, 1.0f);
        BufferRenderer.draw((MeshData)shadowBuffer.buildOrThrow());
        matrixStack.popPose();
        matrixStack.pushPose();
        matrixStack.translate(0.0f, 0.0f, -0.1f);
        Matrix4f iconMatrix = matrixStack.last().pose();
        BufferBuilder buffer = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(iconMatrix, -halfSize, -halfSize, 0.0f).setUv(0.0f, 0.0f).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.addVertex(iconMatrix, -halfSize, halfSize, 0.0f).setUv(0.0f, 1.0f).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.addVertex(iconMatrix, halfSize, halfSize, 0.0f).setUv(1.0f, 1.0f).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        buffer.addVertex(iconMatrix, halfSize, -halfSize, 0.0f).setUv(1.0f, 0.0f).setColor(1.0f, 1.0f, 1.0f, 1.0f);
        BufferRenderer.draw((MeshData)buffer.buildOrThrow());
        matrixStack.popPose();
        float padding = 5.0f;
        matrixStack.translate(0.0f, halfSize + padding, 0.0f);
        float textScale = 0.6f;
        float textWidth = (float)this.cachedDistanceText.length() * 4.5f;
        matrixStack.translate((double)(-(textWidth * textScale)) / 2.0, 0.0, 0.0);
        matrixStack.translate(shadowOffset, shadowOffset, 0.05f);
        this.renderText(matrixStack, this.cachedDistanceText, Color.BLACK, textScale);
        matrixStack.translate(-shadowOffset, -shadowOffset, -0.1f);
        this.renderText(matrixStack, this.cachedDistanceText, Color.WHITE, textScale);
        matrixStack.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void renderText(PoseStack stack, String text, Color color, float scale) {
        Matrix4f mat = stack.last().pose();
        ((BuiltText)Builder.text().font((MsdfFont)BIKO_FONT.get()).text(text).color(color).size(20.0f * scale).build()).render(mat, 0.0f, 0.0f);
    }
}

