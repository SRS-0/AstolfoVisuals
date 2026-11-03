package xyz.angames.astolfoclient.client.effects;
import net.minecraft.client.MinecraftClient;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
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
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.effects.Particle;
import xyz.angames.astolfoclient.client.effects.ParticleManager;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;

@Environment(value=EnvType.CLIENT)
public class ParticleRenderer {
    private static final Identifier BUBBLES_TEX = Identifier.of((String)"astolfoclient", (String)"textures/effects/bubbles.png");
    private static final Identifier STARS_TEX = Identifier.of((String)"astolfoclient", (String)"textures/effects/stars.png");
    private static final Identifier DOLLARS_TEX = Identifier.of((String)"astolfoclient", (String)"textures/effects/dollars.png");
    private static final Identifier HEART_TEX = Identifier.of((String)"astolfoclient", (String)"textures/effects/bloom.png");
    private static final Identifier SMOLESTAR_TEX = Identifier.of((String)"astolfoclient", (String)"textures/effects/mini-star.png");
    private final ParticleManager manager;

    public ParticleRenderer(ParticleManager manager) {
        this.manager = manager;
    }

    public void render(WorldRenderContext context) {
        ParticlesModule module = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
        if (module == null || !module.isEnabled()) {
            return;
        }
        List<Particle> allParticles = this.manager.getParticles();
        if (allParticles.isEmpty()) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
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
                    buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    bound = true;
                }
                float alpha = (progress = (float)age / (float)particle.lifespan) > 0.6f ? 1.0f - (progress - 0.6f) / 0.4f : 1.0f;
                Color particleColor = particle.color != null ? particle.color : new Color(ThemeManager.getThemedColor(particle.creationTime));
                float r = (float)particleColor.getRed() / 255.0f;
                float g = (float)particleColor.getGreen() / 255.0f;
                float b = (float)particleColor.getBlue() / 255.0f;
                double x = MathHelper.lerp((double)tickDelta, (double)particle.prevPosition.x, (double)particle.position.x);
                double y = MathHelper.lerp((double)tickDelta, (double)particle.prevPosition.y, (double)particle.position.y);
                double z = MathHelper.lerp((double)tickDelta, (double)particle.prevPosition.z, (double)particle.position.z);
                MatrixStack matrixStack = context.matrices();
                matrixStack.push();
                matrixStack.translate(x - context.gameRenderer().getCamera().getCameraPos().x, y - context.gameRenderer().getCamera().getCameraPos().y, z - context.gameRenderer().getCamera().getCameraPos().z);
                matrixStack.multiply(context.gameRenderer().getCamera().getRotation());
                float finalScale = particle.scale / 2.5f;
                matrixStack.scale(finalScale, finalScale, finalScale);
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(particle.rotation + (float)age * 0.1f));
                Matrix4f matrix = matrixStack.peek().getPositionMatrix();
                buffer.vertex(matrix, -0.5f, -0.5f, 0.0f).texture(0.0f, 1.0f).color(r, g, b, alpha);
                buffer.vertex(matrix, 0.5f, -0.5f, 0.0f).texture(1.0f, 1.0f).color(r, g, b, alpha);
                buffer.vertex(matrix, 0.5f, 0.5f, 0.0f).texture(1.0f, 0.0f).color(r, g, b, alpha);
                buffer.vertex(matrix, -0.5f, 0.5f, 0.0f).texture(0.0f, 0.0f).color(r, g, b, alpha);
                matrixStack.pop();
            }
            if (!bound || buffer == null) continue;
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
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

