package xyz.angames.astolfoclient.client.module.modules.render;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents.Last;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(EnvType.CLIENT)
public class WingsModule extends Module {
   private final Minecraft mc = Minecraft.getInstance();
   private static final float DEFAULT_SPREAD = 8.0F;
   private static final int DEFAULT_ALPHA = 220;
   private static final WingsModule.WingPoint[] SHAPE = new WingsModule.WingPoint[]{
      new WingsModule.WingPoint(0.08F, 0.1F, 0.88F),
      new WingsModule.WingPoint(0.28F, 0.34F, 0.78F),
      new WingsModule.WingPoint(0.56F, 0.82F, 0.62F),
      new WingsModule.WingPoint(0.86F, 0.3F, 0.52F),
      new WingsModule.WingPoint(1.14F, 0.46F, 0.4F),
      new WingsModule.WingPoint(1.24F, 0.04F, 0.3F),
      new WingsModule.WingPoint(1.02F, -0.18F, 0.28F),
      new WingsModule.WingPoint(1.18F, -0.64F, 0.22F),
      new WingsModule.WingPoint(0.86F, -0.46F, 0.2F),
      new WingsModule.WingPoint(0.8F, -0.98F, 0.14F),
      new WingsModule.WingPoint(0.54F, -0.74F, 0.16F),
      new WingsModule.WingPoint(0.3F, -1.16F, 0.12F),
      new WingsModule.WingPoint(0.1F, -0.54F, 0.18F)
   };
   public final BooleanSetting self = new BooleanSetting("Self", true);
   public final BooleanSetting players = new BooleanSetting("Players", false);
   public final NumberSetting size = new NumberSetting("Size", 1.0, 0.75, 1.35, 0.05);
   private float selfBodyYaw;
   private boolean selfBodyYawInitialized;

   public WingsModule() {
      super("Wings", Module.Category.RENDER);
      this.addSetting(this.self);
      this.addSetting(this.players);
      this.addSetting(this.size);
      WorldRenderEvents.LAST
         .register(
            (Last)context -> {
               if (this.isEnabled() && this.mc.player != null && this.mc.level != null && this.mc.gameRenderer != null) {
                  PoseStack stack = context.matrixStack();
                  float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
                  Vec3 camera = this.mc.gameRenderer.mainCamera().position();
                  stack.pushPose();
                  RenderSystem.enableBlend();
                  RenderSystem.disableCull();
                  RenderSystem.enableDepthTest();
                  RenderSystem.depthMask(false);
                  RenderSystem.setShader(CoreShaders.POSITION_COLOR);
                  if (this.self.get()
                     && !this.mc.options.getCameraType().isFirstPerson()
                     && this.mc.player.isAlive()
                     && !this.hasElytra(this.mc.player)) {
                     try {
                        this.renderWings(stack, this.mc.player, tickDelta, camera);
                     } catch (Exception var10) {
                     }
                  }

                  if (this.players.get()) {
                     for (Entity entity : this.mc.level.entitiesForRendering()) {
                        if (entity instanceof Player player && player != this.mc.player && player.isAlive() && !this.hasElytra(player)) {
                           try {
                              this.renderWings(stack, player, tickDelta, camera);
                           } catch (Exception var9) {
                           }
                        }
                     }
                  }

                  RenderSystem.depthMask(true);
                  RenderSystem.enableCull();
                  RenderSystem.disableBlend();
                  RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
                  RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                  stack.popPose();
               }
            }
         );
   }

   @Override
   public void onDisable() {
      this.selfBodyYawInitialized = false;
   }

   private void renderWings(PoseStack stack, Player player, float tickDelta, Vec3 camera) {
      double x = Mth.lerp(tickDelta, player.xo, player.getX()) - camera.x;
      double y = Mth.lerp(tickDelta, player.yo, player.getY()) - camera.y;
      double z = Mth.lerp(tickDelta, player.zo, player.getZ()) - camera.z;
      float bodyYaw = this.resolveBodyYaw(player, tickDelta);
      float move = Mth.clamp(player.walkAnimation.speed(tickDelta), 0.0F, 1.0F);
      WingsModule.WingPose pose = this.resolvePose(player, tickDelta);
      if (pose != null) {
         float flap = (float)Math.sin((player.tickCount + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
         float open = (8.0F + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
         float wingScale = (float)this.size.get() * pose.scaleMultiplier;
         int baseColor = this.resolveBaseColor();
         int glowColor = this.resolveGlowColor(baseColor);
         int coreColor = this.resolveCoreColor(baseColor);
         boolean isBaby = false;
         if (BabyPlayerModule.INSTANCE != null && BabyPlayerModule.INSTANCE.isEnabled()) {
            if (player == this.mc.player && BabyPlayerModule.INSTANCE.self.get()) {
               isBaby = true;
            } else if (player != this.mc.player && BabyPlayerModule.INSTANCE.friends.get() && FriendManager.isFriend(player.getName().getString())) {
               isBaby = true;
            }
         }

         stack.pushPose();
         stack.translate(x, y, z);
         if (isBaby) {
            stack.scale(0.5F, 0.5F, 0.5F);
         }

         stack.rotate(Axis.YP.rotationDegrees(180.0F - bodyYaw));
         if (pose.preTranslateY != 0.0F || pose.preTranslateZ != 0.0F) {
            stack.translate(0.0F, pose.preTranslateY, pose.preTranslateZ);
         }

         if (pose.pitchRotation != 0.0F) {
            stack.rotate(Axis.XP.rotationDegrees(pose.pitchRotation));
         }

         if (pose.rollRotation != 0.0F) {
            stack.rotate(Axis.ZP.rotationDegrees(pose.rollRotation));
         }

         stack.translate(0.0F, pose.anchorY, pose.anchorZ);
         stack.scale(wingScale, wingScale, wingScale);
         this.renderWingSide(stack, -1.0F, open, baseColor, glowColor, coreColor, pose);
         this.renderWingSide(stack, 1.0F, open, baseColor, glowColor, coreColor, pose);
         stack.popPose();
      }
   }

   private void renderWingSide(PoseStack stack, float side, float open, int baseColor, int glowColor, int coreColor, WingsModule.WingPose pose) {
      stack.pushPose();
      stack.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
      stack.rotate(Axis.YP.rotationDegrees(side * open));
      stack.rotate(Axis.ZP.rotationDegrees(side * pose.sideRoll));
      stack.rotate(Axis.XP.rotationDegrees(pose.sidePitch));
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      this.drawWingLayer(stack, side, 1.22F, setAlpha(glowColor, 48), setAlpha(glowColor, 0));
      this.drawWingLayer(stack, side, 0.84F, setAlpha(coreColor, 57), setAlpha(coreColor, 0));
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
      this.drawWingLayer(stack, side, 1.0F, setAlpha(baseColor, 220), setAlpha(baseColor, 10));
      RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
      this.drawWingOutline(stack, side, 1.0F, setAlpha(baseColor, 136));
      this.drawWingRibs(stack, side, 0.96F, setAlpha(glowColor, 44));
      stack.popPose();
   }

   private void drawWingLayer(PoseStack stack, float side, float scale, int rootColor, int edgeColor) {
      Matrix4f matrix = stack.last().pose();
      BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

      for (int i = 0; i < SHAPE.length; i++) {
         WingsModule.WingPoint cur = SHAPE[i];
         WingsModule.WingPoint next = SHAPE[(i + 1) % SHAPE.length];
         this.vertex(buffer, matrix, 0.0F, 0.0F, 0.0F, rootColor);
         this.vertex(buffer, matrix, side * cur.x * scale, cur.y * scale, 0.0F, this.applyPointAlpha(edgeColor, cur.alphaMul));
         this.vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0.0F, this.applyPointAlpha(edgeColor, next.alphaMul));
      }

      BufferUploader.drawWithShader(buffer.buildOrThrow());
   }

   private void drawWingOutline(PoseStack stack, float side, float scale, int color) {
      Matrix4f matrix = stack.last().pose();
      RenderSystem.lineWidth(1.35F);
      GL11.glEnable(2848);
      BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

      for (WingsModule.WingPoint point : SHAPE) {
         this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0F, color);
      }

      this.vertex(buffer, matrix, side * SHAPE[0].x * scale, SHAPE[0].y * scale, 0.0F, color);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      GL11.glDisable(2848);
   }

   private void drawWingRibs(PoseStack stack, float side, float scale, int color) {
      Matrix4f matrix = stack.last().pose();
      int[] ribIndices = new int[]{2, 4, 7, 9, 11};
      RenderSystem.lineWidth(0.9F);
      BufferBuilder buffer = Tesselator.getInstance().begin(PrimitiveTopology.LINES, DefaultVertexFormat.POSITION_COLOR);

      for (int idx : ribIndices) {
         WingsModule.WingPoint point = SHAPE[idx];
         this.vertex(buffer, matrix, 0.0F, 0.0F, 0.0F, setAlpha(color, Math.max(8, (int)(alpha(color) * 0.75F))));
         this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0F, this.applyPointAlpha(color, point.alphaMul));
      }

      BufferUploader.drawWithShader(buffer.buildOrThrow());
   }

   private int resolveBaseColor() {
      long now = System.currentTimeMillis();
      int colorInt = ThemeManager.getThemedColor(now / 10L);
      return setAlpha(colorInt, 255);
   }

   private int resolveGlowColor(int base) {
      return interpolateColor(base, packARGB(255, 255, 255, 255), 0.28F);
   }

   private int resolveCoreColor(int base) {
      return interpolateColor(base, packARGB(255, 255, 255, 255), 0.55F);
   }

   private static int interpolateColor(int a, int b, float t) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int aa = a >> 24 & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int ba = b >> 24 & 0xFF;
      int r = (int)(ar + (br - ar) * t);
      int g = (int)(ag + (bg - ag) * t);
      int bl2 = (int)(ab + (bb - ab) * t);
      int al = (int)(aa + (ba - aa) * t);
      return packARGB(r, g, bl2, al);
   }

   private static int packARGB(int r, int g, int b, int a) {
      return Mth.clamp(a, 0, 255) << 24 | r << 16 | g << 8 | b;
   }

   private static int setAlpha(int color, int a) {
      return Mth.clamp(a, 0, 255) << 24 | color & 16777215;
   }

   private static int alpha(int color) {
      return color >> 24 & 0xFF;
   }

   private static int red(int color) {
      return color >> 16 & 0xFF;
   }

   private static int green(int color) {
      return color >> 8 & 0xFF;
   }

   private static int blue(int color) {
      return color & 0xFF;
   }

   private int applyPointAlpha(int color, float multiplier) {
      return setAlpha(color, Math.max(0, Math.min(255, (int)(alpha(color) * multiplier))));
   }

   private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
      Vtx.vertex(buffer, matrix, x, y, z).setColor(red(color) / 255.0F, green(color) / 255.0F, blue(color) / 255.0F, alpha(color) / 255.0F);
   }

   private float resolveBodyYaw(Player player, float tickDelta) {
      float target = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);
      if (player != this.mc.player) {
         return target;
      } else if (this.selfBodyYawInitialized && player.tickCount >= 2) {
         this.selfBodyYaw = approachDegrees(this.selfBodyYaw, target, 14.0F);
         return this.selfBodyYaw;
      } else {
         this.selfBodyYaw = target;
         this.selfBodyYawInitialized = true;
         return this.selfBodyYaw;
      }
   }

   private static float approachDegrees(float current, float target, float maxDelta) {
      float delta = Mth.wrapDegrees(target - current);
      delta = Mth.clamp(delta, -maxDelta, maxDelta);
      return current + delta;
   }

   private WingsModule.WingPose resolvePose(Player player, float tickDelta) {
      float pitch = Mth.lerp(tickDelta, player.xRotO, player.getXRot());
      if (player.isFallFlying()) {
         float flightTicks = player.getFallFlyingTicks() + tickDelta;
         float flightProgress = Mth.clamp(flightTicks * flightTicks / 100.0F, 0.0F, 1.0F);
         float pitchRotation = flightProgress * (-90.0F - pitch);
         return new WingsModule.WingPose(0.34F, 0.46F, 0.0F, 0.0F, pitchRotation, 0.0F, 0.76F, 0.92F, 0.1F, 0.58F, 0.05F, 0.06F, -5.0F, -2.0F, 0.13F);
      } else if (player.isInWater()) {
         return null;
      } else {
         return player.isShiftKeyDown()
            ? new WingsModule.WingPose(0.0F, 0.0F, 0.96F, 0.1F, 18.0F, 0.0F, 1.0F, 1.0F, 0.18F, 4.5F, 0.06F, 0.02F, -11.0F, -4.0F, 0.12F)
            : new WingsModule.WingPose(0.0F, 0.0F, 1.38F, 0.1F, 0.0F, 0.0F, 1.0F, 1.0F, 0.18F, 4.5F, 0.06F, 0.02F, -11.0F, -4.0F, 0.12F);
      }
   }

   private boolean hasElytra(Player player) {
      return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
   }

   @Override
   public void onTick() {
   }

   @Environment(EnvType.CLIENT)
   private static final class WingPoint {
      final float x;
      final float y;
      final float alphaMul;

      WingPoint(float x, float y, float alphaMul) {
         this.x = x;
         this.y = y;
         this.alphaMul = alphaMul;
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class WingPose {
      final float preTranslateY;
      final float preTranslateZ;
      final float anchorY;
      final float anchorZ;
      final float pitchRotation;
      final float rollRotation;
      final float openMultiplier;
      final float scaleMultiplier;
      final float motionSpreadBoost;
      final float flapAmplitude;
      final float sideOffset;
      final float sideYOffset;
      final float sideZOffset;
      final float sideRoll;
      final float sidePitch;
      final float flapSpeed;

      WingPose(
         float preTranslateY,
         float preTranslateZ,
         float anchorY,
         float anchorZ,
         float pitchRotation,
         float rollRotation,
         float openMultiplier,
         float scaleMultiplier,
         float motionSpreadBoost,
         float flapAmplitude,
         float sideOffset,
         float sideZOffset,
         float sideRoll,
         float sidePitch,
         float flapSpeed
      ) {
         this(
            preTranslateY,
            preTranslateZ,
            anchorY,
            anchorZ,
            pitchRotation,
            rollRotation,
            openMultiplier,
            scaleMultiplier,
            motionSpreadBoost,
            flapAmplitude,
            sideOffset,
            0.0F,
            sideZOffset,
            sideRoll,
            sidePitch,
            flapSpeed
         );
      }

      WingPose(
         float preTranslateY,
         float preTranslateZ,
         float anchorY,
         float anchorZ,
         float pitchRotation,
         float rollRotation,
         float openMultiplier,
         float scaleMultiplier,
         float motionSpreadBoost,
         float flapAmplitude,
         float sideOffset,
         float sideYOffset,
         float sideZOffset,
         float sideRoll,
         float sidePitch,
         float flapSpeed
      ) {
         this.preTranslateY = preTranslateY;
         this.preTranslateZ = preTranslateZ;
         this.anchorY = anchorY;
         this.anchorZ = anchorZ;
         this.pitchRotation = pitchRotation;
         this.rollRotation = rollRotation;
         this.openMultiplier = openMultiplier;
         this.scaleMultiplier = scaleMultiplier;
         this.motionSpreadBoost = motionSpreadBoost;
         this.flapAmplitude = flapAmplitude;
         this.sideOffset = sideOffset;
         this.sideYOffset = sideYOffset;
         this.sideZOffset = sideZOffset;
         this.sideRoll = sideRoll;
         this.sidePitch = sidePitch;
         this.flapSpeed = flapSpeed;
      }
   }
}
