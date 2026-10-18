package xyz.angames.astolfoclient.client.module.modules.render;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.render.WetSurfaceRenderer;

@Environment(EnvType.CLIENT)
public class RainModule extends Module {
   private final Minecraft mc = Minecraft.getInstance();
   public final ModeSetting preset = new ModeSetting("Preset", "Rain", "Drizzle", "Rain", "Downpour", "Storm");
   public final NumberSetting density = new NumberSetting("Density", 1.0, 0.1, 3.0, 0.1);
   public final NumberSetting radius = new NumberSetting("Radius", 26.0, 8.0, 64.0, 1.0);
   public final NumberSetting altitude = new NumberSetting("Altitude", 16.0, 6.0, 40.0, 1.0);
   public final NumberSetting dropSize = new NumberSetting("Drop Size", 1.0, 0.3, 2.5, 0.1);
   public final NumberSetting fallSpeed = new NumberSetting("Fall Speed", 1.0, 0.3, 2.5, 0.1);
   public final NumberSetting wind = new NumberSetting("Wind", 1.0, 0.0, 3.0, 0.1);
   public final NumberSetting opacity = new NumberSetting("Opacity", 1.0, 0.1, 2.0, 0.05);
   public final BooleanSetting splashes = new BooleanSetting("Splashes", true);
   public final BooleanSetting droplets = new BooleanSetting("Splash Droplets", true) {
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
   public final NumberSetting customRed = new NumberSetting("Custom Red", 191.0, 0.0, 255.0, 1.0) {
      @Override
      public boolean isVisible() {
         return RainModule.this.colorMode.is("Custom");
      }
   };
   public final NumberSetting customGreen = new NumberSetting("Custom Green", 216.0, 0.0, 255.0, 1.0) {
      @Override
      public boolean isVisible() {
         return RainModule.this.colorMode.is("Custom");
      }
   };
   public final NumberSetting customBlue = new NumberSetting("Custom Blue", 230.0, 0.0, 255.0, 1.0) {
      @Override
      public boolean isVisible() {
         return RainModule.this.colorMode.is("Custom");
      }
   };
   public final BooleanSetting wetGround = new BooleanSetting("Wet Ground", true);
   public final NumberSetting wetness = new NumberSetting("Wetness", 1.25, 0.1, 2.0, 0.05) {
      @Override
      public boolean isVisible() {
         return RainModule.this.wetGround.get();
      }
   };
   public final NumberSetting puddleCoverage = new NumberSetting("Puddle Coverage", 0.9, 0.05, 1.0, 0.05) {
      @Override
      public boolean isVisible() {
         return RainModule.this.wetGround.get();
      }
   };
   public final NumberSetting reflectionStrength = new NumberSetting("Reflections", 1.55, 0.1, 2.0, 0.05) {
      @Override
      public boolean isVisible() {
         return RainModule.this.wetGround.get();
      }
   };
   public final NumberSetting reflectionDistance = new NumberSetting("Reflection Distance", 56.0, 12.0, 96.0, 1.0) {
      @Override
      public boolean isVisible() {
         return RainModule.this.wetGround.get();
      }
   };
   public final ModeSetting reflectionQuality = new ModeSetting("Reflection Quality", "Balanced", "Performance", "Balanced", "High") {
      @Override
      public boolean isVisible() {
         return RainModule.this.wetGround.get();
      }
   };
   public final BooleanSetting puddleRipples = new BooleanSetting("Puddle Ripples", true) {
      @Override
      public boolean isVisible() {
         return RainModule.this.wetGround.get();
      }
   };
   public final NumberSetting rippleStrength = new NumberSetting("Ripple Strength", 0.8, 0.0, 1.6, 0.05) {
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
   private final List<RainModule.Drop> drops = new ArrayList<>();
   private final List<RainModule.Splash> splashList = new ArrayList<>();
   private final List<RainModule.Droplet> dropletList = new ArrayList<>();
   private final List<RainModule.Mist> mistList = new ArrayList<>();
   private final Map<Long, Integer> groundCache = new HashMap<>();
   private final MutableBlockPos scratchPos = new MutableBlockPos();
   private final Random random = new Random();
   private long lastFrameNanos;
   private float clock;
   private float flash;
   private float nextFlashIn = 9.0F;
   private int cacheTicks;

   public static RainModule getInstance() {
      return (RainModule)ModuleManager.getModule(RainModule.class);
   }

   public RainModule() {
      super("Rain", "Realistic volumetric rain with wet ground & reflections", Module.Category.RENDER);
      this.addSettings(
         this.preset,
         this.density,
         this.radius,
         this.altitude,
         this.dropSize,
         this.fallSpeed,
         this.wind,
         this.opacity,
         this.splashes,
         this.droplets,
         this.groundMist,
         this.wetGround,
         this.wetness,
         this.puddleCoverage,
         this.reflectionStrength,
         this.reflectionDistance,
         this.reflectionQuality,
         this.puddleRipples,
         this.rippleStrength,
         this.lightning,
         this.skyCheck,
         this.onlyWhenRaining,
         this.colorMode,
         this.customRed,
         this.customGreen,
         this.customBlue
      );
      WorldRenderEvents.LAST.register(this::onWorldRender);
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

   private void clearAll() {
      synchronized (this.drops) {
         this.drops.clear();
      }

      this.splashList.clear();
      this.dropletList.clear();
      this.mistList.clear();
      this.groundCache.clear();
      this.flash = 0.0F;
   }

   @Override
   public void onTick() {
      if (this.mc.player != null && this.mc.level != null) {
         if (++this.cacheTicks > 30) {
            this.cacheTicks = 0;
            this.groundCache.clear();
         }
      } else {
         this.clearAll();
      }
   }

   private float presetDensity() {
      if (this.preset.is("Drizzle")) {
         return 0.45F;
      } else if (this.preset.is("Downpour")) {
         return 1.9F;
      } else {
         return this.preset.is("Storm") ? 2.8F : 1.0F;
      }
   }

   private float presetWind() {
      if (this.preset.is("Drizzle")) {
         return 0.5F;
      } else if (this.preset.is("Downpour")) {
         return 1.35F;
      } else {
         return this.preset.is("Storm") ? 2.4F : 1.0F;
      }
   }

   private float presetMass() {
      if (this.preset.is("Drizzle")) {
         return 0.55F;
      } else if (this.preset.is("Downpour")) {
         return 1.25F;
      } else {
         return this.preset.is("Storm") ? 1.5F : 1.0F;
      }
   }

   private int targetDropCount() {
      float r = (float)this.radius.get();
      float area = r * r * 0.0155F;
      int count = (int)(area * (float)this.density.get() * this.presetDensity() * 26.0F);
      return Mth.clamp(count, 40, 4500);
   }

   private float[] resolveColor() {
      int color;
      if (this.colorMode.is("Custom")) {
         int r = (int)Mth.clamp((float)this.customRed.get(), 0.0F, 255.0F);
         int g = (int)Mth.clamp((float)this.customGreen.get(), 0.0F, 255.0F);
         int b = (int)Mth.clamp((float)this.customBlue.get(), 0.0F, 255.0F);
         color = r << 16 | g << 8 | b;
      } else if (this.colorMode.is("Theme")) {
         color = ThemeManager.getThemedColor(0L);
      } else {
         color = -3613464;
      }

      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      return new float[]{r, g, b};
   }

   private float random(float min, float max) {
      return min + this.random.nextFloat() * (max - min);
   }

   private int surfaceY(int x, int z) {
      long key = (long)x << 32 ^ z & 4294967295L;
      Integer cached = this.groundCache.get(key);
      if (cached != null) {
         return cached;
      }

      int top;
      try {
         top = this.mc.level.getHeight(Types.WORLD_SURFACE, x, z);
      } catch (Throwable ignored) {
         top = this.mc.level.getMinY();
      }

      if (this.groundCache.size() < 20000) {
         this.groundCache.put(key, top);
      }

      return top;
   }

   private boolean isBlocking(double x, double y, double z) {
      if (this.mc.level == null) {
         return false;
      }

      this.scratchPos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
      if (!this.mc.level.getChunkSource().hasChunk(this.scratchPos.getX() >> 4, this.scratchPos.getZ() >> 4)) {
         return false;
      }

      BlockState state = this.mc.level.getBlockState(this.scratchPos);
      if (!state.getFluidState().isEmpty()) {
         return true;
      }

      if (state.isAir()) {
         return false;
      }

      try {
         return !state.getCollisionShape(this.mc.level, this.scratchPos).isEmpty();
      } catch (Throwable ignored) {
         return true;
      }
   }

   private boolean isWater(double x, double y, double z) {
      if (this.mc.level == null) {
         return false;
      }

      this.scratchPos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));

      try {
         return !this.mc.level.getBlockState(this.scratchPos).getFluidState().isEmpty();
      } catch (Throwable ignored) {
         return false;
      }
   }

   private boolean skyVisible(double x, double y, double z) {
      if (this.skyCheck.get() && this.mc.level != null) {
         this.scratchPos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));

         try {
            return this.mc.level.canSeeSky(this.scratchPos);
         } catch (Throwable ignored) {
            return true;
         }
      } else {
         return true;
      }
   }

   private void spawnDrop(Vec3 center, boolean fromTop) {
      float r = (float)this.radius.get();
      double angle = this.random.nextDouble() * Math.PI * 2.0;
      double dist = Math.sqrt(this.random.nextDouble()) * r;
      double x = center.x + Math.cos(angle) * dist;
      double z = center.z + Math.sin(angle) * dist;
      double y = fromTop
         ? center.y + (float)this.altitude.get() * this.random(0.75F, 1.0F)
         : center.y + this.random(-4.0F, (float)this.altitude.get());
      if (this.skyVisible(x, y, z)) {
         RainModule.Drop drop = new RainModule.Drop();
         drop.x = x;
         drop.y = y;
         drop.z = z;
         drop.mass = this.random(0.6F, 1.4F) * this.presetMass();
         drop.vy = -this.random(6.0F, 11.0F) * drop.mass;
         drop.phase = this.random(0.0F, 62.8F);
         drop.brightness = this.random(0.6F, 1.0F);
         drop.ground = this.surfaceY(Mth.floor(x), Mth.floor(z));
         this.drops.add(drop);
      }
   }

   private void updateDrops(Vec3 center, float dt) {
      int target = this.targetDropCount();
      this.drops.removeIf(dx -> {
         double dxx = dx.x - center.x;
         double dz = dx.z - center.z;
         float r = (float)this.radius.get() + 6.0F;
         return dx.dead || dxx * dxx + dz * dz > r * r || dx.y < center.y - (float)this.altitude.get() - 12.0;
      });
      boolean fill = this.drops.size() < target / 2;
      int spawnBudget = fill ? Math.min(target - this.drops.size(), 900) : Math.min(target - this.drops.size(), 260);

      for (int i = 0; i < spawnBudget; i++) {
         this.spawnDrop(center, !fill);
      }

      float speedMul = (float)this.fallSpeed.get();
      float windMul = (float)this.wind.get() * this.presetWind();
      int i = 0;

      for (int size = this.drops.size(); i < size; i++) {
         RainModule.Drop d = this.drops.get(i);
         double gust = 0.65 + 0.35 * Math.sin(this.clock * 0.55 + d.phase * 0.15) + 0.18 * Math.sin(this.clock * 1.7 + d.phase);
         double windX = windMul * 3.1 * gust;
         double windZ = windMul * 1.7 * Math.sin(this.clock * 0.31 + d.phase * 0.05) * gust;
         double drag = 1.0 - Math.min(0.92, Math.abs(d.vy) / (34.0 * d.mass));
         d.vy = d.vy - 22.0 * d.mass * drag * dt;
         if (d.vy < -34.0 * d.mass) {
            d.vy = -34.0 * d.mass;
         }

         d.vx = d.vx + (windX - d.vx) * Math.min(1.0, dt * 3.2);
         d.vz = d.vz + (windZ - d.vz) * Math.min(1.0, dt * 3.2);
         double nx = d.x + d.vx * dt * speedMul;
         double ny = d.y + d.vy * dt * speedMul;
         double nz = d.z + d.vz * dt * speedMul;
         d.prevX = d.x;
         d.prevY = d.y;
         d.prevZ = d.z;
         boolean hit = false;
         if (ny <= d.ground + 2.5) {
            if (this.isBlocking(nx, ny, nz)) {
               hit = true;
            } else if (ny <= d.ground - 3.0) {
               d.dead = true;
            }
         }

         if (hit) {
            if (this.splashes.get()) {
               this.addSplash(nx, ny, nz, d);
            }

            d.dead = true;
         } else {
            d.x = nx;
            d.y = ny;
            d.z = nz;
         }
      }
   }

   private void addSplash(double x, double y, double z, RainModule.Drop d) {
      if (this.splashList.size() <= 700) {
         double impactY = Math.floor(y) + 1.0005;
         RainModule.Splash splash = new RainModule.Splash();
         splash.x = x;
         splash.y = impactY;
         splash.z = z;
         splash.life = 0.0F;
         splash.maxLife = this.random(0.3F, 0.52F);
         splash.scale = (float)(0.32 + 0.22 * d.mass);
         splash.water = this.isWater(x, y - 0.15, z);
         this.splashList.add(splash);
         if (this.droplets.get() && this.dropletList.size() <= 1400) {
            int shards = splash.water ? 4 : 3;

            for (int i = 0; i < shards; i++) {
               double a = this.random.nextDouble() * Math.PI * 2.0;
               double sp = this.random(1.1F, 2.6F) * d.mass;
               RainModule.Droplet drop = new RainModule.Droplet();
               drop.x = x;
               drop.y = impactY + 0.02;
               drop.z = z;
               drop.vx = Math.cos(a) * sp * 0.45 + d.vx * 0.08;
               drop.vz = Math.sin(a) * sp * 0.45 + d.vz * 0.08;
               drop.vy = this.random(1.9F, 4.1F);
               drop.maxLife = this.random(0.28F, 0.5F);
               this.dropletList.add(drop);
            }
         }
      }
   }

   private void updateDroplets(float dt) {
      for (int i = this.dropletList.size() - 1; i >= 0; i--) {
         RainModule.Droplet dr = this.dropletList.get(i);
         dr.life += dt;
         if (dr.life >= dr.maxLife) {
            this.dropletList.remove(i);
         } else {
            dr.prevX = dr.x;
            dr.prevY = dr.y;
            dr.prevZ = dr.z;
            dr.vy -= 12.100000000000001 * dt;
            dr.x = dr.x + dr.vx * dt;
            dr.y = dr.y + dr.vy * dt;
            dr.z = dr.z + dr.vz * dt;
         }
      }
   }

   private void updateSplashes(float dt) {
      for (int i = this.splashList.size() - 1; i >= 0; i--) {
         RainModule.Splash s = this.splashList.get(i);
         s.life += dt;
         if (s.life >= s.maxLife) {
            this.splashList.remove(i);
         }
      }
   }

   private void updateMist(Vec3 center, float dt) {
      if (!this.groundMist.get()) {
         this.mistList.clear();
      } else {
         int target = (int)Mth.clamp(22.0F * (float)this.density.get() * this.presetDensity(), 6.0F, 100.0F);
         this.mistList.removeIf(mx -> {
            mx.life += dt;
            double dx = mx.x - center.x;
            double dz = mx.z - center.z;
            float r = (float)this.radius.get();
            mx.x = mx.x + mx.vx * dt;
            mx.z = mx.z + mx.vz * dt;
            return mx.life > mx.maxLife || dx * dx + dz * dz > r * r;
         });

         while (this.mistList.size() < target) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(this.random.nextDouble()) * (float)this.radius.get() * 0.85;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            int ground = this.surfaceY(Mth.floor(x), Mth.floor(z));
            RainModule.Mist m = new RainModule.Mist();
            m.x = x;
            m.z = z;
            m.y = ground + this.random(0.05F, 0.9F);
            m.size = this.random(2.2F, 5.6F);
            m.maxLife = this.random(2.4F, 5.5F);
            m.life = this.random(0.0F, 1.0F);
            m.vx = this.random(-0.35F, 0.35F) * (float)this.wind.get();
            m.vz = this.random(-0.35F, 0.35F) * (float)this.wind.get();
            this.mistList.add(m);
         }
      }
   }

   private void updateLightning(float dt) {
      if (!this.lightning.get()) {
         this.flash = 0.0F;
      } else {
         this.flash = Math.max(0.0F, this.flash - dt * 3.4F);
         this.nextFlashIn -= dt;
         if (this.nextFlashIn <= 0.0F) {
            this.flash = this.random(0.65F, 1.25F);
            this.nextFlashIn = this.random(6.0F, 16.0F);
         }
      }
   }

   public void renderWetSurfaceFrame(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix, float tickDelta) {
      if (this.isEnabled() && this.wetGround.get() && this.mc.player != null && this.mc.level != null) {
         if (!this.onlyWhenRaining.get() || this.mc.level.isRaining()) {
            float[] rgb = this.resolveColor();
            WetSurfaceRenderer.Parameters parameters = new WetSurfaceRenderer.Parameters();
            parameters.wetness = (float)this.wetness.get();
            parameters.puddleCoverage = (float)this.puddleCoverage.get();
            parameters.reflectionStrength = (float)this.reflectionStrength.get();
            parameters.maxDistance = (float)this.reflectionDistance.get();
            parameters.rippleStrength = (float)this.rippleStrength.get();
            parameters.rainAmount = Mth.clamp(0.55F + (float)this.density.get() * this.presetDensity() * 0.28F, 0.55F, 1.45F);
            parameters.ripples = this.puddleRipples.get();
            parameters.reflectionSteps = this.reflectionQuality.is("High") ? 16 : (this.reflectionQuality.is("Performance") ? 6 : 10);
            parameters.themeR = rgb[0];
            parameters.themeG = rgb[1];
            parameters.themeB = rgb[2];
            parameters.useThemeColor = this.colorMode.is("Theme") || this.colorMode.is("Custom");
            float skyAngle = this.mc.level.environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.SUN_ANGLE, this.mc.player.blockPosition()) / 360.0F;
            float radians = skyAngle * (float) (Math.PI * 2);
            parameters.sunDirX = -((float)Math.sin(radians));
            parameters.sunDirY = (float)Math.cos(radians);
            parameters.sunDirZ = 0.0F;
            parameters.ambientLight = this.ambientLight(camera);
            parameters.lightningFlash = this.flash;
            WetSurfaceRenderer.getInstance().apply(this.mc, camera, viewMatrix, projectionMatrix, parameters);
         }
      }
   }

   public void onWorldRender(WorldRenderContext context) {
      if (this.isEnabled() && this.mc.player != null && this.mc.level != null && this.mc.gameRenderer != null) {
         if (this.onlyWhenRaining.get() && !this.mc.level.isRaining()) {
            this.clearAll();
         } else {
            Camera camera = context.camera();
            if (camera != null) {
               float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(false);
               if (this.wetGround.get()) {
                  PoseStack viewStack = new PoseStack();
                  viewStack.rotate(Axis.XP.rotationDegrees(camera.xRot()));
                  viewStack.rotate(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
                  Matrix4f viewMatrix = viewStack.last().pose();
                  Matrix4f projectionMatrix = context.projectionMatrix();
                  this.renderWetSurfaceFrame(camera, viewMatrix, projectionMatrix, tickDelta);
               }

               long now = System.nanoTime();
               if (this.lastFrameNanos == 0L) {
                  this.lastFrameNanos = now;
               }

               float dt = (float)((now - this.lastFrameNanos) / 1.0E9);
               this.lastFrameNanos = now;
               dt = Mth.clamp(dt, 0.0F, 0.05F);
               this.clock += dt;
               Vec3 cam = camera.position();
               this.updateDrops(cam, dt);
               this.updateSplashes(dt);
               this.updateDroplets(dt);
               this.updateMist(cam, dt);
               this.updateLightning(dt);
               if (!this.drops.isEmpty() || !this.splashList.isEmpty() || !this.dropletList.isEmpty() || !this.mistList.isEmpty()) {
                  float[] rgb = this.resolveColor();
                  float ambient = this.ambientLight(camera);
                  float brightnessMul = Mth.clamp(ambient + this.flash * 0.95F, 0.0F, 1.75F);
                  float alphaMul = (float)this.opacity.get() * brightnessMul;
                  PoseStack matrices = context.matrixStack();
                  RenderSystem.enableBlend();
                  RenderSystem.enableDepthTest();
                  RenderSystem.depthMask(false);
                  RenderSystem.disableCull();
                  RenderSystem.setShader(CoreShaders.POSITION_COLOR);
                  RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                  if (this.groundMist.get()) {
                     this.drawMist(matrices, camera, cam, rgb, alphaMul);
                  }

                  this.drawStreaks(matrices, camera, cam, rgb, alphaMul, false);
                  if (this.splashes.get()) {
                     this.drawRipples(matrices, cam, rgb, alphaMul);
                  }

                  RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
                  this.drawStreaks(matrices, camera, cam, rgb, alphaMul, true);
                  if (this.splashes.get()) {
                     this.drawCrowns(matrices, camera, cam, rgb, alphaMul);
                     if (this.droplets.get()) {
                        this.drawDroplets(matrices, camera, cam, rgb, alphaMul);
                     }
                  }

                  RenderSystem.depthMask(true);
                  RenderSystem.enableCull();
                  RenderSystem.defaultBlendFunc();
                  RenderSystem.disableBlend();
               }
            }
         }
      }
   }

   private void drawStreaks(PoseStack ms, Camera camera, Vec3 cam, float[] rgb, float alphaMul, boolean highlight) {
      if (!this.drops.isEmpty()) {
         BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
         float sizeMul = (float)this.dropSize.get();
         float r = (float)this.radius.get();
         float fadeStart = r * 0.62F;
         int i = 0;

         for (int size = this.drops.size(); i < size; i++) {
            RainModule.Drop d = this.drops.get(i);
            double dx = d.x - cam.x;
            double dy = d.y - cam.y;
            double dz = d.z - cam.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            double dist = Math.sqrt(distSq);
            if (!(dist > r + 4.0)) {
               float distanceFade = dist <= fadeStart ? 1.0F : 1.0F - Mth.clamp((float)((dist - fadeStart) / (r - fadeStart + 1.0F)), 0.0F, 1.0F);
               if (!(distanceFade <= 0.02F)) {
                  float nearFade = dist < 0.7 ? (float)(dist / 0.7) : 1.0F;
                  double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
                  if (!(speed < 1.0E-4)) {
                     float length = (float)Mth.clamp(speed * 0.055 * sizeMul, 0.16, 2.6);
                     float width = (float)(0.014 + 0.019 * d.mass) * sizeMul * (highlight ? 0.45F : 1.0F);
                     float alpha = alphaMul * d.brightness * distanceFade * nearFade * (highlight ? 0.35F : 0.45F);
                     if (!(alpha <= 0.006F)) {
                        alpha = Math.min(alpha, 0.95F);
                        float yawRad = (float)Math.toRadians(camera.yRot());
                        double rightX = Math.cos(yawRad);
                        double rightZ = Math.sin(yawRad);
                        double horizontal = d.vx * rightX + d.vz * rightZ;
                        float tilt = (float)Math.toDegrees(Math.atan2(horizontal, Math.abs(d.vy) + 1.0E-4));
                        tilt = Mth.clamp(tilt, -55.0F, 55.0F);
                        ms.pushPose();
                        ms.translate(dx, dy, dz);
                        ms.rotate(Axis.YP.rotationDegrees(180.0F - camera.yRot()));
                        ms.rotate(Axis.ZP.rotationDegrees(-tilt));
                        Matrix4f m = ms.last().pose();
                        float half = width * 0.5F;
                        float top = length * 0.5F;
                        float bottom = -length * 0.5F;
                        float headAlpha = alpha;
                        float tailAlpha = 0.0F;
                        float headWidth = half;
                        float tailWidth = half * 0.35F;
                        Vtx.vertex(buffer, m, -tailWidth, top, 0.0F).setColor(rgb[0], rgb[1], rgb[2], tailAlpha);
                        Vtx.vertex(buffer, m, tailWidth, top, 0.0F).setColor(rgb[0], rgb[1], rgb[2], tailAlpha);
                        Vtx.vertex(buffer, m, headWidth, bottom, 0.0F).setColor(rgb[0], rgb[1], rgb[2], headAlpha);
                        Vtx.vertex(buffer, m, -headWidth, bottom, 0.0F).setColor(rgb[0], rgb[1], rgb[2], headAlpha);
                        if (!highlight) {
                           float halo = half * 2.6F;
                           float haloAlpha = alpha * 0.24F;
                           Vtx.vertex(buffer, m, -halo * 0.5F, top, 0.0F).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
                           Vtx.vertex(buffer, m, halo * 0.5F, top, 0.0F).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
                           Vtx.vertex(buffer, m, halo, bottom, 0.0F).setColor(rgb[0], rgb[1], rgb[2], haloAlpha);
                           Vtx.vertex(buffer, m, -halo, bottom, 0.0F).setColor(rgb[0], rgb[1], rgb[2], haloAlpha);
                        } else {
                           float lensTop = bottom + length * 0.34F;
                           float lensWidth = half * 1.25F;
                           Vtx.vertex(buffer, m, -lensWidth, lensTop, 0.0F).setColor(1.0F, 1.0F, 1.0F, 0.0F);
                           Vtx.vertex(buffer, m, lensWidth, lensTop, 0.0F).setColor(1.0F, 1.0F, 1.0F, 0.0F);
                           Vtx.vertex(buffer, m, lensWidth, bottom, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha * 0.9F);
                           Vtx.vertex(buffer, m, -lensWidth, bottom, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha * 0.9F);
                        }

                        ms.popPose();
                     }
                  }
               }
            }
         }

         MeshData built = buffer.build();
         if (built != null) {
            BufferUploader.drawWithShader(built);
         }
      }
   }

   private void drawRipples(PoseStack ms, Vec3 cam, float[] rgb, float alphaMul) {
      if (!this.splashList.isEmpty()) {
         BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
         int i = 0;

         for (int size = this.splashList.size(); i < size; i++) {
            RainModule.Splash s = this.splashList.get(i);
            float progress = Mth.clamp(s.life / s.maxLife, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
            float outer = s.scale * (0.18F + eased * (s.water ? 1.35F : 0.85F));
            float inner = outer * (0.55F + 0.35F * eased);
            float alpha = alphaMul * (1.0F - progress) * (s.water ? 0.55F : 0.42F);
            if (!(alpha <= 0.01F)) {
               double dx = s.x - cam.x;
               double dy = s.y - cam.y + 0.015;
               double dz = s.z - cam.z;
               ms.pushPose();
               ms.translate(dx, dy, dz);
               Matrix4f m = ms.last().pose();

               for (int seg = 0; seg < 16; seg++) {
                  double a0 = (Math.PI / 8) * seg;
                  double a1 = (Math.PI / 8) * (seg + 1);
                  float ix0 = (float)(Math.cos(a0) * inner);
                  float iz0 = (float)(Math.sin(a0) * inner);
                  float ix1 = (float)(Math.cos(a1) * inner);
                  float iz1 = (float)(Math.sin(a1) * inner);
                  float ox0 = (float)(Math.cos(a0) * outer);
                  float oz0 = (float)(Math.sin(a0) * outer);
                  float ox1 = (float)(Math.cos(a1) * outer);
                  float oz1 = (float)(Math.sin(a1) * outer);
                  Vtx.vertex(buffer, m, ix0, 0.0F, iz0).setColor(rgb[0], rgb[1], rgb[2], alpha);
                  Vtx.vertex(buffer, m, ix1, 0.0F, iz1).setColor(rgb[0], rgb[1], rgb[2], alpha);
                  Vtx.vertex(buffer, m, ox1, 0.0F, oz1).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
                  Vtx.vertex(buffer, m, ox0, 0.0F, oz0).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
               }

               ms.popPose();
            }
         }

         MeshData built = buffer.build();
         if (built != null) {
            BufferUploader.drawWithShader(built);
         }
      }
   }

   private void drawCrowns(PoseStack ms, Camera camera, Vec3 cam, float[] rgb, float alphaMul) {
      if (!this.splashList.isEmpty()) {
         BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
         int i = 0;

         for (int size = this.splashList.size(); i < size; i++) {
            RainModule.Splash s = this.splashList.get(i);
            float progress = Mth.clamp(s.life / s.maxLife, 0.0F, 1.0F);
            if (!(progress > 0.55F)) {
               float local = progress / 0.55F;
               float height = s.scale * (0.55F * (float)Math.sin(local * Math.PI));
               float alpha = alphaMul * (1.0F - local) * 0.35F;
               if (!(alpha <= 0.01F) && !(height <= 0.005F)) {
                  ms.pushPose();
                  ms.translate(s.x - cam.x, s.y - cam.y + 0.02, s.z - cam.z);
                  ms.rotate(Axis.YP.rotationDegrees(180.0F - camera.yRot()));
                  Matrix4f m = ms.last().pose();
                  float base = s.scale * 0.16F;
                  Vtx.vertex(buffer, m, -base * 0.25F, height, 0.0F).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
                  Vtx.vertex(buffer, m, base * 0.25F, height, 0.0F).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
                  Vtx.vertex(buffer, m, base, 0.0F, 0.0F).setColor(rgb[0], rgb[1], rgb[2], alpha);
                  Vtx.vertex(buffer, m, -base, 0.0F, 0.0F).setColor(rgb[0], rgb[1], rgb[2], alpha);
                  ms.popPose();
               }
            }
         }

         MeshData built = buffer.build();
         if (built != null) {
            BufferUploader.drawWithShader(built);
         }
      }
   }

   private void drawDroplets(PoseStack ms, Camera camera, Vec3 cam, float[] rgb, float alphaMul) {
      if (!this.dropletList.isEmpty()) {
         BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
         float sizeMul = (float)this.dropSize.get();
         int i = 0;

         for (int size = this.dropletList.size(); i < size; i++) {
            RainModule.Droplet d = this.dropletList.get(i);
            float progress = Mth.clamp(d.life / d.maxLife, 0.0F, 1.0F);
            float alpha = alphaMul * (1.0F - progress) * 0.55F;
            if (!(alpha <= 0.01F)) {
               double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
               float length = (float)Mth.clamp(speed * 0.03 * sizeMul, 0.04, 0.4);
               float width = 0.016F * sizeMul;
               float yawRad = (float)Math.toRadians(camera.yRot());
               double horizontal = d.vx * Math.cos(yawRad) + d.vz * Math.sin(yawRad);
               float tilt = (float)Math.toDegrees(Math.atan2(horizontal, -d.vy + 1.0E-4));
               ms.pushPose();
               ms.translate(d.x - cam.x, d.y - cam.y, d.z - cam.z);
               ms.rotate(Axis.YP.rotationDegrees(180.0F - camera.yRot()));
               ms.rotate(Axis.ZP.rotationDegrees(-tilt));
               Matrix4f m = ms.last().pose();
               Vtx.vertex(buffer, m, -width * 0.4F, length * 0.5F, 0.0F).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
               Vtx.vertex(buffer, m, width * 0.4F, length * 0.5F, 0.0F).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
               Vtx.vertex(buffer, m, width, -length * 0.5F, 0.0F).setColor(rgb[0], rgb[1], rgb[2], alpha);
               Vtx.vertex(buffer, m, -width, -length * 0.5F, 0.0F).setColor(rgb[0], rgb[1], rgb[2], alpha);
               ms.popPose();
            }
         }

         MeshData built = buffer.build();
         if (built != null) {
            BufferUploader.drawWithShader(built);
         }
      }
   }

   private void drawMist(PoseStack ms, Camera camera, Vec3 cam, float[] rgb, float alphaMul) {
      if (!this.mistList.isEmpty()) {
         BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
         int i = 0;

         for (int size = this.mistList.size(); i < size; i++) {
            RainModule.Mist mist = this.mistList.get(i);
            float progress = Mth.clamp(mist.life / mist.maxLife, 0.0F, 1.0F);
            float fade = (float)Math.sin(progress * Math.PI);
            float alpha = alphaMul * fade * 0.055F;
            if (!(alpha <= 0.004F)) {
               ms.pushPose();
               ms.translate(mist.x - cam.x, mist.y - cam.y, mist.z - cam.z);
               ms.rotate(Axis.YP.rotationDegrees(180.0F - camera.yRot()));
               Matrix4f m = ms.last().pose();
               float half = mist.size * 0.5F;
               float top = mist.size * 0.42F;
               Vtx.vertex(buffer, m, -half, top, 0.0F).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
               Vtx.vertex(buffer, m, half, top, 0.0F).setColor(rgb[0], rgb[1], rgb[2], 0.0F);
               Vtx.vertex(buffer, m, half, 0.0F, 0.0F).setColor(rgb[0], rgb[1], rgb[2], alpha);
               Vtx.vertex(buffer, m, -half, 0.0F, 0.0F).setColor(rgb[0], rgb[1], rgb[2], alpha);
               ms.popPose();
            }
         }

         MeshData built = buffer.build();
         if (built != null) {
            BufferUploader.drawWithShader(built);
         }
      }
   }

   private float ambientLight(Camera camera) {
      try {
         int light = this.mc.level.getMaxLocalRawBrightness(camera.blockPosition());
         return Mth.clamp(light / 15.0F, 0.3F, 1.0F);
      } catch (Throwable ignored) {
         return 1.0F;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class Drop {
      double x;
      double y;
      double z;
      double prevX;
      double prevY;
      double prevZ;
      double vx;
      double vy;
      double vz;
      double mass = 1.0;
      double ground;
      float phase;
      float brightness = 1.0F;
      boolean dead;
   }

   @Environment(EnvType.CLIENT)
   private static final class Droplet {
      double x;
      double y;
      double z;
      double prevX;
      double prevY;
      double prevZ;
      double vx;
      double vy;
      double vz;
      float life;
      float maxLife = 0.4F;
   }

   @Environment(EnvType.CLIENT)
   private static final class Mist {
      double x;
      double y;
      double z;
      double vx;
      double vz;
      float size = 3.0F;
      float life;
      float maxLife = 4.0F;
   }

   @Environment(EnvType.CLIENT)
   private static final class Splash {
      double x;
      double y;
      double z;
      float life;
      float maxLife = 0.4F;
      float scale = 0.4F;
      boolean water;
   }
}
