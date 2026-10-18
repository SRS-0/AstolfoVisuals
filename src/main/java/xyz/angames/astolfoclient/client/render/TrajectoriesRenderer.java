package xyz.angames.astolfoclient.client.render;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.phys.HitResult.Type;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.ClipContext.Block;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.msdf.MsdfFont;
import xyz.angames.astolfoclient.client.compat.GuiCompat;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.TrajectoriesModule;

@Environment(EnvType.CLIENT)
public class TrajectoriesRenderer {
   private static final Supplier<MsdfFont> TIMER_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
   private static final Identifier BLOOM_TEXTURE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/bloom.png");
   private static final Identifier HIT_TEXTURE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/hit.png");
   private final Minecraft mc = Minecraft.getInstance();
   private final Map<Integer, TrajectoriesRenderer.CachedPearl> cachedPearls = new HashMap<>();
   private final List<TrajectoriesRenderer.TimerTagInfo> timersToRender = new ArrayList<>();
   private final Matrix4f lastModelViewMatrix = new Matrix4f();
   private final Matrix4f lastProjectionMatrix = new Matrix4f();
   private double lastCamX;
   private double lastCamY;
   private double lastCamZ;
   private double lastScaledWidth;
   private double lastScaledHeight;
   private boolean hasMatrices = false;

   public void render(WorldRenderContext context) {
      this.timersToRender.clear();
      this.hasMatrices = false;
      if (this.mc.level != null && this.mc.player != null) {
         Module rawMod = AstolfoclientClient.moduleManager.getModuleByName("Trajectories");
         if (rawMod != null && rawMod.isEnabled()) {
            TrajectoriesModule mod = (TrajectoriesModule)rawMod;
            PoseStack matrices = context.matrixStack();
            Vec3 camPos = context.camera().position();
            Quaternionf cameraRot = context.camera().rotation();
            float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
            Color themeColor = new Color(ThemeManager.getThemedColor(0L));
            this.lastModelViewMatrix.set(RenderSystem.getModelViewMatrix());
            this.lastProjectionMatrix.set(RenderSystem.getProjectionMatrix());
            this.lastCamX = camPos.x;
            this.lastCamY = camPos.y;
            this.lastCamZ = camPos.z;
            this.lastScaledWidth = this.mc.getWindow().getGuiScaledWidth();
            this.lastScaledHeight = this.mc.getWindow().getGuiScaledHeight();
            this.hasMatrices = true;
            if (mod.thrownPearls.get()) {
               long currentTime = System.currentTimeMillis();

               for (Entity entity : this.mc.level.entitiesForRendering()) {
                  if (entity instanceof ThrownEnderpearl pearl) {
                     TrajectoriesRenderer.CachedPearl cache = this.calculatePearlPath(pearl, tickDelta);
                     cache.lastUpdateTime = currentTime;
                     this.cachedPearls.put(pearl.getId(), cache);
                  }
               }

               Iterator<Entry<Integer, TrajectoriesRenderer.CachedPearl>> it = this.cachedPearls.entrySet().iterator();

               while (it.hasNext()) {
                  TrajectoriesRenderer.CachedPearl cache = it.next().getValue();
                  int elapsedTicks = (int)((currentTime - cache.lastUpdateTime) / 50L);
                  int currentTicksToLand = cache.ticksToLand - elapsedTicks;
                  if (currentTicksToLand > -5 && elapsedTicks <= 300) {
                     this.drawCachedPearl(matrices, cache, camPos, cameraRot, themeColor, mod, Math.max(0, currentTicksToLand));
                  } else {
                     it.remove();
                  }
               }
            }

            ItemStack stack = this.mc.player.getMainHandItem();
            if (stack.isEmpty() || !this.isThrowable(stack.getItem())) {
               stack = this.mc.player.getOffhandItem();
               if (stack.isEmpty() || !this.isThrowable(stack.getItem())) {
                  return;
               }
            }

            Item item = stack.getItem();
            boolean isBow = item instanceof BowItem;
            boolean isCrossbow = item instanceof CrossbowItem;
            boolean isTrident = item instanceof TridentItem;
            float velocity = 1.5F;
            float gravity = 0.03F;
            float drag = 0.99F;
            float pitchOffset = 0.0F;
            if (isBow) {
               float charge = (72000 - this.mc.player.getUseItemRemainingTicks()) / 20.0F;
               charge = (charge * charge + charge * 2.0F) / 3.0F;
               if (charge > 1.0F) {
                  charge = 1.0F;
               }

               if (this.mc.player.getUseItemRemainingTicks() == 0) {
                  charge = 1.0F;
               }

               velocity = charge * 3.0F;
               gravity = 0.05F;
            } else if (isCrossbow) {
               velocity = 3.15F;
               gravity = 0.05F;
            } else if (isTrident) {
               velocity = 2.5F;
               gravity = 0.05F;
            } else if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
               velocity = 0.5F;
               gravity = 0.05F;
               pitchOffset = -20.0F;
            } else if (item instanceof ExperienceBottleItem) {
               velocity = 0.7F;
               gravity = 0.07F;
               pitchOffset = -20.0F;
            }

            boolean multishot = isCrossbow && stack.getEnchantments().toString().contains("multishot");
            float waterDrag = !isBow && !isCrossbow && !isTrident ? 0.8F : 0.6F;
            if (multishot) {
               this.simulateAndDraw(matrices, camPos, cameraRot, tickDelta, velocity, gravity, drag, waterDrag, themeColor, mod, -10.0F, pitchOffset);
               this.simulateAndDraw(matrices, camPos, cameraRot, tickDelta, velocity, gravity, drag, waterDrag, themeColor, mod, 10.0F, pitchOffset);
            }

            this.simulateAndDraw(matrices, camPos, cameraRot, tickDelta, velocity, gravity, drag, waterDrag, themeColor, mod, 0.0F, pitchOffset);
         }
      }
   }

   private void simulateAndDraw(
      PoseStack matrices,
      Vec3 camPos,
      Quaternionf cameraRot,
      float tickDelta,
      float velocity,
      float gravity,
      float drag,
      float waterDrag,
      Color color,
      TrajectoriesModule mod,
      float yawOffset,
      float pitchOffset
   ) {
      double yaw = Mth.lerp(tickDelta, this.mc.player.yRotO, this.mc.player.getYRot()) + yawOffset;
      double pitch = Mth.lerp(tickDelta, this.mc.player.xRotO, this.mc.player.getXRot()) + pitchOffset;
      double lerpX = Mth.lerp(tickDelta, this.mc.player.xOld, this.mc.player.getX());
      double lerpY = Mth.lerp(tickDelta, this.mc.player.yOld, this.mc.player.getY());
      double lerpZ = Mth.lerp(tickDelta, this.mc.player.zOld, this.mc.player.getZ());
      double yawRad = Math.toRadians(yaw);
      double pitchRad = Math.toRadians(pitch);
      double posX = lerpX - Math.cos(yawRad) * 0.16;
      double posY = lerpY + this.mc.player.getEyeHeight() - 0.1;
      double posZ = lerpZ - Math.sin(yawRad) * 0.16;
      double motionX = -Math.sin(yawRad) * Math.cos(pitchRad);
      double motionY = -Math.sin(pitchRad);
      double motionZ = Math.cos(yawRad) * Math.cos(pitchRad);
      double distance = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
      motionX = motionX / distance * velocity;
      motionY = motionY / distance * velocity;
      motionZ = motionZ / distance * velocity;
      List<Vec3> path = new ArrayList<>();
      Vec3 currentPos = new Vec3(posX, posY, posZ);
      HitResult hitResult = null;
      int ticksToLand = 0;

      for (int i = 0; i < 300; i++) {
         path.add(new Vec3(posX, posY, posZ));
         Vec3 nextPos = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
         hitResult = this.mc.level.clip(new ClipContext(currentPos, nextPos, Block.COLLIDER, Fluid.NONE, this.mc.player));
         if (hitResult != null && hitResult.getType() == Type.BLOCK) {
            nextPos = hitResult.getLocation();
         }

         AABB boundingBox = new AABB(posX, posY, posZ, posX, posY, posZ).expandTowards(motionX, motionY, motionZ).inflate(1.0);

         for (Entity entity : this.mc.level.getEntities(this.mc.player, boundingBox, e -> e.isPickable() && e.isAlive())) {
            AABB entBox = entity.getBoundingBox().inflate(0.3F);
            if (entBox.contains(currentPos)) {
               hitResult = new EntityHitResult(entity);
               nextPos = currentPos;
               break;
            }
         }

         posX = nextPos.x;
         posY = nextPos.y;
         posZ = nextPos.z;
         ticksToLand++;
         if (hitResult != null && hitResult.getType() != Type.MISS) {
            path.add(new Vec3(posX, posY, posZ));
            break;
         }

         BlockPos blockPos = BlockPos.containing(posX, posY, posZ);
         float currentDrag = this.mc.level.getFluidState(blockPos).isEmpty() ? drag : waterDrag;
         motionX *= currentDrag;
         motionY *= currentDrag;
         motionZ *= currentDrag;
         motionY -= gravity;
         currentPos = nextPos;
      }

      this.drawZapLine(matrices, path, camPos, cameraRot, color, mod.drawThroughWalls.get(), 0.25F, true);
      if (hitResult != null && hitResult.getType() != Type.MISS) {
         if (mod.showHitbox.get()) {
            this.drawLandingBox(
               matrices, hitResult, camPos, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, mod.drawThroughWalls.get()
            );
         }

         ItemStack held = this.mc.player.getMainHandItem();
         if (held.isEmpty() || !(held.getItem() instanceof EnderpearlItem)) {
            held = this.mc.player.getOffhandItem();
         }

         if (!held.isEmpty() && held.getItem() instanceof EnderpearlItem) {
            this.collectCircularTimer(hitResult.getLocation(), ticksToLand, camPos);
         }
      }
   }

   private void drawZapLine(
      PoseStack matrices,
      List<Vec3> path,
      Vec3 camPos,
      Quaternionf cameraRot,
      Color color,
      boolean drawThroughWalls,
      float scaleMultiplier,
      boolean drawAsLine
   ) {
      if (path != null && !path.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         RenderSystem.disableCull();
         if (drawThroughWalls) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
         }

         Tesselator tessellator = Tesselator.getInstance();
         float r = color.getRed() / 255.0F;
         float g = color.getGreen() / 255.0F;
         float b = color.getBlue() / 255.0F;
         if (drawAsLine) {
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            RenderSystem.lineWidth(3.0F);
            BufferBuilder buffer = tessellator.begin(PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f mx = matrices.last().pose();

            for (Vec3 point : path) {
               float lx = (float)(point.x - camPos.x);
               float ly = (float)(point.y - camPos.y);
               float lz = (float)(point.z - camPos.z);
               Vtx.vertex(buffer, mx, lx, ly, lz).setColor(r, g, b, 1.0F);
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.lineWidth(1.0F);
         } else {
            RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            BufferBuilder buffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Vec3 playerPos = this.mc.player != null ? this.mc.player.position() : Vec3.ZERO;

            for (int i = 0; i < path.size() - 1; i++) {
               Vec3 p1 = path.get(i);
               Vec3 p2 = path.get(i + 1);
               double segmentDist = p1.distanceTo(p2);
               if (!(segmentDist < 0.001)) {
                  for (int k = 0; k < 10; k++) {
                     float t = k / 10.0F;
                     Vec3 interpolatedPos = p1.add(p2.subtract(p1).scale(t));
                     if (!(interpolatedPos.distanceTo(playerPos) <= 2.0)) {
                        float size1 = (float)segmentDist / 3.0F * scaleMultiplier;
                        this.drawBloomGlow(matrices, buffer, interpolatedPos, camPos, cameraRot, size1, r, g, b, 1.0F);
                        float size2 = (float)segmentDist * 2.0F * scaleMultiplier;
                        this.drawBloomGlow(matrices, buffer, interpolatedPos, camPos, cameraRot, size2, r, g, b, 0.05F);
                     }
                  }
               }
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
         }

         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
      }
   }

   private void drawBloomGlow(
      PoseStack matrices, BufferBuilder buffer, Vec3 pos, Vec3 camPos, Quaternionf cameraRot, float size, float r, float g, float b, float alpha
   ) {
      matrices.pushPose();
      matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
      matrices.rotate(cameraRot);
      Matrix4f mx = matrices.last().pose();
      Vtx.vertex(buffer, mx, -size / 2.0F, -size / 2.0F, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, alpha);
      Vtx.vertex(buffer, mx, size / 2.0F, -size / 2.0F, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, alpha);
      Vtx.vertex(buffer, mx, size / 2.0F, size / 2.0F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, alpha);
      Vtx.vertex(buffer, mx, -size / 2.0F, size / 2.0F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, alpha);
      matrices.popPose();
   }

   private TrajectoriesRenderer.CachedPearl calculatePearlPath(ThrownEnderpearl pearl, float tickDelta) {
      TrajectoriesRenderer.CachedPearl cache = new TrajectoriesRenderer.CachedPearl();
      cache.path = new ArrayList<>();
      double posX = Mth.lerp(tickDelta, pearl.xOld, pearl.getX());
      double posY = Mth.lerp(tickDelta, pearl.yOld, pearl.getY());
      double posZ = Mth.lerp(tickDelta, pearl.zOld, pearl.getZ());
      double motionX = pearl.getDeltaMovement().x;
      double motionY = pearl.getDeltaMovement().y;
      double motionZ = pearl.getDeltaMovement().z;
      Vec3 currentPos = new Vec3(posX, posY, posZ);
      int ticksToLand = 0;
      HitResult hitResult = null;

      for (int i = 0; i < 300; i++) {
         cache.path.add(new Vec3(posX, posY, posZ));
         Vec3 nextPos = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
         hitResult = this.mc.level.clip(new ClipContext(currentPos, nextPos, Block.COLLIDER, Fluid.NONE, pearl));
         if (hitResult != null && hitResult.getType() == Type.BLOCK) {
            nextPos = hitResult.getLocation();
         }

         AABB boundingBox = new AABB(posX, posY, posZ, posX, posY, posZ).expandTowards(motionX, motionY, motionZ).inflate(1.0);

         for (Entity entity : this.mc.level.getEntities(pearl, boundingBox, e -> e.isPickable() && e.isAlive())) {
            AABB entBox = entity.getBoundingBox().inflate(0.3F);
            if (entBox.contains(currentPos)) {
               hitResult = new EntityHitResult(entity);
               nextPos = currentPos;
               break;
            }
         }

         posX = nextPos.x;
         posY = nextPos.y;
         posZ = nextPos.z;
         ticksToLand++;
         if (hitResult != null && hitResult.getType() != Type.MISS) {
            cache.path.add(new Vec3(posX, posY, posZ));
            break;
         }

         BlockPos blockPos = BlockPos.containing(posX, posY, posZ);
         double currentDrag = this.mc.level.getFluidState(blockPos).isEmpty() ? 0.99 : 0.8;
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

   private void drawCachedPearl(
      PoseStack matrices,
      TrajectoriesRenderer.CachedPearl cache,
      Vec3 camPos,
      Quaternionf cameraRot,
      Color color,
      TrajectoriesModule mod,
      int ticksToLand
   ) {
      this.drawZapLine(matrices, cache.path, camPos, cameraRot, color, mod.drawThroughWalls.get(), 1.0F, false);
      if (cache.hitResult != null && cache.hitResult.getType() != Type.MISS) {
         if (mod.showHitbox.get()) {
            this.drawLandingBox(
               matrices, cache.hitResult, camPos, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, mod.drawThroughWalls.get()
            );
         }

         this.collectCircularTimer(cache.hitResult.getLocation(), ticksToLand, camPos);
      }
   }

   private void collectCircularTimer(Vec3 hitPos, int ticksToLand, Vec3 camPos) {
      double dx = hitPos.x - camPos.x;
      double dy = hitPos.y + 0.75 - camPos.y;
      double dz = hitPos.z - camPos.z;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      Vector3f screenPos = this.project3DTo2D(hitPos.x, hitPos.y + 0.75, hitPos.z);
      if (screenPos != null) {
         this.timersToRender.add(new TrajectoriesRenderer.TimerTagInfo(hitPos, ticksToLand, dist, screenPos.x, screenPos.y));
      }
   }

   private Vector3f project3DTo2D(double x, double y, double z) {
      float rx = (float)(x - this.lastCamX);
      float ry = (float)(y - this.lastCamY);
      float rz = (float)(z - this.lastCamZ);
      Vector4f pos = new Vector4f(rx, ry, rz, 1.0F);
      this.lastModelViewMatrix.transform(pos);
      this.lastProjectionMatrix.transform(pos);
      if (pos.w() <= 0.0F) {
         return null;
      }

      float ndcX = pos.x() / pos.w();
      float ndcY = pos.y() / pos.w();
      float screenX = (ndcX + 1.0F) * 0.5F * (float)this.lastScaledWidth;
      float screenY = (1.0F - ndcY) * 0.5F * (float)this.lastScaledHeight;
      return new Vector3f(screenX, screenY, pos.w());
   }

   public void renderHUD(GuiGraphicsExtractor drawContext) {
      Module rawMod = AstolfoclientClient.moduleManager.getModuleByName("Trajectories");
      if (rawMod != null && rawMod.isEnabled()) {
         TrajectoriesModule mod = (TrajectoriesModule)rawMod;
         if (this.hasMatrices && !this.timersToRender.isEmpty()) {
            Matrix4f base = GuiCompat.matrix4f(drawContext);
            Color themeColor = new Color(ThemeManager.getThemedColor(0L));
            this.timersToRender.sort((t1, t2) -> Double.compare(t2.distance, t1.distance));

            for (TrajectoriesRenderer.TimerTagInfo tag : this.timersToRender) {
               Matrix4f mx = new Matrix4f(base).translate((float)tag.x, (float)tag.y, 0.0F);
               float scale = (float)(mod.scale.get() * (10.0 / Math.min(10.0, Math.max(2.0, tag.distance))));
               scale = (float)Math.min(scale, mod.scale.get() * 2.0);
               scale *= 0.8F;
               mx.scale(scale, scale, 1.0F);
               this.drawCircularTimer2D(mx, tag.ticksToLand, themeColor, mod);
            }
         }
      }
   }

   private void drawCircularTimer2D(Matrix4f mx, int ticksToLand, Color themeColor, TrajectoriesModule mod) {
      float seconds = ticksToLand / 20.0F;
      String text = String.format("%.1fs", seconds);
      MsdfFont font = TIMER_FONT.get();
      float textWidth = font.getWidth(text, 9.0F);
      float circleSize = Math.max(20.0F, textWidth + 8.0F);
      float cx = -circleSize / 2.0F;
      float cy = -circleSize / 2.0F;
      if (mod.background.get()) {
         int alphaInt = (int)((float)mod.bgOpacity.get() * 255.0F);
         if (mod.glow.get()) {
            int layers = 8;
            float maxSpread = 6.0F;

            for (int i = layers; i > 0; i--) {
               float progress = (float)i / layers;
               float fade = 1.0F - progress;
               float alpha = fade * fade * 0.25F * (float)mod.bgOpacity.get();
               int currentAlpha = Mth.clamp((int)(255.0F * alpha), 0, 255);
               if (currentAlpha > 0) {
                  float expand = progress * maxSpread;
                  Color layerColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), currentAlpha);
                  Builder.rectangle()
                     .size(new SizeState(circleSize + expand * 2.0F, circleSize + expand * 2.0F))
                     .radius(new QuadRadiusState((circleSize + expand * 2.0F) / 2.0F))
                     .color(new QuadColorState(layerColor))
                     .build()
                     .render(mx, cx - expand, cy - expand);
               }
            }
         }

         Builder.rectangle()
            .size(new SizeState(circleSize, circleSize))
            .radius(new QuadRadiusState(circleSize / 2.0F))
            .color(new QuadColorState(new Color(5, 5, 8, Math.min(245, alphaInt))))
            .build()
            .render(mx, cx, cy);
      }

      float textX = -(textWidth / 2.0F);
      float textY = -3.5F;
      Builder.text().font(font).text(text).color(-1).size(9.0F).build().render(mx, textX, textY - 0.5F);
   }

   private void drawLandingBox(PoseStack matrices, HitResult hit, Vec3 camPos, float r, float g, float b, boolean drawThroughWalls) {
      Tesselator tessellator = Tesselator.getInstance();
      if (hit.getType() == Type.BLOCK) {
         BlockHitResult blockHit = (BlockHitResult)hit;
         Vec3 hitPos = blockHit.getLocation();
         float size = 1.0F;
         double hX = hitPos.x - camPos.x;
         double hY = hitPos.y - camPos.y;
         double hZ = hitPos.z - camPos.z;
         switch (blockHit.getDirection()) {
            case UP:
               hY += 0.005;
               break;
            case DOWN:
               hY -= 0.005;
               break;
            case NORTH:
               hZ -= 0.005;
               break;
            case SOUTH:
               hZ += 0.005;
               break;
            case WEST:
               hX -= 0.005;
               break;
            case EAST:
               hX += 0.005;
         }

         matrices.pushPose();
         matrices.translate(hX, hY, hZ);
         matrices.rotate(blockHit.getDirection().getRotation());
         matrices.rotate(Axis.XN.rotationDegrees(-90.0F));
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         RenderSystem.disableCull();
         if (drawThroughWalls) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
         } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
         }

         RenderSystem.setShaderTexture(0, HIT_TEXTURE);
         RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
         BufferBuilder buffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         Matrix4f mx = matrices.last().pose();
         Vtx.vertex(buffer, mx, -size / 3.0F, size / 3.0F, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, 1.0F);
         Vtx.vertex(buffer, mx, size / 3.0F, size / 3.0F, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, 1.0F);
         Vtx.vertex(buffer, mx, size / 3.0F, -size / 3.0F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, 1.0F);
         Vtx.vertex(buffer, mx, -size / 3.0F, -size / 3.0F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, 1.0F);
         BufferUploader.drawWithShader(buffer.buildOrThrow());
         matrices.popPose();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
      } else if (hit.getType() == Type.ENTITY) {
         Entity entity = ((EntityHitResult)hit).getEntity();
         AABB bBox = entity.getBoundingBox().move(-camPos.x, -camPos.y, -camPos.z);
         Matrix4f mx = matrices.last().pose();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         if (drawThroughWalls) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
         } else {
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
         }

         RenderSystem.setShader(CoreShaders.POSITION_COLOR);
         BufferBuilder buffer = tessellator.begin(PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
         this.drawBoxLines(buffer, mx, bBox, r, g, b, 1.0F);
         BufferUploader.drawWithShader(buffer.buildOrThrow());
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
      }
   }

   private void drawBoxLines(BufferBuilder buffer, Matrix4f mx, AABB box, float r, float g, float b, float a) {
      float x1 = (float)box.minX;
      float y1 = (float)box.minY;
      float z1 = (float)box.minZ;
      float x2 = (float)box.maxX;
      float y2 = (float)box.maxY;
      float z2 = (float)box.maxZ;
      Vtx.vertex(buffer, mx, x1, y1, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y1, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y1, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y1, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y1, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y1, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y1, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y1, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y2, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y2, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y2, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y2, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y2, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y2, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y2, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y2, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y1, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y2, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y1, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y2, z1).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y1, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x2, y2, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y1, z2).setColor(r, g, b, a);
      Vtx.vertex(buffer, mx, x1, y2, z2).setColor(r, g, b, a);
   }

   private boolean isThrowable(Item item) {
      return item instanceof BowItem
         || item instanceof CrossbowItem
         || item instanceof TridentItem
         || item instanceof EnderpearlItem
         || item instanceof SnowballItem
         || item instanceof EggItem
         || item instanceof SplashPotionItem
         || item instanceof LingeringPotionItem
         || item instanceof ExperienceBottleItem;
   }

   @Environment(EnvType.CLIENT)
   private static class CachedPearl {
      List<Vec3> path;
      HitResult hitResult;
      int ticksToLand;
      long lastUpdateTime;
   }

   @Environment(EnvType.CLIENT)
   private static class TimerTagInfo {
      Vec3 hitPos;
      int ticksToLand;
      double distance;
      double x;
      double y;

      public TimerTagInfo(Vec3 hitPos, int ticksToLand, double distance, double x, double y) {
         this.hitPos = hitPos;
         this.ticksToLand = ticksToLand;
         this.distance = distance;
         this.x = x;
         this.y = y;
      }
   }
}
