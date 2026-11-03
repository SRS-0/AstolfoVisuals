package xyz.angames.astolfoclient.client.module.modules.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.BabyPlayerModule;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(value=EnvType.CLIENT)
public class WingsModule
extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private static final float DEFAULT_SPREAD = 8.0f;
    private static final int DEFAULT_ALPHA = 220;
    private static final WingPoint[] SHAPE = new WingPoint[]{new WingPoint(0.08f, 0.1f, 0.88f), new WingPoint(0.28f, 0.34f, 0.78f), new WingPoint(0.56f, 0.82f, 0.62f), new WingPoint(0.86f, 0.3f, 0.52f), new WingPoint(1.14f, 0.46f, 0.4f), new WingPoint(1.24f, 0.04f, 0.3f), new WingPoint(1.02f, -0.18f, 0.28f), new WingPoint(1.18f, -0.64f, 0.22f), new WingPoint(0.86f, -0.46f, 0.2f), new WingPoint(0.8f, -0.98f, 0.14f), new WingPoint(0.54f, -0.74f, 0.16f), new WingPoint(0.3f, -1.16f, 0.12f), new WingPoint(0.1f, -0.54f, 0.18f)};
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
        WorldRenderEvents.END_MAIN.register(context -> {
            if (!this.isEnabled() || this.mc.player == null || this.mc.world == null || this.mc.gameRenderer == null) {
                return;
            }
            MatrixStack stack = context.matrices();
            float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
            Vec3d camera = this.mc.gameRenderer.getCamera().getCameraPos();
            stack.push();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            if (this.self.get() && !this.mc.options.getPerspective().isFirstPerson() && this.mc.player.isAlive() && !this.hasElytra((PlayerEntity)this.mc.player)) {
                try {
                    this.renderWings(stack, (PlayerEntity)this.mc.player, tickDelta, camera);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (this.players.get()) {
                for (Entity entity : this.mc.world.getEntities()) {
                    PlayerEntity player;
                    if (!(entity instanceof PlayerEntity) || (player = (PlayerEntity)entity) == this.mc.player || !player.isAlive() || this.hasElytra(player)) continue;
                    try {
                        this.renderWings(stack, player, tickDelta, camera);
                    }
                    catch (Exception exception) {}
                }
            }
            RenderSystem.depthMask((boolean)true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.blendFuncSeparate((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_SRC_ALPHA, (com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.ONE, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ZERO);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            stack.pop();
        });
    }

    @Override
    public void onDisable() {
        this.selfBodyYawInitialized = false;
    }

    private void renderWings(MatrixStack stack, PlayerEntity player, float tickDelta, Vec3d camera) {
        double x = MathHelper.lerp((double)tickDelta, (double)player.lastX, (double)player.getX()) - camera.x;
        double y = MathHelper.lerp((double)tickDelta, (double)player.lastY, (double)player.getY()) - camera.y;
        double z = MathHelper.lerp((double)tickDelta, (double)player.lastZ, (double)player.getZ()) - camera.z;
        float bodyYaw = this.resolveBodyYaw(player, tickDelta);
        float move = MathHelper.clamp((float)player.limbAnimator.getSpeed(), (float)0.0f, (float)1.0f);
        WingPose pose = this.resolvePose(player, tickDelta);
        if (pose == null) {
            return;
        }
        float flap = (float)Math.sin(((float)player.age + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
        float open = (8.0f + flap + move * pose.motionSpreadBoost) * pose.openMultiplier;
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
        stack.push();
        stack.translate(x, y, z);
        if (isBaby) {
            stack.scale(0.5f, 0.5f, 0.5f);
        }
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - bodyYaw));
        if (pose.preTranslateY != 0.0f || pose.preTranslateZ != 0.0f) {
            stack.translate(0.0f, pose.preTranslateY, pose.preTranslateZ);
        }
        if (pose.pitchRotation != 0.0f) {
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
        }
        if (pose.rollRotation != 0.0f) {
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pose.rollRotation));
        }
        stack.translate(0.0f, pose.anchorY, pose.anchorZ);
        stack.scale(wingScale, wingScale, wingScale);
        this.renderWingSide(stack, -1.0f, open, baseColor, glowColor, coreColor, pose);
        this.renderWingSide(stack, 1.0f, open, baseColor, glowColor, coreColor, pose);
        stack.pop();
    }

    private void renderWingSide(MatrixStack stack, float side, float open, int baseColor, int glowColor, int coreColor, WingPose pose) {
        stack.push();
        stack.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(side * open));
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * pose.sideRoll));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitch));
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
        this.drawWingLayer(stack, side, 1.22f, WingsModule.setAlpha(glowColor, 48), WingsModule.setAlpha(glowColor, 0));
        this.drawWingLayer(stack, side, 0.84f, WingsModule.setAlpha(coreColor, 57), WingsModule.setAlpha(coreColor, 0));
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_SRC_ALPHA);
        this.drawWingLayer(stack, side, 1.0f, WingsModule.setAlpha(baseColor, 220), WingsModule.setAlpha(baseColor, 10));
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
        this.drawWingOutline(stack, side, 1.0f, WingsModule.setAlpha(baseColor, 136));
        this.drawWingRibs(stack, side, 0.96f, WingsModule.setAlpha(glowColor, 44));
        stack.pop();
    }

    private void drawWingLayer(MatrixStack stack, float side, float scale, int rootColor, int edgeColor) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SHAPE.length; ++i) {
            WingPoint cur = SHAPE[i];
            WingPoint next = SHAPE[(i + 1) % SHAPE.length];
            this.vertex(buffer, matrix, 0.0f, 0.0f, 0.0f, rootColor);
            this.vertex(buffer, matrix, side * cur.x * scale, cur.y * scale, 0.0f, this.applyPointAlpha(edgeColor, cur.alphaMul));
            this.vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0.0f, this.applyPointAlpha(edgeColor, next.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void drawWingOutline(MatrixStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        RenderSystem.lineWidth((float)1.35f);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (WingPoint point : SHAPE) {
            this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0f, color);
        }
        this.vertex(buffer, matrix, side * WingsModule.SHAPE[0].x * scale, WingsModule.SHAPE[0].y * scale, 0.0f, color);
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private void drawWingRibs(MatrixStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        int[] ribIndices = new int[]{2, 4, 7, 9, 11};
        RenderSystem.lineWidth((float)0.9f);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int idx : ribIndices) {
            WingPoint point = SHAPE[idx];
            this.vertex(buffer, matrix, 0.0f, 0.0f, 0.0f, WingsModule.setAlpha(color, Math.max(8, (int)((float)WingsModule.alpha(color) * 0.75f))));
            this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0f, this.applyPointAlpha(color, point.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
    }

    private int resolveBaseColor() {
        long now = System.currentTimeMillis();
        int colorInt = ThemeManager.getThemedColor(now / 10L);
        return WingsModule.setAlpha(colorInt, 255);
    }

    private int resolveGlowColor(int base) {
        return WingsModule.interpolateColor(base, WingsModule.packARGB(255, 255, 255, 255), 0.28f);
    }

    private int resolveCoreColor(int base) {
        return WingsModule.interpolateColor(base, WingsModule.packARGB(255, 255, 255, 255), 0.55f);
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
        int r = (int)((float)ar + (float)(br - ar) * t);
        int g = (int)((float)ag + (float)(bg - ag) * t);
        int bl2 = (int)((float)ab + (float)(bb - ab) * t);
        int al = (int)((float)aa + (float)(ba - aa) * t);
        return WingsModule.packARGB(r, g, bl2, al);
    }

    private static int packARGB(int r, int g, int b, int a) {
        return MathHelper.clamp((int)a, (int)0, (int)255) << 24 | r << 16 | g << 8 | b;
    }

    private static int setAlpha(int color, int a) {
        return MathHelper.clamp((int)a, (int)0, (int)255) << 24 | color & 0xFFFFFF;
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
        return WingsModule.setAlpha(color, Math.max(0, Math.min(255, (int)((float)WingsModule.alpha(color) * multiplier))));
    }

    private void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, int color) {
        buffer.vertex(matrix, x, y, z).color((float)WingsModule.red(color) / 255.0f, (float)WingsModule.green(color) / 255.0f, (float)WingsModule.blue(color) / 255.0f, (float)WingsModule.alpha(color) / 255.0f);
    }

    private float resolveBodyYaw(PlayerEntity player, float tickDelta) {
        float target = MathHelper.lerpAngleDegrees((float)tickDelta, (float)player.lastBodyYaw, (float)player.bodyYaw);
        if (player != this.mc.player) {
            return target;
        }
        if (!this.selfBodyYawInitialized || player.age < 2) {
            this.selfBodyYaw = target;
            this.selfBodyYawInitialized = true;
            return this.selfBodyYaw;
        }
        this.selfBodyYaw = WingsModule.approachDegrees(this.selfBodyYaw, target, 14.0f);
        return this.selfBodyYaw;
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = MathHelper.wrapDegrees((float)(target - current));
        delta = MathHelper.clamp((float)delta, (float)(-maxDelta), (float)maxDelta);
        return current + delta;
    }

    private WingPose resolvePose(PlayerEntity player, float tickDelta) {
        float pitch = MathHelper.lerp((float)tickDelta, (float)player.lastPitch, (float)player.getPitch());
        if (player.isGliding()) {
            float flightTicks = (float)player.getGlidingTicks() + tickDelta;
            float flightProgress = MathHelper.clamp((float)(flightTicks * flightTicks / 100.0f), (float)0.0f, (float)1.0f);
            float pitchRotation = flightProgress * (-90.0f - pitch);
            return new WingPose(0.34f, 0.46f, 0.0f, 0.0f, pitchRotation, 0.0f, 0.76f, 0.92f, 0.1f, 0.58f, 0.05f, 0.06f, -5.0f, -2.0f, 0.13f);
        }
        if (player.isTouchingWater()) {
            return null;
        }
        if (player.isSneaking()) {
            return new WingPose(0.0f, 0.0f, 0.96f, 0.1f, 18.0f, 0.0f, 1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.02f, -11.0f, -4.0f, 0.12f);
        }
        return new WingPose(0.0f, 0.0f, 1.38f, 0.1f, 0.0f, 0.0f, 1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.02f, -11.0f, -4.0f, 0.12f);
    }

    private boolean hasElytra(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    @Override
    public void onTick() {
    }

    @Environment(value=EnvType.CLIENT)
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

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ, float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier, float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideZOffset, float sideRoll, float sidePitch, float flapSpeed) {
            this(preTranslateY, preTranslateZ, anchorY, anchorZ, pitchRotation, rollRotation, openMultiplier, scaleMultiplier, motionSpreadBoost, flapAmplitude, sideOffset, 0.0f, sideZOffset, sideRoll, sidePitch, flapSpeed);
        }

        WingPose(float preTranslateY, float preTranslateZ, float anchorY, float anchorZ, float pitchRotation, float rollRotation, float openMultiplier, float scaleMultiplier, float motionSpreadBoost, float flapAmplitude, float sideOffset, float sideYOffset, float sideZOffset, float sideRoll, float sidePitch, float flapSpeed) {
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

    @Environment(value=EnvType.CLIENT)
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
}

