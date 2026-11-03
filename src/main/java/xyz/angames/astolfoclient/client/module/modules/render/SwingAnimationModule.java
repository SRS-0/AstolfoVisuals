package xyz.angames.astolfoclient.client.module.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ShearsItem;

import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.mixin.HeldItemRendererAccessor;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.modules.render.HandPositionModule;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class SwingAnimationModule
extends Module {
    public final ModeSetting mode = new ModeSetting("Mode", "Mode 1", "Mode 1", "Mode 2", "Mode 3", "Mode 4", "Mode 5", "Vanilla", "HMI");
    public final NumberSetting strength = new NumberSetting("Strength", 20.0, 20.0, 75.0, 0.1);
    public final BooleanSetting slow = new BooleanSetting("Slow", false);
    public final NumberSetting speed = new NumberSetting("Speed", 12.0, 1.0, 50.0, 1.0);
    public final ModeSetting attackMode = new ModeSetting("Attack Mode", "Swings", new String[]{"Swings", "Forward", "Normal"}){

        @Override
        public boolean isVisible() {
            return SwingAnimationModule.this.isHmiVisible();
        }
    };
    public final NumberSetting rightX = new NumberSetting("Right X", 0.0, -2.0, 2.0, 0.1){

        @Override
        public boolean isVisible() {
            return SwingAnimationModule.this.isHmiVisible();
        }
    };
    public final NumberSetting rightZ = new NumberSetting("Right Y", 0.0, -2.0, 2.0, 0.1){

        @Override
        public boolean isVisible() {
            return SwingAnimationModule.this.isHmiVisible();
        }
    };
    public final NumberSetting rightY = new NumberSetting("Right Z", 0.0, -2.0, 2.0, 0.1){

        @Override
        public boolean isVisible() {
            return SwingAnimationModule.this.isHmiVisible();
        }
    };
    public final NumberSetting leftX = new NumberSetting("Left X", 0.0, -2.0, 2.0, 0.1){

        @Override
        public boolean isVisible() {
            return SwingAnimationModule.this.isHmiVisible();
        }
    };
    public final NumberSetting leftZ = new NumberSetting("Left Y", 0.0, -2.0, 2.0, 0.1){

        @Override
        public boolean isVisible() {
            return SwingAnimationModule.this.isHmiVisible();
        }
    };
    public final NumberSetting leftY = new NumberSetting("Left Z", 0.0, -2.0, 2.0, 0.1){

        @Override
        public boolean isVisible() {
            return SwingAnimationModule.this.isHmiVisible();
        }
    };
    public static boolean renderingCustomItem = false;
    private static final double HOLD_MY_ITEMS_MAX_DELTA = 0.05;
    private static final double HOLD_MY_ITEMS_ANIMATION_SPEED = 30.0;
    private double holdMyItemsPrevFrameTime = (double)System.nanoTime() / 1.0E9;
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
        this.addSettings(this.mode, this.strength, this.slow, this.speed, this.attackMode, this.rightX, this.rightZ, this.rightY, this.leftX, this.leftZ, this.leftY);
    }

    private float getFloat(NumberSetting setting) {
        return ((Number)setting.getValue()).floatValue();
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
            float[] scale;
            Minecraft mc = Minecraft.getInstance();
            boolean isMainHand = mc.player != null && arm == mc.player.getMainArm();
            float[] rot = isMainHand ? handPos.getMainHandRot() : handPos.getOffHandRot();
            float[] fArray = scale = isMainHand ? handPos.getMainHandScale() : handPos.getOffHandScale();
            if (rot[0] != 0.0f) {
                matrices.rotate(Axis.XP.rotationDegrees(rot[0]));
            }
            if (rot[1] != 0.0f) {
                matrices.rotate(Axis.YP.rotationDegrees(rot[1]));
            }
            if (rot[2] != 0.0f) {
                matrices.rotate(Axis.ZP.rotationDegrees(rot[2]));
            }
            if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
                matrices.scale(scale[0], scale[1], scale[2]);
            }
        }
    }

    private void handleSwordAnim(PoseStack matrices, float swingProgress, float equipProgress, HumanoidArm arm) {
        float str = this.getFloat(this.strength);
        float g = Mth.sin((float)(Mth.sqrt((float)swingProgress) * (float)Math.PI));
        float anim = (float)Math.sin((double)swingProgress * 1.5707963267948966 * 2.0);
        float isLeft = arm == HumanoidArm.LEFT ? -1.0f : 1.0f;
        String currentMode = String.valueOf(this.mode.get()).toUpperCase();
        if (currentMode.contains("VANILLA")) {
            float n = -0.4f * Mth.sin((float)(Mth.sqrt((float)swingProgress) * (float)Math.PI));
            float mxx = 0.2f * Mth.sin((float)(Mth.sqrt((float)swingProgress) * ((float)Math.PI * 2)));
            float fxxx = -0.2f * Mth.sin((float)(swingProgress * (float)Math.PI));
            matrices.translate(isLeft * n, mxx, fxxx);
            this.applyEquipOffset(matrices, arm, equipProgress);
            this.applySwingOffset(matrices, arm, swingProgress);
            return;
        }
        this.applyEquipOffset(matrices, arm, 0.0f);
        matrices.scale(1.0f, 1.0f, 1.0f);
        if (currentMode.contains("1")) {
            this.applySwingOffset(matrices, arm, swingProgress);
        } else if (currentMode.contains("2")) {
            matrices.translate(isLeft * -0.1f, 0.15f, -0.1f);
            matrices.rotate(Axis.YP.rotationDegrees(isLeft * -60.0f));
            matrices.rotate(Axis.XP.rotationDegrees(50.0f));
            matrices.rotate(Axis.ZP.rotationDegrees(isLeft * (110.0f + str * g)));
        } else if (currentMode.contains("3")) {
            matrices.translate(isLeft * -0.1f, 0.15f, 0.0f);
            matrices.rotate(Axis.XP.rotationDegrees(50.0f));
            matrices.rotate(Axis.YP.rotationDegrees(isLeft * (-30.0f + str * g)));
            matrices.rotate(Axis.ZP.rotationDegrees(isLeft * 110.0f));
        } else if (currentMode.contains("4")) {
            matrices.translate(isLeft * -0.15f, 0.2f, 0.0f);
            matrices.rotate(Axis.YP.rotationDegrees(isLeft * 90.0f));
            matrices.rotate(Axis.ZP.rotationDegrees(isLeft * -30.0f));
            matrices.rotate(Axis.XP.rotationDegrees(-90.0f - str * anim + 10.0f));
        } else if (currentMode.contains("5")) {
            this.applySwingOffset(matrices, arm, swingProgress);
            float spinAngle = swingProgress * 360.0f;
            matrices.translate(0.0f, 0.0f, 0.0f);
            matrices.rotate(Axis.XP.rotationDegrees(-isLeft * spinAngle));
            matrices.translate(0.0f, 0.0f, 0.0f);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void handleRenderItem(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        renderingCustomItem = true;
        try {
            if (this.isHoldMyItemsEnabled()) {
                if (this.shouldUseHoldMyItemsBow(player, hand, item)) {
                    this.renderHMIBow(player, tickDelta, hand, swingProgress, item, 0.0f, matrices, vertexConsumers, light);
                    return;
                }
                if (this.shouldUseHoldMyItemsConsume(player, hand, item)) {
                    this.renderHMIConsume(player, tickDelta, hand, swingProgress, item, 0.0f, matrices, vertexConsumers, light);
                    return;
                }
                if (this.shouldUseHoldMyItemsCustom(player, hand, item)) {
                    this.renderHMI(player, tickDelta, pitch, hand, swingProgress, item, 0.0f, matrices, vertexConsumers, light);
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
                    boolean isCharged = CrossbowItem.isCharged((ItemStack)item);
                    if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        matrices.translate((float)i * -0.4785682f, -0.094387f, 0.05731531f);
                        matrices.rotate(Axis.XP.rotationDegrees(-11.935f));
                        matrices.rotate(Axis.YP.rotationDegrees((float)i * 65.3f));
                        matrices.rotate(Axis.ZP.rotationDegrees((float)i * -9.785f));
                        float f = (float)item.getUseDuration((LivingEntity)mc.player) - ((float)mc.player.getUseItemRemainingTicks() - tickDelta + 1.0f);
                        float g = f / (float)CrossbowItem.getChargeDuration((ItemStack)item, (LivingEntity)mc.player);
                        if (g > 1.0f) {
                            g = 1.0f;
                        }
                        if (g > 0.1f) {
                            float h = Mth.sin((float)((f - 0.1f) * 1.3f));
                            float j = g - 0.1f;
                            float k = h * j;
                            matrices.translate(k * 0.0f, k * 0.004f, k * 0.0f);
                        }
                        matrices.translate(g * 0.0f, g * 0.0f, g * 0.04f);
                        matrices.scale(1.0f, 1.0f, 1.0f + g * 0.2f);
                        matrices.rotate(Axis.YN.rotationDegrees((float)i * 45.0f));
                    } else {
                        float fx = -0.4f * Mth.sin((float)(Mth.sqrt((float)swingProgress) * (float)Math.PI));
                        float gx = 0.2f * Mth.sin((float)(Mth.sqrt((float)swingProgress) * ((float)Math.PI * 2)));
                        float h = -0.2f * Mth.sin((float)(swingProgress * (float)Math.PI));
                        matrices.translate((float)i * fx, gx, h);
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        this.applySwingOffset(matrices, arm, swingProgress);
                        if (isCharged && swingProgress < 0.001f && isMainHand) {
                            matrices.translate((float)i * -0.641864f, 0.0f, 0.0f);
                            matrices.rotate(Axis.YP.rotationDegrees((float)i * 10.0f));
                        }
                    }
                    this.applyHandPositionItem(matrices, arm);
                    this.renderItem((LivingEntity)player, item, isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !isRightArm, matrices, vertexConsumers, light);
                } else {
                    if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                        int l = isRightArm ? 1 : -1;
                        switch (item.getUseAnimation()) {
                            case NONE: 
                            case BLOCK: {
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                break;
                            }
                            case EAT: 
                            case DRINK: {
                                this.applyEatOrDrinkTransformation(matrices, tickDelta, arm, item);
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                break;
                            }
                            case BOW: {
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                matrices.translate((float)l * -0.2785682f, 0.18344387f, 0.15731531f);
                                matrices.rotate(Axis.XP.rotationDegrees(-13.935f));
                                matrices.rotate(Axis.YP.rotationDegrees((float)l * 35.3f));
                                matrices.rotate(Axis.ZP.rotationDegrees((float)l * -9.785f));
                                float mx = (float)item.getUseDuration((LivingEntity)mc.player) - ((float)mc.player.getUseItemRemainingTicks() - tickDelta + 1.0f);
                                float fxx = mx / 20.0f;
                                fxx = (fxx * fxx + fxx * 2.0f) / 3.0f;
                                if (fxx > 1.0f) {
                                    fxx = 1.0f;
                                }
                                if (fxx > 0.1f) {
                                    float gx = Mth.sin((float)((mx - 0.1f) * 1.3f));
                                    float h = fxx - 0.1f;
                                    float j = gx * h;
                                    matrices.translate(j * 0.0f, j * 0.004f, j * 0.0f);
                                }
                                matrices.translate(fxx * 0.0f, fxx * 0.0f, fxx * 0.04f);
                                matrices.scale(1.0f, 1.0f, 1.0f + fxx * 0.2f);
                                matrices.rotate(Axis.YN.rotationDegrees((float)l * 45.0f));
                                break;
                            }
                            case SPEAR: {
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                matrices.translate((float)l * -0.5f, 0.7f, 0.1f);
                                matrices.rotate(Axis.XP.rotationDegrees(-55.0f));
                                matrices.rotate(Axis.YP.rotationDegrees((float)l * 35.3f));
                                matrices.rotate(Axis.ZP.rotationDegrees((float)l * -9.785f));
                                float m = (float)item.getUseDuration((LivingEntity)mc.player) - ((float)mc.player.getUseItemRemainingTicks() - tickDelta + 1.0f);
                                float fx = m / 10.0f;
                                if (fx > 1.0f) {
                                    fx = 1.0f;
                                }
                                if (fx > 0.1f) {
                                    float gx = Mth.sin((float)((m - 0.1f) * 1.3f));
                                    float h = fx - 0.1f;
                                    float j = gx * h;
                                    matrices.translate(j * 0.0f, j * 0.004f, j * 0.0f);
                                }
                                matrices.translate(0.0f, 0.0f, fx * 0.2f);
                                matrices.scale(1.0f, 1.0f, 1.0f + fx * 0.2f);
                                matrices.rotate(Axis.YN.rotationDegrees((float)l * 45.0f));
                                break;
                            }
                            case BRUSH: {
                                this.applyBrushTransformation(matrices, tickDelta, arm, item, equipProgress);
                            }
                        }
                    } else if (player.isAutoSpinAttack()) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        int l = isRightArm ? 1 : -1;
                        matrices.translate((float)l * -0.4f, 0.8f, 0.3f);
                        matrices.rotate(Axis.YP.rotationDegrees((float)l * 65.0f));
                        matrices.rotate(Axis.ZP.rotationDegrees((float)l * -85.0f));
                    } else if (arm == mc.options.mainHand().get() && this.isEnabled()) {
                        this.handleSwordAnim(matrices, swingProgress, equipProgress, arm);
                    } else {
                        float n = -0.4f * Mth.sin((float)(Mth.sqrt((float)swingProgress) * (float)Math.PI));
                        float mxx = 0.2f * Mth.sin((float)(Mth.sqrt((float)swingProgress) * ((float)Math.PI * 2)));
                        float fxxx = -0.2f * Mth.sin((float)(swingProgress * (float)Math.PI));
                        int o = isRightArm ? 1 : -1;
                        matrices.translate((float)o * n, mxx, fxxx);
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        this.applySwingOffset(matrices, arm, swingProgress);
                    }
                    this.applyHandPositionItem(matrices, arm);
                    this.renderItem((LivingEntity)player, item, isRightArm ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !isRightArm, matrices, vertexConsumers, light);
                }
                matrices.popPose();
            }
        }
        finally {
            renderingCustomItem = false;
        }
    }

    private void applyBrushTransformation(PoseStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack, float equipProgress) {
        Minecraft mc = Minecraft.getInstance();
        this.applyEquipOffset(matrices, arm, equipProgress);
        float f = mc.player.getUseItemRemainingTicks() % 10;
        float g = f - tickDelta + 1.0f;
        float h = 1.0f - g / 10.0f;
        float n = -15.0f + 75.0f * Mth.cos((float)(h * 2.0f * (float)Math.PI));
        if (arm != HumanoidArm.RIGHT) {
            matrices.translate(0.1, 0.83, 0.35);
            matrices.rotate(Axis.XP.rotationDegrees(-80.0f));
            matrices.rotate(Axis.YP.rotationDegrees(-90.0f));
            matrices.rotate(Axis.XP.rotationDegrees(n));
            matrices.translate(-0.3, 0.22, 0.35);
        } else {
            matrices.translate(-0.25, 0.22, 0.35);
            matrices.rotate(Axis.XP.rotationDegrees(-80.0f));
            matrices.rotate(Axis.YP.rotationDegrees(90.0f));
            matrices.rotate(Axis.ZP.rotationDegrees(0.0f));
            matrices.rotate(Axis.XP.rotationDegrees(n));
        }
    }

    private void applyEatOrDrinkTransformation(PoseStack matrices, float tickDelta, HumanoidArm arm, ItemStack stack) {
        float h;
        Minecraft mc = Minecraft.getInstance();
        float f = (float)mc.player.getUseItemRemainingTicks() - tickDelta + 1.0f;
        float g = f / (float)stack.getUseDuration((LivingEntity)mc.player);
        if (g < 0.8f) {
            h = Mth.abs((float)(Mth.cos((float)(f / 4.0f * (float)Math.PI)) * 0.1f));
            matrices.translate(0.0f, h, 0.0f);
        }
        h = 1.0f - (float)Math.pow(g, 27.0);
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate(h * 0.6f * (float)i, h * -0.5f, h * 0.0f);
        matrices.rotate(Axis.YP.rotationDegrees((float)i * h * 90.0f));
        matrices.rotate(Axis.XP.rotationDegrees(h * 10.0f));
        matrices.rotate(Axis.ZP.rotationDegrees((float)i * h * 30.0f));
    }

    private void applyEquipOffset(PoseStack matrices, HumanoidArm arm, float equipProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        matrices.translate((float)i * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
    }

    private void applySwingOffset(PoseStack matrices, HumanoidArm arm, float swingProgress) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        float f = Mth.sin((float)(swingProgress * swingProgress * (float)Math.PI));
        float g = Mth.sin((float)(Mth.sqrt((float)swingProgress) * (float)Math.PI));
        matrices.rotate(Axis.YP.rotationDegrees((float)i * (45.0f + f * -20.0f)));
        matrices.rotate(Axis.ZP.rotationDegrees((float)i * g * -20.0f));
        matrices.rotate(Axis.XP.rotationDegrees(g * -80.0f));
        matrices.rotate(Axis.YP.rotationDegrees((float)i * -45.0f));
    }

    public void renderItem(LivingEntity entity,ItemStack stack,ItemDisplayContext renderMode,boolean leftHanded,PoseStack matrices,SubmitNodeCollector vertexConsumers,int light) {if(!stack.isEmpty()){var state=new net.minecraft.client.renderer.item.ItemStackRenderState();Minecraft.getInstance().getItemModelResolver().updateForLiving(state,stack,renderMode,entity);state.submit(matrices,vertexConsumers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);}}

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
        if (!this.isEnabled()) {
            return false;
        }
        return "HMI".equals(String.valueOf(this.mode.get()));
    }

    public void updatePhysics(LocalPlayer player, float tickDelta) {
        if (!this.isEnabled() || !this.isHoldMyItemsEnabled()) {
            return;
        }
        double currentTime = (double)System.nanoTime() / 1.0E9;
        this.holdMyItemsDeltaTime = Math.min(0.05, Math.max(0.0, currentTime - this.holdMyItemsPrevFrameTime));
        this.holdMyItemsPrevFrameTime = currentTime;
        this.holdMyItemsPhysicsUpdatedThisFrame = false;
        float f = player.getSwingAnimation(tickDelta);
        if (f > 0.0f && this.holdMyItemsPrevSwingProgress == 0.0f) {
            this.holdMyItemsLeft = !this.holdMyItemsLeft;
        }
        this.holdMyItemsPrevSwingProgress = f;
    }

    private boolean shouldUseHoldMyItemsCustom(AbstractClientPlayer player, InteractionHand handIn, ItemStack stack) {
        if (!this.isHoldMyItemsEnabled()) {
            return false;
        }
        return !(stack.getItem() instanceof MapItem) && !(stack.getItem() instanceof CrossbowItem) && (!player.isUsingItem() || player.getUsedItemHand() != handIn) && !player.isAutoSpinAttack();
    }

    private boolean shouldUseHoldMyItemsBow(AbstractClientPlayer player, InteractionHand handIn, ItemStack stack) {
        if (!this.isHoldMyItemsEnabled()) {
            return false;
        }
        return stack.getUseAnimation() == ItemUseAnimation.BOW && player.isUsingItem() && player.getUsedItemHand() == handIn;
    }

    private boolean shouldUseHoldMyItemsConsume(AbstractClientPlayer player, InteractionHand handIn, ItemStack stack) {
        if (!this.isHoldMyItemsEnabled()) {
            return false;
        }
        ItemStack consumeStack = player.getUseItem() != null && !player.getUseItem().isEmpty() && player.getUsedItemHand() == handIn ? player.getUseItem() : stack;
        ItemUseAnimation action = consumeStack.getUseAnimation();
        return (action == ItemUseAnimation.EAT || action == ItemUseAnimation.DRINK) && player.isUsingItem() && player.getUsedItemHand() == handIn;
    }

    private void updateChestRightHandMotion() {
        Minecraft mc = Minecraft.getInstance();
        float target = mc.gui.screen() instanceof AbstractContainerScreen && !(mc.gui.screen() instanceof InventoryScreen) ? 1.0f : 0.0f;
        this.chestRightHandMotion = Mth.lerp((float)0.18f, (float)this.chestRightHandMotion, (float)target);
    }

    private float getHoldMyItemsAttackDamage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0f;
        }
        String name = stack.getItem().toString().toLowerCase();
        if (name.contains("sword")) {
            if (name.contains("netherite")) {
                return 8.0f;
            }
            if (name.contains("diamond")) {
                return 7.0f;
            }
            if (name.contains("iron")) {
                return 6.0f;
            }
            if (name.contains("stone")) {
                return 5.0f;
            }
            return 4.0f;
        }
        if (name.contains("axe")) {
            if (name.contains("netherite") || name.contains("diamond") || name.contains("iron") || name.contains("stone")) {
                return 9.0f;
            }
            return 7.0f;
        }
        return 0.0f;
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
        return stack.is(net.minecraft.tags.ItemTags.MINING_ENCHANTABLE) || stack.getItem() instanceof ShearsItem || stack.getItem() instanceof TridentItem;
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
        return stack.is(Items.STRING) || stack.is(Items.REDSTONE) || stack.is(Items.LEVER) || stack.is(Items.TRIPWIRE_HOOK) || block instanceof IronBarsBlock || block.defaultBlockState().is(BlockTags.RAILS) || block.defaultBlockState().is(BlockTags.CLIMBABLE) || block.defaultBlockState().is(BlockTags.DOORS);
    }

    private boolean isHoldMyItemsTorch(ItemStack stack) {
        String name = stack.getHoverName().getString().toLowerCase();
        return name.contains("torch") || name.contains("\u0444\u0430\u043a\u0435\u043b");
    }

    private boolean isHoldMyItemsSmallItem(ItemStack stack) {
        return !(stack.getItem() instanceof BlockItem) && !this.isHoldMyItemsTool(stack) && !this.isHoldMyItemsWeapon(stack) && !(stack.getItem() instanceof FishingRodItem) && !(stack.getItem() instanceof BucketItem) && stack.getUseAnimation() != ItemUseAnimation.BOW && stack.getUseAnimation() != ItemUseAnimation.SPEAR && stack.getUseAnimation() != ItemUseAnimation.BLOCK;
    }

    private float holdMyItemsEase(float value) {
        float c1 = 1.70158f;
        float c2 = c1 * 1.525f;
        if (value < 0.5f) {
            float doubled = 2.0f * value;
            return doubled * doubled * ((c2 + 1.0f) * doubled - c2) * 0.5f;
        }
        float shifted = 2.0f * value - 2.0f;
        return (shifted * shifted * ((c2 + 1.0f) * shifted + c2) + 2.0f) * 0.5f;
    }

    private float getHoldMyItemsSwingRot(float swingProgress) {
        if (swingProgress < 0.6f) {
            return Mth.sin((float)(Mth.clamp((float)swingProgress, (float)0.0f, (float)0.12506f) * 12.56f));
        }
        return Mth.sin((float)(Mth.clamp((float)swingProgress, (float)0.62532f, (float)0.75038f) * 12.56f));
    }

    private void applyHoldMyItemsBaseHandPose(PoseStack matrices, HumanoidArm arm, float equippedProgress, float swingProgress) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float swingSin = Mth.sin((float)(swingProgress * (float)Math.PI));
        matrices.translate((double)direction, (double)(-equippedProgress) * 0.3, 0.3);
        matrices.rotate(Axis.YP.rotationDegrees(45.0f * (float)direction));
        matrices.rotate(Axis.ZP.rotationDegrees(-40.0f * (float)direction));
        matrices.rotate(Axis.XP.rotationDegrees(30.0f));
        matrices.rotate(Axis.YP.rotationDegrees((float)direction * (45.0f + swingSin * 0.0f)));
        matrices.rotate(Axis.YP.rotationDegrees((float)direction * -45.0f));
        matrices.scale(0.9f, 0.9f, 0.9f);
    }

    private void applyHoldMyItemsArmPrePose(PoseStack matrices, ItemStack stack, HumanoidArm arm) {
        int direction;
        int n = direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        if (this.isHoldMyItemsLantern(stack)) {
            matrices.translate(0.1 * (double)direction, 0.0, -0.1);
            matrices.rotate(Axis.XP.rotationDegrees(10.0f));
            return;
        }
        if (stack.getUseAnimation() == ItemUseAnimation.BLOCK) {
            matrices.translate(0.0, -0.2, 0.0);
        }
    }

    private void applyHoldMyItemsEnvironment(PoseStack matrices, AbstractClientPlayer player, InteractionHand handIn, HumanoidArm arm, ItemStack stack, float swingProgress, float partialTicks) {
        int armDirection;
        float pitchFactor;
        float yaw = Mth.lerp((float)partialTicks, (float)player.yRotO, (float)player.getYRot());
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians);
        double forwardZ = Math.cos(radians);
        Vec3 velocity = player.getDeltaMovement();
        double dotProduct = velocity.x * forwardX + velocity.z * forwardZ;
        double crossProduct = velocity.x * forwardZ - velocity.z * forwardX;
        float f = pitchFactor = player.getXRot() != 0.0f ? 90.0f / player.getXRot() / 10.0f : 1.0f;
        if (pitchFactor > 1.0f || pitchFactor < 0.0f) {
            pitchFactor = 1.0f;
        }
        boolean crawling = this.isHoldMyItemsCrawling(player);
        boolean climbing = this.isHoldMyItemsClimbing(player);
        boolean elytraFlying = player.isFallFlying();
        double tt = this.holdMyItemsDeltaTime * 30.0;
        float handDirection = handIn == InteractionHand.MAIN_HAND ? 1.0f : -1.0f;
        int n = armDirection = arm == HumanoidArm.RIGHT ? 1 : -1;
        if (elytraFlying) {
            if (!this.holdMyItemsPhysicsUpdatedThisFrame) {
                this.holdMyItemsClimbBlend = 0.0f;
                this.holdMyItemsInWaterCounter = 0.0f;
                this.holdMyItemsVertAngleY *= (float)Math.pow(0.72, tt);
                this.holdMyItemsVertVelocityYSlime *= (float)Math.pow(0.72, tt);
                this.holdMyItemsVertAngleYSlime *= (float)Math.pow(0.72, tt);
                this.holdMyItemsPhysicsUpdatedThisFrame = true;
            }
            if (!stack.isEmpty() && stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
                matrices.translate(0.0, -0.1, 0.1);
            }
            if (this.isHoldMyItemsLantern(stack)) {
                matrices.translate(0.0, 0.1, 0.0);
            }
            return;
        }
        if (!this.holdMyItemsPhysicsUpdatedThisFrame) {
            double speed = velocity.length();
            if (speed >= 0.08) {
                double clampedSpeed = Math.min(speed, 0.22);
                double clampedDot = Mth.clamp((double)dotProduct, (double)-0.22, (double)0.22);
                double clampedCross = Mth.clamp((double)crossProduct, (double)-0.22, (double)0.22);
                this.holdMyItemsCrawlCount = (float)((double)this.holdMyItemsCrawlCount + 0.1 * clampedSpeed * 2.0 * tt);
                this.holdMyItemsDirectionalCrawlCount = (float)((double)this.holdMyItemsDirectionalCrawlCount + 0.1 * clampedDot * 4.0 * tt);
                this.holdMyItemsDirectionalCrawlCount = (float)((double)this.holdMyItemsDirectionalCrawlCount + (clampedDot > 0.0 ? 0.1 * Math.abs(clampedCross) * 4.0 * tt : 0.1 * Math.abs(clampedCross) * -4.0 * tt));
            }
            if (velocity.y > 0.0) {
                this.holdMyItemsClimbCount = (float)((double)this.holdMyItemsClimbCount + 0.1 * tt);
            }
            if (velocity.y < 0.0) {
                this.holdMyItemsClimbCount = (float)((double)this.holdMyItemsClimbCount - 0.1 * tt);
            }
            float motionYNormalized = player.onGround() ? 0.0f : (float)Mth.clamp((double)velocity.y, (double)-0.42, (double)0.42);
            this.holdMyItemsVertAngleY = (float)((double)this.holdMyItemsVertAngleY + (double)motionYNormalized * 0.015 * tt);
            this.holdMyItemsVertAngleY = (float)((double)this.holdMyItemsVertAngleY - 0.1 * (double)this.holdMyItemsVertAngleY * tt);
            this.holdMyItemsVertAngleY = (float)((double)this.holdMyItemsVertAngleY * Math.pow(0.88, tt));
            this.holdMyItemsVertVelocityYSlime = (float)((double)this.holdMyItemsVertVelocityYSlime + (double)motionYNormalized * 0.015 * tt);
            this.holdMyItemsVertVelocityYSlime = (float)((double)this.holdMyItemsVertVelocityYSlime - 0.1 * (double)this.holdMyItemsVertAngleYSlime * tt);
            this.holdMyItemsVertVelocityYSlime = (float)((double)this.holdMyItemsVertVelocityYSlime * Math.pow(0.88, tt));
            this.holdMyItemsVertAngleYSlime = (float)((double)this.holdMyItemsVertAngleYSlime + (double)this.holdMyItemsVertVelocityYSlime * tt);
            if (player.isInWater() && !player.isUnderWater()) {
                this.holdMyItemsInWaterCounter = (float)((double)this.holdMyItemsInWaterCounter + 0.1 * tt);
                if (this.holdMyItemsInWaterCounter > 1.0f) {
                    this.holdMyItemsInWaterCounter = 1.0f;
                }
            } else {
                this.holdMyItemsInWaterCounter = (float)((double)this.holdMyItemsInWaterCounter * Math.pow(0.88, tt));
            }
            this.holdMyItemsPhysicsUpdatedThisFrame = true;
        }
        if (!(!crawling && !climbing || player.isUsingItem() && player.getUsedItemHand() == handIn || swingProgress != 0.0f)) {
            this.holdMyItemsClimbBlend = (float)((double)this.holdMyItemsClimbBlend + 0.1 * tt);
            if (this.holdMyItemsClimbBlend > 1.0f) {
                this.holdMyItemsClimbBlend = 1.0f;
            }
            if (!this.isHoldMyItemsLantern(stack)) {
                matrices.rotate(Axis.XP.rotationDegrees(-20.0f * this.holdMyItemsClimbBlend));
            }
        } else {
            this.holdMyItemsClimbBlend = (float)((double)this.holdMyItemsClimbBlend * Math.pow(0.88, tt));
        }
        if (swingProgress == 0.0f) {
            float pitch = player.getXRot();
            matrices.translate(handDirection > 0.0f ? pitch / 650.0f * this.holdMyItemsClimbBlend * -1.0f : pitch / 650.0f * this.holdMyItemsClimbBlend, 0.0f, 0.0f);
            matrices.rotate(Axis.XP.rotationDegrees(pitch * this.holdMyItemsClimbBlend));
        }
        if (!this.isHoldMyItemsLantern(stack)) {
            matrices.translate(0.0, 0.0, (double)(player.getXRot() / 120.0f * this.holdMyItemsClimbBlend));
        } else if (swingProgress == 0.0f) {
            matrices.translate(0.0, 0.0, (double)(player.getXRot() / 80.0f * this.holdMyItemsClimbBlend));
        }
        if (!(!climbing || this.isHoldMyItemsLantern(stack) || player.isUsingItem() && player.getUsedItemHand() == handIn)) {
            matrices.translate(0.0, 0.1, -0.2);
        }
        matrices.translate(0.0, 0.02 * (double)this.holdMyItemsInWaterCounter, 0.0);
        matrices.rotate(Axis.ZP.rotationDegrees(8.0f * handDirection * this.holdMyItemsInWaterCounter));
        matrices.translate(0.0, (double)(-this.holdMyItemsVertAngleY), 0.0);
        matrices.translate(0.0, Math.sin((double)player.tickCount * 0.1) * 0.007 * (double)armDirection, 0.0);
        matrices.rotate(Axis.YP.rotationDegrees(0.15f * (float)Math.sin((float)player.tickCount * 0.15f) * (float)armDirection));
        if ((!stack.isEmpty() || crawling || climbing || player.isUnderWater()) && stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
            matrices.translate(0.0, -0.1, 0.1);
        }
        if (this.isHoldMyItemsLantern(stack)) {
            matrices.translate(0.0, 0.1, 0.0);
            if (player.isUnderWater()) {
                matrices.translate(0.0, -0.1, 0.1);
            }
        }
        if (player.isUnderWater() && swingProgress == 0.0f) {
            double distance = (double)((float)player.tickCount + partialTicks) * 0.2;
            double handRotation = Math.sin(distance) * 1.5;
            double smoothRotation = handRotation * 0.8 + this.holdMyItemsPreviousRotation * 0.2;
            matrices.rotate(Axis.YP.rotationDegrees((float)(handIn == InteractionHand.MAIN_HAND ? smoothRotation : -smoothRotation)));
            matrices.translate(0.0, 0.0, smoothRotation * 0.2);
            this.holdMyItemsPreviousRotation = smoothRotation;
        }
        if (!(!climbing && !crawling || player.isUsingItem() && player.getUsedItemHand() == handIn || swingProgress != 0.0f)) {
            float crawlProgress = Mth.sin((float)(this.holdMyItemsDirectionalCrawlCount * 4.0f));
            float upAndDown = Mth.cos((float)(this.holdMyItemsDirectionalCrawlCount * 4.0f));
            if (this.isHoldMyItemsLantern(stack)) {
                crawlProgress *= 0.14f;
                upAndDown *= 0.14f;
            }
            matrices.translate(0.2 * (double)crawlProgress, 0.3 * (double)crawlProgress * (double)armDirection, -0.2 * (double)crawlProgress * (double)armDirection * (double)pitchFactor);
            matrices.rotate(Axis.YP.rotationDegrees(25.0f * crawlProgress));
            matrices.rotate(Axis.XP.rotationDegrees(Mth.clamp((float)(20.0f * upAndDown * (float)armDirection), (float)0.0f, (float)20.0f)));
        }
    }

    private void applyHoldMyItemsLanternPose(PoseStack matrices, AbstractClientPlayer player, HumanoidArm arm, float swingProgress) {
        float dt = (float)(this.holdMyItemsDeltaTime * 30.0);
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float yawDelta = player.yHeadRotO - player.yHeadRot;
        float pitchDelta = player.xRotO - player.getXRot();
        this.holdMyItemsSwingVelocityY += yawDelta * 0.015f * dt;
        this.holdMyItemsSwingVelocityY += swingProgress * 2.0f * dt;
        this.holdMyItemsSwingVelocityX += pitchDelta * 0.015f * dt;
        this.holdMyItemsSwingVelocityY -= 0.1f * this.holdMyItemsSwingAngleY * dt;
        this.holdMyItemsSwingVelocityX -= 0.1f * this.holdMyItemsSwingAngleX * dt;
        this.holdMyItemsSwingVelocityY = (float)((double)this.holdMyItemsSwingVelocityY * Math.pow(0.88, dt));
        this.holdMyItemsSwingVelocityX = (float)((double)this.holdMyItemsSwingVelocityX * Math.pow(0.88, dt));
        this.holdMyItemsSwingAngleY += this.holdMyItemsSwingVelocityY * dt;
        this.holdMyItemsSwingAngleX += this.holdMyItemsSwingVelocityX * dt;
        double currentSpeed = player.getDeltaMovement().length();
        this.holdMyItemsSwingVelocityZ = (float)((double)this.holdMyItemsSwingVelocityZ + (direction > 0 ? (currentSpeed * -15.0 - (double)this.holdMyItemsSwingVelocityZ) * 0.1 * (double)dt : (currentSpeed * 15.0 - (double)this.holdMyItemsSwingVelocityZ) * 0.1 * (double)dt));
        if (currentSpeed > 0.09 && (player.onGround() || player.isUnderWater() || this.isHoldMyItemsClimbing(player)) && ((Boolean)Minecraft.getInstance().options.bobView().get()).booleanValue()) {
            this.holdMyItemsSwingVelocityY += (float)((Math.random() < 0.5 ? -5.5 : 5.5) * currentSpeed * (double)dt);
        }
        matrices.translate(0.0, 0.0, -0.1);
        matrices.rotate(Axis.YN.rotationDegrees(35.0f * (float)direction + this.holdMyItemsSwingAngleY));
        matrices.rotate(Axis.XP.rotationDegrees(15.0f + this.holdMyItemsSwingAngleX));
        matrices.rotate(Axis.ZP.rotationDegrees(75.0f * (float)direction + this.holdMyItemsSwingVelocityZ));
        matrices.translate(0.3 * (double)direction, -0.35, 0.0);
        matrices.translate(0.0, 0.0, 0.1);
        matrices.scale(1.5f, 1.5f, 1.5f);
    }

    private void applyHoldMyItemsItemPose(PoseStack matrices, AbstractClientPlayer player, InteractionHand handIn, HumanoidArm arm, ItemStack stack, float swingProgress) {
        boolean mainHand;
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        boolean bl = mainHand = handIn == InteractionHand.MAIN_HAND;
        if (player.getMainArm() == HumanoidArm.LEFT) {
            mainHand = !mainHand;
        }
        matrices.translate(-0.3 * (double)direction, 0.65, -0.1);
        matrices.rotate(Axis.YP.rotationDegrees(-65.0f * (float)direction));
        matrices.rotate(Axis.XP.rotationDegrees(10.0f));
        if (stack.getItem() instanceof BlockItem && !(stack.getItem() instanceof BucketItem) && stack.getUseAnimation() != ItemUseAnimation.EAT) {
            Block block = ((BlockItem)stack.getItem()).getBlock();
            if (block instanceof AbstractSkullBlock) {
                matrices.translate(0.1 * (double)direction, 0.15, 0.1);
                matrices.scale(0.7f, 0.7f, 0.7f);
                matrices.rotate(Axis.YP.rotationDegrees(245.0f * (float)direction));
                matrices.rotate(Axis.XP.rotationDegrees(25.0f));
                matrices.rotate(Axis.ZP.rotationDegrees(-15.0f * (float)direction));
                return;
            }
            if (this.isHoldMyItemsTorch(stack)) {
                matrices.scale(1.5f, 1.5f, 1.5f);
                matrices.rotate(Axis.YN.rotationDegrees(25.0f * (float)direction));
                matrices.rotate(Axis.XP.rotationDegrees(5.0f));
                matrices.rotate(Axis.ZP.rotationDegrees(75.0f * (float)direction));
                matrices.translate(0.2 * (double)direction, 0.2, 0.05);
                return;
            }
            if (this.isHoldMyItemsThinBlock(stack)) {
                matrices.translate(0.0, 0.0, -0.1);
                matrices.rotate(Axis.YN.rotationDegrees(5.0f * (float)direction));
                matrices.rotate(Axis.XP.rotationDegrees(15.0f));
                matrices.rotate(Axis.ZP.rotationDegrees(75.0f * (float)direction));
                return;
            }
            if (this.isHoldMyItemsLantern(stack)) {
                this.applyHoldMyItemsLanternPose(matrices, player, arm, swingProgress);
                return;
            }
            matrices.rotate(Axis.YN.rotationDegrees(25.0f * (float)direction));
            matrices.rotate(Axis.XP.rotationDegrees(5.0f));
            matrices.rotate(Axis.ZP.rotationDegrees(75.0f * (float)direction));
            matrices.translate(0.2 * (double)direction, 0.2, 0.05);
            return;
        }
        if (this.isHoldMyItemsSmallItem(stack) && this.getHoldMyItemsAttackDamage(stack) == 0.0f) {
            matrices.rotate(Axis.YN.rotationDegrees(5.0f * (float)direction));
            matrices.rotate(Axis.XP.rotationDegrees(15.0f));
            matrices.rotate(Axis.ZP.rotationDegrees(75.0f * (float)direction));
            matrices.translate(0.0, -0.05, -0.1);
            matrices.scale(0.7f, 0.7f, 0.7f);
            return;
        }
        if (stack.getUseAnimation() == ItemUseAnimation.BLOCK && stack.getUseAnimation() != ItemUseAnimation.SPEAR) {
            matrices.rotate(Axis.ZP.rotationDegrees(160.0f * (float)direction));
            matrices.rotate(Axis.YP.rotationDegrees(-60.0f * (float)direction));
            matrices.rotate(Axis.XP.rotationDegrees(-70.0f));
            matrices.scale(0.75f, 0.75f, 0.75f);
            matrices.translate(0.15 * (double)direction, mainHand ? 0.35 : 0.45, mainHand ? -0.15 : -0.1);
            matrices.translate(0.17 * (double)direction, 0.0, 0.3);
            matrices.rotate(Axis.YP.rotationDegrees(-90.0f * (float)direction));
            return;
        }
        if (stack.getUseAnimation() == ItemUseAnimation.SPEAR) {
            matrices.rotate(Axis.YN.rotationDegrees(75.0f * (float)direction));
            matrices.rotate(Axis.XP.rotationDegrees(90.0f));
            matrices.rotate(Axis.ZP.rotationDegrees(45.0f * (float)direction));
            matrices.translate(-0.3 * (double)direction, 0.0, 0.0);
            return;
        }
        matrices.rotate(Axis.YN.rotationDegrees(75.0f * (float)direction));
        matrices.rotate(Axis.XP.rotationDegrees(70.0f));
        matrices.rotate(Axis.ZP.rotationDegrees(45.0f * (float)direction));
        if (stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
            matrices.scale(1.2f, 1.2f, 1.2f);
        }
        if (stack.getUseAnimation() == ItemUseAnimation.BOW && !player.isUsingItem()) {
            matrices.translate(-0.1 * (double)direction, -0.2, 0.0);
        }
    }

    private void applyHoldMyItemsGenericSwing(PoseStack matrices, float direction, float swingRot, float swing) {
        matrices.translate(0.1 * (double)direction * (double)swingRot, 0.1 * (double)swingRot, -0.1 * (double)swing);
        matrices.rotate(Axis.XN.rotationDegrees(-30.0f * swingRot));
        matrices.rotate(Axis.ZP.rotationDegrees(-10.0f * swingRot * direction));
        matrices.rotate(Axis.XN.rotationDegrees(40.0f * swing));
        matrices.rotate(Axis.YP.rotationDegrees(10.0f * swing * direction));
    }

    private void applyHoldMyItemsSwing(PoseStack matrices, AbstractClientPlayer player, InteractionHand handIn, ItemStack stack, float swingProgress) {
        boolean normalHandsAttack;
        boolean mainHand;
        boolean bl = mainHand = handIn == InteractionHand.MAIN_HAND;
        if (player.getMainArm() == HumanoidArm.LEFT) {
            mainHand = !mainHand;
        }
        boolean hasAuraTarget = true;
        float ll = mainHand ? 1.0f : -1.0f;
        float handDirection = handIn == InteractionHand.MAIN_HAND ? 1.0f : -1.0f;
        float swingRot = this.getHoldMyItemsSwingRot(swingProgress);
        float swing = this.holdMyItemsEase(Mth.sin((float)(swingProgress * (float)Math.PI)));
        String currentAttackMode = this.attackMode.get();
        boolean forwardHandsAttack = "Forward".equals(currentAttackMode) && hasAuraTarget;
        boolean bl2 = normalHandsAttack = "Normal".equals(currentAttackMode) && hasAuraTarget;
        if (stack.is(net.minecraft.tags.ItemTags.SWORDS) && forwardHandsAttack) {
            matrices.translate(0.12 * (double)ll * (double)swingRot, 0.04 * (double)swingRot, -0.95 * (double)swing);
            matrices.translate(0.02 * (double)ll * (double)swing, 0.1 * (double)swing, -0.1 * (double)swingRot);
            matrices.rotate(Axis.YP.rotationDegrees(8.0f * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(-14.0f * swingRot));
            matrices.rotate(Axis.ZP.rotationDegrees(-18.0f * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(32.0f * swing));
            return;
        }
        if (stack.is(net.minecraft.tags.ItemTags.SWORDS) && normalHandsAttack) {
            this.applyHoldMyItemsGenericSwing(matrices, ll, swingRot, swing);
            return;
        }
        if ((this.holdMyItemsLeft || stack.is(net.minecraft.tags.ItemTags.AXES) || stack.getUseAnimation() == ItemUseAnimation.SPEAR || stack.getUseAnimation() == ItemUseAnimation.BLOCK) && !this.isHoldMyItemsShovel(stack)) {
            if (this.isHoldMyItemsWeapon(stack)) {
                matrices.translate(0.8 * (double)ll * (double)swingRot, 0.3 * (double)swingRot, -0.5 * (double)swing);
                matrices.rotate(Axis.YP.rotationDegrees(15.0f * swingRot * ll));
                matrices.rotate(Axis.XN.rotationDegrees(-20.0f * swingRot));
                matrices.rotate(Axis.ZP.rotationDegrees(-70.0f * swingRot * ll));
                matrices.rotate(Axis.XN.rotationDegrees((stack.is(net.minecraft.tags.ItemTags.SWORDS) ? 40.0f : 30.0f) * swing));
                return;
            }
            if (stack.getUseAnimation() == ItemUseAnimation.SPEAR) {
                matrices.translate(0.0, 0.0, 0.45 * (double)swingRot);
                matrices.translate(-0.25 * (double)handDirection * (double)swing, -0.35 * (double)swingRot, -0.6 * (double)swing);
                matrices.translate(0.0, 0.1 * (double)swing, 0.0);
                matrices.rotate(Axis.YP.rotationDegrees(15.0f * swingRot * ll));
                matrices.rotate(Axis.ZP.rotationDegrees(30.0f * swingRot * ll));
                return;
            }
            if (this.isHoldMyItemsTool(stack) && stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
                matrices.translate(0.1 * (double)ll * (double)swingRot, 0.1 * (double)swingRot, -0.5 * (double)swing);
                matrices.rotate(Axis.XN.rotationDegrees(-30.0f * swingRot));
                matrices.rotate(Axis.ZP.rotationDegrees(-20.0f * swingRot * ll));
                matrices.rotate(Axis.XN.rotationDegrees(40.0f * swing));
                return;
            }
            if (stack.getUseAnimation() != ItemUseAnimation.BLOCK) {
                matrices.translate(0.1 * (double)ll * (double)swingRot, 0.1 * (double)swingRot, -0.1 * (double)swing);
                matrices.rotate(Axis.XN.rotationDegrees(-30.0f * swingRot));
                matrices.rotate(Axis.ZP.rotationDegrees(-10.0f * swingRot * ll));
                matrices.rotate(Axis.XN.rotationDegrees(40.0f * swing));
                matrices.rotate(Axis.YP.rotationDegrees(10.0f * swing * ll));
                return;
            }
            matrices.translate(0.1 * (double)ll * (double)swingRot, 0.1 * (double)swingRot, -0.2 * (double)swing);
            matrices.rotate(Axis.XN.rotationDegrees(-10.0f * swingRot));
            matrices.rotate(Axis.ZP.rotationDegrees(-10.0f * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(20.0f * swing));
            return;
        }
        if (this.isHoldMyItemsShovel(stack)) {
            matrices.translate(0.0, 0.15 * (double)swingRot, -0.25 * (double)swingRot);
            matrices.translate(0.0, 0.0, -0.2 * (double)swing);
            matrices.rotate(Axis.YP.rotationDegrees(15.0f * swingRot));
            matrices.rotate(Axis.XP.rotationDegrees(-35.0f * swingRot));
            matrices.rotate(Axis.XP.rotationDegrees(30.0f * swing));
            return;
        }
        if (stack.is(net.minecraft.tags.ItemTags.SWORDS)) {
            matrices.translate(-0.55 * (double)ll * (double)swingRot, -0.8 * (double)swingRot, -0.77 * (double)swing);
            matrices.rotate(Axis.YP.rotationDegrees(5.0f * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(-30.0f * swingRot));
            matrices.rotate(Axis.ZP.rotationDegrees(70.0f * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(50.0f * swing));
            return;
        }
        if (this.isHoldMyItemsTool(stack)) {
            matrices.translate(0.1 * (double)ll * (double)swingRot, 0.1 * (double)swingRot, -0.5 * (double)swing);
            matrices.rotate(Axis.XN.rotationDegrees(-30.0f * swingRot));
            matrices.rotate(Axis.ZP.rotationDegrees(-20.0f * swingRot * ll));
            matrices.rotate(Axis.XN.rotationDegrees(40.0f * swing));
            return;
        }
        this.applyHoldMyItemsGenericSwing(matrices, ll, swingRot, swing);
    }

    private void applyChestRightHandMotion(PoseStack matrices, HumanoidArm arm) {
        if (arm != HumanoidArm.RIGHT || this.chestRightHandMotion <= 0.001f) {
            return;
        }
        float progress = this.chestRightHandMotion;
        float time = (float)(System.currentTimeMillis() % 1200L) / 1200.0f;
        float pulse = Mth.sin((float)(time * ((float)Math.PI * 2))) * progress;
        matrices.translate(0.04 * (double)progress, -0.03 * (double)progress + 0.01 * (double)pulse, -0.12 * (double)progress);
        matrices.rotate(Axis.YN.rotationDegrees(12.0f * progress));
        matrices.rotate(Axis.XP.rotationDegrees(8.0f * progress + 2.5f * pulse));
        matrices.rotate(Axis.ZP.rotationDegrees(-4.0f * progress));
    }

    private void applyHoldMyItemsUseJitter(PoseStack matrices, float useTicks, float progress) {
        if (progress <= 0.1f) {
            return;
        }
        float pulse = Mth.sin((float)((useTicks - 0.1f) * 1.3f));
        float offset = pulse * (progress - 0.1f);
        matrices.translate(0.0, (double)offset * 0.004, 0.0);
    }

    private void renderHMIBow(AbstractClientPlayer player, float tickDelta, InteractionHand handIn, float swingProgress, ItemStack stack, float equippedProgress, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light) {
        boolean isMainHand = handIn == InteractionHand.MAIN_HAND;
        HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean rightHand = arm == HumanoidArm.RIGHT;
        int handDirection = rightHand ? 1 : -1;
        float useTicks = (float)stack.getUseDuration((LivingEntity)player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0f);
        float drawLinear = Mth.clamp((float)(useTicks / 20.0f), (float)0.0f, (float)1.0f);
        matrices.pushPose();
        this.applyHandPositionBase(matrices, arm);
        this.applyHoldMyItemsEnvironment(matrices, player, handIn, arm, stack, swingProgress, tickDelta);
        matrices.pushPose();
        this.applyHoldMyItemsUseJitter(matrices, useTicks, drawLinear);
        matrices.translate(rightHand ? -0.1 : 0.1, 0.0, (double)drawLinear * 0.15);
        FirstPersonHandsAndItemsRenderer heldItemRenderer = Minecraft.getInstance().gameRenderer.firstPersonHandsAndItemsRenderer;
        if (heldItemRenderer instanceof HeldItemRendererAccessor) {
            ((HeldItemRendererAccessor)heldItemRenderer).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equippedProgress, swingProgress, arm, Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.playerRenderState);
        }
        matrices.popPose();
        matrices.pushPose();
        matrices.translate(rightHand ? -0.5 : 0.5, -0.45, 0.1);
        matrices.rotate(Axis.XP.rotation(0.3f));
        if (rightHand) {
            matrices.rotate(Axis.ZN.rotation(-0.3f));
            matrices.rotate(Axis.YN.rotation(1.0f));
            if (heldItemRenderer instanceof HeldItemRendererAccessor) {
                ((HeldItemRendererAccessor)heldItemRenderer).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equippedProgress, swingProgress, arm.getOpposite(), Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.playerRenderState);
            }
            matrices.rotate(Axis.YN.rotation(2.5f));
        } else {
            matrices.rotate(Axis.ZP.rotation(-0.3f));
            matrices.rotate(Axis.YP.rotation(1.0f));
            if (heldItemRenderer instanceof HeldItemRendererAccessor) {
                ((HeldItemRendererAccessor)heldItemRenderer).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, equippedProgress, swingProgress, arm.getOpposite(), Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.playerRenderState);
            }
            matrices.rotate(Axis.YP.rotation(2.5f));
        }
        matrices.translate(rightHand ? -0.65 : 0.65, -0.35, 0.27);
        matrices.popPose();
        matrices.rotate(Axis.XN.rotationDegrees(75.0f));
        matrices.rotate(Axis.ZN.rotationDegrees(-15.0f * (float)handDirection));
        matrices.translate(0.8 * (double)handDirection, (double)(-equippedProgress) * 0.3, -0.1);
        this.applyHoldMyItemsUseJitter(matrices, useTicks, drawLinear);
        this.applyHoldMyItemsItemPose(matrices, player, handIn, arm, stack, swingProgress);
        this.applyHandPositionItem(matrices, arm);
        this.renderItem((LivingEntity)player, stack, rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, !rightHand, matrices, vertexConsumers, light);
        matrices.popPose();
        this.holdMyItemsIsAttacking = Minecraft.getInstance().options.keyAttack.isDown();
    }

    private void renderHMIConsume(AbstractClientPlayer player, float tickDelta, InteractionHand handIn, float swingProgress, ItemStack stack, float equippedProgress, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light) {
        ItemStack consumeStack = player.getUseItem() != null && !player.getUseItem().isEmpty() && player.getUsedItemHand() == handIn ? player.getUseItem() : stack;
        boolean isMainHand = handIn == InteractionHand.MAIN_HAND;
        HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float useTicks = (float)consumeStack.getUseDuration((LivingEntity)player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0f);
        float progress = Mth.clamp((float)(useTicks / 5.0f), (float)0.0f, (float)1.0f);
        float wobble = Mth.sin((float)(useTicks / 2.0f * (float)Math.PI)) * 0.1f;
        matrices.pushPose();
        this.applyHandPositionBase(matrices, arm);
        matrices.translate((double)direction, 0.1, 0.3);
        matrices.translate(0.2 * (double)direction * (double)progress, -0.7 * (double)progress, -0.2 * (double)progress);
        matrices.translate(0.0, -0.2 * (double)wobble, -0.2 * (double)wobble);
        matrices.translate(0.0, 0.1 * (double)this.holdMyItemsEase(Mth.sin((float)(progress * (float)Math.PI))), 0.0);
        matrices.rotate(Axis.YP.rotationDegrees(45.0f * (float)direction));
        matrices.rotate(Axis.ZP.rotationDegrees(-40.0f * (float)direction));
        matrices.rotate(Axis.XP.rotationDegrees(30.0f));
        matrices.scale(0.9f, 0.9f, 0.9f);
        matrices.rotate(Axis.YP.rotationDegrees(45.0f * progress * (float)direction));
        FirstPersonHandsAndItemsRenderer heldItemRenderer = Minecraft.getInstance().gameRenderer.firstPersonHandsAndItemsRenderer;
        if (heldItemRenderer instanceof HeldItemRendererAccessor) {
            ((HeldItemRendererAccessor)heldItemRenderer).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, swingProgress, arm, Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.playerRenderState);
        }
        this.applyHoldMyItemsItemPose(matrices, player, handIn, arm, consumeStack, swingProgress);
        this.applyHandPositionItem(matrices, arm);
        this.renderItem((LivingEntity)player, consumeStack, arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, arm == HumanoidArm.LEFT, matrices, vertexConsumers, light);
        matrices.popPose();
        this.holdMyItemsIsAttacking = Minecraft.getInstance().options.keyAttack.isDown();
    }

    private void renderHMI(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light) {
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
            ((HeldItemRendererAccessor)heldItemRenderer).invokeRenderArmHoldingItem(matrices, vertexConsumers, light, 0.0f, 0.0f, arm, Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.playerRenderState);
        }
        this.applyHoldMyItemsItemPose(matrices, player, hand, arm, item, swingProgress);
        this.applyHandPositionItem(matrices, arm);
        this.renderItem((LivingEntity)player, item, arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, arm == HumanoidArm.LEFT, matrices, vertexConsumers, light);
        matrices.popPose();
    }
}

