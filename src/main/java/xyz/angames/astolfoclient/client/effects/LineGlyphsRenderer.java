package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.LineGlyphsModule;

@Environment(EnvType.CLIENT)
public class LineGlyphsRenderer {
   private final List<LineGlyphsRenderer.GlyphsVecGen> glyphs = new ArrayList<>();
   private final Random rand = new Random(93882L);
   private final Minecraft client = Minecraft.getInstance();
   private long lastTickTime = 0L;

   public void render(WorldRenderContext context) {
      Module mod = AstolfoclientClient.moduleManager.getModuleByName("LineGlyphs");
      if (mod instanceof LineGlyphsModule && mod.isEnabled() && this.client.player != null && this.client.level != null) {
         LineGlyphsModule module = (LineGlyphsModule)mod;
         int maxCount = (int)module.glyphsCount.get();
         boolean slow = module.slowSpeed.get();
         boolean glowing = module.linesGlowing.get();
         long now = System.currentTimeMillis();
         if (now - this.lastTickTime > 50L) {
            this.glyphsUpdate(slow);
            this.addAllGlyphs(maxCount);
            this.lastTickTime = now;
         }

         this.glyphs.removeIf(genx -> genx.isToRemove());
         if (!this.glyphs.isEmpty()) {
            PoseStack stack = context.matrixStack();
            Vec3 cam = context.camera().position();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, glowing ? DestFactor.ONE : DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            Tesselator tessellator = Tesselator.getInstance();
            float pTicks = context.tickCounter().getGameTimeDeltaPartialTick(true);
            int colorIndex = 0;

            for (LineGlyphsRenderer.GlyphsVecGen gen : this.glyphs) {
               this.clientColoredBegin(gen, ++colorIndex, pTicks, cam, stack, tessellator, 1.0F, 0.0F);
            }

            if (glowing) {
               colorIndex = 0;

               for (LineGlyphsRenderer.GlyphsVecGen gen : this.glyphs) {
                  this.clientColoredBegin(gen, ++colorIndex, pTicks, cam, stack, tessellator, 1.5F, 4.0F);
               }

               colorIndex = 0;

               for (LineGlyphsRenderer.GlyphsVecGen gen : this.glyphs) {
                  this.clientColoredBegin(gen, ++colorIndex, pTicks, cam, stack, tessellator, 1.9F, 9.0F);
               }
            }

            RenderSystem.lineWidth(1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      } else {
         this.glyphs.clear();
      }
   }

   private void clientColoredBegin(
      LineGlyphsRenderer.GlyphsVecGen gen, int colorIndex, float pTicks, Vec3 cam, PoseStack stack, Tesselator tessellator, float widthMul, float widthAdd
   ) {
      if (gen.vecGens.size() >= 2) {
         float lineWidth = this.calcLineWidth(gen, cam);
         RenderSystem.lineWidth(Math.min(lineWidth * widthMul + widthAdd, 15.0F));
         Matrix4f mat = stack.last().pose();
         BufferBuilder buffer = tessellator.begin(PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
         List<Vec3> vecs = gen.getPosVectors(pTicks);
         float alphaPC = gen.getAlphaPC();
         if (widthAdd == 4.0F) {
            alphaPC *= 0.1F;
         }

         if (widthAdd == 9.0F) {
            alphaPC *= 0.04F;
         }

         int index = 0;

         for (Vec3 vec : vecs) {
            float pointAlpha = alphaPC * (0.25F + (float)index / gen.vecGens.size() / 1.75F);
            Color c = new Color(ThemeManager.getThemedColor(colorIndex * 15 + index * 5));
            float r = c.getRed() / 255.0F * 0.9F + 0.1F;
            float g = c.getGreen() / 255.0F * 0.9F + 0.1F;
            float b = c.getBlue() / 255.0F * 0.9F + 0.1F;
            Vtx.vertex(buffer, mat, (float)(vec.x - cam.x), (float)(vec.y - cam.y), (float)(vec.z - cam.z))
               .setColor(r, g, b, pointAlpha);
            index++;
         }

         BufferUploader.drawWithShader(buffer.buildOrThrow());
      }
   }

   private float calcLineWidth(LineGlyphsRenderer.GlyphsVecGen gen, Vec3 cam) {
      Vec3i pos = gen.vecGens
         .stream()
         .min(Comparator.comparingDouble(v -> cam.distanceToSqr(v.getX(), v.getY(), v.getZ())))
         .orElse(gen.vecGens.get(0));
      double dst = Math.sqrt(cam.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()));
      return 1.0E-4F + 3.0F * (float)Mth.clamp(1.0 - dst / 20.0, 0.0, 1.0);
   }

   private void glyphsUpdate(boolean slowSpeed) {
      for (LineGlyphsRenderer.GlyphsVecGen gen : this.glyphs) {
         gen.update(slowSpeed);
      }
   }

   private void addAllGlyphs(int countCap) {
      while (this.glyphs.size() < countCap) {
         Vec3i pos = this.randGlyphSpawnPos();
         this.glyphs.add(new LineGlyphsRenderer.GlyphsVecGen(pos, this.randInt(7, 12)));
      }
   }

   private Vec3i randGlyphSpawnPos() {
      Vec3 cam = this.client.player != null ? this.client.player.position() : Vec3.ZERO;
      double fov = ((Integer)this.client.options.fov().get()).intValue();
      float yaw = this.client.player != null ? this.client.player.getYRot() : 0.0F;

      for (int attempt = 16; attempt > 0; attempt--) {
         double dst = this.randInt(6, 24);
         int yawMin = (int)(yaw - fov * 0.75);
         int yawMax = (int)(yaw + fov * 0.75);
         float radYaw = (float)Math.toRadians(this.randInt(yawMin, yawMax));
         int randXOff = (int)(-(Mth.sin(radYaw) * dst));
         int randYOff = this.randInt(0, 12);
         int randZOff = (int)(Mth.cos(radYaw) * dst);
         Vec3i pos = new Vec3i((int)cam.x + randXOff, (int)cam.y + randYOff, (int)cam.z + randZOff);
         if (this.isSpawnPosFree(pos)) {
            return pos;
         }
      }

      return new Vec3i((int)cam.x, (int)cam.y, (int)cam.z);
   }

   private boolean isSpawnPosFree(Vec3i pos) {
      if (this.client.level == null) {
         return true;
      } else {
         BlockPos bp = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
         BlockState state = this.client.level.getBlockState(bp);
         if (state.isAir()) {
            return true;
         } else {
            return !state.getFluidState().isEmpty() ? false : state.getCollisionShape(this.client.level, bp).isEmpty();
         }
      }
   }

   private int randInt(int min, int max) {
      return max <= min ? min : this.rand.nextInt(max - min) + min;
   }

   private int[] getR360XY() {
      return new int[]{this.rand.nextInt(4) * 90, (this.rand.nextInt(2) - 1) * 90};
   }

   private int[] getA90R(int[] outdated) {
      int a = outdated[0];
      int b = outdated[1];

      for (int maxAttempt = 150; maxAttempt > 0 && Math.abs(b - outdated[1]) != 90; maxAttempt--) {
         b = (this.rand.nextInt(4) - 2) * 90;
      }

      for (int maxAttempt = 5; maxAttempt > 0 && (Math.abs(a - outdated[0]) != 90 || Math.abs(a - outdated[0]) != 270); maxAttempt--) {
         a = this.rand.nextInt(4) * 90;
      }

      return new int[]{a, b};
   }

   private Vec3i offsetFromRXYR(Vec3i vec3i, int[] rxy, int r) {
      float yawR = (float)Math.toRadians(rxy[0]);
      float pitchR = (float)Math.toRadians(rxy[1]);
      float r1 = r;
      int ry = (int)(Mth.sin(pitchR) * r1);
      if (pitchR != 0.0F) {
         r1 = 0.0F;
      }

      int rx = (int)(-(Mth.sin(yawR) * r1));
      int rz = (int)(Mth.cos(yawR) * r1);
      return new Vec3i(vec3i.getX() + rx, vec3i.getY() + ry, vec3i.getZ() + rz);
   }

   @Environment(EnvType.CLIENT)
   private class GlyphsVecGen {
      private final List<Vec3i> vecGens = new ArrayList<>();
      private int currentStepTicks;
      private int lastStepSet;
      private int stepsAmount;
      private int[] lastYawPitch;
      private long spawnTime;
      private boolean removing = false;
      private long removeTime;

      GlyphsVecGen(Vec3i spawnPos, int maxStepsAmount) {
         this.vecGens.add(spawnPos);
         this.lastYawPitch = LineGlyphsRenderer.this.getR360XY();
         this.stepsAmount = maxStepsAmount;
         this.spawnTime = System.currentTimeMillis();
      }

      private void update(boolean slowSpeed) {
         if (this.stepsAmount == 0 && !this.removing) {
            this.removing = true;
            this.removeTime = System.currentTimeMillis();
         }

         if (this.currentStepTicks > 0) {
            this.currentStepTicks -= slowSpeed ? 1 : 2;
            if (this.currentStepTicks < 0) {
               this.currentStepTicks = 0;
            }
         } else if (!this.removing) {
            Vec3i last = this.vecGens.get(this.vecGens.size() - 1);
            boolean added = false;

            for (int attempt = 6; attempt > 0; attempt--) {
               int[] nextR = LineGlyphsRenderer.this.getA90R(this.lastYawPitch);
               int step = LineGlyphsRenderer.this.randInt(0, 3);
               Vec3i next = LineGlyphsRenderer.this.offsetFromRXYR(last, nextR, step);
               if (LineGlyphsRenderer.this.isSpawnPosFree(next)) {
                  this.lastYawPitch = nextR;
                  this.lastStepSet = this.currentStepTicks = step;
                  this.vecGens.add(next);
                  this.stepsAmount--;
                  added = true;
                  break;
               }
            }

            if (!added) {
               this.stepsAmount = 0;
            }
         }
      }

      public List<Vec3> getPosVectors(float pTicks) {
         List<Vec3> smoothVecs = new ArrayList<>();
         float advance = Math.min(Math.max(1.0F - (this.currentStepTicks - pTicks) / Math.max(1, this.lastStepSet), 0.0F), 1.0F);

         for (int i = 0; i < this.vecGens.size(); i++) {
            Vec3i v = this.vecGens.get(i);
            double x = v.getX();
            double y = v.getY();
            double z = v.getZ();
            if (this.vecGens.size() >= 2 && i == this.vecGens.size() - 1 && !this.removing) {
               Vec3i prev = this.vecGens.get(this.vecGens.size() - 2);
               x = prev.getX() + (x - prev.getX()) * advance;
               y = prev.getY() + (y - prev.getY()) * advance;
               z = prev.getZ() + (z - prev.getZ()) * advance;
            }

            smoothVecs.add(new Vec3(x, y, z));
         }

         return smoothVecs;
      }

      public float getAlphaPC() {
         long now = System.currentTimeMillis();
         return this.removing ? Math.max(0.0F, 1.0F - (float)(now - this.removeTime) / 500.0F) : Math.min(1.0F, (float)(now - this.spawnTime) / 500.0F);
      }

      public boolean isToRemove() {
         return this.removing && System.currentTimeMillis() - this.removeTime > 500L;
      }
   }
}
