package xyz.angames.astolfoclient.client.module.modules.render;

import com.mojang.renderpearl.backend.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
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
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class DashTrailModule
extends Module {
    public static DashTrailModule INSTANCE;
    private static final Identifier DASH_BLOOM;
    private static final int MAX_CUBICS = 1000;
    private final Minecraft mc = Minecraft.getInstance();
    public final BooleanSetting firstPerson = new BooleanSetting("First Person", false);
    public final ModeSetting colorMode = new ModeSetting("Color", "Client", "Client", "Rainbow");
    public final BooleanSetting motionsSmoothing = new BooleanSetting("Motion Smoothing", false);
    public final BooleanSetting dashDots = new BooleanSetting("Sparks", true);
    public final BooleanSetting lighting = new BooleanSetting("Lighting", true);
    public final NumberSetting dashLength = new NumberSetting("Length", 0.75, 0.5, 2.0, 0.05);
    private final List<Identifier> dashCubicTextures = new ArrayList<Identifier>();
    private final List<List<Identifier>> dashCubicAnimatedTextures = new ArrayList<List<Identifier>>();
    private final List<DashCubic> dashCubics = new ArrayList<DashCubic>();
    private final Random random = new Random(1234567891L);
    private Vec3 prevPlayerPos = null;

    public DashTrailModule() {
        super("DashTrail", "Dash trail behind player", Module.Category.RENDER);
        INSTANCE = this;
        this.addSettings(this.firstPerson, this.colorMode, this.motionsSmoothing, this.dashDots, this.lighting, this.dashLength);
        this.loadTextures();
        LevelRenderEvents.END_MAIN.register(this::onRender3D);
    }

    private void loadTextures() {
        for (int i = 1; i <= 21; ++i) {
            this.dashCubicTextures.add(Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)("textures/effects/dashtrail/dashcubics/dashcubic" + i + ".png")));
        }
        int[] groupCounts = new int[]{11, 23, 32, 16, 32};
        for (int g = 0; g < groupCounts.length; ++g) {
            ArrayList<Identifier> group = new ArrayList<Identifier>();
            for (int f = 1; f <= groupCounts[g]; ++f) {
                group.add(Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)("textures/effects/dashtrail/dashcubics/group_dashs/group" + (g + 1) + "/dashcubic" + f + ".png")));
            }
            this.dashCubicAnimatedTextures.add(group);
        }
    }

    private int getColorDashCubic() {
        return switch (this.colorMode.get()) {
            case "Rainbow" -> Color.getHSBColor((float)(System.currentTimeMillis() % 1000L) / 1000.0f, 0.8f, 1.0f).getRGB();
            case "Client" -> ThemeManager.getThemedColor(0L);
            default -> ThemeManager.getThemedColor(0L);
        };
    }

    private static int swapAlpha(int color, float alpha) {
        return color & 0xFFFFFF | Mth.clamp((int)((int)alpha), (int)0, (int)255) << 24;
    }

    private static int toDark(int color, float factor) {
        int a = color >> 24 & 0xFF;
        return (int)((float)(color >> 16 & 0xFF) / 255.0f * factor * 255.0f) << 16 | (int)((float)(color >> 8 & 0xFF) / 255.0f * factor * 255.0f) << 8 | (int)((float)(color & 0xFF) / 255.0f * factor * 255.0f) | a << 24;
    }

    private static int getOverallColorFrom(int c1, int c2, float f) {
        f = Mth.clamp((float)f, (float)0.0f, (float)1.0f);
        int r = (int)((float)(c1 >> 16 & 0xFF) + (float)((c2 >> 16 & 0xFF) - (c1 >> 16 & 0xFF)) * f);
        int g = (int)((float)(c1 >> 8 & 0xFF) + (float)((c2 >> 8 & 0xFF) - (c1 >> 8 & 0xFF)) * f);
        int b = (int)((float)(c1 & 0xFF) + (float)((c2 & 0xFF) - (c1 & 0xFF)) * f);
        int a = (int)((float)(c1 >> 24 & 0xFF) + (float)((c2 >> 24 & 0xFF) - (c1 >> 24 & 0xFF)) * f);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static float easeInOutQuadWave(float t) {
        return t < 0.5f ? 2.0f * t * t : 1.0f - (-2.0f * t + 2.0f) * (-2.0f * t + 2.0f) / 2.0f;
    }

    private static void addVertex(BufferBuilder bb, Matrix4f matrix, float x, float y, float z, float u, float v, int color) {
        bb.addVertex(matrix, x, y, z).setUv(u, v).setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >> 24 & 0xFF);
    }

    private static void bufferEnd(BufferBuilder bb) {
        BufferRenderer.drawWithGlobalProgram((MeshData)bb.buildOrThrow());
    }

    @Override
    public void onTick() {
        if (!this.isEnabled() || this.mc.level == null || this.mc.player == null) {
            return;
        }
        for (int i = this.dashCubics.size() - 1; i >= 0; --i) {
            DashCubic c = this.dashCubics.get(i);
            if (c.getTimePC() >= 1.0f && c.alphaTarget != 0.0f) {
                c.alphaTarget = 0.0f;
            }
            if (!(c.getTimePC() >= 1.0f) || c.alphaTarget != 0.0f || !(c.alphaValue < 0.02f)) continue;
            this.dashCubics.remove(i);
        }
        int size = this.dashCubics.size();
        for (int i = 0; i < size; ++i) {
            DashCubic current = this.dashCubics.get(i);
            DashCubic next = this.motionsSmoothing.get() && i + 1 < size ? this.dashCubics.get(i + 1) : null;
            current.motionCubicProcess(next);
        }
        LocalPlayer player = this.mc.player;
        Vec3 currentPos = player.position();
        if (this.prevPlayerPos != null) {
            double dx = currentPos.x - this.prevPlayerPos.x;
            double dy = currentPos.y - this.prevPlayerPos.y;
            double dz = currentPos.z - this.prevPlayerPos.z;
            double entitySpeed = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double entitySpeedXZ = Math.sqrt(dx * dx + dz * dz);
            if (entitySpeedXZ >= 0.05) {
                int countMax = entitySpeed > 1.5 ? 4 : 2;
                boolean[] dashPops = this.getDashPops();
                for (int count = 0; count < countMax; ++count) {
                    this.dashCubics.add(new DashCubic(new DashBase(this, (LivingEntity)player, 0.04f, new DashTexture(this, true), (float)count / (float)countMax, this.getRandomTimeAnimationPerTime()), dashPops[0] || dashPops[1]));
                    if (this.dashCubics.size() <= 1000) continue;
                    this.dashCubics.remove(0);
                }
            }
        }
        this.prevPlayerPos = currentPos;
    }

    private boolean[] getDashPops() {
        return new boolean[]{false, this.dashDots.get()};
    }

    private int getRandomTimeAnimationPerTime() {
        return (int)((float)(550 + this.random.nextInt(300)) * this.dashLength.getFloat());
    }

    private boolean hasChancedAnimatedTextureSet() {
        return this.random.nextInt(100) > 40;
    }

    public void onRender3D(LevelRenderContext event) {
        double dz;
        double dy;
        double dx;
        if (!this.isEnabled() || this.dashCubics.isEmpty()) {
            return;
        }
        if (!this.firstPerson.get() && this.mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float lightingPC = this.lighting.get() ? 1.0f : 0.0f;
        Camera camera = event.gameRenderer().mainCamera();
        Vec3 cam = camera.position();
        double camX = cam.x;
        double camY = cam.y;
        double camZ = cam.z;
        PoseStack matrices = event.poseStack();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ZERO);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
        for (DashCubic cubic : this.dashCubics) {
            if (cubic.alphaValue <= 0.05f || (dx = cubic.getRenderPosX(tickDelta) - camX) * dx + (dy = cubic.getRenderPosY(tickDelta) - camY) * dy + (dz = cubic.getRenderPosZ(tickDelta) - camZ) * dz > 2500.0) continue;
            cubic.drawDash(matrices, tickDelta, false, 1.0f, lightingPC, camX, camY, camZ, camera);
        }
        for (DashCubic cubic : this.dashCubics) {
            if (cubic.alphaValue <= 0.05f || (dx = cubic.getRenderPosX(tickDelta) - camX) * dx + (dy = cubic.getRenderPosY(tickDelta) - camY) * dy + (dz = cubic.getRenderPosZ(tickDelta) - camZ) * dz > 2500.0) continue;
            cubic.drawDash(matrices, tickDelta, true, 1.0f, lightingPC, camX, camY, camZ, camera);
        }
        if (this.dashDots.get()) {
            RenderSystem.setShaderTexture((int)0, (Identifier)DASH_BLOOM);
            BufferBuilder bb = Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            boolean drew = false;
            for (DashCubic cubic : this.dashCubics) {
                if (cubic.alphaValue <= 0.05f || cubic.sparks.isEmpty()) continue;
                float aPC = cubic.alphaValue;
                for (DashSpark spark : cubic.sparks) {
                    float sparkAPC = DashTrailModule.easeInOutQuadWave(Mth.clamp((float)((float)spark.alphaPC() * aPC), (float)0.0f, (float)1.0f));
                    if (sparkAPC <= 0.01f) continue;
                    int c = DashTrailModule.getOverallColorFrom(cubic.color, DashTrailModule.swapAlpha(-1, cubic.color >> 24 & 0xFF), 1.0f - sparkAPC);
                    c = DashTrailModule.swapAlpha(c, (float)(c >> 24 & 0xFF) * sparkAPC / 3.0f);
                    double rx = spark.getRenderPosX(tickDelta) + cubic.getRenderPosX(tickDelta) - camX;
                    double ry = spark.getRenderPosY(tickDelta) + cubic.getRenderPosY(tickDelta) - camY;
                    double rz = spark.getRenderPosZ(tickDelta) + cubic.getRenderPosZ(tickDelta) - camZ;
                    matrices.pushPose();
                    matrices.translate(rx, ry, rz);
                    matrices.rotate(camera.rotation());
                    float sz = 0.06f * sparkAPC;
                    Matrix4f matrix = matrices.last().pose();
                    DashTrailModule.addVertex(bb, matrix, -sz, -sz, 0.0f, 0.0f, 1.0f, c);
                    DashTrailModule.addVertex(bb, matrix, sz, -sz, 0.0f, 1.0f, 1.0f, c);
                    DashTrailModule.addVertex(bb, matrix, sz, sz, 0.0f, 1.0f, 0.0f, c);
                    DashTrailModule.addVertex(bb, matrix, -sz, sz, 0.0f, 0.0f, 0.0f, c);
                    drew = true;
                    matrices.popPose();
                }
            }
            if (drew) {
                DashTrailModule.bufferEnd(bb);
            }
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Override
    public void onDisable() {
        this.dashCubics.clear();
        this.prevPlayerPos = null;
        super.onDisable();
    }

    static {
        DASH_BLOOM = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/effects/dashtrail/dashbloom.png");
    }

    @Environment(value=EnvType.CLIENT)
    private class DashCubic {
        private float alphaValue = 1.0f;
        private float alphaTarget = 1.0f;
        private final long startTime = System.currentTimeMillis();
        private final DashBase base;
        private final int color = DashTrailModule.this.getColorDashCubic();
        private final List<DashSpark> sparks = new ArrayList<DashSpark>();
        private final boolean addDops;

        private DashCubic(DashBase base, boolean addDops) {
            this.base = base;
            this.addDops = addDops;
        }

        private float getTimePC() {
            return Mth.clamp((float)((float)(System.currentTimeMillis() - this.startTime) / (float)this.base.rMTime), (float)0.0f, (float)1.0f);
        }

        private double getRenderPosX(float tickDelta) {
            return this.base.prevPosX + (this.base.posX - this.base.prevPosX) * (double)tickDelta;
        }

        private double getRenderPosY(float tickDelta) {
            return this.base.prevPosY + (this.base.posY - this.base.prevPosY) * (double)tickDelta;
        }

        private double getRenderPosZ(float tickDelta) {
            return this.base.prevPosZ + (this.base.posZ - this.base.prevPosZ) * (double)tickDelta;
        }

        private void motionCubicProcess(DashCubic nextCubic) {
            float speed = 0.035f;
            this.alphaValue += (this.alphaTarget - this.alphaValue) * speed * 10.0f;
            if (Math.abs(this.alphaValue - this.alphaTarget) < 0.01f) {
                this.alphaValue = this.alphaTarget;
            }
            this.base.prevPosX = this.base.posX;
            this.base.prevPosY = this.base.posY;
            this.base.prevPosZ = this.base.posZ;
            this.base.motionX = (nextCubic != null ? nextCubic.base.motionX : this.base.motionX) / 1.05;
            this.base.posX += 5.0 * this.base.motionX;
            this.base.motionY = (nextCubic != null ? nextCubic.base.motionY : this.base.motionY) / 1.05;
            this.base.posY = this.base.posY + 5.0 * this.base.motionY / (double)(this.base.motionY < 0.0 ? 1.0f : 3.5f);
            this.base.motionZ = (nextCubic != null ? nextCubic.base.motionZ : this.base.motionZ) / 1.05;
            this.base.posZ += 5.0 * this.base.motionZ;
            if (this.addDops) {
                if (this.getTimePC() < 0.3f && DashTrailModule.this.random.nextInt(12) > 5) {
                    this.sparks.add(new DashSpark(DashTrailModule.this));
                }
                this.sparks.forEach(DashSpark::motionSparkProcess);
            }
            this.sparks.removeIf(DashSpark::toRemove);
        }

        private void drawDash(PoseStack stack, float tickDelta, boolean isBloom, float alphaPC, float lightingPC, double camX, double camY, double camZ, Camera camera) {
            Identifier texId;
            Identifier identifier = texId = isBloom ? DASH_BLOOM : this.base.dashTexture.getResource();
            if (texId == null) {
                return;
            }
            float aPC = this.alphaValue * alphaPC;
            if (aPC < 0.01f) {
                return;
            }
            double rx = this.getRenderPosX(tickDelta) - camX;
            double ry = this.getRenderPosY(tickDelta) - camY;
            double rz = this.getRenderPosZ(tickDelta) - camZ;
            if (isBloom) {
                float scale = 0.033f * aPC;
                float extXY = 64.0f * scale;
                float timePcOf = Math.max(0.0f, 1.0f - this.getTimePC());
                stack.pushPose();
                stack.translate(rx, ry, rz);
                stack.rotate(camera.rotation());
                int color1 = DashTrailModule.getOverallColorFrom(this.color, -1, 0.15f);
                int bloomColor = DashTrailModule.swapAlpha(color1, 55.0f * aPC);
                RenderSystem.setShaderTexture((int)0, (Identifier)DASH_BLOOM);
                BufferBuilder bb = Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                Matrix4f matrix = stack.last().pose();
                float s = extXY / 1.75f * 0.1f;
                DashTrailModule.addVertex(bb, matrix, -s, -s, 0.0f, 0.0f, 1.0f, bloomColor);
                DashTrailModule.addVertex(bb, matrix, s, -s, 0.0f, 1.0f, 1.0f, bloomColor);
                DashTrailModule.addVertex(bb, matrix, s, s, 0.0f, 1.0f, 0.0f, bloomColor);
                DashTrailModule.addVertex(bb, matrix, -s, s, 0.0f, 0.0f, 0.0f, bloomColor);
                DashTrailModule.bufferEnd(bb);
                if (lightingPC != 0.0f) {
                    float aMul = aPC * lightingPC;
                    float s2 = (extXY *= 1.0f + 6.0f * timePcOf * aMul) / 2.0f * 0.1f;
                    int glowColor = DashTrailModule.swapAlpha(DashTrailModule.toDark(color1, aMul / 4.0f), 90.0f * aMul);
                    bb = Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                    DashTrailModule.addVertex(bb, matrix, -s2, -s2, 0.0f, 0.0f, 1.0f, glowColor);
                    DashTrailModule.addVertex(bb, matrix, s2, -s2, 0.0f, 1.0f, 1.0f, glowColor);
                    DashTrailModule.addVertex(bb, matrix, s2, s2, 0.0f, 1.0f, 0.0f, glowColor);
                    DashTrailModule.addVertex(bb, matrix, -s2, s2, 0.0f, 0.0f, 0.0f, glowColor);
                    DashTrailModule.bufferEnd(bb);
                }
                stack.popPose();
            } else {
                float scale = 0.033f * aPC;
                float extX = 64.0f * scale * 0.1f;
                float extY = 64.0f * scale * 0.1f;
                float halfX = extX / 2.0f;
                float halfY = extY / 2.0f;
                stack.pushPose();
                stack.translate(rx, ry, rz);
                stack.rotate(camera.rotation());
                int mainColor = DashTrailModule.toDark(DashTrailModule.getOverallColorFrom(this.color, -1, 0.4f), aPC);
                RenderSystem.setShaderTexture((int)0, (Identifier)texId);
                BufferBuilder bb = Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                Matrix4f matrix = stack.last().pose();
                DashTrailModule.addVertex(bb, matrix, -halfX, -halfY, 0.0f, 0.0f, 1.0f, mainColor);
                DashTrailModule.addVertex(bb, matrix, halfX, -halfY, 0.0f, 1.0f, 1.0f, mainColor);
                DashTrailModule.addVertex(bb, matrix, halfX, halfY, 0.0f, 1.0f, 0.0f, mainColor);
                DashTrailModule.addVertex(bb, matrix, -halfX, halfY, 0.0f, 0.0f, 0.0f, mainColor);
                DashTrailModule.bufferEnd(bb);
                stack.popPose();
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class DashBase {
        private final LivingEntity entity;
        private double motionX;
        private double motionY;
        private double motionZ;
        private double posX;
        private double posY;
        private double posZ;
        private double prevPosX;
        private double prevPosY;
        private double prevPosZ;
        private final int rMTime;
        private final DashTexture dashTexture;

        private DashBase(DashTrailModule dashTrailModule, LivingEntity entity, float speedDash, DashTexture dashTexture, float offsetTickPC, int rmTime) {
            this.rMTime = rmTime;
            this.entity = entity;
            this.motionX = entity.getX() - entity.xo;
            this.motionY = entity.getY() - entity.yo;
            this.motionZ = entity.getZ() - entity.zo;
            this.posX = entity.xo - this.motionX * (double)offsetTickPC + -0.0875 + 0.175 * Math.random();
            this.posY = entity.yo - this.motionY * (double)offsetTickPC + (double)(entity.getBbHeight() / 3.0f) + (double)(entity.getBbHeight() / 4.0f) * Math.random() * 0.7;
            this.posZ = entity.zo - this.motionZ * (double)offsetTickPC + -0.0875 + 0.175 * Math.random();
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.motionX *= (double)speedDash;
            this.motionY *= (double)speedDash;
            this.motionZ *= (double)speedDash;
            this.dashTexture = dashTexture;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class DashTexture {
        private final List<Identifier> textures;
        private final boolean animated;
        private final long timeAfterSpawn;
        private final long animationPerTime;

        private DashTexture(DashTrailModule dashTrailModule, boolean animated) {
            boolean isAnimated;
            this.animated = isAnimated = animated && dashTrailModule.hasChancedAnimatedTextureSet();
            if (isAnimated) {
                this.timeAfterSpawn = System.currentTimeMillis();
                this.textures = new ArrayList<Identifier>((Collection)dashTrailModule.dashCubicAnimatedTextures.get(dashTrailModule.random.nextInt(dashTrailModule.dashCubicAnimatedTextures.size())));
                this.animationPerTime = dashTrailModule.getRandomTimeAnimationPerTime();
            } else {
                this.textures = new ArrayList<Identifier>();
                this.textures.add(dashTrailModule.dashCubicTextures.get(dashTrailModule.random.nextInt(dashTrailModule.dashCubicTextures.size())));
                this.timeAfterSpawn = 0L;
                this.animationPerTime = 0L;
            }
        }

        private Identifier getResource() {
            if (this.animated && !this.textures.isEmpty()) {
                float timePC = (float)((System.currentTimeMillis() - this.timeAfterSpawn) % this.animationPerTime) / (float)this.animationPerTime;
                int fragNumber = Mth.clamp((int)((int)(timePC * (float)this.textures.size())), (int)0, (int)(this.textures.size() - 1));
                return this.textures.get(fragNumber);
            }
            return this.textures.isEmpty() ? null : this.textures.get(0);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private class DashSpark {
        double posX;
        double posY;
        double posZ;
        double prevPosX;
        double prevPosY;
        double prevPosZ;
        double speed = Math.random() / 50.0;
        double radianYaw = Math.random() * 360.0;
        double radianPitch = -90.0 + Math.random() * 180.0;
        long startTime = System.currentTimeMillis();

        DashSpark(DashTrailModule dashTrailModule) {
        }

        double timePC() {
            return Mth.clamp((float)((float)(System.currentTimeMillis() - this.startTime) / 1000.0f), (float)0.0f, (float)1.0f);
        }

        double alphaPC() {
            return 1.0 - this.timePC();
        }

        boolean toRemove() {
            return this.timePC() >= 1.0;
        }

        void motionSparkProcess() {
            double radYaw = Math.toRadians(this.radianYaw);
            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;
            this.posX += Math.sin(radYaw) * this.speed;
            this.posY += Math.cos(Math.toRadians(this.radianPitch - 90.0)) * this.speed;
            this.posZ += Math.cos(radYaw) * this.speed;
        }

        double getRenderPosX(float tickDelta) {
            return this.prevPosX + (this.posX - this.prevPosX) * (double)tickDelta;
        }

        double getRenderPosY(float tickDelta) {
            return this.prevPosY + (this.posY - this.prevPosY) * (double)tickDelta;
        }

        double getRenderPosZ(float tickDelta) {
            return this.prevPosZ + (this.posZ - this.prevPosZ) * (double)tickDelta;
        }
    }
}

