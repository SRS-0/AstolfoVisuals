package xyz.angames.astolfoclient.client.effects;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
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
import xyz.angames.astolfoclient.client.effects.Particle;
import xyz.angames.astolfoclient.client.effects.ParticleManager;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;

@Environment(value=EnvType.CLIENT)
public class ParticleRenderer {
    private static final Identifier BUBBLES_TEX = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/bubbles.png");
    private static final Identifier STARS_TEX = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/stars.png");
    private static final Identifier DOLLARS_TEX = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/dollars.png");
    private static final Identifier HEART_TEX = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/bloom.png");
    private static final Identifier SMOLESTAR_TEX = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/mini-star.png");
    private final ParticleManager manager;

    public ParticleRenderer(ParticleManager manager) {
        this.manager = manager;
    }

    public void render(LevelRenderContext context) {
        ParticlesModule module = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
        if (module == null || !module.isEnabled()) {
            return;
        }
        List<Particle> allParticles = this.manager.getParticles();
        if (allParticles.isEmpty()) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFunc((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        Tesselator tessellator = Tesselator.getInstance();
        for (ParticlesModule.ParticleType type : ParticlesModule.ParticleType.values()) {
            boolean bound = false;
            BufferBuilder buffer = null;
            for (Particle particle : allParticles) {
                float progress;
                long age;
                if (particle.type != type || (age = currentTime - particle.creationTime) > particle.lifespan) continue;
                if (!bound) {
                    RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
                    RenderSystem.setShaderTexture((int)0, (Identifier)this.getTextureForType(type));
                    buffer = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                    bound = true;
                }
                float alpha = (progress = (float)age / (float)particle.lifespan) > 0.6f ? 1.0f - (progress - 0.6f) / 0.4f : 1.0f;
                Color particleColor = particle.color != null ? particle.color : new Color(ThemeManager.getThemedColor(particle.creationTime));
                float r = (float)particleColor.getRed() / 255.0f;
                float g = (float)particleColor.getGreen() / 255.0f;
                float b = (float)particleColor.getBlue() / 255.0f;
                double x = Mth.lerp((double)tickDelta, (double)particle.prevPosition.x, (double)particle.position.x);
                double y = Mth.lerp((double)tickDelta, (double)particle.prevPosition.y, (double)particle.position.y);
                double z = Mth.lerp((double)tickDelta, (double)particle.prevPosition.z, (double)particle.position.z);
                PoseStack matrixStack = context.poseStack();
                matrixStack.pushPose();
                matrixStack.translate(x - context.gameRenderer().mainCamera().position().x, y - context.gameRenderer().mainCamera().position().y, z - context.gameRenderer().mainCamera().position().z);
                matrixStack.rotate(context.gameRenderer().mainCamera().rotation());
                float finalScale = particle.scale / 2.5f;
                matrixStack.scale(finalScale, finalScale, finalScale);
                matrixStack.rotate(Axis.ZP.rotationDegrees(particle.rotation + (float)age * 0.1f));
                Matrix4f matrix = matrixStack.last().pose();
                buffer.addVertex(matrix, -0.5f, -0.5f, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, 0.5f, -0.5f, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, 0.5f, 0.5f, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, -0.5f, 0.5f, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, alpha);
                matrixStack.popPose();
            }
            if (!bound || buffer == null) continue;
            BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private Identifier getTextureForType(ParticlesModule.ParticleType type) {
        switch (type) {
            case BUBBLES: {
                return BUBBLES_TEX;
            }
            case DOLLARS: {
                return DOLLARS_TEX;
            }
            case HEART: {
                return HEART_TEX;
            }
            case SMOLESTAR: {
                return SMOLESTAR_TEX;
            }
        }
        return STARS_TEX;
    }
}

