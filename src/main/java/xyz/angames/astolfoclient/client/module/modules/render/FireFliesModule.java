package xyz.angames.astolfoclient.client.module.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
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
import org.joml.Quaternionf;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class FireFliesModule
extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    public final BooleanSetting darkImprint = new BooleanSetting("DarkImprint", false);
    public final BooleanSetting lighting = new BooleanSetting("Lighting", false);
    public final NumberSetting spawnDelay = new NumberSetting("SpawnDelay", 3.0, 1.0, 10.0, 0.5);
    private static final int MAX_FIREFLIES = 20;
    private static final long MAX_PART_ALIVE_TIME = 6000L;
    private final List<FirePart> partList = new ArrayList<FirePart>();
    private static final Identifier ICON_TEXTURE = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/bloom.png");

    public FireFliesModule() {
        super("FireFlies", "Renders beautiful glowing fireflies around you", Module.Category.RENDER);
        this.addSettings(this.darkImprint, this.lighting, this.spawnDelay);
        LevelRenderEvents.END_MAIN.register(this::render3D);
    }

    @Override
    public void onDisable() {
        this.partList.clear();
    }

    @Override
    public void onTick() {
        if (!this.isEnabled() || this.mc.player == null || this.mc.level == null) {
            this.partList.clear();
            return;
        }
        if (this.mc.player.tickCount == 1) {
            for (FirePart part2 : this.partList) {
                part2.setToRemove();
            }
        }
        long currentTime = System.currentTimeMillis();
        for (FirePart part3 : this.partList) {
            part3.updatePart(this.mc);
        }
        this.partList.removeIf(part -> part.toRemove || currentTime - part.startTime >= 6000L);
        while (this.partList.size() > 20) {
            this.partList.remove(0);
        }
        if (this.partList.size() < 20 && this.mc.player.tickCount % ((int)this.spawnDelay.get() + 1) == 0) {
            this.partList.add(new FirePart(this.generateVecForPart(this.mc, 10.0, 4.0), 6000.0f));
            this.partList.add(new FirePart(this.generateVecForPart(this.mc, 6.0, 5.0), 6000.0f));
        }
    }

    private Vec3 generateVecForPart(Minecraft mc, double rangeXZ, double rangeY) {
        Vec3 pos = mc.player.position().add((double)FireFliesModule.getRandom(-rangeXZ, rangeXZ), (double)FireFliesModule.getRandom(-rangeY / 2.0, rangeY), (double)FireFliesModule.getRandom(-rangeXZ, rangeXZ));
        for (int i = 0; i < 30; ++i) {
            pos = mc.player.position().add((double)FireFliesModule.getRandom(-rangeXZ, rangeXZ), (double)FireFliesModule.getRandom(-rangeY / 2.0, rangeY), (double)FireFliesModule.getRandom(-rangeXZ, rangeXZ));
        }
        return pos;
    }

    private static float getRandom(double min, double max) {
        return (float)(min + Math.random() * (max - min));
    }

    private static float lerp(float from, float to, float pct) {
        return from + (to - from) * pct;
    }

    private void render3D(LevelRenderContext context) {
        float partAlpha;
        if (!this.isEnabled() || this.partList.isEmpty() || this.mc.player == null || this.mc.level == null) {
            return;
        }
        PoseStack matrixStack = context.poseStack();
        float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 cameraPos = context.gameRenderer().mainCamera().position();
        Quaternionf cameraRot = context.gameRenderer().mainCamera().rotation();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        int baseColor = ThemeManager.getThemedColor(0L);
        float r = (float)(baseColor >> 16 & 0xFF) / 255.0f;
        float g = (float)(baseColor >> 8 & 0xFF) / 255.0f;
        float b = (float)(baseColor & 0xFF) / 255.0f;
        if (this.darkImprint.get()) {
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.blendFunc((com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.ONE);
        }
        Tesselator tessellator = Tesselator.getInstance();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder sparkBuffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean hasSparks = false;
        for (FirePart part : this.partList) {
            partAlpha = part.getAlphaPC();
            if (part.sparkParts.isEmpty()) continue;
            for (SparkPart spark : part.sparkParts) {
                double sparkX = spark.prevPosX + (spark.posX - spark.prevPosX) * (double)tickDelta - cameraPos.x;
                double sparkY = spark.prevPosY + (spark.posY - spark.prevPosY) * (double)tickDelta - cameraPos.y;
                double sparkZ = spark.prevPosZ + (spark.posZ - spark.prevPosZ) * (double)tickDelta - cameraPos.z;
                matrixStack.pushPose();
                matrixStack.translate(sparkX, sparkY, sparkZ);
                matrixStack.mulPose(cameraRot);
                float sparkSize = 0.02f;
                matrixStack.scale(sparkSize, sparkSize, sparkSize);
                Matrix4f sm = matrixStack.last().pose();
                float sparkAlpha = partAlpha * (1.0f - (float)spark.timePC());
                sparkBuffer.addVertex(sm, -0.5f, -0.5f, 0.0f).setColor(r, g, b, sparkAlpha);
                sparkBuffer.addVertex(sm, 0.5f, -0.5f, 0.0f).setColor(r, g, b, sparkAlpha);
                sparkBuffer.addVertex(sm, 0.5f, 0.5f, 0.0f).setColor(r, g, b, sparkAlpha);
                sparkBuffer.addVertex(sm, -0.5f, 0.5f, 0.0f).setColor(r, g, b, sparkAlpha);
                matrixStack.popPose();
                hasSparks = true;
            }
        }
        if (hasSparks) {
            BufferRenderer.drawWithGlobalProgram((MeshData)sparkBuffer.buildOrThrow());
        }
        for (FirePart part : this.partList) {
            if (part.trailParts.size() < 2) continue;
            partAlpha = part.getAlphaPC();
            double dist = cameraPos.distanceTo(part.posVec);
            float width = 1.0E-5f + 8.0f * Mth.clamp((float)(1.0f - ((float)dist - 3.0f) / 20.0f), (float)0.0f, (float)1.0f);
            RenderSystem.lineWidth((float)width);
            BufferBuilder lineBuffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            matrixStack.pushPose();
            matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            Matrix4f lm = matrixStack.last().pose();
            for (int i = 0; i < part.trailParts.size(); ++i) {
                TrailPart trail = part.trailParts.get(i);
                float sizePC = (float)i / (float)part.trailParts.size();
                if (sizePC > 0.5f) {
                    sizePC = 1.0f - sizePC;
                }
                float trailAlpha = partAlpha * (sizePC *= 2.0f);
                lineBuffer.addVertex(lm, (float)trail.x, (float)trail.y, (float)trail.z).setColor(r, g, b, trailAlpha);
            }
            BufferRenderer.drawWithGlobalProgram((MeshData)lineBuffer.buildOrThrow());
            matrixStack.popPose();
        }
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.setShaderTexture((int)0, (Identifier)ICON_TEXTURE);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder textureBuffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        boolean hasTexture = false;
        for (FirePart part : this.partList) {
            float partAlpha2 = part.getAlphaPC();
            double x = part.prevPos.x + (part.posVec.x - part.prevPos.x) * (double)tickDelta - cameraPos.x;
            double y = part.prevPos.y + (part.posVec.y - part.prevPos.y) * (double)tickDelta - cameraPos.y;
            double z = part.prevPos.z + (part.posVec.z - part.prevPos.z) * (double)tickDelta - cameraPos.z;
            matrixStack.pushPose();
            matrixStack.translate(x, y, z);
            matrixStack.mulPose(cameraRot);
            float scale = 0.08f;
            matrixStack.scale(scale, scale, scale);
            Matrix4f m = matrixStack.last().pose();
            textureBuffer.addVertex(m, -0.5f, -0.5f, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, partAlpha2);
            textureBuffer.addVertex(m, 0.5f, -0.5f, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, partAlpha2);
            textureBuffer.addVertex(m, 0.5f, 0.5f, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, partAlpha2);
            textureBuffer.addVertex(m, -0.5f, 0.5f, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, partAlpha2);
            if (this.lighting.get()) {
                matrixStack.scale(3.0f, 3.0f, 3.0f);
                Matrix4f mGlow = matrixStack.last().pose();
                float glowR = r * 0.4f;
                float glowG = g * 0.4f;
                float glowB = b * 0.4f;
                float glowAlpha = partAlpha2 / 5.0f;
                textureBuffer.addVertex(mGlow, -0.5f, -0.5f, 0.0f).setUv(0.0f, 1.0f).setColor(glowR, glowG, glowB, glowAlpha);
                textureBuffer.addVertex(mGlow, 0.5f, -0.5f, 0.0f).setUv(1.0f, 1.0f).setColor(glowR, glowG, glowB, glowAlpha);
                textureBuffer.addVertex(mGlow, 0.5f, 0.5f, 0.0f).setUv(1.0f, 0.0f).setColor(glowR, glowG, glowB, glowAlpha);
                textureBuffer.addVertex(mGlow, -0.5f, 0.5f, 0.0f).setUv(0.0f, 0.0f).setColor(glowR, glowG, glowB, glowAlpha);
            }
            matrixStack.popPose();
            hasTexture = true;
        }
        if (hasTexture) {
            BufferRenderer.drawWithGlobalProgram((MeshData)textureBuffer.buildOrThrow());
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Environment(value=EnvType.CLIENT)
    private static class FirePart {
        Vec3 posVec;
        Vec3 prevPos;
        final List<TrailPart> trailParts = new ArrayList<TrailPart>();
        final List<SparkPart> sparkParts = new ArrayList<SparkPart>();
        float anim = 0.0f;
        float animTo = 1.0f;
        float animSpeed = 0.02f;
        int msChangeSideRate;
        float moveYawSet;
        float speed;
        float yMotion;
        float moveYaw;
        float maxAlive;
        long startTime;
        long rateTimer;
        boolean toRemove = false;

        public FirePart(Vec3 posVec, float maxAlive) {
            this.posVec = posVec;
            this.prevPos = posVec;
            this.maxAlive = maxAlive;
            this.moveYawSet = FireFliesModule.getRandom(0.0, 360.0);
            this.speed = FireFliesModule.getRandom(0.1, 0.25);
            this.yMotion = FireFliesModule.getRandom(-0.075, 0.1);
            this.moveYaw = this.moveYawSet;
            this.msChangeSideRate = this.calculateMsChangeSideRate();
            this.startTime = System.currentTimeMillis();
            this.rateTimer = System.currentTimeMillis();
        }

        public float getTimePC() {
            return Mth.clamp((float)((float)(System.currentTimeMillis() - this.startTime) / this.maxAlive), (float)0.0f, (float)1.0f);
        }

        public void setAlphaPCTo(float to) {
            this.animTo = to;
        }

        public float getAlphaPC() {
            return this.anim;
        }

        public void updatePart(Minecraft mc) {
            AABB box;
            Iterable collisions;
            this.anim += (this.animTo - this.anim) * this.animSpeed;
            this.anim = Mth.clamp((float)this.anim, (float)0.0f, (float)1.0f);
            if (System.currentTimeMillis() - this.rateTimer >= (long)this.msChangeSideRate) {
                this.msChangeSideRate = this.calculateMsChangeSideRate();
                this.rateTimer = System.currentTimeMillis();
                this.moveYawSet = FireFliesModule.getRandom(0.0, 360.0);
            }
            this.moveYaw = FireFliesModule.lerp(this.moveYaw, this.moveYawSet, 0.065f);
            this.speed /= 1.005f;
            float motionX = -((float)Math.sin(Math.toRadians(this.moveYaw))) * this.speed;
            float motionZ = (float)Math.cos(Math.toRadians(this.moveYaw)) * this.speed;
            this.prevPos = this.posVec;
            double scaleBox = 0.1;
            boolean collides = false;
            if (mc.level != null && (collisions = mc.level.getBlockCollisions(null, box = new AABB(this.posVec.x - scaleBox / 2.0, this.posVec.y, this.posVec.z - scaleBox / 2.0, this.posVec.x + scaleBox / 2.0, this.posVec.y + scaleBox, this.posVec.z + scaleBox / 2.0))).iterator().hasNext()) {
                collides = true;
            }
            float delente = collides ? 0.3f : 1.0f;
            this.yMotion /= 1.02f;
            this.posVec = this.posVec.add((double)(motionX / delente), (double)(this.yMotion / delente), (double)(motionZ / delente));
            if (this.getTimePC() >= 1.0f) {
                this.setAlphaPCTo(0.0f);
                if (this.getAlphaPC() < 0.003921569f) {
                    this.setToRemove();
                }
            }
            this.trailParts.add(new TrailPart(this, 400));
            this.trailParts.removeIf(TrailPart::toRemove);
            for (int i = 0; i < 2; ++i) {
                this.sparkParts.add(new SparkPart(this, 300));
            }
            for (SparkPart spark : this.sparkParts) {
                spark.motionSparkProcess();
            }
            this.sparkParts.removeIf(SparkPart::toRemove);
        }

        public void setToRemove() {
            this.toRemove = true;
        }

        private int calculateMsChangeSideRate() {
            return (int)FireFliesModule.getRandom(300.5, 900.5);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class SparkPart {
        double posX;
        double posY;
        double posZ;
        double prevPosX;
        double prevPosY;
        double prevPosZ;
        double speed;
        double radianYaw;
        double radianPitch;
        long startTime;
        int maxTime;

        public SparkPart(FirePart part, int maxTime) {
            this.posX = part.posVec.x;
            this.posY = part.posVec.y;
            this.posZ = part.posVec.z;
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.speed = Math.random() / 30.0;
            this.radianYaw = Math.random() * 360.0;
            this.radianPitch = -90.0 + Math.random() * 180.0;
            this.startTime = System.currentTimeMillis();
            this.maxTime = maxTime;
        }

        public double timePC() {
            return Mth.clamp((float)((float)(System.currentTimeMillis() - this.startTime) / (float)this.maxTime), (float)0.0f, (float)1.0f);
        }

        public boolean toRemove() {
            return this.timePC() == 1.0;
        }

        public void motionSparkProcess() {
            double radYaw = Math.toRadians(this.radianYaw);
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.posX += Math.sin(radYaw) * this.speed;
            this.posY += Math.cos(Math.toRadians(this.radianPitch - 90.0)) * this.speed;
            this.posZ += Math.cos(radYaw) * this.speed;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class TrailPart {
        double x;
        double y;
        double z;
        long startTime;
        int maxTime;

        public TrailPart(FirePart part, int maxTime) {
            this.x = part.posVec.x;
            this.y = part.posVec.y;
            this.z = part.posVec.z;
            this.startTime = System.currentTimeMillis();
            this.maxTime = maxTime;
        }

        public float getTimePC() {
            return Mth.clamp((float)((float)(System.currentTimeMillis() - this.startTime) / (float)this.maxTime), (float)0.0f, (float)1.0f);
        }

        public boolean toRemove() {
            return this.getTimePC() == 1.0f;
        }
    }
}

