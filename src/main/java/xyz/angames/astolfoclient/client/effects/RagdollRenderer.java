package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import xyz.angames.astolfoclient.client.render.compat.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgram;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import xyz.angames.astolfoclient.client.render.compat.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
public class RagdollRenderer {
    private final List<Ragdoll> ragdolls = new ArrayList<Ragdoll>();
    private ClientLevel lastWorld = null;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addRagdoll(LivingEntity entity) {
        block11: {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || entity == null) {
                return;
            }
            try {
                EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
                EntityRenderer renderer = dispatcher.getRenderer((Entity)entity);
                if (renderer == null) {
                    return;
                }
                LivingEntityRenderState stateCopy = (LivingEntityRenderState)renderer.createRenderState();
                try {
                    renderer.extractRenderState((Entity)entity, (EntityRenderState)stateCopy, 1.0f);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                SnapshotVertexConsumer snapshotConsumer = new SnapshotVertexConsumer();
                MultiBufferSource captureProvider = layer -> snapshotConsumer;
                PoseStack matrices = new PoseStack();
                try {
                    if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer livingRenderer) {
                        var model = livingRenderer.getModel();
                        model.setupAnim(stateCopy);
                        model.renderToBuffer(matrices, snapshotConsumer, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, -1);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                snapshotConsumer.commitLast();
                if (snapshotConsumer.vertices.isEmpty()) break block11;
                double ex = entity.getX();
                double ey = entity.getY();
                double ez = entity.getZ();
                float height = entity.getBbHeight();
                List<Ragdoll> list = this.ragdolls;
                synchronized (list) {
                    this.ragdolls.add(new Ragdoll(snapshotConsumer.vertices, ex, ey, ez, height));
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Vec3 rotateAroundAxis(Vec3 point, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = point.x * axis.x + point.y * axis.y + point.z * axis.z;
        double crossX = axis.y * point.z - axis.z * point.y;
        double crossY = axis.z * point.x - axis.x * point.z;
        double crossZ = axis.x * point.y - axis.y * point.x;
        double rx = point.x * cos + crossX * sin + axis.x * dot * (1.0 - cos);
        double ry = point.y * cos + crossY * sin + axis.y * dot * (1.0 - cos);
        double rz = point.z * cos + crossZ * sin + axis.z * dot * (1.0 - cos);
        return new Vec3(rx, ry, rz);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(LevelRenderContext context) {
        Module ragdollModule;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            List<Ragdoll> list = this.ragdolls;
            synchronized (list) {
                this.ragdolls.clear();
            }
            this.lastWorld = null;
            return;
        }
        if (mc.level != this.lastWorld) {
            List<Ragdoll> list = this.ragdolls;
            synchronized (list) {
                this.ragdolls.clear();
            }
            this.lastWorld = mc.level;
        }
        if ((ragdollModule = AstolfoclientClient.moduleManager.getModuleByName("Ragdoll")) == null || !ragdollModule.isEnabled()) {
            List<Ragdoll> list = this.ragdolls;
            synchronized (list) {
                this.ragdolls.clear();
            }
            return;
        }
        long now = System.currentTimeMillis();
        ArrayList<Ragdoll> active = new ArrayList<Ragdoll>();
        List<Ragdoll> list = this.ragdolls;
        synchronized (list) {
            Iterator<Ragdoll> it = this.ragdolls.iterator();
            while (it.hasNext()) {
                Ragdoll r = it.next();
                if (now - r.spawnTime > 1500L) {
                    it.remove();
                    continue;
                }
                active.add(r);
            }
        }
        if (active.isEmpty()) {
            return;
        }
        Vec3 cameraPos = context.gameRenderer().mainCamera().position();
        PoseStack matrices = context.poseStack();
        Tesselator tessellator = Tesselator.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.enableCull();
        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        BufferBuilder[] bufferHolder = new BufferBuilder[]{tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)};
        Matrix4f m = matrices.last().pose();
        for (Ragdoll ragdoll : active) {
            long age = now - ragdoll.spawnTime;
            float explosionProgress = 0.0f;
            float explosionAgeSecs = 0.0f;
            if (age >= 800L) {
                explosionProgress = (float)(age - 800L) / 700.0f;
                explosionAgeSecs = (float)(age - 800L) / 1000.0f;
            }
            if (explosionProgress > 1.0f) {
                explosionProgress = 1.0f;
            }
            float progress = (float)age / 1500.0f;
            float alphaVal = 0.5f * (1.0f - progress);
            for (Ragdoll.Shard shard : ragdoll.shards) {
                for (int j = 0; j < 4; ++j) {
                    SnapshotVertexConsumer.CapturedVertex v = shard.vertices[j];
                    Vec3 distortionOffset = shard.vertexDistortions[j].scale((double)explosionProgress);
                    double vxLocal = (double)v.x + distortionOffset.x;
                    double vyLocal = (double)v.y + distortionOffset.y;
                    double vzLocal = (double)v.z + distortionOffset.z;
                    Vec3 localToCenter = new Vec3(vxLocal - shard.center.x, vyLocal - shard.center.y, vzLocal - shard.center.z);
                    Vec3 rotated = RagdollRenderer.rotateAroundAxis(localToCenter, shard.rotationAxis, shard.rotationSpeed * explosionAgeSecs);
                    Vec3 rotatedScaled = rotated.scale(1.0 - (double)explosionProgress);
                    Vec3 finalLocalPos = shard.center.add(rotatedScaled).add(shard.velocity.scale((double)explosionAgeSecs));
                    double vx = ragdoll.x + finalLocalPos.x;
                    double vy = ragdoll.y + finalLocalPos.y;
                    double vz = ragdoll.z + finalLocalPos.z;
                    bufferHolder[0].addVertex(m, (float)vx, (float)vy, (float)vz).setUv(v.u, v.v).setColor(v.r, v.g, v.b, alphaVal);
                }
            }
            ShaderProgram shader = RenderSystem.setShader((ShaderProgramKey)AstolfoclientClient.COSMOS_FILL_SHADER);
            if (shader != null) {
                float timeSecs = (float)(System.currentTimeMillis() % 1000000L) / 1000.0f;
                if (shader.getUniform("uTime") != null) {
                    shader.getUniform("uTime").set(timeSecs);
                }
                if (shader.getUniform("uResolution") != null) {
                    shader.getUniform("uResolution").set(1.0f, 1.0f);
                }
                int color1 = ThemeManager.getThemedColor(0L);
                float r1 = (float)(color1 >> 16 & 0xFF) / 255.0f;
                float g1 = (float)(color1 >> 8 & 0xFF) / 255.0f;
                float b1 = (float)(color1 & 0xFF) / 255.0f;
                if (shader.getUniform("uColor1") != null) {
                    shader.getUniform("uColor1").set(r1, g1, b1);
                }
                if (shader.getUniform("uColor2") != null) {
                    shader.getUniform("uColor2").set(0.0f, 0.0f, 0.0f);
                }
                if (shader.getUniform("uBlockCenter") != null) {
                    var center = m.transformPosition(new org.joml.Vector3f((float)ragdoll.x, (float)ragdoll.y, (float)ragdoll.z));
                    shader.getUniform("uBlockCenter").set(center.x, center.y, center.z);
                }
                if (shader.getUniform("ColorModulator") != null) {
                    shader.getUniform("ColorModulator").set(1.0f, 1.0f, 1.0f, 1.0f);
                }
                if (shader.getUniform("uAlphaMode") != null) {
                    shader.getUniform("uAlphaMode").set(1.0f);
                }
            }
            try {
                BufferRenderer.drawWithGlobalProgram((MeshData)bufferHolder[0].buildOrThrow());
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (ragdoll == active.get(active.size() - 1)) continue;
            bufferHolder[0] = tessellator.begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        }
        matrices.popPose();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    @Environment(value=EnvType.CLIENT)
    public static class SnapshotVertexConsumer
    implements VertexConsumer {
        public final List<CapturedVertex> vertices = new ArrayList<CapturedVertex>();
        private CapturedVertex currentVertex = new CapturedVertex();
        private boolean hasVertex = false;

        public VertexConsumer addVertex(float x, float y, float z) {
            if (this.hasVertex) {
                this.vertices.add(this.currentVertex);
            }
            this.currentVertex = new CapturedVertex();
            this.currentVertex.x = x;
            this.currentVertex.y = y;
            this.currentVertex.z = z;
            this.hasVertex = true;
            return this;
        }

        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.currentVertex.r = (float)red / 255.0f;
            this.currentVertex.g = (float)green / 255.0f;
            this.currentVertex.b = (float)blue / 255.0f;
            this.currentVertex.a = (float)alpha / 255.0f;
            return this;
        }

        public VertexConsumer setColor(float red, float green, float blue, float alpha) {
            this.currentVertex.r = red;
            this.currentVertex.g = green;
            this.currentVertex.b = blue;
            this.currentVertex.a = alpha;
            return this;
        }

        public VertexConsumer setUv(float u, float v) {
            this.currentVertex.u = u;
            this.currentVertex.v = v;
            return this;
        }

        public VertexConsumer setUv3(float u, float v) { return this; }

        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        public com.mojang.blaze3d.vertex.VertexConsumer setColor(int color) { return setColor((color >> 16) & 255,(color >> 8) & 255,color & 255,(color >>> 24)); }
        public com.mojang.blaze3d.vertex.VertexConsumer setLineWidth(float width) { return this; }

        public void commitLast() {
            if (this.hasVertex) {
                this.vertices.add(this.currentVertex);
                this.hasVertex = false;
            }
        }

        @Environment(value=EnvType.CLIENT)
        public static class CapturedVertex {
            public float x;
            public float y;
            public float z;
            public float u;
            public float v;
            public float r = 1.0f;
            public float g = 1.0f;
            public float b = 1.0f;
            public float a = 1.0f;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Ragdoll {
        public final List<Shard> shards = new ArrayList<Shard>();
        public final double x;
        public final double y;
        public final double z;
        public final long spawnTime;
        public static final long LIFESPAN = 1500L;

        public Ragdoll(List<SnapshotVertexConsumer.CapturedVertex> capturedVertices, double x, double y, double z, float entityHeight) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.spawnTime = System.currentTimeMillis();
            Vec3 entityCenter = new Vec3(0.0, (double)entityHeight / 2.0, 0.0);
            int i = 0;
            while (i + 3 < capturedVertices.size()) {
                SnapshotVertexConsumer.CapturedVertex v0 = capturedVertices.get(i);
                SnapshotVertexConsumer.CapturedVertex v1 = capturedVertices.get(i + 1);
                SnapshotVertexConsumer.CapturedVertex v2 = capturedVertices.get(i + 2);
                SnapshotVertexConsumer.CapturedVertex v3 = capturedVertices.get(i + 3);
                this.shards.add(new Shard(v0, v1, v2, v2, entityCenter));
                this.shards.add(new Shard(v0, v2, v3, v3, entityCenter));
                i += 4;
            }
        }

        @Environment(value=EnvType.CLIENT)
        public static class Shard {
            public final SnapshotVertexConsumer.CapturedVertex[] vertices = new SnapshotVertexConsumer.CapturedVertex[4];
            public final Vec3[] vertexDistortions = new Vec3[4];
            public final Vec3 center;
            public final Vec3 velocity;
            public final Vec3 rotationAxis;
            public final float rotationSpeed;

            public Shard(SnapshotVertexConsumer.CapturedVertex v0, SnapshotVertexConsumer.CapturedVertex v1, SnapshotVertexConsumer.CapturedVertex v2, SnapshotVertexConsumer.CapturedVertex v3, Vec3 entityCenter) {
                this.vertices[0] = v0;
                this.vertices[1] = v1;
                this.vertices[2] = v2;
                this.vertices[3] = v3;
                double cx = (double)(v0.x + v1.x + v2.x + v3.x) / 4.0;
                double cy = (double)(v0.y + v1.y + v2.y + v3.y) / 4.0;
                double cz = (double)(v0.z + v1.z + v2.z + v3.z) / 4.0;
                this.center = new Vec3(cx, cy, cz);
                for (int i = 0; i < 4; ++i) {
                    this.vertexDistortions[i] = new Vec3((Math.random() - 0.5) * 0.25, (Math.random() - 0.5) * 0.25, (Math.random() - 0.5) * 0.25);
                }
                double dx = cx - entityCenter.x;
                double dy = cy - entityCenter.y;
                double dz = cz - entityCenter.z;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 0.01) {
                    dx = Math.random() - 0.5;
                    dy = Math.random() - 0.5;
                    dz = Math.random() - 0.5;
                    len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                }
                double speed = 0.4 + Math.random() * 0.8;
                this.velocity = new Vec3(dx / len * speed + (Math.random() - 0.5) * 0.2, dy / len * speed + Math.random() * 0.5 + 0.3, dz / len * speed + (Math.random() - 0.5) * 0.2);
                this.rotationAxis = new Vec3(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5).normalize();
                this.rotationSpeed = (float)(Math.random() * 8.0 + 3.0);
            }
        }
    }
}

