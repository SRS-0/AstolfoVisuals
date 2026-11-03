package xyz.angames.astolfoclient.client.module.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.lwjgl.BufferUtils;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class HitGlowModule
extends Module {
    public static HitGlowModule INSTANCE;
    public final NumberSetting radius = new NumberSetting("Radius", 22.0, 5.0, 40.0, 1.0);
    public final NumberSetting glowIntensity = new NumberSetting("Glow Intensity", 1.2, 0.2, 3.0, 0.1);
    public final NumberSetting speed = new NumberSetting("Speed", 1.0, 0.2, 3.0, 0.1);
    public final NumberSetting ringWidth = new NumberSetting("Ring Width", 1.6, 0.5, 4.0, 0.1);
    public final BooleanSetting distortion = new BooleanSetting("Distortion Wave", true);
    public final NumberSetting distortionStrength = new NumberSetting("Distortion Strength", 1.0, 0.1, 3.0, 0.1){

        @Override
        public boolean isVisible() {
            return HitGlowModule.this.distortion.get();
        }
    };
    public final BooleanSetting distortEntities = new BooleanSetting("Distort Entities", false){

        @Override
        public boolean isVisible() {
            return HitGlowModule.this.distortion.get();
        }
    };
    public final BooleanSetting chromatic = new BooleanSetting("Chromatic", true){

        @Override
        public boolean isVisible() {
            return HitGlowModule.this.distortion.get();
        }
    };
    public final BooleanSetting blockCorners = new BooleanSetting("Block Corners Glow", true);
    public final NumberSetting cornerGlow = new NumberSetting("Corner Glow", 2.2, 0.2, 5.0, 0.1){

        @Override
        public boolean isVisible() {
            return HitGlowModule.this.blockCorners.get();
        }
    };
    public final NumberSetting cornerSize = new NumberSetting("Corner Size", 0.18, 0.04, 0.45, 0.01){

        @Override
        public boolean isVisible() {
            return HitGlowModule.this.blockCorners.get();
        }
    };
    public final BooleanSetting edgeLines = new BooleanSetting("Edge Lines", false){

        @Override
        public boolean isVisible() {
            return HitGlowModule.this.blockCorners.get();
        }
    };
    public final NumberSetting edgeWidth = new NumberSetting("Edge Width", 0.035, 0.01, 0.1, 0.005){

        @Override
        public boolean isVisible() {
            return HitGlowModule.this.blockCorners.get() && HitGlowModule.this.edgeLines.get();
        }
    };
    public final BooleanSetting particles = new BooleanSetting("Particles", true);
    public final ModeSetting particleMode = new ModeSetting("Particle Mode", "Soul Wisps", new String[]{"Soul Wisps", "Vanilla Souls", "Both", "Glow Dots"}){

        @Override
        public boolean isVisible() {
            return HitGlowModule.this.particles.get();
        }
    };
    public final NumberSetting particleDensity = new NumberSetting("Particle Density", 1.0, 0.2, 3.0, 0.1){

        @Override
        public boolean isVisible() {
            return HitGlowModule.this.particles.get();
        }
    };
    private static final Identifier BLOOM_TEXTURE;
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final List<GlowWave> activeWaves = new CopyOnWriteArrayList<GlowWave>();
    private final List<GlowParticle> activeParticles = new CopyOnWriteArrayList<GlowParticle>();
    private Framebuffer glowFbo = null;
    private int glowProgram = -1;
    private int glowVao = -1;
    private int glowVbo = -1;

    public HitGlowModule() {
        super("HitGlow", Module.Category.RENDER);
        INSTANCE = this;
        this.addSettings(this.radius, this.glowIntensity, this.speed, this.ringWidth, this.distortion, this.distortionStrength, this.distortEntities, this.chromatic, this.blockCorners, this.cornerGlow, this.cornerSize, this.edgeLines, this.edgeWidth, this.particles, this.particleMode, this.particleDensity);
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (this.isEnabled() && player == this.mc.player && entity != null) {
                HitGlowModule.addWave(entity.getEntityPos());
            }
            return ActionResult.PASS;
        });
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            if (this.isEnabled() && !this.distortEntities.get()) {
                this.renderWorldPass(context);
            }
        });
        WorldRenderEvents.END_MAIN.register(context -> {
            if (this.isEnabled()) {
                if (this.distortEntities.get()) {
                    this.renderWorldPass(context);
                }
                this.renderParticlesPass(context);
            }
        });
    }

    public static void addWave(Vec3d pos) {
        if (INSTANCE != null && INSTANCE.isEnabled() && pos != null) {
            HitGlowModule.INSTANCE.activeWaves.add(new GlowWave(pos));
            INSTANCE.spawnWaveParticles(pos);
        }
    }

    @Override
    public void onEnable() {
        this.activeWaves.clear();
        this.activeParticles.clear();
    }

    @Override
    public void onDisable() {
        this.activeWaves.clear();
        this.activeParticles.clear();
    }

    private void spawnWaveParticles(Vec3d center) {
        if (!this.particles.get()) {
            return;
        }
        int rgb = ThemeManager.getThemedColor(0L);
        float r = (float)(rgb >> 16 & 0xFF) / 255.0f;
        float g = (float)(rgb >> 8 & 0xFF) / 255.0f;
        float b = (float)(rgb & 0xFF) / 255.0f;
        int count = (int)(14.0 * this.particleDensity.get());
        boolean isGlowDot = this.particleMode.is("Glow Dots");
        for (int i = 0; i < count; ++i) {
            long maxAge;
            double angle = Math.random() * Math.PI * 2.0;
            double dist = Math.random() * 1.8;
            double px = center.x + Math.cos(angle) * dist;
            double py = center.y + 0.1 + Math.random() * 0.8;
            double pz = center.z + Math.sin(angle) * dist;
            double driftX = Math.cos(angle) * (0.2 + Math.random() * 0.3);
            double driftZ = Math.sin(angle) * (0.2 + Math.random() * 0.3);
            float scale = isGlowDot ? 0.09f + (float)Math.random() * 0.06f : 0.16f + (float)Math.random() * 0.12f;
            long l = maxAge = isGlowDot ? 600L + (long)(Math.random() * 500.0) : 1100L + (long)(Math.random() * 800.0);
            if (!this.particleMode.is("Vanilla Souls")) {
                this.activeParticles.add(new GlowParticle(px, py, pz, driftX, driftZ, r, g, b, scale, maxAge));
            }
            if (!this.particleMode.is("Vanilla Souls") && !this.particleMode.is("Both") || this.mc.world == null || !(Math.random() < 0.4)) continue;
            this.mc.world.addParticleClient((ParticleEffect)(Math.random() < 0.5 ? ParticleTypes.SOUL : ParticleTypes.SCULK_SOUL), px, py, pz, driftX * 0.5, 0.05 + Math.random() * 0.04, driftZ * 0.5);
        }
    }

    private void renderWorldPass(WorldRenderContext context) {
        if (!this.isEnabled() || this.mc.world == null || this.mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        double currentSpeed = this.speed.get();
        this.activeWaves.removeIf(wave -> (double)(now - wave.startTime) * currentSpeed > 1400.0);
        if (this.activeWaves.isEmpty()) {
            return;
        }
        this.renderGlowShader(context, now, currentSpeed);
    }

    private void renderParticlesPass(WorldRenderContext context) {
        if (!this.isEnabled() || this.mc.world == null || this.mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        this.activeParticles.removeIf(p -> now - p.spawnTime > p.maxAge);
        if (this.particles.get() && !this.activeParticles.isEmpty() && !this.particleMode.is("Vanilla Souls")) {
            this.renderParticles(context, now);
        }
    }

    private void renderGlowShader(WorldRenderContext context, long now, double currentSpeed) {
        var mainFbo = MinecraftClient.getInstance().getFramebuffer();
        if(mainFbo == null || mainFbo.getDepthAttachmentView() == null) return;
        int width=mainFbo.textureWidth,height=mainFbo.textureHeight;
        var copy=dev.sxmurxy.mre.renderers.GpuSceneCopy.capture();
        var pass=new xyz.angames.astolfoclient.client.render.compat.ScreenPass("hit_glow");
        pass.texture(0,copy.getColorAttachmentView(),true).texture(1,mainFbo.getDepthAttachmentView(),false);
        Matrix4f viewRotMat = new Matrix4f().rotation((Quaternionfc)new Quaternionf((Quaternionfc)context.gameRenderer().getCamera().getRotation()).conjugate());
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
        pass.uniform("uGlowIntensity", (float)this.glowIntensity.get());
        pass.uniform("uRingWidth", (float)this.ringWidth.get());
        pass.uniform("uDistortion", this.distortion.get() ? 1 : 0);
        pass.uniform("uDistortionStrength", (float)this.distortionStrength.get());
        pass.uniform("uChromatic", this.chromatic.get() ? 1 : 0);
        Vec3d camPos = context.gameRenderer().getCamera().getCameraPos();
        pass.uniform("uCameraPos", (float)camPos.x, (float)camPos.y, (float)camPos.z);
        pass.uniform("uBlockCorners", this.blockCorners.get() ? 1 : 0);
        pass.uniform("uCornerGlow", (float)this.cornerGlow.get());
        pass.uniform("uCornerSize", (float)this.cornerSize.get());
        pass.uniform("uEdgeLines", this.edgeLines.get() ? 1 : 0);
        pass.uniform("uEdgeWidth", (float)this.edgeWidth.get());
        int colorInt = ThemeManager.getThemedColor(0L);
        float tr = (float)(colorInt >> 16 & 0xFF) / 255.0f;
        float tg = (float)(colorInt >> 8 & 0xFF) / 255.0f;
        float tb = (float)(colorInt & 0xFF) / 255.0f;
        pass.uniform("uThemeColor", tr, tg, tb);
        double maxRad = this.radius.get();
        int maxWaves = 16;
        int count = 0;
        for (GlowWave wave : this.activeWaves) {
            if (count >= maxWaves) break;
            double progress = (double)(now - wave.startTime) * currentSpeed / 1400.0;
            if (progress >= 1.0) continue;
            float easeRadius = (float)(maxRad * Math.sin(progress * Math.PI / 2.0));
            float alpha = (float)(1.0 - Math.pow(progress, 2.0));
            pass.uniform("uWavePos[" + count + "]", (float)(wave.pos.x - camPos.x), (float)(wave.pos.y - camPos.y), (float)(wave.pos.z - camPos.z));
            pass.uniform("uWaveRadius[" + count + "]", easeRadius);
            pass.uniform("uWaveAlpha[" + count + "]", alpha);
            pass.uniform("uWaveProgress[" + count + "]", (float)progress);
            ++count;
        }
        pass.uniform("uWaveCount", count);

        pass.draw(mainFbo);
    }

    private void renderParticles(WorldRenderContext context, long time) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShaderTexture((int)0, (Identifier)BLOOM_TEXTURE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder particleBuffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        MatrixStack matrices = context.matrices();
        Vec3d camPos = context.gameRenderer().getCamera().getCameraPos();
        Quaternionf camRot = context.gameRenderer().getCamera().getRotation();
        boolean isGlowDot = this.particleMode.is("Glow Dots");
        for (GlowParticle p : this.activeParticles) {
            float pAlpha;
            float lifePC = (float)(time - p.spawnTime) / (float)p.maxAge;
            if (lifePC < 0.0f || lifePC >= 1.0f || (pAlpha = (float)Math.sin((double)lifePC * Math.PI) * (float)this.glowIntensity.get() * 0.85f) <= 0.01f) continue;
            double px = p.getX(time) - camPos.x;
            double py = p.getY(time) - camPos.y;
            double pz = p.getZ(time) - camPos.z;
            float scale = p.baseScale * (0.6f + 0.6f * (float)Math.sin((double)lifePC * Math.PI));
            matrices.push();
            matrices.translate(px, py, pz);
            matrices.multiply(camRot);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            if (!isGlowDot) {
                float auraScale = scale * 1.5f;
                float auraAlpha = pAlpha * 0.4f;
                particleBuffer.vertex(matrix, -auraScale, -auraScale, 0.0f).texture(0.0f, 1.0f).color(p.r, p.g, p.b, auraAlpha);
                particleBuffer.vertex(matrix, auraScale, -auraScale, 0.0f).texture(1.0f, 1.0f).color(p.r, p.g, p.b, auraAlpha);
                particleBuffer.vertex(matrix, auraScale, auraScale, 0.0f).texture(1.0f, 0.0f).color(p.r, p.g, p.b, auraAlpha);
                particleBuffer.vertex(matrix, -auraScale, auraScale, 0.0f).texture(0.0f, 0.0f).color(p.r, p.g, p.b, auraAlpha);
                float coreR = Math.min(1.0f, p.r * 0.65f + 0.35f);
                float coreG = Math.min(1.0f, p.g * 0.65f + 0.35f);
                float coreB = Math.min(1.0f, p.b * 0.65f + 0.35f);
                particleBuffer.vertex(matrix, -scale, -scale, 0.0f).texture(0.0f, 1.0f).color(coreR, coreG, coreB, pAlpha);
                particleBuffer.vertex(matrix, scale, -scale, 0.0f).texture(1.0f, 1.0f).color(coreR, coreG, coreB, pAlpha);
                particleBuffer.vertex(matrix, scale, scale, 0.0f).texture(1.0f, 0.0f).color(coreR, coreG, coreB, pAlpha);
                particleBuffer.vertex(matrix, -scale, scale, 0.0f).texture(0.0f, 0.0f).color(coreR, coreG, coreB, pAlpha);
            } else {
                particleBuffer.vertex(matrix, -scale, -scale, 0.0f).texture(0.0f, 1.0f).color(p.r, p.g, p.b, pAlpha);
                particleBuffer.vertex(matrix, scale, -scale, 0.0f).texture(1.0f, 1.0f).color(p.r, p.g, p.b, pAlpha);
                particleBuffer.vertex(matrix, scale, scale, 0.0f).texture(1.0f, 0.0f).color(p.r, p.g, p.b, pAlpha);
                particleBuffer.vertex(matrix, -scale, scale, 0.0f).texture(0.0f, 0.0f).color(p.r, p.g, p.b, pAlpha);
            }
            matrices.pop();
        }
        BuiltBuffer built = particleBuffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)built);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    

    

    

    

    

    

    

    

    

    static {
        BLOOM_TEXTURE = Identifier.of((String)"astolfoclient", (String)"textures/effects/bloom.png");
    }

    @Environment(value=EnvType.CLIENT)
    public static class GlowWave {
        public final Vec3d pos;
        public final long startTime;

        public GlowWave(Vec3d pos) {
            this.pos = pos;
            this.startTime = System.currentTimeMillis();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class GlowParticle {
        double startX;
        double startY;
        double startZ;
        double riseSpeed;
        double swaySpeedX;
        double swayAmpX;
        double swayPhaseX;
        double swaySpeedZ;
        double swayAmpZ;
        double swayPhaseZ;
        double driftX;
        double driftZ;
        float r;
        float g;
        float b;
        float baseScale;
        long spawnTime;
        long maxAge;

        public GlowParticle(double x, double y, double z, double driftX, double driftZ, float r, float g, float b, float scale, long maxAge) {
            this.startX = x;
            this.startY = y;
            this.startZ = z;
            this.driftX = driftX;
            this.driftZ = driftZ;
            this.riseSpeed = 0.7 + Math.random() * 0.8;
            this.swaySpeedX = 2.5 + Math.random() * 2.0;
            this.swayAmpX = 0.06 + Math.random() * 0.1;
            this.swayPhaseX = Math.random() * Math.PI * 2.0;
            this.swaySpeedZ = 2.5 + Math.random() * 2.0;
            this.swayAmpZ = 0.06 + Math.random() * 0.1;
            this.swayPhaseZ = Math.random() * Math.PI * 2.0;
            this.r = r;
            this.g = g;
            this.b = b;
            this.baseScale = scale;
            this.spawnTime = System.currentTimeMillis();
            this.maxAge = maxAge;
        }

        public double getX(long currentTime) {
            double dt = (double)(currentTime - this.spawnTime) / 1000.0;
            return this.startX + this.driftX * dt + Math.sin(dt * this.swaySpeedX + this.swayPhaseX) * this.swayAmpX;
        }

        public double getY(long currentTime) {
            double dt = (double)(currentTime - this.spawnTime) / 1000.0;
            return this.startY + this.riseSpeed * dt;
        }

        public double getZ(long currentTime) {
            double dt = (double)(currentTime - this.spawnTime) / 1000.0;
            return this.startZ + this.driftZ * dt + Math.cos(dt * this.swaySpeedZ + this.swayPhaseZ) * this.swayAmpZ;
        }
    }
}

