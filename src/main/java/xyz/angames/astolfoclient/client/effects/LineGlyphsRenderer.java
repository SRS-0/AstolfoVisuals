package xyz.angames.astolfoclient.client.effects;

import com.mojang.renderpearl.backend.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
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
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.LineGlyphsModule;

@Environment(value=EnvType.CLIENT)
public class LineGlyphsRenderer {
    private final List<GlyphsVecGen> glyphs = new ArrayList<GlyphsVecGen>();
    private final Random rand = new Random(93882L);
    private final Minecraft client = Minecraft.getInstance();
    private long lastTickTime = 0L;

    public void render(LevelRenderContext context) {
        Module mod = AstolfoclientClient.moduleManager.getModuleByName("LineGlyphs");
        if (!(mod instanceof LineGlyphsModule) || !mod.isEnabled() || this.client.player == null || this.client.level == null) {
            this.glyphs.clear();
            return;
        }
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
        this.glyphs.removeIf(gen -> gen.isToRemove());
        if (this.glyphs.isEmpty()) {
            return;
        }
        PoseStack stack = context.poseStack();
        Vec3 cam = context.gameRenderer().mainCamera().position();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate((com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.SRC_ALPHA, (com.mojang.renderpearl.api.pipeline.BlendFactor)(glowing ? com.mojang.renderpearl.api.pipeline.BlendFactor.ONE : com.mojang.renderpearl.api.pipeline.BlendFactor.ONE_MINUS_SRC_ALPHA), (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ONE, (com.mojang.renderpearl.api.pipeline.BlendFactor)com.mojang.renderpearl.api.pipeline.BlendFactor.ZERO);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_COLOR);
        Tesselator tessellator = Tesselator.getInstance();
        float pTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        int colorIndex = 0;
        for (GlyphsVecGen gen2 : this.glyphs) {
            this.clientColoredBegin(gen2, ++colorIndex, pTicks, cam, stack, tessellator, 1.0f, 0.0f);
        }
        if (glowing) {
            colorIndex = 0;
            for (GlyphsVecGen gen2 : this.glyphs) {
                this.clientColoredBegin(gen2, ++colorIndex, pTicks, cam, stack, tessellator, 1.5f, 4.0f);
            }
            colorIndex = 0;
            for (GlyphsVecGen gen2 : this.glyphs) {
                this.clientColoredBegin(gen2, ++colorIndex, pTicks, cam, stack, tessellator, 1.9f, 9.0f);
            }
        }
        RenderSystem.lineWidth((float)1.0f);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void clientColoredBegin(GlyphsVecGen gen, int colorIndex, float pTicks, Vec3 cam, PoseStack stack, Tesselator tessellator, float widthMul, float widthAdd) {
        if (gen.vecGens.size() < 2) {
            return;
        }
        float lineWidth = this.calcLineWidth(gen, cam);
        RenderSystem.lineWidth((float)Math.min(lineWidth * widthMul + widthAdd, 15.0f));
        Matrix4f mat = stack.last().pose();
        BufferBuilder buffer = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        List<Vec3> vecs = gen.getPosVectors(pTicks);
        float alphaPC = gen.getAlphaPC();
        if (widthAdd == 4.0f) {
            alphaPC *= 0.1f;
        }
        if (widthAdd == 9.0f) {
            alphaPC *= 0.04f;
        }
        int index = 0;
        for (Vec3 vec : vecs) {
            float pointAlpha = alphaPC * (0.25f + (float)index / (float)gen.vecGens.size() / 1.75f);
            Color c = new Color(ThemeManager.getThemedColor(colorIndex * 15 + index * 5));
            float r = (float)c.getRed() / 255.0f * 0.9f + 0.1f;
            float g = (float)c.getGreen() / 255.0f * 0.9f + 0.1f;
            float b = (float)c.getBlue() / 255.0f * 0.9f + 0.1f;
            buffer.addVertex(mat, (float)(vec.x - cam.x), (float)(vec.y - cam.y), (float)(vec.z - cam.z)).setColor(r, g, b, pointAlpha);
            ++index;
        }
        BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
    }

    private float calcLineWidth(GlyphsVecGen gen, Vec3 cam) {
        Vec3i pos = gen.vecGens.stream().min(Comparator.comparingDouble(v -> cam.distanceToSqr((double)v.getX(), (double)v.getY(), (double)v.getZ()))).orElse(gen.vecGens.get(0));
        double dst = Math.sqrt(cam.distanceToSqr((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()));
        return 1.0E-4f + 3.0f * (float)Mth.clamp((double)(1.0 - dst / 20.0), (double)0.0, (double)1.0);
    }

    private void glyphsUpdate(boolean slowSpeed) {
        for (GlyphsVecGen gen : this.glyphs) {
            gen.update(slowSpeed);
        }
    }

    private void addAllGlyphs(int countCap) {
        while (this.glyphs.size() < countCap) {
            Vec3i pos = this.randGlyphSpawnPos();
            this.glyphs.add(new GlyphsVecGen(pos, this.randInt(7, 12)));
        }
    }

    private Vec3i randGlyphSpawnPos() {
        Vec3 cam = this.client.player != null ? this.client.player.position() : Vec3.ZERO;
        double fov = ((Integer)this.client.options.fov().get()).intValue();
        float yaw = this.client.player != null ? this.client.player.getYRot() : 0.0f;
        for (int attempt = 16; attempt > 0; --attempt) {
            int randZOff;
            int randYOff;
            double dst = this.randInt(6, 24);
            int yawMin = (int)((double)yaw - fov * 0.75);
            int yawMax = (int)((double)yaw + fov * 0.75);
            float radYaw = (float)Math.toRadians(this.randInt(yawMin, yawMax));
            int randXOff = (int)(-((double)Mth.sin((float)radYaw) * dst));
            Vec3i pos = new Vec3i((int)cam.x + randXOff, (int)cam.y + (randYOff = this.randInt(0, 12)), (int)cam.z + (randZOff = (int)((double)Mth.cos((float)radYaw) * dst)));
            if (!this.isSpawnPosFree(pos)) continue;
            return pos;
        }
        return new Vec3i((int)cam.x, (int)cam.y, (int)cam.z);
    }

    private boolean isSpawnPosFree(Vec3i pos) {
        if (this.client.level == null) {
            return true;
        }
        BlockPos bp = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
        BlockState state = this.client.level.getBlockState(bp);
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return state.getCollisionShape((BlockGetter)this.client.level, bp).isEmpty();
    }

    private int randInt(int min, int max) {
        if (max <= min) {
            return min;
        }
        return this.rand.nextInt(max - min) + min;
    }

    private int[] getR360XY() {
        return new int[]{this.rand.nextInt(4) * 90, (this.rand.nextInt(2) - 1) * 90};
    }

    private int[] getA90R(int[] outdated) {
        int maxAttempt;
        int a = outdated[0];
        int b = outdated[1];
        for (maxAttempt = 150; maxAttempt > 0 && Math.abs(b - outdated[1]) != 90; --maxAttempt) {
            b = (this.rand.nextInt(4) - 2) * 90;
        }
        for (maxAttempt = 5; maxAttempt > 0 && (Math.abs(a - outdated[0]) != 90 || Math.abs(a - outdated[0]) != 270); --maxAttempt) {
            a = this.rand.nextInt(4) * 90;
        }
        return new int[]{a, b};
    }

    private Vec3i offsetFromRXYR(Vec3i vec3i, int[] rxy, int r) {
        float yawR = (float)Math.toRadians(rxy[0]);
        float pitchR = (float)Math.toRadians(rxy[1]);
        float r1 = r;
        int ry = (int)(Mth.sin((float)pitchR) * r1);
        if (pitchR != 0.0f) {
            r1 = 0.0f;
        }
        int rx = (int)(-(Mth.sin((float)yawR) * r1));
        int rz = (int)(Mth.cos((float)yawR) * r1);
        return new Vec3i(vec3i.getX() + rx, vec3i.getY() + ry, vec3i.getZ() + rz);
    }

    @Environment(value=EnvType.CLIENT)
    private class GlyphsVecGen {
        private final List<Vec3i> vecGens = new ArrayList<Vec3i>();
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
                for (int attempt = 6; attempt > 0; --attempt) {
                    int step;
                    int[] nextR = LineGlyphsRenderer.this.getA90R(this.lastYawPitch);
                    Vec3i next = LineGlyphsRenderer.this.offsetFromRXYR(last, nextR, step = LineGlyphsRenderer.this.randInt(0, 3));
                    if (!LineGlyphsRenderer.this.isSpawnPosFree(next)) continue;
                    this.lastYawPitch = nextR;
                    this.lastStepSet = this.currentStepTicks = step;
                    this.vecGens.add(next);
                    --this.stepsAmount;
                    added = true;
                    break;
                }
                if (!added) {
                    this.stepsAmount = 0;
                }
            }
        }

        public List<Vec3> getPosVectors(float pTicks) {
            ArrayList<Vec3> smoothVecs = new ArrayList<Vec3>();
            float advance = Math.min(Math.max(1.0f - ((float)this.currentStepTicks - pTicks) / (float)Math.max(1, this.lastStepSet), 0.0f), 1.0f);
            for (int i = 0; i < this.vecGens.size(); ++i) {
                Vec3i v = this.vecGens.get(i);
                double x = v.getX();
                double y = v.getY();
                double z = v.getZ();
                if (this.vecGens.size() >= 2 && i == this.vecGens.size() - 1 && !this.removing) {
                    Vec3i prev = this.vecGens.get(this.vecGens.size() - 2);
                    x = (double)prev.getX() + (x - (double)prev.getX()) * (double)advance;
                    y = (double)prev.getY() + (y - (double)prev.getY()) * (double)advance;
                    z = (double)prev.getZ() + (z - (double)prev.getZ()) * (double)advance;
                }
                smoothVecs.add(new Vec3(x, y, z));
            }
            return smoothVecs;
        }

        public float getAlphaPC() {
            long now = System.currentTimeMillis();
            if (this.removing) {
                return Math.max(0.0f, 1.0f - (float)(now - this.removeTime) / 500.0f);
            }
            return Math.min(1.0f, (float)(now - this.spawnTime) / 500.0f);
        }

        public boolean isToRemove() {
            return this.removing && System.currentTimeMillis() - this.removeTime > 500L;
        }
    }
}

