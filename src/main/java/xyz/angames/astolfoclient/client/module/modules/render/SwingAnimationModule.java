package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import xyz.angames.astolfoclient.client.mixin.HeldItemRendererAccessor;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(EnvType.CLIENT)
public class SwingAnimationModule extends Module {
   public final ModeSetting mode = new ModeSetting("Mode", "Mode 1", "Mode 1", "Mode 2", "Mode 3", "Mode 4", "Mode 5", "Vanilla", "HMI");
   public final NumberSetting strength = new NumberSetting("Strength", 20.0, 20.0, 75.0, 0.1);
   public final BooleanSetting slow = new BooleanSetting("Slow", false);
   public final NumberSetting speed = new NumberSetting("Speed", 12.0, 1.0, 50.0, 1.0);
   public final ModeSetting attackMode = new ModeSetting("Attack Mode", "Swings", "Swings", "Forward", "Normal") {
      @Override
      public boolean isVisible() {
         return SwingAnimationModule.this.isHmiVisible();
      }
   };
   public final NumberSetting rightX = new NumberSetting("Right X", 0.0, -2.0, 2.0, 0.1) {
      @Override
      public boolean isVisible() {
         return SwingAnimationModule.this.isHmiVisible();
      }
   };
   public final NumberSetting rightZ = new NumberSetting("Right Y", 0.0, -2.0, 2.0, 0.1) {
      @Override
      public boolean isVisible() {
         return SwingAnimationModule.this.isHmiVisible();
      }
   };
   public final NumberSetting rightY = new NumberSetting("Right Z", 0.0, -2.0, 2.0, 0.1) {
      @Override
      public boolean isVisible() {
         return SwingAnimationModule.this.isHmiVisible();
      }
   };
   public final NumberSetting leftX = new NumberSetting("Left X", 0.0, -2.0, 2.0, 0.1) {
      @Override
      public boolean isVisible() {
         return SwingAnimationModule.this.isHmiVisible();
      }
   };
   public final NumberSetting leftZ = new NumberSetting("Left Y", 0.0, -2.0, 2.0, 0.1) {
      @Override
      public boolean isVisible() {
         return SwingAnimationModule.this.isHmiVisible();
      }
   };
   public final NumberSetting leftY = new NumberSetting("Left Z", 0.0, -2.0, 2.0, 0.1) {
      @Override
      public boolean isVisible() {
         return SwingAnimationModule.this.isHmiVisible();
      }
   };
   public static boolean renderingCustomItem = false;
   /** Set by the hand mixin for the hand being submitted; 26.3 renders held items from pre-extracted states. */
   public static ItemStackRenderState currentItemState;
   public static PlayerRenderState currentPlayerState;
   private static final double HOLD_MY_ITEMS_MAX_DELTA = 0.05;
   private static final double HOLD_MY_ITEMS_ANIMATION_SPEED = 30.0;
   private double holdMyItemsPrevFrameTime = System.nanoTime() / 1.0E9;
   private double holdMyItemsDeltaTime;
   private double holdMyItemsPreviousRotation;
   private float holdMyItemsSwingAngleY;
   private float holdMyItemsSwingAngleX;
   private float holdMyItemsSwingVelocityY;
   private float holdMyItemsSwingVelocityX;
   private float holdMyItemsSwingVelocityZ;
   private float holdMyItemsVertAngleY;
   private float holdMyItemsVertVelocityYSlime;
   private float holdMyItemsVertAngleYSlime;
   private float holdMyItemsClimbBlend;
   private float holdMyItemsCrawlCount;
   private float holdMyItemsDirectionalCrawlCount;
   private float holdMyItemsClimbCount;
   private float holdMyItemsInWaterCounter;
   private boolean holdMyItemsIsAttacking;
   private boolean holdMyItemsLeft;
   private float holdMyItemsPrevSwingProgress;
   private boolean holdMyItemsPhysicsUpdatedThisFrame;
   private float chestRightHandMotion;

   private boolean isHmiVisible() {
      return "HMI".equals(this.mode.get());
   }

   public SwingAnimationModule() {
      super("SwingAnimation", "Custom Swing Animations", Module.Category.RENDER);
      this.addSettings(
         this.mode, this.strength, this.slow, this.speed, this.attackMode, this.rightX, this.rightZ, this.rightY, this.leftX, this.leftZ, this.leftY
      );
   }

   private float getFloat(NumberSetting setting) {
      return Double.valueOf(setting.getValue()).floatValue();
   }

   public boolean isAuraActive() {
      return false;
   }

   private void applyHandPosition(PoseStack matrices, HumanoidArm arm) {
      this.applyHandPositionBase(matrices, arm);
      this.applyHandPositionItem(matrices, arm);
   }

   private void applyHandPositionBase(PoseStack matrices, HumanoidArm arm) {
      HandPositionModule handPos = (HandPositionModule)ModuleManager.getModule(HandPositionModule.class);
      if (handPos != null && handPos.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         boolean isMainHand = mc.player != null && arm == mc.player.getMainArm();
         float[] pos = isMainHand ? handPos.getMainHandPos() : handPos.getOffHandPos();
         matrices.translate(pos[0], pos[1], pos[2]);
      }
   }

   private void applyHandPositionItem(PoseStack matrices, HumanoidArm arm) {
      HandPositionModule handPos = (HandPositionModule)ModuleManager.getModule(HandPositionModule.class);
      if (handPos != null && handPos.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         boolean isMainHand = mc.player != null && arm == mc.player.getMainArm();
         float[] rot = isMainHand ? handPos.getMainHandRot() : handPos.getOffHandRot();
         float[] scale = isMainHand ? handPos.getMainHandScale() : handPos.getOffHandScale();
         if (rot[0] != 0.0F) {
            matrices.rotate(Axis.XP.rotationDegrees(rot[0]));
         }

         if (rot[1] != 0.0F) {
            matrices.rotate(Axis.YP.rotationDegrees(rot[1]));
         }

         if (rot[2] != 0.0F) {
            matrices.rotate(Axis.ZP.rotationDegrees(rot[2]));
         }

         if (scale[0] != 1.0F || scale[1] != 1.0F || scale[2] != 1.0F) {
            matrices.scale(scale[0], scale[1], scale[2]);
         }
      }
   }

   private void handleSwordAnim(PoseStack matrices, float swingProgress, float equipProgress, HumanoidArm arm) {
      float str = this.getFloat(this.strength);
      float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
      float anim = (float)Math.sin(swingProgress * (Math.PI / 2) * 2.0);
      float isLeft = arm == HumanoidArm.LEFT ? -1.0F : 1.0F;
      String currentMode = String.valueOf(this.mode.get()).toUpperCase();
      if (currentMode.contains("VANILLA")) {
         float n = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
         float mxx = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
         float fxxx = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
         matrices.translate(isLeft * n, mxx, fxxx);
         this.applyEquipOffset(matrices, arm, equipProgress);
         this.applySwingOffset(matrices, arm, swingProgress);
      } else {
         this.applyEquipOffset(matrices, arm, 0.0F);
         matrices.scale(1.0F, 1.0F, 1.0F);
         if (currentMode.contains("1")) {
            this.applySwingOffset(matrices, arm, swingProgress);
         } else if (currentMode.contains("2")) {
            matrices.translate(isLeft * -0.1F, 0.15F, -0.1F);
            matrices.rotate(Axis.YP.rotationDegrees(isLeft * -60.0F));
            matrices.rotate(Axis.XP.rotationDegrees(50.0F));
            matrices.rotate(Axis.ZP.rotationDegrees(isLeft * (110.0F + str * g)));
         } else if (currentMode.contains("3")) {
            matrices.translate(isLeft * -0.1F, 0.15F, 0.0F);
            matrices.rotate(Axis.XP.rotationDegrees(50.0F));
            matrices.rotate(Axis.YP.rotationDegrees(isLeft * (-30.0F + str * g)));
            matrices.rotate(Axis.ZP.rotationDegrees(isLeft * 110.0F));
         } else if (currentMode.contains("4")) {
            matrices.translate(isLeft * -0.15F, 0.2F, 0.0F);
            matrices.rotate(Axis.YP.rotationDegrees(isLeft * 90.0F));
            matrices.rotate(Axis.ZP.rotationDegrees(isLeft * -30.0F));
            matrices.rotate(Axis.XP.rotationDegrees(-90.0F - str * anim + 10.0F));
         } else if (currentMode.contains("5")) {
            this.applySwingOffset(matrices, arm, swingProgress);
            float spinAngle = swingProgress * 360.0F;
            matrices.translate(0.0F, 0.0F, 0.0F);
            matrices.rotate(Axis.XP.rotationDegrees(-isLeft * spinAngle));
            matrices.translate(0.0F, 0.0F, 0.0F);
         }
      }
   }

   public void handleRenderItem(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector vertexConsumers,
      int light
   ) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         renderingCustomItem = true;

         try {
            if (this.isHoldMyItemsEnabled()) {
               if (this.shouldUseHoldMyItemsBow(player, hand, item)) {
                  this.renderHMIBow(player, tickDelta, hand, swingProgress, item, 0.0F, matrices, vertexConsumers, light);
                  return;
               }

               if (this.shouldUseHoldMyItemsConsume(player, hand, item)) {
                  this.renderHMIConsume(player, tickDelta, hand, swingProgress, item, 0.0F, matrices, vertexConsumers, light);
                  return;
               }

               if (this.shouldUseHoldMyItemsCustom(player, hand, item)) {
                  this.renderHMI(player, tickDelta, pitch, hand, swingProgress, item, 0.0F, matrices, vertexConsumers, light);
                  return;
               }
            }

            if (!player.isScoping()) {
               boolean isMainHand = hand == InteractionHand.MAIN_HAND;
               HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
               boolean isRightArm = arm == HumanoidArm.RIGHT;
               int i = isRightArm ? 1 : -1;
               matrices.pushPose();
               this.applyHandPositionBase(matrices, arm);
               if (item.is(Items.CROSSBOW)) {
                  boolean isCharged = CrossbowItem.isCharged(item);
                  if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     matrices.translate(i * -0.4785682F, -0.094387F, 0.05731531F);
                     matrices.rotate(Axis.XP.rotationDegrees(-11.935F));
                     matrices.rotate(Axis.YP.rotationDegrees(i * 65.3F));
                     matrices.rotate(Axis.ZP.rotationDegrees(i * -9.785F));
                     float f = item.getUseDuration(mc.player) - (mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                     float g = f / CrossbowItem.getChargeDuration(item, mc.player);
                     if (g > 1.0F) {
                        g = 1.0F;
                     }

                     if (g > 0.1F) {
                        float h = Mth.sin((f - 0.1F) * 1.3F);
                        float j = g - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                     }

                     matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                     matrices.scale(1.0F, 1.0F, 1.0F + g * 0.2F);
                     matrices.rotate(Axis.YN.rotationDegrees(i * 45.0F));
                  } else {
                     float fx = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                     float gx = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
                     float h = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
                     matrices.translate(i * fx, gx, h);
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     this.applySwingOffset(matrices, arm, swingProgress);
                     if (isCharged && swingProgress < 0.001F && isMainHand) {
                        matrices.translate(i * -0.641864F, 0.0F, 0.0F);
                        matrices.rotate(Axis.YP.rotationDegrees(i * 10.0F));
                     }
                  }

                  this.applyHandPositionItem(matrices, arm);
                  this.renderItem(player, item, isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !isRightArm, matrices, vertexConsumers, light);
               } else {
                  if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                     int l = isRightArm ? 1 : -1;
                     switch (item.getUseAnimation()) {
                        case NONE:
                        case BLOCK:
                           this.applyEquipOffset(matrices, arm, equipProgress);
                           break;
                        case EAT:
                        case DRINK:
                           this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item);
                           this.applyEquipOffset(matrices, arm, equipProgress);
                           break;
                        case BOW:
                           this.applyEquipOffset(matrices, arm, equipProgress);
                           matrices.translate(l * -0.2785682F, 0.18344387F, 0.15731531F);
                           matrices.rotate(Axis.XP.rotationDegrees(-13.935F));
                           matrices.rotate(Axis.YP.rotationDegrees(l * 35.3F));
                           matrices.rotate(Axis.ZP.rotationDegrees(l * -9.785F));
                           float mx = item.getUseDuration(mc.player) - (mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                           float fxx = mx / 20.0F;
                           fxx = (fxx * fxx + fxx * 2.0F) / 3.0F;
                           if (fxx > 1.0F) {
                              fxx = 1.0F;
                           }

                           if (fxx > 0.1F) {
                              float gx = Mth.sin((mx - 0.1F) * 1.3F);
                              float h = fxx - 0.1F;
                              float j = gx * h;
                              matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                           }

                           matrices.translate(fxx * 0.0F, fxx * 0.0F, fxx * 0.04F);
                           matrices.scale(1.0F, 1.0F, 1.0F + fxx * 0.2F);
                           matrices.rotate(Axis.YN.rotationDegrees(l * 45.0F));
                           break;
                        case SPEAR:
                           this.applyEquipOffset(matrices, arm, equipProgress);
                           matrices.translate(l * -0.5F, 0.7F, 0.1F);
                           matrices.rotate(Axis.XP.rotationDegrees(-55.0F));
                           matrices.rotate(Axis.YP.rotationDegrees(l * 35.3F));
                           matrices.rotate(Axis.ZP.rotationDegrees(l * -9.785F));
                           float m = item.getUseDuration(mc.player) - (mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                           float fx = m / 10.0F;
                           if (fx > 1.0F) {
                              fx = 1.0F;
                           }

                           if (fx > 0.1F) {
                              float gx = Mth.sin((m - 0.1F) * 1.3F);
                              float h = fx - 0.1F;
                              float j = gx * h;
                              matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                           }

                           matrices.translate(0.0F, 0.0F, fx * 0.2F);
                           matrices.scale(1.0F, 1.0F, 1.0F + fx * 0.2F);
                           matrices.rotate(Axis.YN.rotationDegrees(l * 45.0F));
                           break;
                        case BRUSH:
                           this.applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
                     }
                  } else if (player.isAutoSpinAttack()) {
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     int l = isRightArm ? 1 : -1;
                     matrices.translate(l * -0.4F, 0.8F, 0.3F);
                     matrices.rotate(Axis.YP.rotationDegrees(l * 65.0F));
                     matrices.rotate(Axis.ZP.rotationDegrees(l * -85.0F));
                  } else if (arm == mc.options.mainHand().get() && this.isEnabled()) {
                     this.handleSwordAnim(matrices, swingProgress, equipProgress, arm);
                  } else {
                     float n = -0.4F * Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
                     float mxx = 0.2F * Mth.sin(Mth.sqrt(swingProgress) * (float) (Math.PI * 2));
                     float fxxx = -0.2F * Mth.sin(swingProgress * (float) Math.PI);
                     int o = isRightArm ? 1 : -1;
                     matrices.translate(o * n, mxx, fxxx);
                     this.applyEquipOffset(matrices, arm, equipProgress);
                     this.applySwingOffset(matrices, arm, swingProgress);
                  }

                  this.applyHandPositionItem(matrices, arm);
                  this.renderItem(player, item, isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !isRightArm, matrices, vertexConsumers, light);
               }

               matrices.popPose();
            }
         } finally {
            renderingCustomItem = false;
         }
      }
   }

   private void applyBrushTransformation(PoseStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack, float equipProgress) {
      Minecraft mc = Minecraft.getInstance();
      this.applyEquipOffset(matrices, arm, equipProgress);
      float f = mc.player.getUseItemRemainingTicks() % 10;
      float g = f - tickDelta + 1.0F;
      float h = 1.0F - g / 10.0F;
      float n = -15.0F + 75.0F * Mth.cos(h * 2.0F * (float) Math.PI);
      if (arm != HumanoidArm.RIGHT) {
         matrices.translate(0.1, 0.83, 0.35);
         matrices.rotate(Axis.XP.rotationDegrees(-80.0F));
         matrices.rotate(Axis.YP.rotationDegrees(-90.0F));
         matrices.rotate(Axis.XP.rotationDegrees(n));
         matrices.translate(-0.3, 0.22, 0.35);
      } else {
         matrices.translate(-0.25, 0.22, 0.35);
         matrices.rotate(Axis.XP.rotationDegrees(-80.0F));
         matrices.rotate(Axis.YP.rotationDegrees(90.0F));
         matrices.rotate(Axis.ZP.rotationDegrees(0.0F));
         matrices.rotate(Axis.XP.rotationDegrees(n));
      }
   }

   private void applyEatOrDrinkTransformation(PoseStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack) {
      Minecraft mc = Minecraft.getInstance();
      float f = mc.player.getUseItemRemainingTicks() - tickDelta + 1.0F;
      float g = f / stack.getUseDuration(mc.player);
      if (g < 0.8F) {
         float h = Mth.abs(Mth.cos(f / 4.0F * (float) Math.PI) * 0.1F);
         matrices.translate(0.0F, h, 0.0F);
      }

      float h = 1.0F - (float)Math.pow(g, 27.0);
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      matrices.translate(h * 0.6F * i, h * -0.5F, h * 0.0F);
      matrices.rotate(Axis.YP.rotationDegrees(i * h * 90.0F));
      matrices.rotate(Axis.XP.rotationDegrees(h * 10.0F));
      matrices.rotate(Axis.ZP.rotationDegrees(i * h * 30.0F));
   }

   private void applyEquipOffset(PoseStack matrices, HumanoidArm arm, float equipProgress) {
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      matrices.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
   }

   private void applySwingOffset(PoseStack matrices, HumanoidArm arm, float swingProgress) {
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
      float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
      matrices.rotate(Axis.YP.rotationDegrees(i * (45.0F + f * -20.0F)));
      matrices.rotate(Axis.ZP.rotationDegrees(i * g * -20.0F));
      matrices.rotate(Axis.XP.rotationDegrees(g * -80.0F));
      matrices.rotate(Axis.YP.rotationDegrees(i * -45.0F));
   }

   public void renderItem(
      LivingEntity entity, ItemStack stack, ItemDisplayContext renderMode, boolean leftHanded, PoseStack matrices, SubmitNodeCollector collector, int light
   ) {
      if (!stack.isEmpty()) {
         if (currentItemState != null) {
            currentItemState.submit(matrices, collector, light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
         }
      }
   }

   public boolean auraCheck() {
      return true;
   }

   public float getRightX() {
      return this.getFloat(this.rightX);
   }

   public float getRightY() {
      return this.getFloat(this.rightY);
   }

   public float getRightZ() {
      return this.getFloat(this.rightZ);
   }

   public float getLeftX() {
      return this.getFloat(this.leftX);
   }

   public float getLeftY() {
      return this.getFloat(this.leftY);
   }

   public float getLeftZ() {
      return this.getFloat(this.leftZ);
   }

   public boolean isHoldMyItemsEnabled() {
      return !this.isEnabled() ? false : "HMI".equals(String.valueOf(this.mode.get()));
   }

   public void updatePhysics(LocalPlayer player, float tickDelta, float attackAnim) {
      if (this.isEnabled() && this.isHoldMyItemsEnabled()) {
         double currentTime = System.nanoTime() / 1.0E9;
         this.holdMyItemsDeltaTime = Math.min(0.05, Math.max(0.0, currentTime - this.holdMyItemsPrevFrameTime));
         this.holdMyItemsPrevFrameTime = currentTime;
         this.holdMyItemsPhysicsUpdatedThisFrame = false;
         float f = attackAnim;
         if (f > 0.0F && this.holdMyItemsPrevSwingProgress == 0.0F) {
            this.holdMyItemsLeft = !this.holdMyItemsLeft;
         }

         this.holdMyItemsPrevSwingProgress = f;
      }
   }

   private boolean shouldUseHoldMyItemsCustom(AbstractClientPlayer player, InteractionHand handIn, ItemStack stack) {
      return !this.isHoldMyItemsEnabled()
         ? false
         : !(stack.getItem() instanceof MapItem)
            && !(stack.getItem() instanceof CrossbowItem)
            && (!player.isUsingItem() || player.getUsedItemHand() != handIn)
            && !player.isAutoSpinAttack();
   }

   private boolean shouldUseHoldMyItemsBow(AbstractClientPlayer player, InteractionHand handIn, ItemStack stack) {
      return !this.isHoldMyItemsEnabled() ? false : stack.getUseAnimation() == ItemUseAnimation.BOW && player.isUsingItem() && player.getUsedItemHand() == handIn;
   }

   private boolean shouldUseHoldMyItemsConsume(AbstractClientPlayer player, InteractionHand handIn, ItemStack stack) {
      if (!this.isHoldMyItemsEnabled()) {
         return false;
      }

      ItemStack consumeStack = player.getUseItem() != null && !player.getUseItem().isEmpty() && player.getUsedItemHand() == handIn
         ? player.getUseItem()
         : stack;
      ItemUseAnimation action = consumeStack.getUseAnimation();
      return (action == ItemUseAnimation.EAT || action == ItemUseAnimation.DRINK) && player.isUsingItem() && player.getUsedItemHand() == handIn;
   }

   private void updateChestRightHandMotion() {
      Minecraft mc = Minecraft.getInstance();
      float target = mc.gui.screen() instanceof AbstractContainerScreen && !(mc.gui.screen() instanceof InventoryScreen) ? 1.0F : 0.0F;
      this.chestRightHandMotion = Mth.lerp(0.18F, this.chestRightHandMotion, target);
   }

   private float getHoldMyItemsAttackDamage(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0.0F;
      }

      String name = stack.getItem().toString().toLowerCase();
      if (name.contains("sword")) {
         if (name.contains("netherite")) {
            return 8.0F;
         } else if (name.contains("diamond")) {
            return 7.0F;
         } else if (name.contains("iron")) {
            return 6.0F;
         } else {
            return name.contains("stone") ? 5.0F : 4.0F;
         }
      } else if (name.contains("axe")) {
         return !name.contains("netherite") && !name.contains("diamond") && !name.contains("iron") && !name.contains("stone") ? 7.0F : 9.0F;
      } else {
         return 0.0F;
      }
   }

   private boolean isHoldMyItemsCrawling(AbstractClientPlayer player) {
      return player.isVisuallySwimming() && !player.isInWater();
   }

   private boolean isHoldMyItemsClimbing(AbstractClientPlayer player) {
      return player.onClimbable() && !player.onGround() && Math.abs(player.getDeltaMovement().y) > 0.0;
   }

   private boolean isHoldMyItemsWeapon(ItemStack stack) {
      return stack.is(net.minecraft.tags.ItemTags.SWORDS) || stack.is(net.minecraft.tags.ItemTags.AXES);
   }

   private boolean isHoldMyItemsTool(ItemStack stack) {
      return (stack.is(net.minecraft.tags.ItemTags.PICKAXES) || stack.is(net.minecraft.tags.ItemTags.SHOVELS) || stack.is(net.minecraft.tags.ItemTags.HOES)) || stack.getItem() instanceof ShearsItem || stack.getItem() instanceof TridentItem;
   }

   private boolean isHoldMyItemsShovel(ItemStack stack) {
      return stack.is(net.minecraft.tags.ItemTags.SHOVELS);
   }

   private boolean isHoldMyItemsLantern(ItemStack stack) {
      return stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN);
   }

   private boolean isHoldMyItemsThinBlock(ItemStack stack) {
      if (!(stack.getItem() instanceof BlockItem)) {
         return false;
      }

      Block block = ((BlockItem)stack.getItem()).getBlock();
      return stack.is(Items.STRING)
         || stack.is(Items.REDSTONE)
         || stack.is(Items.LEVER)
         || stack.is(Items.TRIPWIRE_HOOK)
         || block instanceof IronBarsBlock
         || block.defaultBlockState().is(BlockTags.RAILS)
         || block.defaultBlockState().is(BlockTags.CLIMBABLE)
         || block.defaultBlockState().is(BlockTags.DOORS);
   }

   private boolean isHoldMyItemsTorch(ItemStack stack) {
      String name = stack.getHoverName().getString().toLowerCase();
      return name.contains("torch") || name.contains("факел");
   }

   private boolean isHoldMyItemsSmallItem(ItemStack stack) {
      return !(stack.getItem() instanceof BlockItem)
         && !this.isHoldMyItemsTool(stack)
         && !this.isHoldMyItemsWeapon(stack)
         && !(stack.getItem() instanceof FishingRodItem)
         && !(stack.getItem() instanceof BucketItem)
         && stack.getUseAnimation() != ItemUseAnimation.BOW
         && stack.getUseAnimation() != ItemUseAnimation.SPEAR
         && stack.getUseAnimation() != ItemUseAnimation.BLOCK;
   }

   private float holdMyItemsEase(float value) {
      float c1 = 1.70158F;
      float c2 = c1 * 1.525F;
      if (value < 0.5F) {
         float doubled = 2.0F * value;
         return doubled * doubled * ((c2 + 1.0F) * doubled - c2) * 0.5F;
      } else {
         float shifted = 2.0F * value - 2.0F;
         return (shifted * shifted * ((c2 + 1.0F) * shifted + c2) + 2.0F) * 0.5F;
      }
   }

   private float getHoldMyItemsSwingRot(float swingProgress) {
      return swingProgress < 0.6F
         ? Mth.sin(Mth.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F)
         : Mth.sin(Mth.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
   }

   private void applyHoldMyItemsBaseHandPose(PoseStack matrices, HumanoidArm arm, float equippedProgress, float swingProgress) {
      int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
      float swingSin = Mth.sin(swingProgress * (float) Math.PI);
      matrices.translate(direction, -equippedProgress * 0.3, 0.3);
      matrices.rotate(Axis.YP.rotationDegrees(45.0F * direction));
      matrices.rotate(Axis.ZP.rotationDegrees(-40.0F * direction));
      matrices.rotate(Axis.XP.rotationDegrees(30.0F));
      matrices.rotate(Axis.YP.rotationDegrees(direction * (45.0F + swingSin * 0.0F)));
      matrices.rotate(Axis.YP.rotationDegrees(direction * -45.0F));
      matrices.scale(0.9F, 0.9F, 0.9F);
   }

   private void applyHoldMyItemsArmPrePose(PoseStack matrices, ItemStack stack, HumanoidArm arm) {
      int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
      if (this.isHoldMyItemsLantern(stack)) {
         matrices.translate(0.1 * direction, 0.0, -0.1);
         matrices.rotate(Axis.XP.rotationDegrees(10.0F));
      } else {
         if (stack.getUseAnimation() == ItemUseAnimation.BLOCK) {
            matrices.translate(0.0, -0.2, 0.0);
         }
      }
   }

   private void applyHoldMyItemsEnvironment(
      PoseStack matrices, AbstractClientPlayer player, InteractionHand handIn, HumanoidArm arm, ItemStack stack, float swingProgress, float partialTicks
   ) {
      float yaw = Mth.lerp(partialTicks, player.yRotO, player.getYRot());
      double radians = Math.toRadians(yaw);
      double forwardX = -Math.sin(radians);
      double forwardZ = Math.cos(radians);
      Vec3 velocity = player.getDeltaMovement();
      double dotProduct = velocity.x * forwardX + velocity.z * forwardZ;
      double crossProduct = velocity.x * forwardZ - velocity.z * forwardX;
      float pitchFactor = player.getXRot() != 0.0F ? 90.0F / player.getXRot() / 10.0F : 1.0F;
      if (pitchFactor > 1.0F || pitchFactor < 0.0F) {
         pitchFactor = 1.0F;
      }

      boolean crawling = this.isHoldMyItemsCrawling(player);
      boolean climbing = this.isHoldMyItemsClimbing(player);
      boolean elytraFlying = player.isFallFlying();
      double tt = this.holdMyItemsDeltaTime * 30.0;
      float handDirection = handIn == InteractionHand.MAIN_HAND ? 1.0F : -1.0F;
      int armDirection = arm == HumanoidArm.RIGHT ? 1 : -1;
      if (elytraFlying) {
         if (!this.holdMyItemsPhysicsUpdatedThisFrame) {
            this.holdMyItemsClimbBlend = 0.0F;
            this.holdMyItemsInWaterCounter = 0.0F;
            this.holdMyItemsVertAngleY = this.holdMyItemsVertAngleY * (float)Math.pow(0.72, tt);
            this.holdMyItemsVertVelocityYSlime = this.holdMyItemsVertVelocityYSlime * (float)Math.pow(0.72, tt);
            this.holdMyItemsVertAngleYSlime = this.holdMyItemsVertAngleYSlime * (float)Math.pow(0.72, tt);
            this.holdMyItemsPhysicsUpdatedThisFrame = true;
         }

         if (!stack.isEmpty() && stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
            matrices.translate(0.0, -0.1, 0.1);
         }

         if (this.isHoldMyItemsLantern(stack)) {
            matrices.translate(0.0, 0.1, 0.0);
         }
      } else {
         if (!this.holdMyItemsPhysicsUpdatedThisFrame) {
            double speed = velocity.length();
            if (speed >= 0.08) {
               double clampedSpeed = Math.min(speed, 0.22);
               double clampedDot = Mth.clamp(dotProduct, -0.22, 0.22);
               double clampedCross = Mth.clamp(crossProduct, -0.22, 0.22);
               this.holdMyItemsCrawlCount = (float)(this.holdMyItemsCrawlCount + 0.1 * clampedSpeed * 2.0 * tt);
               this.holdMyItemsDirectionalCrawlCount = (float)(this.holdMyItemsDirectionalCrawlCount + 0.1 * clampedDot * 4.0 * tt);
               this.holdMyItemsDirectionalCrawlCount = (float)(
                  this.holdMyItemsDirectionalCrawlCount
                     + (clampedDot > 0.0 ? 0.1 * Math.abs(clampedCross) * 4.0 * tt : 0.1 * Math.abs(clampedCross) * -4.0 * tt)
               );
            }

            if (velocity.y > 0.0) {
               this.holdMyItemsClimbCount = (float)(this.holdMyItemsClimbCount + 0.1 * tt);
            }

            if (velocity.y < 0.0) {
               this.holdMyItemsClimbCount = (float)(this.holdMyItemsClimbCount - 0.1 * tt);
            }

            float motionYNormalized = player.onGround() ? 0.0F : (float)Mth.clamp(velocity.y, -0.42, 0.42);
            this.holdMyItemsVertAngleY = (float)(this.holdMyItemsVertAngleY + motionYNormalized * 0.015 * tt);
            this.holdMyItemsVertAngleY = (float)(this.holdMyItemsVertAngleY - 0.1 * this.holdMyItemsVertAngleY * tt);
            this.holdMyItemsVertAngleY = (float)(this.holdMyItemsVertAngleY * Math.pow(0.88, tt));
            this.holdMyItemsVertVelocityYSlime = (float)(this.holdMyItemsVertVelocityYSlime + motionYNormalized * 0.015 * tt);
            this.holdMyItemsVertVelocityYSlime = (float)(this.holdMyItemsVertVelocityYSlime - 0.1 * this.holdMyItemsVertAngleYSlime * tt);
            this.holdMyItemsVertVelocityYSlime = (float)(this.holdMyItemsVertVelocityYSlime * Math.pow(0.88, tt));
            this.holdMyItemsVertAngleYSlime = (float)(this.holdMyItemsVertAngleYSlime + this.holdMyItemsVertVelocityYSlime * tt);
            if (player.isInWater() && !player.isUnderWater()) {
               this.holdMyItemsInWaterCounter = (float)(this.holdMyItemsInWaterCounter + 0.1 * tt);
               if (this.holdMyItemsInWaterCounter > 1.0F) {
                  this.holdMyItemsInWaterCounter = 1.0F;
               }
            } else {
               this.holdMyItemsInWaterCounter = (float)(this.holdMyItemsInWaterCounter * Math.pow(0.88, tt));
            }

            this.holdMyItemsPhysicsUpdatedThisFrame = true;
         }

         if ((crawling || climbing) && (!player.isUsingItem() || player.getUsedItemHand() != handIn) && swingProgress == 0.0F) {
            this.holdMyItemsClimbBlend = (float)(this.holdMyItemsClimbBlend + 0.1 * tt);
            if (this.holdMyItemsClimbBlend > 1.0F) {
               this.holdMyItemsClimbBlend = 1.0F;
            }

            if (!this.isHoldMyItemsLantern(stack)) {
               matrices.rotate(Axis.XP.rotationDegrees(-20.0F * this.holdMyItemsClimbBlend));
            }
         } else {
            this.holdMyItemsClimbBlend = (float)(this.holdMyItemsClimbBlend * Math.pow(0.88, tt));
         }

         if (swingProgress == 0.0F) {
            float pitch = player.getXRot();
            matrices.translate(
               handDirection > 0.0F ? pitch / 650.0F * this.holdMyItemsClimbBlend * -1.0F : pitch / 650.0F * this.holdMyItemsClimbBlend, 0.0F, 0.0F
            );
            matrices.rotate(Axis.XP.rotationDegrees(pitch * this.holdMyItemsClimbBlend));
         }

         if (!this.isHoldMyItemsLantern(stack)) {
            matrices.translate(0.0, 0.0, player.getXRot() / 120.0F * this.holdMyItemsClimbBlend);
         } else if (swingProgress == 0.0F) {
            matrices.translate(0.0, 0.0, player.getXRot() / 80.0F * this.holdMyItemsClimbBlend);
         }

         if (climbing && !this.isHoldMyItemsLantern(stack) && (!player.isUsingItem() || player.getUsedItemHand() != handIn)) {
            matrices.translate(0.0, 0.1, -0.2);
         }

         matrices.translate(0.0, 0.02 * this.holdMyItemsInWaterCounter, 0.0);
         matrices.rotate(Axis.ZP.rotationDegrees(8.0F * handDirection * this.holdMyItemsInWaterCounter));
         matrices.translate(0.0, -this.holdMyItemsVertAngleY, 0.0);
         matrices.translate(0.0, Math.sin(player.tickCount * 0.1) * 0.007 * armDirection, 0.0);
         matrices.rotate(Axis.YP.rotationDegrees(0.15F * (float)Math.sin(player.tickCount * 0.15F) * armDirection));
         if ((!stack.isEmpty() || crawling || climbing || player.isUnderWater()) && stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
            matrices.translate(0.0, -0.1, 0.1);
         }

         if (this.isHoldMyItemsLantern(stack)) {
            matrices.translate(0.0, 0.1, 0.0);
            if (player.isUnderWater()) {
               matrices.translate(0.0, -0.1, 0.1);
            }
         }

         if (player.isUnderWater() && swingProgress == 0.0F) {
            double distance = (player.tickCount + partialTicks) * 0.2;
            double handRotation = Math.sin(distance) * 1.5;
            double smoothRotation = handRotation * 0.8 + this.holdMyItemsPreviousRotation * 0.2;
            matrices.rotate(Axis.YP.rotationDegrees((float)(handIn == InteractionHand.MAIN_HAND ? smoothRotation : -smoothRotation)));
            matrices.translate(0.0, 0.0, smoothRotation * 0.2);
            this.holdMyItemsPreviousRotation = smoothRotation;
         }

         if ((climbing || crawling) && (!player.isUsingItem() || player.getUsedItemHand() != handIn) && swingProgress == 0.0F) {
            float crawlProgress = Mth.sin(this.holdMyItemsDirectionalCrawlCount * 4.0F);
            float upAndDown = Mth.cos(this.holdMyItemsDirectionalCrawlCount * 4.0F);
            if (this.isHoldMyItemsLantern(stack)) {
               crawlProgress *= 0.14F;
               upAndDown *= 0.14F;
            }

            matrices.translate(0.2 * crawlProgress, 0.3 * crawlProgress * armDirection, -0.2 * crawlProgress * armDirection * pitchFactor);
            matrices.rotate(Axis.YP.rotationDegrees(25.0F * crawlProgress));
            matrices.rotate(Axis.XP.rotationDegrees(Mth.clamp(20.0F * upAndDown * armDirection, 0.0F, 20.0F)));
         }
      }
   }

   private void applyHoldMyItemsLanternPose(PoseStack matrices, AbstractClientPlayer player, HumanoidArm arm, float swingProgress) {
      float dt = (float)(this.holdMyItemsDeltaTime * 30.0);
      int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
      float yawDelta = player.yHeadRotO - player.yHeadRot;
      float pitchDelta = player.xRotO - player.getXRot();
      this.holdMyItemsSwingVelocityY += yawDelta * 0.015F * dt;
      this.holdMyItemsSwingVelocityY += swingProgress * 2.0F * dt;
      this.holdMyItemsSwingVelocityX += pitchDelta * 0.015F * dt;
      this.holdMyItemsSwingVelocityY = this.holdMyItemsSwingVelocityY - 0.1F * this.holdMyItemsSwingAngleY * dt;
      this.holdMyItemsSwingVelocityX = this.holdMyItemsSwingVelocityX - 0.1F * this.holdMyItemsSwingAngleX * dt;
      this.holdMyItemsSwingVelocityY = (float)(this.holdMyItemsSwingVelocityY * Math.pow(0.88, dt));
      this.holdMyItemsSwingVelocityX = (float)(this.holdMyItemsSwingVelocityX * Math.pow(0.88, dt));
      this.holdMyItemsSwingAngleY = this.holdMyItemsSwingAngleY + this.holdMyItemsSwingVelocityY * dt;
      this.holdMyItemsSwingAngleX = this.holdMyItemsSwingAngleX + this.holdMyItemsSwingVelocityX * dt;
      double currentSpeed = player.getDeltaMovement().length();
      this.holdMyItemsSwingVelocityZ = (float)(
         this.holdMyItemsSwingVelocityZ
            + (
               direction > 0
                  ? (currentSpeed * -15.0 - this.holdMyItemsSwingVelocityZ) * 0.1 * dt
                  : (currentSpeed * 15.0 - this.holdMyItemsSwingVelocityZ) * 0.1 * dt
            )
      );
      if (currentSpeed > 0.09
         && (player.onGround() || player.isUnderWater() || this.isHoldMyItemsClimbing(player))
         && (Boolean)Minecraft.getInstance().options.bobView().get()) {
         this.holdMyItemsSwingVelocityY = this.holdMyItemsSwingVelocityY + (float)((Math.random() < 0.5 ? -5.5 : 5.5) * currentSpeed * dt);
      }

      matrices.translate(0.0, 0.0, -0.1);
      matrices.rotate(Axis.YN.rotationDegrees(35.0F * direction + this.holdMyItemsSwingAngleY));
      matrices.rotate(Axis.XP.rotationDegrees(15.0F + this.holdMyItemsSwingAngleX));
      matrices.rotate(Axis.ZP.rotationDegrees(75.0F * direction + this.holdMyItemsSwingVelocityZ));
      matrices.translate(0.3 * direction, -0.35, 0.0);
      matrices.translate(0.0, 0.0, 0.1);
      matrices.scale(1.5F, 1.5F, 1.5F);
   }

   private void applyHoldMyItemsItemPose(PoseStack matrices, AbstractClientPlayer player, InteractionHand handIn, HumanoidArm arm, ItemStack stack, float swingProgress) {
      int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
      boolean mainHand = handIn == InteractionHand.MAIN_HAND;
      if (player.getMainArm() == HumanoidArm.LEFT) {
         mainHand = !mainHand;
      }

      matrices.translate(-0.3 * direction, 0.65, -0.1);
      matrices.rotate(Axis.YP.rotationDegrees(-65.0F * direction));
      matrices.rotate(Axis.XP.rotationDegrees(10.0F));
      if (stack.getItem() instanceof BlockItem && !(stack.getItem() instanceof BucketItem) && stack.getUseAnimation() != ItemUseAnimation.EAT) {
         Block block = ((BlockItem)stack.getItem()).getBlock();
         if (block instanceof AbstractSkullBlock) {
            matrices.translate(0.1 * direction, 0.15, 0.1);
            matrices.scale(0.7F, 0.7F, 0.7F);
            matrices.rotate(Axis.YP.rotationDegrees(245.0F * direction));
            matrices.rotate(Axis.XP.rotationDegrees(25.0F));
            matrices.rotate(Axis.ZP.rotationDegrees(-15.0F * direction));
         } else if (this.isHoldMyItemsTorch(stack)) {
            matrices.scale(1.5F, 1.5F, 1.5F);
            matrices.rotate(Axis.YN.rotationDegrees(25.0F * direction));
            matrices.rotate(Axis.XP.rotationDegrees(5.0F));
            matrices.rotate(Axis.ZP.rotationDegrees(75.0F * direction));
            matrices.translate(0.2 * direction, 0.2, 0.05);
         } else if (this.isHoldMyItemsThinBlock(stack)) {
            matrices.translate(0.0, 0.0, -0.1);
            matrices.rotate(Axis.YN.rotationDegrees(5.0F * direction));
            matrices.rotate(Axis.XP.rotationDegrees(15.0F));
            matrices.rotate(Axis.ZP.rotationDegrees(75.0F * direction));
         } else if (this.isHoldMyItemsLantern(stack)) {
            this.applyHoldMyItemsLanternPose(matrices, player, arm, swingProgress);
         } else {
            matrices.rotate(Axis.YN.rotationDegrees(25.0F * direction));
            matrices.rotate(Axis.XP.rotationDegrees(5.0F));
            matrices.rotate(Axis.ZP.rotationDegrees(75.0F * direction));
            matrices.translate(0.2 * direction, 0.2, 0.05);
         }
      } else if (this.isHoldMyItemsSmallItem(stack) && this.getHoldMyItemsAttackDamage(stack) == 0.0F) {
         matrices.rotate(Axis.YN.rotationDegrees(5.0F * direction));
         matrices.rotate(Axis.XP.rotationDegrees(15.0F));
         matrices.rotate(Axis.ZP.rotationDegrees(75.0F * direction));
         matrices.translate(0.0, -0.05, -0.1);
         matrices.scale(0.7F, 0.7F, 0.7F);
      } else if (stack.getUseAnimation() == ItemUseAnimation.BLOCK && stack.getUseAnimation() != ItemUseAnimation.SPEAR) {
         matrices.rotate(Axis.ZP.rotationDegrees(160.0F * direction));
         matrices.rotate(Axis.YP.rotationDegrees(-60.0F * direction));
         matrices.rotate(Axis.XP.rotationDegrees(-70.0F));
         matrices.scale(0.75F, 0.75F, 0.75F);
         matrices.translate(0.15 * direction, mainHand ? 0.35 : 0.45, mainHand ? -0.15 : -0.1);
         matrices.translate(0.17 * direction, 0.0, 0.3);
         matrices.rotate(Axis.YP.rotationDegrees(-90.0F * direction));
      } else if (stack.getUseAnimation() == ItemUseAnimation.SPEAR) {
         matrices.rotate(Axis.YN.rotationDegrees(75.0F * direction));
         matrices.rotate(Axis.XP.rotationDegrees(90.0F));
         matrices.rotate(Axis.ZP.rotationDegrees(45.0F * direction));
         matrices.translate(-0.3 * direction, 0.0, 0.0);
      } else {
         matrices.rotate(Axis.YN.rotationDegrees(75.0F * direction));
         matrices.rotate(Axis.XP.rotationDegrees(70.0F));
         matrices.rotate(Axis.ZP.rotationDegrees(45.0F * direction));
         if (stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
            matrices.scale(1.2F, 1.2F, 1.2F);
         }

         if (stack.getUseAnimation() == ItemUseAnimation.BOW && !player.isUsingItem()) {
            matrices.translate(-0.1 * direction, -0.2, 0.0);
         }
      }
   }

   private void applyHoldMyItemsGenericSwing(PoseStack matrices, float direction, float swingRot, float swing) {
      matrices.translate(0.1 * direction * swingRot, 0.1 * swingRot, -0.1 * swing);
      matrices.rotate(Axis.XN.rotationDegrees(-30.0F * swingRot));
      matrices.rotate(Axis.ZP.rotationDegrees(-10.0F * swingRot * direction));
      matrices.rotate(Axis.XN.rotationDegrees(40.0F * swing));
      matrices.rotate(Axis.YP.rotationDegrees(10.0F * swing * direction));
   }

   private void applyHoldMyItemsSwing(PoseStack matrices, AbstractClientPlayer player, InteractionHand handIn, ItemStack stack, float swingProgress) {
      boolean mainHand = handIn == InteractionHand.MAIN_HAND;
      if (player.getMainArm() == HumanoidArm.LEFT) {
         mainHand = !mainHand;
      }

      boolean hasAuraTarget = true;
      float ll = mainHand ? 1.0F : -1.0F;
      float handDirection = handIn == InteractionHand.MAIN_HAND ? 1.0F : -1.0F;
      float swingRot = this.getHoldMyItemsSwingRot(swingProgress);
      float swing = this.holdMyItemsEase(Mth.sin(swingProgress * (float) Math.PI));
      String currentAttackMode = this.attackMode.get();
      boolean forwardHandsAttack = "Forward".equals(currentAttackMode) && hasAuraTarget;
      boolean normalHandsAttack = "Normal".equals(currentAttackMode) && hasAuraTarget;
      if (stack.is(net.minecraft.tags.ItemTags.SWORDS) && forwardHandsAttack) {
         matrices.translate(0.12 * ll * swingRot, 0.04 * swingRot, -0.95 * swing);
         matrices.translate(0.02 * ll * swing, 0.1 * swing, -0.1 * swingRot);
         matrices.rotate(Axis.YP.rotationDegrees(8.0F * swingRot * ll));
         matrices.rotate(Axis.XN.rotationDegrees(-14.0F * swingRot));
         matrices.rotate(Axis.ZP.rotationDegrees(-18.0F * swingRot * ll));
         matrices.rotate(Axis.XN.rotationDegrees(32.0F * swing));
      } else if (stack.is(net.minecraft.tags.ItemTags.SWORDS) && normalHandsAttack) {
         this.applyHoldMyItemsGenericSwing(matrices, ll, swingRot, swing);
      } else if ((
            this.holdMyItemsLeft
               || stack.is(net.minecraft.tags.ItemTags.AXES)
               || stack.getUseAnimation() == ItemUseAnimation.SPEAR
               || stack.getUseAnimation() == ItemUseAnimation.BLOCK
         )
         && !this.isHoldMyItemsShovel(stack)) {
         if (this.isHoldMyItemsWeapon(stack)) {
            matrices.translate(0.8 * ll * swingRot, 0.3 * swingRot, -0.5 * swing);
            matrices.rotate(Axis.YP.rotationDegrees(15.0F * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(-20.0F * swingRot));
            matrices.rotate(Axis.ZP.rotationDegrees(-70.0F * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees((stack.is(net.minecraft.tags.ItemTags.SWORDS) ? 40.0F : 30.0F) * swing));
         } else if (stack.getUseAnimation() == ItemUseAnimation.SPEAR) {
            matrices.translate(0.0, 0.0, 0.45 * swingRot);
            matrices.translate(-0.25 * handDirection * swing, -0.35 * swingRot, -0.6 * swing);
            matrices.translate(0.0, 0.1 * swing, 0.0);
            matrices.rotate(Axis.YP.rotationDegrees(15.0F * swingRot * ll));
            matrices.rotate(Axis.ZP.rotationDegrees(30.0F * swingRot * ll));
         } else if (this.isHoldMyItemsTool(stack) && stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
            matrices.translate(0.1 * ll * swingRot, 0.1 * swingRot, -0.5 * swing);
            matrices.rotate(Axis.XN.rotationDegrees(-30.0F * swingRot));
            matrices.rotate(Axis.ZP.rotationDegrees(-20.0F * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(40.0F * swing));
         } else if (stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
            matrices.translate(0.1 * ll * swingRot, 0.1 * swingRot, -0.1 * swing);
            matrices.rotate(Axis.XN.rotationDegrees(-30.0F * swingRot));
            matrices.rotate(Axis.ZP.rotationDegrees(-10.0F * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(40.0F * swing));
            matrices.rotate(Axis.YP.rotationDegrees(10.0F * swing * ll));
         } else {
            matrices.translate(0.1 * ll * swingRot, 0.1 * swingRot, -0.2 * swing);
            matrices.rotate(Axis.XN.rotationDegrees(-10.0F * swingRot));
            matrices.rotate(Axis.ZP.rotationDegrees(-10.0F * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(20.0F * swing));
         }
      } else if (this.isHoldMyItemsShovel(stack)) {
         matrices.translate(0.0, 0.15 * swingRot, -0.25 * swingRot);
         matrices.translate(0.0, 0.0, -0.2 * swing);
         matrices.rotate(Axis.YP.rotationDegrees(15.0F * swingRot));
         matrices.rotate(Axis.XP.rotationDegrees(-35.0F * swingRot));
         matrices.rotate(Axis.XP.rotationDegrees(30.0F * swing));
      } else if (stack.is(net.minecraft.tags.ItemTags.SWORDS)) {
         matrices.translate(-0.55 * ll * swingRot, -0.8 * swingRot, -0.77 * swing);
         matrices.rotate(Axis.YP.rotationDegrees(5.0F * swingRot * ll));
         matrices.rotate(Axis.XN.rotationDegrees(-30.0F * swingRot));
         matrices.rotate(Axis.ZP.rotationDegrees(70.0F * swingRot * ll));
         matrices.rotate(Axis.XN.rotationDegrees(50.0F * swing));
      } else if (this.isHoldMyItemsTool(stack)) {
         matrices.translate(0.1 * ll * swingRot, 0.1 * swingRot, -0.5 * swing);
         matrices.rotate(Axis.XN.rotationDegrees(-30.0F * swingRot));
         matrices.rotate(Axis.ZP.rotationDegrees(-20.0F * swingRot * ll));
         matrices.rotate(Axis.XN.rotationDegrees(40.0F * swing));
      } else {
         this.applyHoldMyItemsGenericSwing(matrices, ll, swingRot, swing);
      }
   }

   private void applyChestRightHandMotion(PoseStack matrices, HumanoidArm arm) {
      if (arm == HumanoidArm.RIGHT && !(this.chestRightHandMotion <= 0.001F)) {
         float progress = this.chestRightHandMotion;
         float time = (float)(System.currentTimeMillis() % 1200L) / 1200.0F;
         float pulse = Mth.sin(time * (float) (Math.PI * 2)) * progress;
         matrices.translate(0.04 * progress, -0.03 * progress + 0.01 * pulse, -0.12 * progress);
         matrices.rotate(Axis.YN.rotationDegrees(12.0F * progress));
         matrices.rotate(Axis.XP.rotationDegrees(8.0F * progress + 2.5F * pulse));
         matrices.rotate(Axis.ZP.rotationDegrees(-4.0F * progress));
      }
   }

   private void applyHoldMyItemsUseJitter(PoseStack matrices, float useTicks, float progress) {
      if (!(progress <= 0.1F)) {
         float pulse = Mth.sin((useTicks - 0.1F) * 1.3F);
         float offset = pulse * (progress - 0.1F);
         matrices.translate(0.0, offset * 0.004, 0.0);
      }
   }

   private void renderHMIBow(
      AbstractClientPlayer player,
      float tickDelta,
      InteractionHand handIn,
      float swingProgress,
      ItemStack stack,
      float equippedProgress,
      PoseStack matrices,
      SubmitNodeCollector vertexConsumers,
      int light
   ) {
      boolean isMainHand = handIn == InteractionHand.MAIN_HAND;
      HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      boolean rightHand = arm == HumanoidArm.RIGHT;
      int handDirection = rightHand ? 1 : -1;
      float useTicks = stack.getUseDuration(player) - (player.getUseItemRemainingTicks() - tickDelta + 1.0F);
      float drawLinear = Mth.clamp(useTicks / 20.0F, 0.0F, 1.0F);
      matrices.pushPose();
      this.applyHandPositionBase(matrices, arm);
      this.applyHoldMyItemsEnvironment(matrices, player, handIn, arm, stack, swingProgress, tickDelta);
      matrices.pushPose();
      this.applyHoldMyItemsUseJitter(matrices, useTicks, drawLinear);
      matrices.translate(rightHand ? -0.1 : 0.1, 0.0, drawLinear * 0.15);
      FirstPersonHandsAndItemsRenderer heldItemRenderer = Minecraft.getInstance().gameRenderer.firstPersonHandsAndItemsRenderer;
      if (heldItemRenderer instanceof HeldItemRendererAccessor) {
         ((HeldItemRendererAccessor)heldItemRenderer).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equippedProgress, swingProgress, arm, currentPlayerState);
      }

      matrices.popPose();
      matrices.pushPose();
      matrices.translate(rightHand ? -0.5 : 0.5, -0.45, 0.1);
      matrices.rotate(Axis.XP.rotation(0.3F));
      if (rightHand) {
         matrices.rotate(Axis.ZN.rotation(-0.3F));
         matrices.rotate(Axis.YN.rotation(1.0F));
         if (heldItemRenderer instanceof HeldItemRendererAccessor) {
            ((HeldItemRendererAccessor)heldItemRenderer)
               .invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equippedProgress, swingProgress, arm.getOpposite(), currentPlayerState);
         }

         matrices.rotate(Axis.YN.rotation(2.5F));
      } else {
         matrices.rotate(Axis.ZP.rotation(-0.3F));
         matrices.rotate(Axis.YP.rotation(1.0F));
         if (heldItemRenderer instanceof HeldItemRendererAccessor) {
            ((HeldItemRendererAccessor)heldItemRenderer)
               .invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equippedProgress, swingProgress, arm.getOpposite(), currentPlayerState);
         }

         matrices.rotate(Axis.YP.rotation(2.5F));
      }

      matrices.translate(rightHand ? -0.65 : 0.65, -0.35, 0.27);
      matrices.popPose();
      matrices.rotate(Axis.XN.rotationDegrees(75.0F));
      matrices.rotate(Axis.ZN.rotationDegrees(-15.0F * handDirection));
      matrices.translate(0.8 * handDirection, -equippedProgress * 0.3, -0.1);
      this.applyHoldMyItemsUseJitter(matrices, useTicks, drawLinear);
      this.applyHoldMyItemsItemPose(matrices, player, handIn, arm, stack, swingProgress);
      this.applyHandPositionItem(matrices, arm);
      this.renderItem(player, stack, rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !rightHand, matrices, vertexConsumers, light);
      matrices.popPose();
      this.holdMyItemsIsAttacking = Minecraft.getInstance().options.keyAttack.isDown();
   }

   private void renderHMIConsume(
      AbstractClientPlayer player,
      float tickDelta,
      InteractionHand handIn,
      float swingProgress,
      ItemStack stack,
      float equippedProgress,
      PoseStack matrices,
      SubmitNodeCollector vertexConsumers,
      int light
   ) {
      ItemStack consumeStack = player.getUseItem() != null && !player.getUseItem().isEmpty() && player.getUsedItemHand() == handIn
         ? player.getUseItem()
         : stack;
      boolean isMainHand = handIn == InteractionHand.MAIN_HAND;
      HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
      float useTicks = consumeStack.getUseDuration(player) - (player.getUseItemRemainingTicks() - tickDelta + 1.0F);
      float progress = Mth.clamp(useTicks / 5.0F, 0.0F, 1.0F);
      float wobble = Mth.sin(useTicks / 2.0F * (float) Math.PI) * 0.1F;
      matrices.pushPose();
      this.applyHandPositionBase(matrices, arm);
      matrices.translate(direction, 0.1, 0.3);
      matrices.translate(0.2 * direction * progress, -0.7 * progress, -0.2 * progress);
      matrices.translate(0.0, -0.2 * wobble, -0.2 * wobble);
      matrices.translate(0.0, 0.1 * this.holdMyItemsEase(Mth.sin(progress * (float) Math.PI)), 0.0);
      matrices.rotate(Axis.YP.rotationDegrees(45.0F * direction));
      matrices.rotate(Axis.ZP.rotationDegrees(-40.0F * direction));
      matrices.rotate(Axis.XP.rotationDegrees(30.0F));
      matrices.scale(0.9F, 0.9F, 0.9F);
      matrices.rotate(Axis.YP.rotationDegrees(45.0F * progress * direction));
      FirstPersonHandsAndItemsRenderer heldItemRenderer = Minecraft.getInstance().gameRenderer.firstPersonHandsAndItemsRenderer;
      if (heldItemRenderer instanceof HeldItemRendererAccessor) {
         ((HeldItemRendererAccessor)heldItemRenderer).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, swingProgress, arm, currentPlayerState);
      }

      this.applyHoldMyItemsItemPose(matrices, player, handIn, arm, consumeStack, swingProgress);
      this.applyHandPositionItem(matrices, arm);
      this.renderItem(
         player,
         consumeStack,
         arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
         arm == HumanoidArm.LEFT,
         matrices,
         vertexConsumers,
         light
      );
      matrices.popPose();
      this.holdMyItemsIsAttacking = Minecraft.getInstance().options.keyAttack.isDown();
   }

   private void renderHMI(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector vertexConsumers,
      int light
   ) {
      boolean isMainHand = hand == InteractionHand.MAIN_HAND;
      HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      this.updateChestRightHandMotion();
      matrices.pushPose();
      this.applyHandPositionBase(matrices, arm);
      this.applyChestRightHandMotion(matrices, arm);
      this.applyHoldMyItemsSwing(matrices, player, hand, item, swingProgress);
      this.applyHoldMyItemsEnvironment(matrices, player, hand, arm, item, swingProgress, tickDelta);
      this.applyHoldMyItemsArmPrePose(matrices, item, arm);
      this.applyHoldMyItemsBaseHandPose(matrices, arm, equipProgress, swingProgress);
      FirstPersonHandsAndItemsRenderer heldItemRenderer = Minecraft.getInstance().gameRenderer.firstPersonHandsAndItemsRenderer;
      if (heldItemRenderer instanceof HeldItemRendererAccessor) {
         ((HeldItemRendererAccessor)heldItemRenderer).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0F, 0.0F, arm, currentPlayerState);
      }

      this.applyHoldMyItemsItemPose(matrices, player, hand, arm, item, swingProgress);
      this.applyHandPositionItem(matrices, arm);
      this.renderItem(
         player,
         item,
         arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
         arm == HumanoidArm.LEFT,
         matrices,
         vertexConsumers,
         light
      );
      matrices.popPose();
   }
}
