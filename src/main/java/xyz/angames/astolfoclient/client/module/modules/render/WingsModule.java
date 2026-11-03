package xyz.angames.astolfoclient.client.module.modules.render;

import com.mojang.renderpearl.backend.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
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
import com.mojang.math.Axis;
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
    private final Minecraft mc = Minecraft.getInstance();
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
        LevelRenderEvents.END_MAIN.register(context -> {
            if (!this.isEnabled() || this.mc.player == null || this.mc.level == null || this.mc.gameRenderer == null) {
                return;
            }
            PoseStack stack = context.poseStack();
            float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            Vec3 camera = this.mc.gameRenderer.mainCamera().position();
            stack.pushPose();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            if (this.self.get() && !this.mc.options.getCameraType().isFirstPerson() && this.mc.player.isAlive() && !this.hasElytra((Player)this.mc.player)) {
                try {
                    this.renderWings(stack, (Player)this.mc.player, tickDelta, camera);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (this.players.get()) {
                for (Entity entity : this.mc.level.entitiesForRendering()) {
                    Player player;
                    if (!(entity instanceof Player) || (player = (Player)entity) == this.mc.player || !player.isAlive() || this.hasElytra(player)) continue;
                    try {
                        this.renderWings(stack, player, tickDelta, camera);
                    }
                    catch (Exception exception) {}
                }
            }
            RenderSystem.depthMask((boolean)true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.blendFuncSeparate((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE_MINUS_SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ZERO);
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            stack.popPose();
        });
    }

    @Override
    public void onDisable() {
        this.selfBodyYawInitialized = false;
    }

    private void renderWings(PoseStack stack, Player player, float tickDelta, Vec3 camera) {
        double x = Mth.lerp((double)tickDelta, (double)player.xo, (double)player.getX()) - camera.x;
        double y = Mth.lerp((double)tickDelta, (double)player.yo, (double)player.getY()) - camera.y;
        double z = Mth.lerp((double)tickDelta, (double)player.zo, (double)player.getZ()) - camera.z;
        float bodyYaw = this.resolveBodyYaw(player, tickDelta);
        float move = Mth.clamp((float)player.walkAnimation.speed(), (float)0.0f, (float)1.0f);
        WingPose pose = this.resolvePose(player, tickDelta);
        if (pose == null) {
            return;
        }
        float flap = (float)Math.sin(((float)player.tickCount + tickDelta) * pose.flapSpeed) * pose.flapAmplitude;
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
        stack.pushPose();
        stack.translate(x, y, z);
        if (isBaby) {
            stack.scale(0.5f, 0.5f, 0.5f);
        }
        stack.rotate(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        if (pose.preTranslateY != 0.0f || pose.preTranslateZ != 0.0f) {
            stack.translate(0.0f, pose.preTranslateY, pose.preTranslateZ);
        }
        if (pose.pitchRotation != 0.0f) {
            stack.rotate(Axis.XP.rotationDegrees(pose.pitchRotation));
        }
        if (pose.rollRotation != 0.0f) {
            stack.rotate(Axis.ZP.rotationDegrees(pose.rollRotation));
        }
        stack.translate(0.0f, pose.anchorY, pose.anchorZ);
        stack.scale(wingScale, wingScale, wingScale);
        this.renderWingSide(stack, -1.0f, open, baseColor, glowColor, coreColor, pose);
        this.renderWingSide(stack, 1.0f, open, baseColor, glowColor, coreColor, pose);
        stack.popPose();
    }

    private void renderWingSide(PoseStack stack, float side, float open, int baseColor, int glowColor, int coreColor, WingPose pose) {
        stack.pushPose();
        stack.translate(side * pose.sideOffset, pose.sideYOffset, pose.sideZOffset);
        stack.rotate(Axis.YP.rotationDegrees(side * open));
        stack.rotate(Axis.ZP.rotationDegrees(side * pose.sideRoll));
        stack.rotate(Axis.XP.rotationDegrees(pose.sidePitch));
        RenderSystem.blendFunc((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE);
        this.drawWingLayer(stack, side, 1.22f, WingsModule.setAlpha(glowColor, 48), WingsModule.setAlpha(glowColor, 0));
        this.drawWingLayer(stack, side, 0.84f, WingsModule.setAlpha(coreColor, 57), WingsModule.setAlpha(coreColor, 0));
        RenderSystem.blendFunc((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE_MINUS_SRC_ALPHA);
        this.drawWingLayer(stack, side, 1.0f, WingsModule.setAlpha(baseColor, 220), WingsModule.setAlpha(baseColor, 10));
        RenderSystem.blendFunc((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE);
        this.drawWingOutline(stack, side, 1.0f, WingsModule.setAlpha(baseColor, 136));
        this.drawWingRibs(stack, side, 0.96f, WingsModule.setAlpha(glowColor, 44));
        stack.popPose();
    }

    private void drawWingLayer(PoseStack stack, float side, float scale, int rootColor, int edgeColor) {
        Matrix4f matrix = stack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < SHAPE.length; ++i) {
            WingPoint cur = SHAPE[i];
            WingPoint next = SHAPE[(i + 1) % SHAPE.length];
            this.vertex(buffer, matrix, 0.0f, 0.0f, 0.0f, rootColor);
            this.vertex(buffer, matrix, side * cur.x * scale, cur.y * scale, 0.0f, this.applyPointAlpha(edgeColor, cur.alphaMul));
            this.vertex(buffer, matrix, side * next.x * scale, next.y * scale, 0.0f, this.applyPointAlpha(edgeColor, next.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
    }

    private void drawWingOutline(PoseStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.last().pose();
        RenderSystem.lineWidth((float)1.35f);
        BufferBuilder buffer = Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (WingPoint point : SHAPE) {
            this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0f, color);
        }
        this.vertex(buffer, matrix, side * WingsModule.SHAPE[0].x * scale, WingsModule.SHAPE[0].y * scale, 0.0f, color);
        BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
    }

    private void drawWingRibs(PoseStack stack, float side, float scale, int color) {
        Matrix4f matrix = stack.last().pose();
        int[] ribIndices = new int[]{2, 4, 7, 9, 11};
        RenderSystem.lineWidth((float)0.9f);
        BufferBuilder buffer = Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int idx : ribIndices) {
            WingPoint point = SHAPE[idx];
            this.vertex(buffer, matrix, 0.0f, 0.0f, 0.0f, WingsModule.setAlpha(color, Math.max(8, (int)((float)WingsModule.alpha(color) * 0.75f))));
            this.vertex(buffer, matrix, side * point.x * scale, point.y * scale, 0.0f, this.applyPointAlpha(color, point.alphaMul));
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
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
        return Mth.clamp((int)a, (int)0, (int)255) << 24 | r << 16 | g << 8 | b;
    }

    private static int setAlpha(int color, int a) {
        return Mth.clamp((int)a, (int)0, (int)255) << 24 | color & 0xFFFFFF;
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
        buffer.addVertex(matrix, x, y, z).setColor((float)WingsModule.red(color) / 255.0f, (float)WingsModule.green(color) / 255.0f, (float)WingsModule.blue(color) / 255.0f, (float)WingsModule.alpha(color) / 255.0f);
    }

    private float resolveBodyYaw(Player player, float tickDelta) {
        float target = Mth.rotLerp((float)tickDelta, (float)player.yBodyRotO, (float)player.yBodyRot);
        if (player != this.mc.player) {
            return target;
        }
        if (!this.selfBodyYawInitialized || player.tickCount < 2) {
            this.selfBodyYaw = target;
            this.selfBodyYawInitialized = true;
            return this.selfBodyYaw;
        }
        this.selfBodyYaw = WingsModule.approachDegrees(this.selfBodyYaw, target, 14.0f);
        return this.selfBodyYaw;
    }

    private static float approachDegrees(float current, float target, float maxDelta) {
        float delta = Mth.wrapDegrees((float)(target - current));
        delta = Mth.clamp((float)delta, (float)(-maxDelta), (float)maxDelta);
        return current + delta;
    }

    private WingPose resolvePose(Player player, float tickDelta) {
        float pitch = Mth.lerp((float)tickDelta, (float)player.xRotO, (float)player.getXRot());
        if (player.isFallFlying()) {
            float flightTicks = (float)player.getFallFlyingTicks() + tickDelta;
            float flightProgress = Mth.clamp((float)(flightTicks * flightTicks / 100.0f), (float)0.0f, (float)1.0f);
            float pitchRotation = flightProgress * (-90.0f - pitch);
            return new WingPose(0.34f, 0.46f, 0.0f, 0.0f, pitchRotation, 0.0f, 0.76f, 0.92f, 0.1f, 0.58f, 0.05f, 0.06f, -5.0f, -2.0f, 0.13f);
        }
        if (player.isInWater()) {
            return null;
        }
        if (player.isShiftKeyDown()) {
            return new WingPose(0.0f, 0.0f, 0.96f, 0.1f, 18.0f, 0.0f, 1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.02f, -11.0f, -4.0f, 0.12f);
        }
        return new WingPose(0.0f, 0.0f, 1.38f, 0.1f, 0.0f, 0.0f, 1.0f, 1.0f, 0.18f, 4.5f, 0.06f, 0.02f, -11.0f, -4.0f, 0.12f);
    }

    private boolean hasElytra(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
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

