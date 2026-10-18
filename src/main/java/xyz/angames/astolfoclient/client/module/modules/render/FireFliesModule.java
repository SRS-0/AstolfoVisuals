package xyz.angames.astolfoclient.client.module.modules.render;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(EnvType.CLIENT)
public class FireFliesModule extends Module {
   private final Minecraft mc = Minecraft.getInstance();
   public final BooleanSetting darkImprint = new BooleanSetting("DarkImprint", false);
   public final BooleanSetting lighting = new BooleanSetting("Lighting", false);
   public final NumberSetting spawnDelay = new NumberSetting("SpawnDelay", 3.0, 1.0, 10.0, 0.5);
   private static final int MAX_FIREFLIES = 20;
   private static final long MAX_PART_ALIVE_TIME = 6000L;
   private final List<FireFliesModule.FirePart> partList = new ArrayList<>();
   private static final Identifier ICON_TEXTURE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/bloom.png");

   public FireFliesModule() {
      super("FireFlies", "Renders beautiful glowing fireflies around you", Module.Category.RENDER);
      this.addSettings(this.darkImprint, this.lighting, this.spawnDelay);
      WorldRenderEvents.LAST.register(this::render3D);
   }

   @Override
   public void onDisable() {
      this.partList.clear();
   }

   @Override
   public void onTick() {
      if (this.isEnabled() && this.mc.player != null && this.mc.level != null) {
         if (this.mc.player.tickCount == 1) {
            for (FireFliesModule.FirePart part : this.partList) {
               part.setToRemove();
            }
         }

         long currentTime = System.currentTimeMillis();

         for (FireFliesModule.FirePart part : this.partList) {
            part.updatePart(this.mc);
         }

         this.partList.removeIf(partx -> partx.toRemove || currentTime - partx.startTime >= 6000L);

         while (this.partList.size() > 20) {
            this.partList.remove(0);
         }

         if (this.partList.size() < 20 && this.mc.player.tickCount % ((int)this.spawnDelay.get() + 1) == 0) {
            this.partList.add(new FireFliesModule.FirePart(this.generateVecForPart(this.mc, 10.0, 4.0), 6000.0F));
            this.partList.add(new FireFliesModule.FirePart(this.generateVecForPart(this.mc, 6.0, 5.0), 6000.0F));
         }
      } else {
         this.partList.clear();
      }
   }

   private Vec3 generateVecForPart(Minecraft mc, double rangeXZ, double rangeY) {
      Vec3 pos = mc.player.position().add(getRandom(-rangeXZ, rangeXZ), getRandom(-rangeY / 2.0, rangeY), getRandom(-rangeXZ, rangeXZ));

      for (int i = 0; i < 30; i++) {
         pos = mc.player.position().add(getRandom(-rangeXZ, rangeXZ), getRandom(-rangeY / 2.0, rangeY), getRandom(-rangeXZ, rangeXZ));
      }

      return pos;
   }

   private static float getRandom(double min, double max) {
      return (float)(min + Math.random() * (max - min));
   }

   private static float lerp(float from, float to, float pct) {
      return from + (to - from) * pct;
   }

   private void render3D(WorldRenderContext context) {
      if (this.isEnabled() && !this.partList.isEmpty() && this.mc.player != null && this.mc.level != null) {
         PoseStack matrixStack = context.matrixStack();
         float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
         Vec3 cameraPos = context.camera().position();
         Quaternionf cameraRot = context.camera().rotation();
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         int baseColor = ThemeManager.getThemedColor(0L);
         float r = (baseColor >> 16 & 0xFF) / 255.0F;
         float g = (baseColor >> 8 & 0xFF) / 255.0F;
         float b = (baseColor & 0xFF) / 255.0F;
         if (this.darkImprint.get()) {
            RenderSystem.defaultBlendFunc();
         } else {
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         }

         Tesselator tessellator = Tesselator.getInstance();
         RenderSystem.setShader(CoreShaders.POSITION_COLOR);
         BufferBuilder sparkBuffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
         boolean hasSparks = false;

         for (FireFliesModule.FirePart part : this.partList) {
            float partAlpha = part.getAlphaPC();
            if (!part.sparkParts.isEmpty()) {
               for (FireFliesModule.SparkPart spark : part.sparkParts) {
                  double sparkX = spark.prevPosX + (spark.posX - spark.prevPosX) * tickDelta - cameraPos.x;
                  double sparkY = spark.prevPosY + (spark.posY - spark.prevPosY) * tickDelta - cameraPos.y;
                  double sparkZ = spark.prevPosZ + (spark.posZ - spark.prevPosZ) * tickDelta - cameraPos.z;
                  matrixStack.pushPose();
                  matrixStack.translate(sparkX, sparkY, sparkZ);
                  matrixStack.rotate(cameraRot);
                  float sparkSize = 0.02F;
                  matrixStack.scale(sparkSize, sparkSize, sparkSize);
                  Matrix4f sm = matrixStack.last().pose();
                  float sparkAlpha = partAlpha * (1.0F - (float)spark.timePC());
                  Vtx.vertex(sparkBuffer, sm, -0.5F, -0.5F, 0.0F).setColor(r, g, b, sparkAlpha);
                  Vtx.vertex(sparkBuffer, sm, 0.5F, -0.5F, 0.0F).setColor(r, g, b, sparkAlpha);
                  Vtx.vertex(sparkBuffer, sm, 0.5F, 0.5F, 0.0F).setColor(r, g, b, sparkAlpha);
                  Vtx.vertex(sparkBuffer, sm, -0.5F, 0.5F, 0.0F).setColor(r, g, b, sparkAlpha);
                  matrixStack.popPose();
                  hasSparks = true;
               }
            }
         }

         if (hasSparks) {
            BufferUploader.drawWithShader(sparkBuffer.buildOrThrow());
         }

         for (FireFliesModule.FirePart part : this.partList) {
            if (part.trailParts.size() >= 2) {
               float partAlpha = part.getAlphaPC();
               double dist = cameraPos.distanceTo(part.posVec);
               float width = 1.0E-5F + 8.0F * Mth.clamp(1.0F - ((float)dist - 3.0F) / 20.0F, 0.0F, 1.0F);
               RenderSystem.lineWidth(width);
               BufferBuilder lineBuffer = tessellator.begin(PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
               matrixStack.pushPose();
               matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
               Matrix4f lm = matrixStack.last().pose();

               for (int i = 0; i < part.trailParts.size(); i++) {
                  FireFliesModule.TrailPart trail = part.trailParts.get(i);
                  float sizePC = (float)i / part.trailParts.size();
                  if (sizePC > 0.5F) {
                     sizePC = 1.0F - sizePC;
                  }

                  sizePC *= 2.0F;
                  float trailAlpha = partAlpha * sizePC;
                  Vtx.vertex(lineBuffer, lm, (float)trail.x, (float)trail.y, (float)trail.z).setColor(r, g, b, trailAlpha);
               }

               BufferUploader.drawWithShader(lineBuffer.buildOrThrow());
               matrixStack.popPose();
            }
         }

         RenderSystem.lineWidth(1.0F);
         RenderSystem.setShaderTexture(0, ICON_TEXTURE);
         RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
         BufferBuilder textureBuffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         boolean hasTexture = false;

         for (FireFliesModule.FirePart part : this.partList) {
            float partAlpha = part.getAlphaPC();
            double x = part.prevPos.x + (part.posVec.x - part.prevPos.x) * tickDelta - cameraPos.x;
            double y = part.prevPos.y + (part.posVec.y - part.prevPos.y) * tickDelta - cameraPos.y;
            double z = part.prevPos.z + (part.posVec.z - part.prevPos.z) * tickDelta - cameraPos.z;
            matrixStack.pushPose();
            matrixStack.translate(x, y, z);
            matrixStack.rotate(cameraRot);
            float scale = 0.08F;
            matrixStack.scale(scale, scale, scale);
            Matrix4f m = matrixStack.last().pose();
            Vtx.vertex(textureBuffer, m, -0.5F, -0.5F, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, partAlpha);
            Vtx.vertex(textureBuffer, m, 0.5F, -0.5F, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, partAlpha);
            Vtx.vertex(textureBuffer, m, 0.5F, 0.5F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, partAlpha);
            Vtx.vertex(textureBuffer, m, -0.5F, 0.5F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, partAlpha);
            if (this.lighting.get()) {
               matrixStack.scale(3.0F, 3.0F, 3.0F);
               Matrix4f mGlow = matrixStack.last().pose();
               float glowR = r * 0.4F;
               float glowG = g * 0.4F;
               float glowB = b * 0.4F;
               float glowAlpha = partAlpha / 5.0F;
               Vtx.vertex(textureBuffer, mGlow, -0.5F, -0.5F, 0.0F).setUv(0.0F, 1.0F).setColor(glowR, glowG, glowB, glowAlpha);
               Vtx.vertex(textureBuffer, mGlow, 0.5F, -0.5F, 0.0F).setUv(1.0F, 1.0F).setColor(glowR, glowG, glowB, glowAlpha);
               Vtx.vertex(textureBuffer, mGlow, 0.5F, 0.5F, 0.0F).setUv(1.0F, 0.0F).setColor(glowR, glowG, glowB, glowAlpha);
               Vtx.vertex(textureBuffer, mGlow, -0.5F, 0.5F, 0.0F).setUv(0.0F, 0.0F).setColor(glowR, glowG, glowB, glowAlpha);
            }

            matrixStack.popPose();
            hasTexture = true;
         }

         if (hasTexture) {
            BufferUploader.drawWithShader(textureBuffer.buildOrThrow());
         }

         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   @Environment(EnvType.CLIENT)
   private static class FirePart {
      Vec3 posVec;
      Vec3 prevPos;
      final List<FireFliesModule.TrailPart> trailParts = new ArrayList<>();
      final List<FireFliesModule.SparkPart> sparkParts = new ArrayList<>();
      float anim = 0.0F;
      float animTo = 1.0F;
      float animSpeed = 0.02F;
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
         return Mth.clamp((float)(System.currentTimeMillis() - this.startTime) / this.maxAlive, 0.0F, 1.0F);
      }

      public void setAlphaPCTo(float to) {
         this.animTo = to;
      }

      public float getAlphaPC() {
         return this.anim;
      }

      public void updatePart(Minecraft mc) {
         this.anim = this.anim + (this.animTo - this.anim) * this.animSpeed;
         this.anim = Mth.clamp(this.anim, 0.0F, 1.0F);
         if (System.currentTimeMillis() - this.rateTimer >= this.msChangeSideRate) {
            this.msChangeSideRate = this.calculateMsChangeSideRate();
            this.rateTimer = System.currentTimeMillis();
            this.moveYawSet = FireFliesModule.getRandom(0.0, 360.0);
         }

         this.moveYaw = FireFliesModule.lerp(this.moveYaw, this.moveYawSet, 0.065F);
         this.speed /= 1.005F;
         float motionX = -((float)Math.sin(Math.toRadians(this.moveYaw))) * this.speed;
         float motionZ = (float)Math.cos(Math.toRadians(this.moveYaw)) * this.speed;
         this.prevPos = this.posVec;
         double scaleBox = 0.1;
         boolean collides = false;
         if (mc.level != null) {
            AABB box = new AABB(
               this.posVec.x - scaleBox / 2.0,
               this.posVec.y,
               this.posVec.z - scaleBox / 2.0,
               this.posVec.x + scaleBox / 2.0,
               this.posVec.y + scaleBox,
               this.posVec.z + scaleBox / 2.0
            );
            Iterable<VoxelShape> collisions = mc.level.getBlockCollisions(null, box);
            if (collisions.iterator().hasNext()) {
               collides = true;
            }
         }

         float delente = collides ? 0.3F : 1.0F;
         this.yMotion /= 1.02F;
         this.posVec = this.posVec.add(motionX / delente, this.yMotion / delente, motionZ / delente);
         if (this.getTimePC() >= 1.0F) {
            this.setAlphaPCTo(0.0F);
            if (this.getAlphaPC() < 0.003921569F) {
               this.setToRemove();
            }
         }

         this.trailParts.add(new FireFliesModule.TrailPart(this, 400));
         this.trailParts.removeIf(FireFliesModule.TrailPart::toRemove);

         for (int i = 0; i < 2; i++) {
            this.sparkParts.add(new FireFliesModule.SparkPart(this, 300));
         }

         for (FireFliesModule.SparkPart spark : this.sparkParts) {
            spark.motionSparkProcess();
         }

         this.sparkParts.removeIf(FireFliesModule.SparkPart::toRemove);
      }

      public void setToRemove() {
         this.toRemove = true;
      }

      private int calculateMsChangeSideRate() {
         return (int)FireFliesModule.getRandom(300.5, 900.5);
      }
   }

   @Environment(EnvType.CLIENT)
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

      public SparkPart(FireFliesModule.FirePart part, int maxTime) {
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
         return Mth.clamp((float)(System.currentTimeMillis() - this.startTime) / this.maxTime, 0.0F, 1.0F);
      }

      public boolean toRemove() {
         return this.timePC() == 1.0;
      }

      public void motionSparkProcess() {
         double radYaw = Math.toRadians(this.radianYaw);
         this.prevPosX = this.posX;
         this.prevPosY = this.posY;
         this.prevPosZ = this.posZ;
         this.posX = this.posX + Math.sin(radYaw) * this.speed;
         this.posY = this.posY + Math.cos(Math.toRadians(this.radianPitch - 90.0)) * this.speed;
         this.posZ = this.posZ + Math.cos(radYaw) * this.speed;
      }
   }

   @Environment(EnvType.CLIENT)
   private static class TrailPart {
      double x;
      double y;
      double z;
      long startTime;
      int maxTime;

      public TrailPart(FireFliesModule.FirePart part, int maxTime) {
         this.x = part.posVec.x;
         this.y = part.posVec.y;
         this.z = part.posVec.z;
         this.startTime = System.currentTimeMillis();
         this.maxTime = maxTime;
      }

      public float getTimePC() {
         return Mth.clamp((float)(System.currentTimeMillis() - this.startTime) / this.maxTime, 0.0F, 1.0F);
      }

      public boolean toRemove() {
         return this.getTimePC() == 1.0F;
      }
   }
}
