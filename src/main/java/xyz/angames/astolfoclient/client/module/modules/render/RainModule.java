package xyz.angames.astolfoclient.client.module.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.render.WetSurfaceRenderer;

@Environment(value=EnvType.CLIENT)
public class RainModule
extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    public final ModeSetting preset = new ModeSetting("Preset", "Rain", "Drizzle", "Rain", "Downpour", "Storm");
    public final NumberSetting density = new NumberSetting("Density", 1.0, 0.1, 3.0, 0.1);
    public final NumberSetting radius = new NumberSetting("Radius", 26.0, 8.0, 64.0, 1.0);
    public final NumberSetting altitude = new NumberSetting("Altitude", 16.0, 6.0, 40.0, 1.0);
    public final NumberSetting dropSize = new NumberSetting("Drop Size", 1.0, 0.3, 2.5, 0.1);
    public final NumberSetting fallSpeed = new NumberSetting("Fall Speed", 1.0, 0.3, 2.5, 0.1);
    public final NumberSetting wind = new NumberSetting("Wind", 1.0, 0.0, 3.0, 0.1);
    public final NumberSetting opacity = new NumberSetting("Opacity", 1.0, 0.1, 2.0, 0.05);
    public final BooleanSetting splashes = new BooleanSetting("Splashes", true);
    public final BooleanSetting droplets = new BooleanSetting("Splash Droplets", true){

        @Override
        public boolean isVisible() {
            return RainModule.this.splashes.get();
        }
    };
    public final BooleanSetting groundMist = new BooleanSetting("Ground Mist", true);
    public final BooleanSetting lightning = new BooleanSetting("Lightning", false);
    public final BooleanSetting skyCheck = new BooleanSetting("Sky Check", true);
    public final BooleanSetting onlyWhenRaining = new BooleanSetting("Only When Raining", false);
    public final ModeSetting colorMode = new ModeSetting("Color Mode", "Theme", "Theme", "Realistic", "Custom");
    public final NumberSetting customRed = new NumberSetting("Custom Red", 191.0, 0.0, 255.0, 1.0){

        @Override
        public boolean isVisible() {
            return RainModule.this.colorMode.is("Custom");
        }
    };
    public final NumberSetting customGreen = new NumberSetting("Custom Green", 216.0, 0.0, 255.0, 1.0){

        @Override
        public boolean isVisible() {
            return RainModule.this.colorMode.is("Custom");
        }
    };
    public final NumberSetting customBlue = new NumberSetting("Custom Blue", 230.0, 0.0, 255.0, 1.0){

        @Override
        public boolean isVisible() {
            return RainModule.this.colorMode.is("Custom");
        }
    };
    public final BooleanSetting wetGround = new BooleanSetting("Wet Ground", true);
    public final NumberSetting wetness = new NumberSetting("Wetness", 1.25, 0.1, 2.0, 0.05){

        @Override
        public boolean isVisible() {
            return RainModule.this.wetGround.get();
        }
    };
    public final NumberSetting puddleCoverage = new NumberSetting("Puddle Coverage", 0.9, 0.05, 1.0, 0.05){

        @Override
        public boolean isVisible() {
            return RainModule.this.wetGround.get();
        }
    };
    public final NumberSetting reflectionStrength = new NumberSetting("Reflections", 1.55, 0.1, 2.0, 0.05){

        @Override
        public boolean isVisible() {
            return RainModule.this.wetGround.get();
        }
    };
    public final NumberSetting reflectionDistance = new NumberSetting("Reflection Distance", 56.0, 12.0, 96.0, 1.0){

        @Override
        public boolean isVisible() {
            return RainModule.this.wetGround.get();
        }
    };
    public final ModeSetting reflectionQuality = new ModeSetting("Reflection Quality", "Balanced", new String[]{"Performance", "Balanced", "High"}){

        @Override
        public boolean isVisible() {
            return RainModule.this.wetGround.get();
        }
    };
    public final BooleanSetting puddleRipples = new BooleanSetting("Puddle Ripples", true){

        @Override
        public boolean isVisible() {
            return RainModule.this.wetGround.get();
        }
    };
    public final NumberSetting rippleStrength = new NumberSetting("Ripple Strength", 0.8, 0.0, 1.6, 0.05){

        @Override
        public boolean isVisible() {
            return RainModule.this.wetGround.get() && RainModule.this.puddleRipples.get();
        }
    };
    private static final double GRAVITY = 22.0;
    private static final double TERMINAL_VELOCITY = 34.0;
    private static final int RIPPLE_SEGMENTS = 16;
    private static final int MAX_DROPS = 4500;
    private static final int MAX_MIST = 100;
    private final List<Drop> drops = new ArrayList<Drop>();
    private final List<Splash> splashList = new ArrayList<Splash>();
    private final List<Droplet> dropletList = new ArrayList<Droplet>();
    private final List<Mist> mistList = new ArrayList<Mist>();
    private final Map<Long, Integer> groundCache = new HashMap<Long, Integer>();
    private final BlockPos.Mutable scratchPos = new BlockPos.Mutable();
    private final Random random = new Random();
    private long lastFrameNanos;
    private float clock;
    private float flash;
    private float nextFlashIn = 9.0f;
    private int cacheTicks;

    public static RainModule getInstance() {
        return (RainModule)ModuleManager.getModule(RainModule.class);
    }

    public RainModule() {
        super("Rain", "Realistic volumetric rain with wet ground & reflections", Module.Category.RENDER);
        this.addSettings(this.preset, this.density, this.radius, this.altitude, this.dropSize, this.fallSpeed, this.wind, this.opacity, this.splashes, this.droplets, this.groundMist, this.wetGround, this.wetness, this.puddleCoverage, this.reflectionStrength, this.reflectionDistance, this.reflectionQuality, this.puddleRipples, this.rippleStrength, this.lightning, this.skyCheck, this.onlyWhenRaining, this.colorMode, this.customRed, this.customGreen, this.customBlue);
        WorldRenderEvents.END_MAIN.register(this::onWorldRender);
    }

    @Override
    public void onEnable() {
        this.lastFrameNanos = 0L;
        WetSurfaceRenderer.getInstance().reset();
        this.clearAll();
    }

    @Override
    public void onDisable() {
        WetSurfaceRenderer.getInstance().reset();
        this.clearAll();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void clearAll() {
        List<Drop> list = this.drops;
        synchronized (list) {
            this.drops.clear();
        }
        this.splashList.clear();
        this.dropletList.clear();
        this.mistList.clear();
        this.groundCache.clear();
        this.flash = 0.0f;
    }

    @Override
    public void onTick() {
        if (this.mc.player == null || this.mc.world == null) {
            this.clearAll();
            return;
        }
        if (++this.cacheTicks > 30) {
            this.cacheTicks = 0;
            this.groundCache.clear();
        }
    }

    private float presetDensity() {
        if (this.preset.is("Drizzle")) {
            return 0.45f;
        }
        if (this.preset.is("Downpour")) {
            return 1.9f;
        }
        if (this.preset.is("Storm")) {
            return 2.8f;
        }
        return 1.0f;
    }

    private float presetWind() {
        if (this.preset.is("Drizzle")) {
            return 0.5f;
        }
        if (this.preset.is("Downpour")) {
            return 1.35f;
        }
        if (this.preset.is("Storm")) {
            return 2.4f;
        }
        return 1.0f;
    }

    private float presetMass() {
        if (this.preset.is("Drizzle")) {
            return 0.55f;
        }
        if (this.preset.is("Downpour")) {
            return 1.25f;
        }
        if (this.preset.is("Storm")) {
            return 1.5f;
        }
        return 1.0f;
    }

    private int targetDropCount() {
        float r = (float)this.radius.get();
        float area = r * r * 0.0155f;
        int count = (int)(area * (float)this.density.get() * this.presetDensity() * 26.0f);
        return MathHelper.clamp((int)count, (int)40, (int)4500);
    }

    private float[] resolveColor() {
        int color;
        if (this.colorMode.is("Custom")) {
            int r = (int)MathHelper.clamp((float)((float)this.customRed.get()), (float)0.0f, (float)255.0f);
            int g = (int)MathHelper.clamp((float)((float)this.customGreen.get()), (float)0.0f, (float)255.0f);
            int b = (int)MathHelper.clamp((float)((float)this.customBlue.get()), (float)0.0f, (float)255.0f);
            color = r << 16 | g << 8 | b;
        } else {
            color = this.colorMode.is("Theme") ? ThemeManager.getThemedColor(0L) : -3613464;
        }
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        return new float[]{r, g, b};
    }

    private float random(float min, float max) {
        return min + this.random.nextFloat() * (max - min);
    }

    private int surfaceY(int x, int z) {
        int top;
        long key = (long)x << 32 ^ (long)z & 0xFFFFFFFFL;
        Integer cached = this.groundCache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            top = this.mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
        }
        catch (Throwable ignored) {
            top = this.mc.world.getBottomY();
        }
        if (this.groundCache.size() < 20000) {
            this.groundCache.put(key, top);
        }
        return top;
    }

    private boolean isBlocking(double x, double y, double z) {
        if (this.mc.world == null) {
            return false;
        }
        this.scratchPos.set(MathHelper.floor((double)x), MathHelper.floor((double)y), MathHelper.floor((double)z));
        if (!this.mc.world.getChunkManager().isChunkLoaded(this.scratchPos.getX() >> 4, this.scratchPos.getZ() >> 4)) {
            return false;
        }
        BlockState state = this.mc.world.getBlockState((BlockPos)this.scratchPos);
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        if (state.isAir()) {
            return false;
        }
        try {
            return !state.getCollisionShape((BlockView)this.mc.world, (BlockPos)this.scratchPos).isEmpty();
        }
        catch (Throwable ignored) {
            return true;
        }
    }

    private boolean isWater(double x, double y, double z) {
        if (this.mc.world == null) {
            return false;
        }
        this.scratchPos.set(MathHelper.floor((double)x), MathHelper.floor((double)y), MathHelper.floor((double)z));
        try {
            return !this.mc.world.getBlockState((BlockPos)this.scratchPos).getFluidState().isEmpty();
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private boolean skyVisible(double x, double y, double z) {
        if (!this.skyCheck.get() || this.mc.world == null) {
            return true;
        }
        this.scratchPos.set(MathHelper.floor((double)x), MathHelper.floor((double)y), MathHelper.floor((double)z));
        try {
            return this.mc.world.isSkyVisible((BlockPos)this.scratchPos);
        }
        catch (Throwable ignored) {
            return true;
        }
    }

    private void spawnDrop(Vec3d center, boolean fromTop) {
        double y;
        float r = (float)this.radius.get();
        double angle = this.random.nextDouble() * Math.PI * 2.0;
        double dist = Math.sqrt(this.random.nextDouble()) * (double)r;
        double x = center.x + Math.cos(angle) * dist;
        double z = center.z + Math.sin(angle) * dist;
        double d = y = fromTop ? center.y + (double)((float)this.altitude.get() * this.random(0.75f, 1.0f)) : center.y + (double)this.random(-4.0f, (float)this.altitude.get());
        if (!this.skyVisible(x, y, z)) {
            return;
        }
        Drop drop = new Drop();
        drop.x = x;
        drop.y = y;
        drop.z = z;
        drop.mass = this.random(0.6f, 1.4f) * this.presetMass();
        drop.vy = (double)(-this.random(6.0f, 11.0f)) * drop.mass;
        drop.phase = this.random(0.0f, 62.8f);
        drop.brightness = this.random(0.6f, 1.0f);
        drop.ground = this.surfaceY(MathHelper.floor((double)x), MathHelper.floor((double)z));
        this.drops.add(drop);
    }

    private void updateDrops(Vec3d center, float dt) {
        int target = this.targetDropCount();
        this.drops.removeIf(d -> {
            double dx = d.x - center.x;
            double dz = d.z - center.z;
            float r = (float)this.radius.get() + 6.0f;
            return d.dead || dx * dx + dz * dz > (double)(r * r) || d.y < center.y - (double)((float)this.altitude.get()) - 12.0;
        });
        boolean fill = this.drops.size() < target / 2;
        int spawnBudget = fill ? Math.min(target - this.drops.size(), 900) : Math.min(target - this.drops.size(), 260);
        for (int i = 0; i < spawnBudget; ++i) {
            this.spawnDrop(center, !fill);
        }
        float speedMul = (float)this.fallSpeed.get();
        float windMul = (float)this.wind.get() * this.presetWind();
        int size = this.drops.size();
        for (int i = 0; i < size; ++i) {
            Drop d2 = this.drops.get(i);
            double gust = 0.65 + 0.35 * Math.sin((double)this.clock * 0.55 + (double)d2.phase * 0.15) + 0.18 * Math.sin((double)this.clock * 1.7 + (double)d2.phase);
            double windX = (double)windMul * 3.1 * gust;
            double windZ = (double)windMul * 1.7 * Math.sin((double)this.clock * 0.31 + (double)d2.phase * 0.05) * gust;
            double drag = 1.0 - Math.min(0.92, Math.abs(d2.vy) / (34.0 * d2.mass));
            d2.vy -= 22.0 * d2.mass * drag * (double)dt;
            if (d2.vy < -34.0 * d2.mass) {
                d2.vy = -34.0 * d2.mass;
            }
            d2.vx += (windX - d2.vx) * Math.min(1.0, (double)dt * 3.2);
            d2.vz += (windZ - d2.vz) * Math.min(1.0, (double)dt * 3.2);
            double nx = d2.x + d2.vx * (double)dt * (double)speedMul;
            double ny = d2.y + d2.vy * (double)dt * (double)speedMul;
            double nz = d2.z + d2.vz * (double)dt * (double)speedMul;
            d2.lastX = d2.x;
            d2.lastY = d2.y;
            d2.lastZ = d2.z;
            boolean hit = false;
            if (ny <= d2.ground + 2.5) {
                if (this.isBlocking(nx, ny, nz)) {
                    hit = true;
                } else if (ny <= d2.ground - 3.0) {
                    d2.dead = true;
                }
            }
            if (hit) {
                if (this.splashes.get()) {
                    this.addSplash(nx, ny, nz, d2);
                }
                d2.dead = true;
                continue;
            }
            d2.x = nx;
            d2.y = ny;
            d2.z = nz;
        }
    }

    private void addSplash(double x, double y, double z, Drop d) {
        if (this.splashList.size() > 700) {
            return;
        }
        double impactY = Math.floor(y) + 1.0005;
        Splash splash = new Splash();
        splash.x = x;
        splash.y = impactY;
        splash.z = z;
        splash.life = 0.0f;
        splash.maxLife = this.random(0.3f, 0.52f);
        splash.scale = (float)(0.32 + 0.22 * d.mass);
        splash.water = this.isWater(x, y - 0.15, z);
        this.splashList.add(splash);
        if (!this.droplets.get() || this.dropletList.size() > 1400) {
            return;
        }
        int shards = splash.water ? 4 : 3;
        for (int i = 0; i < shards; ++i) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double sp = (double)this.random(1.1f, 2.6f) * d.mass;
            Droplet drop = new Droplet();
            drop.x = x;
            drop.y = impactY + 0.02;
            drop.z = z;
            drop.vx = Math.cos(a) * sp * 0.45 + d.vx * 0.08;
            drop.vz = Math.sin(a) * sp * 0.45 + d.vz * 0.08;
            drop.vy = this.random(1.9f, 4.1f);
            drop.maxLife = this.random(0.28f, 0.5f);
            this.dropletList.add(drop);
        }
    }

    private void updateDroplets(float dt) {
        for (int i = this.dropletList.size() - 1; i >= 0; --i) {
            Droplet dr = this.dropletList.get(i);
            dr.life += dt;
            if (dr.life >= dr.maxLife) {
                this.dropletList.remove(i);
                continue;
            }
            dr.lastX = dr.x;
            dr.lastY = dr.y;
            dr.lastZ = dr.z;
            dr.vy -= 12.100000000000001 * (double)dt;
            dr.x += dr.vx * (double)dt;
            dr.y += dr.vy * (double)dt;
            dr.z += dr.vz * (double)dt;
        }
    }

    private void updateSplashes(float dt) {
        for (int i = this.splashList.size() - 1; i >= 0; --i) {
            Splash s = this.splashList.get(i);
            s.life += dt;
            if (!(s.life >= s.maxLife)) continue;
            this.splashList.remove(i);
        }
    }

    private void updateMist(Vec3d center, float dt) {
        if (!this.groundMist.get()) {
            this.mistList.clear();
            return;
        }
        int target = (int)MathHelper.clamp((float)(22.0f * (float)this.density.get() * this.presetDensity()), (float)6.0f, (float)100.0f);
        this.mistList.removeIf(m -> {
            m.life += dt;
            double dx = m.x - center.x;
            double dz = m.z - center.z;
            float r = (float)this.radius.get();
            m.x += m.vx * (double)dt;
            m.z += m.vz * (double)dt;
            return m.life > m.maxLife || dx * dx + dz * dz > (double)(r * r);
        });
        while (this.mistList.size() < target) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(this.random.nextDouble()) * (double)((float)this.radius.get()) * 0.85;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            int ground = this.surfaceY(MathHelper.floor((double)x), MathHelper.floor((double)z));
            Mist m2 = new Mist();
            m2.x = x;
            m2.z = z;
            m2.y = (float)ground + this.random(0.05f, 0.9f);
            m2.size = this.random(2.2f, 5.6f);
            m2.maxLife = this.random(2.4f, 5.5f);
            m2.life = this.random(0.0f, 1.0f);
            m2.vx = this.random(-0.35f, 0.35f) * (float)this.wind.get();
            m2.vz = this.random(-0.35f, 0.35f) * (float)this.wind.get();
            this.mistList.add(m2);
        }
    }

    private void updateLightning(float dt) {
        if (!this.lightning.get()) {
            this.flash = 0.0f;
            return;
        }
        this.flash = Math.max(0.0f, this.flash - dt * 3.4f);
        this.nextFlashIn -= dt;
        if (this.nextFlashIn <= 0.0f) {
            this.flash = this.random(0.65f, 1.25f);
            this.nextFlashIn = this.random(6.0f, 16.0f);
        }
    }

    public void renderWetSurfaceFrame(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix, float tickDelta) {
        if (!this.isEnabled() || !this.wetGround.get() || this.mc.player == null || this.mc.world == null) {
            return;
        }
        if (this.onlyWhenRaining.get() && !this.mc.world.isRaining()) {
            return;
        }
        float[] rgb = this.resolveColor();
        WetSurfaceRenderer.Parameters parameters = new WetSurfaceRenderer.Parameters();
        parameters.wetness = (float)this.wetness.get();
        parameters.puddleCoverage = (float)this.puddleCoverage.get();
        parameters.reflectionStrength = (float)this.reflectionStrength.get();
        parameters.maxDistance = (float)this.reflectionDistance.get();
        parameters.rippleStrength = (float)this.rippleStrength.get();
        parameters.rainAmount = MathHelper.clamp((float)(0.55f + (float)this.density.get() * this.presetDensity() * 0.28f), (float)0.55f, (float)1.45f);
        parameters.ripples = this.puddleRipples.get();
        parameters.reflectionSteps = this.reflectionQuality.is("High") ? 16 : (this.reflectionQuality.is("Performance") ? 6 : 10);
        parameters.themeR = rgb[0];
        parameters.themeG = rgb[1];
        parameters.themeB = rgb[2];
        parameters.useThemeColor = this.colorMode.is("Theme") || this.colorMode.is("Custom");
        float skyAngle = (float)((this.mc.world.getTimeOfDay()%24000L)/24000.0);
        float radians = skyAngle * ((float)Math.PI * 2);
        parameters.sunDirX = -((float)Math.sin(radians));
        parameters.sunDirY = (float)Math.cos(radians);
        parameters.sunDirZ = 0.0f;
        parameters.ambientLight = this.ambientLight(camera);
        parameters.lightningFlash = this.flash;
        WetSurfaceRenderer.getInstance().apply(this.mc, camera, viewMatrix, projectionMatrix, parameters);
    }

    public void onWorldRender(WorldRenderContext context) {
        if (!this.isEnabled() || this.mc.player == null || this.mc.world == null || this.mc.gameRenderer == null) {
            return;
        }
        if (this.onlyWhenRaining.get() && !this.mc.world.isRaining()) {
            this.clearAll();
            return;
        }
        Camera camera = context.gameRenderer().getCamera();
        if (camera == null) {
            return;
        }
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
        if (this.wetGround.get()) {
            MatrixStack viewStack = new MatrixStack();
            viewStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            viewStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));
            Matrix4f viewMatrix = viewStack.peek().getPositionMatrix();
            Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
            this.renderWetSurfaceFrame(camera, viewMatrix, projectionMatrix, tickDelta);
        }
        long now = System.nanoTime();
        if (this.lastFrameNanos == 0L) {
            this.lastFrameNanos = now;
        }
        float dt = (float)((double)(now - this.lastFrameNanos) / 1.0E9);
        this.lastFrameNanos = now;
        dt = MathHelper.clamp((float)dt, (float)0.0f, (float)0.05f);
        this.clock += dt;
        Vec3d cam = camera.getCameraPos();
        this.updateDrops(cam, dt);
        this.updateSplashes(dt);
        this.updateDroplets(dt);
        this.updateMist(cam, dt);
        this.updateLightning(dt);
        if (this.drops.isEmpty() && this.splashList.isEmpty() && this.dropletList.isEmpty() && this.mistList.isEmpty()) {
            return;
        }
        float[] rgb = this.resolveColor();
        float ambient = this.ambientLight(camera);
        float brightnessMul = MathHelper.clamp((float)(ambient + this.flash * 0.95f), (float)0.0f, (float)1.75f);
        float alphaMul = (float)this.opacity.get() * brightnessMul;
        MatrixStack matrices = context.matrices();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableCull();
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (this.groundMist.get()) {
            this.drawMist(matrices, camera, cam, rgb, alphaMul);
        }
        this.drawStreaks(matrices, camera, cam, rgb, alphaMul, false);
        if (this.splashes.get()) {
            this.drawRipples(matrices, cam, rgb, alphaMul);
        }
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
        this.drawStreaks(matrices, camera, cam, rgb, alphaMul, true);
        if (this.splashes.get()) {
            this.drawCrowns(matrices, camera, cam, rgb, alphaMul);
            if (this.droplets.get()) {
                this.drawDroplets(matrices, camera, cam, rgb, alphaMul);
            }
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawStreaks(MatrixStack ms, Camera camera, Vec3d cam, float[] rgb, float alphaMul, boolean highlight) {
        if (this.drops.isEmpty()) {
            return;
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float sizeMul = (float)this.dropSize.get();
        float r = (float)this.radius.get();
        float fadeStart = r * 0.62f;
        int size = this.drops.size();
        for (int i = 0; i < size; ++i) {
            float distanceFade;
            Drop d = this.drops.get(i);
            double dx = d.x - cam.x;
            double dy = d.y - cam.y;
            double dz = d.z - cam.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            double dist = Math.sqrt(distSq);
            if (dist > (double)r + 4.0) continue;
            float f = distanceFade = dist <= (double)fadeStart ? 1.0f : 1.0f - MathHelper.clamp((float)((float)((dist - (double)fadeStart) / (double)(r - fadeStart + 1.0f))), (float)0.0f, (float)1.0f);
            if (distanceFade <= 0.02f) continue;
            float nearFade = dist < 0.7 ? (float)(dist / 0.7) : 1.0f;
            double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
            if (speed < 1.0E-4) continue;
            float length = (float)MathHelper.clamp((double)(speed * 0.055 * (double)sizeMul), (double)0.16, (double)2.6);
            float width = (float)(0.014 + 0.019 * d.mass) * sizeMul * (highlight ? 0.45f : 1.0f);
            float alpha = alphaMul * d.brightness * distanceFade * nearFade * (highlight ? 0.35f : 0.45f);
            if (alpha <= 0.006f) continue;
            alpha = Math.min(alpha, 0.95f);
            float yawRad = (float)Math.toRadians(camera.getYaw());
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);
            double horizontal = d.vx * rightX + d.vz * rightZ;
            float tilt = (float)Math.toDegrees(Math.atan2(horizontal, Math.abs(d.vy) + 1.0E-4));
            tilt = MathHelper.clamp((float)tilt, (float)-55.0f, (float)55.0f);
            ms.push();
            ms.translate(dx, dy, dz);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - camera.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-tilt));
            Matrix4f m = ms.peek().getPositionMatrix();
            float half = width * 0.5f;
            float top = length * 0.5f;
            float bottom = -length * 0.5f;
            float headAlpha = alpha;
            float tailAlpha = 0.0f;
            float headWidth = half;
            float tailWidth = half * 0.35f;
            buffer.vertex(m, -tailWidth, top, 0.0f).color(rgb[0], rgb[1], rgb[2], tailAlpha);
            buffer.vertex(m, tailWidth, top, 0.0f).color(rgb[0], rgb[1], rgb[2], tailAlpha);
            buffer.vertex(m, headWidth, bottom, 0.0f).color(rgb[0], rgb[1], rgb[2], headAlpha);
            buffer.vertex(m, -headWidth, bottom, 0.0f).color(rgb[0], rgb[1], rgb[2], headAlpha);
            if (!highlight) {
                float halo = half * 2.6f;
                float haloAlpha = alpha * 0.24f;
                buffer.vertex(m, -halo * 0.5f, top, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
                buffer.vertex(m, halo * 0.5f, top, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
                buffer.vertex(m, halo, bottom, 0.0f).color(rgb[0], rgb[1], rgb[2], haloAlpha);
                buffer.vertex(m, -halo, bottom, 0.0f).color(rgb[0], rgb[1], rgb[2], haloAlpha);
            } else {
                float lensTop = bottom + length * 0.34f;
                float lensWidth = half * 1.25f;
                buffer.vertex(m, -lensWidth, lensTop, 0.0f).color(1.0f, 1.0f, 1.0f, 0.0f);
                buffer.vertex(m, lensWidth, lensTop, 0.0f).color(1.0f, 1.0f, 1.0f, 0.0f);
                buffer.vertex(m, lensWidth, bottom, 0.0f).color(1.0f, 1.0f, 1.0f, alpha * 0.9f);
                buffer.vertex(m, -lensWidth, bottom, 0.0f).color(1.0f, 1.0f, 1.0f, alpha * 0.9f);
            }
            ms.pop();
        }
        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)built);
        }
    }

    private void drawRipples(MatrixStack ms, Vec3d cam, float[] rgb, float alphaMul) {
        if (this.splashList.isEmpty()) {
            return;
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int size = this.splashList.size();
        for (int i = 0; i < size; ++i) {
            Splash s = this.splashList.get(i);
            float progress = MathHelper.clamp((float)(s.life / s.maxLife), (float)0.0f, (float)1.0f);
            float eased = 1.0f - (1.0f - progress) * (1.0f - progress);
            float outer = s.scale * (0.18f + eased * (s.water ? 1.35f : 0.85f));
            float inner = outer * (0.55f + 0.35f * eased);
            float alpha = alphaMul * (1.0f - progress) * (s.water ? 0.55f : 0.42f);
            if (alpha <= 0.01f) continue;
            double dx = s.x - cam.x;
            double dy = s.y - cam.y + 0.015;
            double dz = s.z - cam.z;
            ms.push();
            ms.translate(dx, dy, dz);
            Matrix4f m = ms.peek().getPositionMatrix();
            for (int seg = 0; seg < 16; ++seg) {
                double a0 = 0.39269908169872414 * (double)seg;
                double a1 = 0.39269908169872414 * (double)(seg + 1);
                float ix0 = (float)(Math.cos(a0) * (double)inner);
                float iz0 = (float)(Math.sin(a0) * (double)inner);
                float ix1 = (float)(Math.cos(a1) * (double)inner);
                float iz1 = (float)(Math.sin(a1) * (double)inner);
                float ox0 = (float)(Math.cos(a0) * (double)outer);
                float oz0 = (float)(Math.sin(a0) * (double)outer);
                float ox1 = (float)(Math.cos(a1) * (double)outer);
                float oz1 = (float)(Math.sin(a1) * (double)outer);
                buffer.vertex(m, ix0, 0.0f, iz0).color(rgb[0], rgb[1], rgb[2], alpha);
                buffer.vertex(m, ix1, 0.0f, iz1).color(rgb[0], rgb[1], rgb[2], alpha);
                buffer.vertex(m, ox1, 0.0f, oz1).color(rgb[0], rgb[1], rgb[2], 0.0f);
                buffer.vertex(m, ox0, 0.0f, oz0).color(rgb[0], rgb[1], rgb[2], 0.0f);
            }
            ms.pop();
        }
        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)built);
        }
    }

    private void drawCrowns(MatrixStack ms, Camera camera, Vec3d cam, float[] rgb, float alphaMul) {
        if (this.splashList.isEmpty()) {
            return;
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int size = this.splashList.size();
        for (int i = 0; i < size; ++i) {
            Splash s = this.splashList.get(i);
            float progress = MathHelper.clamp((float)(s.life / s.maxLife), (float)0.0f, (float)1.0f);
            if (progress > 0.55f) continue;
            float local = progress / 0.55f;
            float height = s.scale * (0.55f * (float)Math.sin((double)local * Math.PI));
            float alpha = alphaMul * (1.0f - local) * 0.35f;
            if (alpha <= 0.01f || height <= 0.005f) continue;
            ms.push();
            ms.translate(s.x - cam.x, s.y - cam.y + 0.02, s.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - camera.getYaw()));
            Matrix4f m = ms.peek().getPositionMatrix();
            float base = s.scale * 0.16f;
            buffer.vertex(m, -base * 0.25f, height, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, base * 0.25f, height, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, base, 0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);
            buffer.vertex(m, -base, 0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);
            ms.pop();
        }
        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)built);
        }
    }

    private void drawDroplets(MatrixStack ms, Camera camera, Vec3d cam, float[] rgb, float alphaMul) {
        if (this.dropletList.isEmpty()) {
            return;
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float sizeMul = (float)this.dropSize.get();
        int size = this.dropletList.size();
        for (int i = 0; i < size; ++i) {
            Droplet d = this.dropletList.get(i);
            float progress = MathHelper.clamp((float)(d.life / d.maxLife), (float)0.0f, (float)1.0f);
            float alpha = alphaMul * (1.0f - progress) * 0.55f;
            if (alpha <= 0.01f) continue;
            double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
            float length = (float)MathHelper.clamp((double)(speed * 0.03 * (double)sizeMul), (double)0.04, (double)0.4);
            float width = 0.016f * sizeMul;
            float yawRad = (float)Math.toRadians(camera.getYaw());
            double horizontal = d.vx * Math.cos(yawRad) + d.vz * Math.sin(yawRad);
            float tilt = (float)Math.toDegrees(Math.atan2(horizontal, -d.vy + 1.0E-4));
            ms.push();
            ms.translate(d.x - cam.x, d.y - cam.y, d.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - camera.getYaw()));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-tilt));
            Matrix4f m = ms.peek().getPositionMatrix();
            buffer.vertex(m, -width * 0.4f, length * 0.5f, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, width * 0.4f, length * 0.5f, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, width, -length * 0.5f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);
            buffer.vertex(m, -width, -length * 0.5f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);
            ms.pop();
        }
        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)built);
        }
    }

    private void drawMist(MatrixStack ms, Camera camera, Vec3d cam, float[] rgb, float alphaMul) {
        if (this.mistList.isEmpty()) {
            return;
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int size = this.mistList.size();
        for (int i = 0; i < size; ++i) {
            Mist mist = this.mistList.get(i);
            float progress = MathHelper.clamp((float)(mist.life / mist.maxLife), (float)0.0f, (float)1.0f);
            float fade = (float)Math.sin((double)progress * Math.PI);
            float alpha = alphaMul * fade * 0.055f;
            if (alpha <= 0.004f) continue;
            ms.push();
            ms.translate(mist.x - cam.x, mist.y - cam.y, mist.z - cam.z);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - camera.getYaw()));
            Matrix4f m = ms.peek().getPositionMatrix();
            float half = mist.size * 0.5f;
            float top = mist.size * 0.42f;
            buffer.vertex(m, -half, top, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, half, top, 0.0f).color(rgb[0], rgb[1], rgb[2], 0.0f);
            buffer.vertex(m, half, 0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);
            buffer.vertex(m, -half, 0.0f, 0.0f).color(rgb[0], rgb[1], rgb[2], alpha);
            ms.pop();
        }
        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)built);
        }
    }

    private float ambientLight(Camera camera) {
        try {
            int light = this.mc.world.getLightLevel(camera.getBlockPos());
            return MathHelper.clamp((float)((float)light / 15.0f), (float)0.3f, (float)1.0f);
        }
        catch (Throwable ignored) {
            return 1.0f;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static final class Drop {
        double x;
        double y;
        double z;
        double lastX;
        double lastY;
        double lastZ;
        double vx;
        double vy;
        double vz;
        double mass = 1.0;
        double ground;
        float phase;
        float brightness = 1.0f;
        boolean dead;

        private Drop() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static final class Splash {
        double x;
        double y;
        double z;
        float life;
        float maxLife = 0.4f;
        float scale = 0.4f;
        boolean water;

        private Splash() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static final class Droplet {
        double x;
        double y;
        double z;
        double lastX;
        double lastY;
        double lastZ;
        double vx;
        double vy;
        double vz;
        float life;
        float maxLife = 0.4f;

        private Droplet() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static final class Mist {
        double x;
        double y;
        double z;
        double vx;
        double vz;
        float size = 3.0f;
        float life;
        float maxLife = 4.0f;

        private Mist() {
        }
    }
}

