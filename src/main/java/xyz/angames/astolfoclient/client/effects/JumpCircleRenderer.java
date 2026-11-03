package xyz.angames.astolfoclient.client.effects;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.nio.FloatBuffer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.lwjgl.BufferUtils;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.effects.JumpCircle;
import xyz.angames.astolfoclient.client.effects.JumpCircleManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.JumpCircleModule;

@Environment(value=EnvType.CLIENT)
public class JumpCircleRenderer {
    private static final Identifier TEXTURE_CLIENT = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/circle.png");
    private static final Identifier TEXTURE_LARGE = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/jump_circle/circle_large.png");
    private static final Identifier TEXTURE_SLIM = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/jump_circle/circle_slim.png");
    private final JumpCircleManager manager;
    private RenderTarget distortionFbo = null;
    private int distortionProgram = -1;
    private int distortionVao = -1;
    private int distortionVbo = -1;

    public JumpCircleRenderer(JumpCircleManager manager) {
        this.manager = manager;
    }

    private Identifier getCircleTexture(JumpCircleModule module) {
        return switch (module.style.get()) {
            case "circle_large.png", "Large", "circle_large" -> TEXTURE_LARGE;
            case "circle_slim.png", "Slim", "circle_slim" -> TEXTURE_SLIM;
            default -> TEXTURE_CLIENT;
        };
    }

    public void render(LevelRenderContext context) {
        Module jumpCircleModule = AstolfoclientClient.moduleManager.getModuleByName("JumpCircle");
        if (jumpCircleModule == null || !jumpCircleModule.isEnabled()) {
            return;
        }
        if (!(jumpCircleModule instanceof JumpCircleModule)) {
            return;
        }
        JumpCircleModule jcm = (JumpCircleModule)jumpCircleModule;
        List<JumpCircle> circles = this.manager.getCircles();
        if (circles.isEmpty()) {
            return;
        }
        if (jcm.distortion.get()) {
            this.renderDistortionShader(context, jcm, circles);
        }
        if (jcm.texture.get()) {
            this.renderJumpCircleMeshes(context, jcm, circles);
        }
    }

    private void renderDistortionShader(LevelRenderContext context, JumpCircleModule jcm, List<JumpCircle> circles) {
        var mainFbo = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if(mainFbo == null || mainFbo.getDepthTextureView() == null) return;
        int width=mainFbo.width,height=mainFbo.height;
        var copy=dev.sxmurxy.mre.renderers.GpuSceneCopy.capture();
        var pass=new xyz.angames.astolfoclient.client.render.compat.ScreenPass("jump_distortion");
        pass.texture(0,copy.getColorTextureView(),true).texture(1,mainFbo.getDepthTextureView(),false);
        Matrix4f viewRotMat = new Matrix4f().rotation((Quaternionfc)new Quaternionf((Quaternionfc)context.gameRenderer().mainCamera().rotation()).conjugate());
        Matrix4f projMat = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        Matrix4f viewProjMat = new Matrix4f((Matrix4fc)projMat).mul((Matrix4fc)viewRotMat);
        Matrix4f invViewProjMat = new Matrix4f((Matrix4fc)viewProjMat).invert();
        float[] invViewProjArr = new float[16];
        invViewProjMat.get(invViewProjArr);
        float[] viewProjArr = new float[16];
        viewProjMat.get(viewProjArr);
        pass.uniform("uInvViewProjMat", invViewProjArr);
        pass.uniform("uViewProjMat", viewProjArr);
        pass.uniform("uResolution", width, height);
        pass.uniform("uDistortionStrength", (float)jcm.distortionStrength.get());
        pass.uniform("uChromatic", jcm.chromatic.get() ? 1 : 0);
        double camX = context.gameRenderer().mainCamera().position().x;
        double camY = context.gameRenderer().mainCamera().position().y;
        double camZ = context.gameRenderer().mainCamera().position().z;
        float baseRadius = (float)jcm.radius.get();
        int maxCircles = 16;
        int count = 0;
        for (JumpCircle circle : circles) {
            if (count >= maxCircles) break;
            long age = System.currentTimeMillis() - circle.creationTime;
            if (age > 1300L) continue;
            float progress = (float)age / 1300.0f;
            float sizeEase = 1.0f - (float)Math.pow(1.0f - progress, 3.0);
            float radius = sizeEase * baseRadius * 0.5f;
            float alpha = 1.0f - progress;
            if (alpha <= 0.0f) continue;
            float relX = (float)(circle.x - camX);
            float relY = (float)(circle.y - camY);
            float relZ = (float)(circle.z - camZ);
            pass.uniform("uCirclePos[" + count + "]", relX, relY, relZ);
            pass.uniform("uCircleRadius[" + count + "]", radius);
            pass.uniform("uCircleProgress[" + count + "]", progress);
            pass.uniform("uCircleAlpha[" + count + "]", alpha);
            ++count;
        }
        pass.uniform("uCircleCount", count);

        pass.draw(mainFbo);
    }

    private void renderJumpCircleMeshes(LevelRenderContext context, JumpCircleModule jcm, List<JumpCircle> circles) {
        double camX = context.gameRenderer().mainCamera().position().x;
        double camY = context.gameRenderer().mainCamera().position().y;
        double camZ = context.gameRenderer().mainCamera().position().z;
        int themeColorInt = ThemeManager.getThemedColor(0L);
        float r = (float)(themeColorInt >> 16 & 0xFF) / 255.0f;
        float g = (float)(themeColorInt >> 8 & 0xFF) / 255.0f;
        float b = (float)(themeColorInt & 0xFF) / 255.0f;
        float baseRadius = (float)jcm.radius.get();
        Tesselator tessellator = Tesselator.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc((int)515);
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShaderTexture((int)0, (Identifier)this.getCircleTexture(jcm));
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        for (JumpCircle circle : circles) {
            long age = System.currentTimeMillis() - circle.creationTime;
            if (age > 1300L) continue;
            float progress = (float)age / 1300.0f;
            float sizeEase = 1.0f - (float)Math.pow(1.0f - progress, 3.0);
            float size = sizeEase * baseRadius;
            float spinEase = progress * progress * progress;
            float rotationAngle = spinEase * 360.0f;
            float alpha = 1.0f - progress;
            if (alpha <= 0.0f) continue;
            PoseStack matrixStack = context.poseStack();
            matrixStack.pushPose();
            matrixStack.translate(circle.x - camX, circle.y - camY + 0.02, circle.z - camZ);
            matrixStack.mulPose(Axis.YP.rotationDegrees(rotationAngle));
            matrixStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            matrixStack.scale(size, size, size);
            Matrix4f matrix = matrixStack.last().pose();
            BufferBuilder buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            buffer.addVertex(matrix, -0.5f, -0.5f, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, alpha);
            buffer.addVertex(matrix, -0.5f, 0.5f, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, alpha);
            buffer.addVertex(matrix, 0.5f, 0.5f, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, alpha);
            buffer.addVertex(matrix, 0.5f, -0.5f, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, alpha);
            BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
            matrixStack.popPose();
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    

    

    

    

    

    

    

    

    
}

