package xyz.angames.astolfoclient.client.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.TrajectoriesModule;

@Environment(value=EnvType.CLIENT)
public class TrajectoriesRenderer {
    private static final Identifier BLOOM_TEXTURE = Identifier.of((String)"astolfoclient", (String)"textures/effects/bloom.png");
    private static final Identifier HIT_TEXTURE = Identifier.of((String)"astolfoclient", (String)"textures/effects/hit.png");
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<Integer, CachedPearl> cachedPearls = new HashMap<Integer, CachedPearl>();
    private final List<TimerTagInfo> timersToRender = new ArrayList<TimerTagInfo>();
    private final Matrix4f lastModelViewMatrix = new Matrix4f();
    private final Matrix4f lastProjectionMatrix = new Matrix4f();
    private double lastCamX;
    private double lastCamY;
    private double lastCamZ;
    private double lastScaledWidth;
    private double lastScaledHeight;
    private boolean hasMatrices = false;

    public void render(WorldRenderContext context) {
        float waterDrag;
        ItemStack stack;
        this.timersToRender.clear();
        this.hasMatrices = false;
        if (this.mc.world == null || this.mc.player == null) {
            return;
        }
        Module rawMod = AstolfoclientClient.moduleManager.getModuleByName("Trajectories");
        if (rawMod == null || !rawMod.isEnabled()) {
            return;
        }
        TrajectoriesModule mod = (TrajectoriesModule)rawMod;
        MatrixStack matrices = context.matrices();
        Vec3d camPos = context.gameRenderer().getCamera().getCameraPos();
        Quaternionf cameraRot = context.gameRenderer().getCamera().getRotation();
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
        Color themeColor = new Color(ThemeManager.getThemedColor(0L));
        this.lastModelViewMatrix.set((Matrix4fc)RenderSystem.getModelViewMatrix());
        this.lastProjectionMatrix.set((Matrix4fc)RenderSystem.getProjectionMatrix());
        this.lastCamX = camPos.x;
        this.lastCamY = camPos.y;
        this.lastCamZ = camPos.z;
        this.lastScaledWidth = this.mc.getWindow().getScaledWidth();
        this.lastScaledHeight = this.mc.getWindow().getScaledHeight();
        this.hasMatrices = true;
        if (mod.thrownPearls.get()) {
            long currentTime = System.currentTimeMillis();
            for (Entity entity : this.mc.world.getEntities()) {
                if (!(entity instanceof EnderPearlEntity)) continue;
                EnderPearlEntity pearl = (EnderPearlEntity)entity;
                CachedPearl cache = this.calculatePearlPath(pearl, tickDelta);
                cache.lastUpdateTime = currentTime;
                this.cachedPearls.put(pearl.getId(), cache);
            }
            Iterator<Map.Entry<Integer, CachedPearl>> it = this.cachedPearls.entrySet().iterator();
            while (it.hasNext()) {
                CachedPearl cache = it.next().getValue();
                int elapsedTicks = (int)((currentTime - cache.lastUpdateTime) / 50L);
                int currentTicksToLand = cache.ticksToLand - elapsedTicks;
                if (currentTicksToLand <= -5 || elapsedTicks > 300) {
                    it.remove();
                    continue;
                }
                this.drawCachedPearl(matrices, cache, camPos, cameraRot, themeColor, mod, Math.max(0, currentTicksToLand));
            }
        }
        if (!(!(stack = this.mc.player.getMainHandStack()).isEmpty() && this.isThrowable(stack.getItem()) || !(stack = this.mc.player.getOffHandStack()).isEmpty() && this.isThrowable(stack.getItem()))) {
            return;
        }
        Item item = stack.getItem();
        boolean isBow = item instanceof BowItem;
        boolean isCrossbow = item instanceof CrossbowItem;
        boolean isTrident = item instanceof TridentItem;
        float velocity = 1.5f;
        float gravity = 0.03f;
        float drag = 0.99f;
        float pitchOffset = 0.0f;
        if (isBow) {
            float charge = (float)(72000 - this.mc.player.getItemUseTimeLeft()) / 20.0f;
            if ((charge = (charge * charge + charge * 2.0f) / 3.0f) > 1.0f) {
                charge = 1.0f;
            }
            if (this.mc.player.getItemUseTimeLeft() == 0) {
                charge = 1.0f;
            }
            velocity = charge * 3.0f;
            gravity = 0.05f;
        } else if (isCrossbow) {
            velocity = 3.15f;
            gravity = 0.05f;
        } else if (isTrident) {
            velocity = 2.5f;
            gravity = 0.05f;
        } else if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            velocity = 0.5f;
            gravity = 0.05f;
            pitchOffset = -20.0f;
        } else if (item instanceof ExperienceBottleItem) {
            velocity = 0.7f;
            gravity = 0.07f;
            pitchOffset = -20.0f;
        }
        boolean multishot = isCrossbow && stack.getEnchantments().toString().contains("multishot");
        float f = waterDrag = isBow || isCrossbow || isTrident ? 0.6f : 0.8f;
        if (multishot) {
            this.simulateAndDraw(matrices, camPos, cameraRot, tickDelta, velocity, gravity, drag, waterDrag, themeColor, mod, -10.0f, pitchOffset);
            this.simulateAndDraw(matrices, camPos, cameraRot, tickDelta, velocity, gravity, drag, waterDrag, themeColor, mod, 10.0f, pitchOffset);
        }
        this.simulateAndDraw(matrices, camPos, cameraRot, tickDelta, velocity, gravity, drag, waterDrag, themeColor, mod, 0.0f, pitchOffset);
    }

    private void simulateAndDraw(MatrixStack matrices, Vec3d camPos, Quaternionf cameraRot, float tickDelta, float velocity, float gravity, float drag, float waterDrag, Color color, TrajectoriesModule mod, float yawOffset, float pitchOffset) {
        double yaw = MathHelper.lerp((float)tickDelta, (float)this.mc.player.lastYaw, (float)this.mc.player.getYaw()) + yawOffset;
        double pitch = MathHelper.lerp((float)tickDelta, (float)this.mc.player.lastPitch, (float)this.mc.player.getPitch()) + pitchOffset;
        double lerpX = MathHelper.lerp((double)tickDelta, (double)this.mc.player.lastRenderX, (double)this.mc.player.getX());
        double lerpY = MathHelper.lerp((double)tickDelta, (double)this.mc.player.lastRenderY, (double)this.mc.player.getY());
        double lerpZ = MathHelper.lerp((double)tickDelta, (double)this.mc.player.lastRenderZ, (double)this.mc.player.getZ());
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double posX = lerpX - Math.cos(yawRad) * 0.16;
        double posY = lerpY + (double)this.mc.player.getStandingEyeHeight() - 0.1;
        double posZ = lerpZ - Math.sin(yawRad) * 0.16;
        double motionX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double motionY = -Math.sin(pitchRad);
        double motionZ = Math.cos(yawRad) * Math.cos(pitchRad);
        double distance = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX = motionX / distance * (double)velocity;
        motionY = motionY / distance * (double)velocity;
        motionZ = motionZ / distance * (double)velocity;
        ArrayList<Vec3d> path = new ArrayList<Vec3d>();
        Vec3d currentPos = new Vec3d(posX, posY, posZ);
        HitResult hitResult = null;
        int ticksToLand = 0;
        for (int i = 0; i < 300; ++i) {
            path.add(new Vec3d(posX, posY, posZ));
            Vec3d nextPos = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
            hitResult = this.mc.world.raycast(new RaycastContext(currentPos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)this.mc.player));
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                nextPos = hitResult.getPos();
            }
            Box boundingBox = new Box(posX, posY, posZ, posX, posY, posZ).stretch(motionX, motionY, motionZ).expand(1.0);
            for (Entity entity : this.mc.world.getOtherEntities((Entity)this.mc.player, boundingBox, e -> e.canHit() && e.isAlive())) {
                Box entBox = entity.getBoundingBox().expand((double)0.3f);
                if (!entBox.contains(currentPos)) continue;
                hitResult = new EntityHitResult(entity);
                nextPos = currentPos;
                break;
            }
            posX = nextPos.x;
            posY = nextPos.y;
            posZ = nextPos.z;
            ++ticksToLand;
            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                path.add(new Vec3d(posX, posY, posZ));
                break;
            }
            BlockPos blockPos = BlockPos.ofFloored((double)posX, (double)posY, (double)posZ);
            float currentDrag = this.mc.world.getFluidState(blockPos).isEmpty() ? drag : waterDrag;
            motionX *= (double)currentDrag;
            motionY *= (double)currentDrag;
            motionZ *= (double)currentDrag;
            motionY -= (double)gravity;
            currentPos = nextPos;
        }
        this.drawZapLine(matrices, path, camPos, cameraRot, color, mod.drawThroughWalls.get(), 0.25f, true);
        if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
            ItemStack held;
            if (mod.showHitbox.get()) {
                this.drawLandingBox(matrices, (HitResult)hitResult, camPos, (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, mod.drawThroughWalls.get());
            }
            if ((held = this.mc.player.getMainHandStack()).isEmpty() || !(held.getItem() instanceof EnderPearlItem)) {
                held = this.mc.player.getOffHandStack();
            }
            if (!held.isEmpty() && held.getItem() instanceof EnderPearlItem) {
                this.collectCircularTimer(hitResult.getPos(), ticksToLand, camPos);
            }
        }
    }

    private void drawZapLine(MatrixStack matrices, List<Vec3d> path, Vec3d camPos, Quaternionf cameraRot, Color color, boolean drawThroughWalls, float scaleMultiplier, boolean drawAsLine) {
        if (path == null || path.isEmpty()) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
        RenderSystem.disableCull();
        if (drawThroughWalls) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask((boolean)false);
        }
        Tessellator tessellator = Tessellator.getInstance();
        float r = (float)color.getRed() / 255.0f;
        float g = (float)color.getGreen() / 255.0f;
        float b = (float)color.getBlue() / 255.0f;
        if (drawAsLine) {
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            RenderSystem.lineWidth((float)3.0f);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            Matrix4f mx = matrices.peek().getPositionMatrix();
            for (Vec3d point : path) {
                float lx = (float)(point.x - camPos.x);
                float ly = (float)(point.y - camPos.y);
                float lz = (float)(point.z - camPos.z);
                buffer.vertex(mx, lx, ly, lz).color(r, g, b, 1.0f);
            }
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            RenderSystem.lineWidth((float)1.0f);
        } else {
            RenderSystem.setShaderTexture((int)0, (Identifier)BLOOM_TEXTURE);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Vec3d playerPos = this.mc.player != null ? this.mc.player.getEntityPos() : Vec3d.ZERO;
            for (int i = 0; i < path.size() - 1; ++i) {
                Vec3d p2;
                Vec3d p1 = path.get(i);
                double segmentDist = p1.distanceTo(p2 = path.get(i + 1));
                if (segmentDist < 0.001) continue;
                for (int k = 0; k < 10; ++k) {
                    float t = (float)k / 10.0f;
                    Vec3d interpolatedPos = p1.add(p2.subtract(p1).multiply((double)t));
                    if (interpolatedPos.distanceTo(playerPos) <= 2.0) continue;
                    float size1 = (float)segmentDist / 3.0f * scaleMultiplier;
                    this.drawBloomGlow(matrices, buffer, interpolatedPos, camPos, cameraRot, size1, r, g, b, 1.0f);
                    float size2 = (float)segmentDist * 2.0f * scaleMultiplier;
                    this.drawBloomGlow(matrices, buffer, interpolatedPos, camPos, cameraRot, size2, r, g, b, 0.05f);
                }
            }
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
    }

    private void drawBloomGlow(MatrixStack matrices, BufferBuilder buffer, Vec3d pos, Vec3d camPos, Quaternionf cameraRot, float size, float r, float g, float b, float alpha) {
        matrices.push();
        matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
        matrices.multiply(cameraRot);
        Matrix4f mx = matrices.peek().getPositionMatrix();
        buffer.vertex(mx, -size / 2.0f, -size / 2.0f, 0.0f).texture(0.0f, 1.0f).color(r, g, b, alpha);
        buffer.vertex(mx, size / 2.0f, -size / 2.0f, 0.0f).texture(1.0f, 1.0f).color(r, g, b, alpha);
        buffer.vertex(mx, size / 2.0f, size / 2.0f, 0.0f).texture(1.0f, 0.0f).color(r, g, b, alpha);
        buffer.vertex(mx, -size / 2.0f, size / 2.0f, 0.0f).texture(0.0f, 0.0f).color(r, g, b, alpha);
        matrices.pop();
    }

    private CachedPearl calculatePearlPath(EnderPearlEntity pearl, float tickDelta) {
        CachedPearl cache = new CachedPearl();
        cache.path = new ArrayList<Vec3d>();
        double posX = MathHelper.lerp((double)tickDelta, (double)pearl.lastRenderX, (double)pearl.getX());
        double posY = MathHelper.lerp((double)tickDelta, (double)pearl.lastRenderY, (double)pearl.getY());
        double posZ = MathHelper.lerp((double)tickDelta, (double)pearl.lastRenderZ, (double)pearl.getZ());
        double motionX = pearl.getVelocity().x;
        double motionY = pearl.getVelocity().y;
        double motionZ = pearl.getVelocity().z;
        Vec3d currentPos = new Vec3d(posX, posY, posZ);
        int ticksToLand = 0;
        HitResult hitResult = null;
        for (int i = 0; i < 300; ++i) {
            cache.path.add(new Vec3d(posX, posY, posZ));
            Vec3d nextPos = new Vec3d(posX + motionX, posY + motionY, posZ + motionZ);
            hitResult = this.mc.world.raycast(new RaycastContext(currentPos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, (Entity)pearl));
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                nextPos = hitResult.getPos();
            }
            Box boundingBox = new Box(posX, posY, posZ, posX, posY, posZ).stretch(motionX, motionY, motionZ).expand(1.0);
            for (Entity entity : this.mc.world.getOtherEntities((Entity)pearl, boundingBox, e -> e.canHit() && e.isAlive())) {
                Box entBox = entity.getBoundingBox().expand((double)0.3f);
                if (!entBox.contains(currentPos)) continue;
                hitResult = new EntityHitResult(entity);
                nextPos = currentPos;
                break;
            }
            posX = nextPos.x;
            posY = nextPos.y;
            posZ = nextPos.z;
            ++ticksToLand;
            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                cache.path.add(new Vec3d(posX, posY, posZ));
                break;
            }
            BlockPos blockPos = BlockPos.ofFloored((double)posX, (double)posY, (double)posZ);
            double currentDrag = this.mc.world.getFluidState(blockPos).isEmpty() ? 0.99 : 0.8;
            motionX *= currentDrag;
            motionY *= currentDrag;
            motionZ *= currentDrag;
            motionY -= 0.03;
            currentPos = nextPos;
        }
        cache.hitResult = hitResult;
        cache.ticksToLand = ticksToLand;
        return cache;
    }

    private void drawCachedPearl(MatrixStack matrices, CachedPearl cache, Vec3d camPos, Quaternionf cameraRot, Color color, TrajectoriesModule mod, int ticksToLand) {
        this.drawZapLine(matrices, cache.path, camPos, cameraRot, color, mod.drawThroughWalls.get(), 1.0f, false);
        if (cache.hitResult != null && cache.hitResult.getType() != HitResult.Type.MISS) {
            if (mod.showHitbox.get()) {
                this.drawLandingBox(matrices, cache.hitResult, camPos, (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, mod.drawThroughWalls.get());
            }
            this.collectCircularTimer(cache.hitResult.getPos(), ticksToLand, camPos);
        }
    }

    private void collectCircularTimer(Vec3d hitPos, int ticksToLand, Vec3d camPos) {
        double dx = hitPos.x - camPos.x;
        double dy = hitPos.y + 0.75 - camPos.y;
        double dz = hitPos.z - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Vector3f screenPos = this.project3DTo2D(hitPos.x, hitPos.y + 0.75, hitPos.z);
        if (screenPos != null) {
            this.timersToRender.add(new TimerTagInfo(hitPos, ticksToLand, dist, screenPos.x, screenPos.y));
        }
    }

    private Vector3f project3DTo2D(double x, double y, double z) {
        float rx = (float)(x - this.lastCamX);
        float ry = (float)(y - this.lastCamY);
        float rz = (float)(z - this.lastCamZ);
        Vector4f pos = new Vector4f(rx, ry, rz, 1.0f);
        this.lastModelViewMatrix.transform(pos);
        this.lastProjectionMatrix.transform(pos);
        if (pos.w() <= 0.0f) {
            return null;
        }
        float ndcX = pos.x() / pos.w();
        float ndcY = pos.y() / pos.w();
        float screenX = (ndcX + 1.0f) * 0.5f * (float)this.lastScaledWidth;
        float screenY = (1.0f - ndcY) * 0.5f * (float)this.lastScaledHeight;
        return new Vector3f(screenX, screenY, pos.w());
    }

    public void renderHUD(DrawContext drawContext) {
        Module rawMod = AstolfoclientClient.moduleManager.getModuleByName("Trajectories");
        if (rawMod == null || !rawMod.isEnabled()) {
            return;
        }
        TrajectoriesModule mod = (TrajectoriesModule)rawMod;
        if (!this.hasMatrices || this.timersToRender.isEmpty()) {
            return;
        }
        var matrices = drawContext.getMatrices();
        Color themeColor = new Color(ThemeManager.getThemedColor(0L));
        this.timersToRender.sort((t1, t2) -> Double.compare(t2.distance, t1.distance));
        for (TimerTagInfo tag : this.timersToRender) {
            matrices.pushMatrix();
            matrices.translate((float)tag.x, (float)tag.y);
            float scale = (float)(mod.scale.get() * (10.0 / Math.min(10.0, Math.max(2.0, tag.distance))));
            scale = (float)Math.min((double)scale, mod.scale.get() * 2.0);
            matrices.scale(scale *= 0.8f, scale);
            this.drawCircularTimer2D(drawContext, tag.ticksToLand, themeColor, mod);
            matrices.popMatrix();
        }
    }

    private void drawCircularTimer2D(DrawContext context, int ticksToLand, Color themeColor, TrajectoriesModule mod) {
        var matrices = context.getMatrices();
        float seconds = (float)ticksToLand / 20.0f;
        String text = String.format("%.1fs", Float.valueOf(seconds));
        TextRenderer tr = this.mc.textRenderer;
        float textWidth = tr.getWidth(text);
        float circleSize = Math.max(20.0f, textWidth + 8.0f);
        matrices.pushMatrix();
        Matrix4f mx = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(matrices);
        float cx = -circleSize / 2.0f;
        float cy = -circleSize / 2.0f;
        if (mod.background.get()) {
            int alphaInt = (int)((float)mod.bgOpacity.get() * 255.0f);
            if (mod.glow.get()) {
                int layers = 8;
                float maxSpread = 6.0f;
                for (int i = layers; i > 0; --i) {
                    float progress = (float)i / (float)layers;
                    float fade = 1.0f - progress;
                    float alpha = fade * fade * 0.25f * (float)mod.bgOpacity.get();
                    int currentAlpha = MathHelper.clamp((int)((int)(255.0f * alpha)), (int)0, (int)255);
                    if (currentAlpha <= 0) continue;
                    float expand = progress * maxSpread;
                    Color layerColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), currentAlpha);
                    ((BuiltRectangle)Builder.rectangle().size(new SizeState(circleSize + expand * 2.0f, circleSize + expand * 2.0f)).radius(new QuadRadiusState((circleSize + expand * 2.0f) / 2.0f)).color(new QuadColorState(layerColor)).build()).render(mx, cx - expand, cy - expand);
                }
            }
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(circleSize, circleSize)).radius(new QuadRadiusState(circleSize / 2.0f)).color(new QuadColorState(new Color(5, 5, 8, Math.min(245, alphaInt)))).build()).render(mx, cx, cy);
        }
        float textX = -(textWidth / 2.0f);
        float textY = -3.5f;
        matrices.translate(textX, textY);
        context.drawText(tr, text, 0, 0, -1, true);
        matrices.popMatrix();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }

    private void drawLandingBox(MatrixStack matrices, HitResult hit, Vec3d camPos, float r, float g, float b, boolean drawThroughWalls) {
        Tessellator tessellator = Tessellator.getInstance();
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult)hit;
            Vec3d hitPos = blockHit.getPos();
            float size = 1.0f;
            double hX = hitPos.x - camPos.x;
            double hY = hitPos.y - camPos.y;
            double hZ = hitPos.z - camPos.z;
            switch (blockHit.getSide()) {
                case UP: {
                    hY += 0.005;
                    break;
                }
                case DOWN: {
                    hY -= 0.005;
                    break;
                }
                case NORTH: {
                    hZ -= 0.005;
                    break;
                }
                case SOUTH: {
                    hZ += 0.005;
                    break;
                }
                case WEST: {
                    hX -= 0.005;
                    break;
                }
                case EAST: {
                    hX += 0.005;
                }
            }
            matrices.push();
            matrices.translate(hX, hY, hZ);
            matrices.multiply(blockHit.getSide().getRotationQuaternion());
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-90.0f));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc((com.mojang.blaze3d.platform.SourceFactor)com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.DestFactor)com.mojang.blaze3d.platform.DestFactor.ONE);
            RenderSystem.disableCull();
            if (drawThroughWalls) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask((boolean)false);
            } else {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask((boolean)true);
            }
            RenderSystem.setShaderTexture((int)0, (Identifier)HIT_TEXTURE);
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f mx = matrices.peek().getPositionMatrix();
            buffer.vertex(mx, -size / 3.0f, size / 3.0f, 0.0f).texture(0.0f, 1.0f).color(r, g, b, 1.0f);
            buffer.vertex(mx, size / 3.0f, size / 3.0f, 0.0f).texture(1.0f, 1.0f).color(r, g, b, 1.0f);
            buffer.vertex(mx, size / 3.0f, -size / 3.0f, 0.0f).texture(1.0f, 0.0f).color(r, g, b, 1.0f);
            buffer.vertex(mx, -size / 3.0f, -size / 3.0f, 0.0f).texture(0.0f, 0.0f).color(r, g, b, 1.0f);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            matrices.pop();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)true);
        } else if (hit.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult)hit).getEntity();
            Box bBox = entity.getBoundingBox().offset(-camPos.x, -camPos.y, -camPos.z);
            Matrix4f mx = matrices.peek().getPositionMatrix();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            if (drawThroughWalls) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask((boolean)false);
            } else {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask((boolean)true);
            }
            RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            this.drawBoxLines(buffer, mx, bBox, r, g, b, 1.0f);
            BufferRenderer.drawWithGlobalProgram((BuiltBuffer)buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)true);
        }
    }

    private void drawBoxLines(BufferBuilder buffer, Matrix4f mx, Box box, float r, float g, float b, float a) {
        float x1 = (float)box.minX;
        float y1 = (float)box.minY;
        float z1 = (float)box.minZ;
        float x2 = (float)box.maxX;
        float y2 = (float)box.maxY;
        float z2 = (float)box.maxZ;
        buffer.vertex(mx, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(mx, x2, y1, z1).color(r, g, b, a);
        buffer.vertex(mx, x2, y1, z1).color(r, g, b, a);
        buffer.vertex(mx, x2, y1, z2).color(r, g, b, a);
        buffer.vertex(mx, x2, y1, z2).color(r, g, b, a);
        buffer.vertex(mx, x1, y1, z2).color(r, g, b, a);
        buffer.vertex(mx, x1, y1, z2).color(r, g, b, a);
        buffer.vertex(mx, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(mx, x1, y2, z1).color(r, g, b, a);
        buffer.vertex(mx, x2, y2, z1).color(r, g, b, a);
        buffer.vertex(mx, x2, y2, z1).color(r, g, b, a);
        buffer.vertex(mx, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(mx, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(mx, x1, y2, z2).color(r, g, b, a);
        buffer.vertex(mx, x1, y2, z2).color(r, g, b, a);
        buffer.vertex(mx, x1, y2, z1).color(r, g, b, a);
        buffer.vertex(mx, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(mx, x1, y2, z1).color(r, g, b, a);
        buffer.vertex(mx, x2, y1, z1).color(r, g, b, a);
        buffer.vertex(mx, x2, y2, z1).color(r, g, b, a);
        buffer.vertex(mx, x2, y1, z2).color(r, g, b, a);
        buffer.vertex(mx, x2, y2, z2).color(r, g, b, a);
        buffer.vertex(mx, x1, y1, z2).color(r, g, b, a);
        buffer.vertex(mx, x1, y2, z2).color(r, g, b, a);
    }

    private boolean isThrowable(Item item) {
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem || item instanceof EnderPearlItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem || item instanceof ExperienceBottleItem;
    }

    @Environment(value=EnvType.CLIENT)
    private static class CachedPearl {
        List<Vec3d> path;
        HitResult hitResult;
        int ticksToLand;
        long lastUpdateTime;

        private CachedPearl() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class TimerTagInfo {
        Vec3d hitPos;
        int ticksToLand;
        double distance;
        double x;
        double y;

        public TimerTagInfo(Vec3d hitPos, int ticksToLand, double distance, double x, double y) {
            this.hitPos = hitPos;
            this.ticksToLand = ticksToLand;
            this.distance = distance;
            this.x = x;
            this.y = y;
        }
    }
}

