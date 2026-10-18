package xyz.angames.astolfoclient.client.compat;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.UniformType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/**
 * Emulation of the pre-1.21.5 global render state (blend/depth/cull/shader/texture) on top of the 26.x
 * pipeline API. State setters only record; {@link #draw(MeshData)} resolves them into a cached
 * {@link RenderPipeline} and issues an immediate draw on the main render target.
 */
public final class RenderSystem {
   private static boolean blend;
   private static BlendFactor srcRgb = BlendFactor.SRC_ALPHA;
   private static BlendFactor dstRgb = BlendFactor.ONE_MINUS_SRC_ALPHA;
   private static BlendFactor srcA = BlendFactor.ONE;
   private static BlendFactor dstA = BlendFactor.ZERO;
   private static boolean depthTest = true;
   private static CompareOp depthFunc = CompareOp.GREATER_THAN_OR_EQUAL;
   private static boolean depthMask = true;
   private static boolean cull = true;
   private static float lineWidth = 1.0F;
   private static CoreShaders shader = CoreShaders.POSITION_COLOR;
   private static ShaderProgram custom;
   private static final GpuTextureView[] texViews = new GpuTextureView[12];
   private static final GpuSampler[] texSamplers = new GpuSampler[12];
   private static boolean deferWorld;
   private static final java.util.List<Deferred> DEFERRED = new java.util.ArrayList<>();
   private static final Vector4f shaderColor = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Map<PipelineKey, RenderPipeline> PIPELINES = new HashMap<>();
   private static int pipelineCounter;

   private RenderSystem() {
   }

   /**
    * The legacy setters also apply to real GL state (through the backend's cached GlStateManager so its cache stays
    * coherent): the mod's raw-GL effects rely on them, while pipeline draws carry their own state anyway.
    */
   private static void gl(Runnable r) {
      if (com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread()) {
         try {
            r.run();
         } catch (Throwable ignored) {
         }
      }
   }

   private static int glFactor(BlendFactor f) {
      for (SourceFactor s : SourceFactor.values()) {
         if (s.factor == f) {
            return s.value;
         }
      }

      return 1;
   }

   private static void applyBlendFunc() {
      gl(() -> com.mojang.renderpearl.backend.opengl.GlStateManager._blendFuncSeparate(glFactor(srcRgb), glFactor(dstRgb), glFactor(srcA), glFactor(dstA)));
   }

   private static int glCompare(CompareOp op) {
      return switch (op) {
         case NEVER_PASS -> 512;
         case LESS_THAN -> 513;
         case EQUAL -> 514;
         case LESS_THAN_OR_EQUAL -> 515;
         case GREATER_THAN -> 516;
         case NOT_EQUAL -> 517;
         case GREATER_THAN_OR_EQUAL -> 518;
         default -> 519;
      };
   }

   public static void enableBlend() {
      blend = true;
      gl(() -> com.mojang.renderpearl.backend.opengl.GlStateManager._enableBlend(0));
   }

   public static void disableBlend() {
      blend = false;
      gl(() -> com.mojang.renderpearl.backend.opengl.GlStateManager._disableBlend(0));
   }

   public static void defaultBlendFunc() {
      srcRgb = BlendFactor.SRC_ALPHA;
      dstRgb = BlendFactor.ONE_MINUS_SRC_ALPHA;
      srcA = BlendFactor.ONE;
      dstA = BlendFactor.ZERO;
      applyBlendFunc();
   }

   public static void blendFunc(SourceFactor src, DestFactor dst) {
      srcRgb = srcA = src.factor;
      dstRgb = dstA = dst.factor;
      applyBlendFunc();
   }

   public static void blendFunc(int src, int dst) {
      srcRgb = srcA = SourceFactor.fromGl(src);
      dstRgb = dstA = DestFactor.fromGl(dst);
      applyBlendFunc();
   }

   public static void blendFuncSeparate(SourceFactor src, DestFactor dst, SourceFactor srcAlpha, DestFactor dstAlpha) {
      srcRgb = src.factor;
      dstRgb = dst.factor;
      srcA = srcAlpha.factor;
      dstA = dstAlpha.factor;
      applyBlendFunc();
   }

   public static void blendFuncSeparate(int src, int dst, int srcAlpha, int dstAlpha) {
      srcRgb = SourceFactor.fromGl(src);
      dstRgb = DestFactor.fromGl(dst);
      srcA = SourceFactor.fromGl(srcAlpha);
      dstA = DestFactor.fromGl(dstAlpha);
      applyBlendFunc();
   }

   public static void enableDepthTest() {
      depthTest = true;
      gl(com.mojang.renderpearl.backend.opengl.GlStateManager::_enableDepthTest);
   }

   public static void disableDepthTest() {
      depthTest = false;
      gl(com.mojang.renderpearl.backend.opengl.GlStateManager::_disableDepthTest);
   }

   /** Takes the legacy GL constant; the 26.x depth buffer is reversed-Z so the comparison is flipped. */
   public static void depthFunc(int gl) {
      depthFunc = switch (gl) {
         case 512 -> CompareOp.NEVER_PASS;
         case 513 -> CompareOp.GREATER_THAN;
         case 514 -> CompareOp.EQUAL;
         case 515 -> CompareOp.GREATER_THAN_OR_EQUAL;
         case 516 -> CompareOp.LESS_THAN;
         case 517 -> CompareOp.NOT_EQUAL;
         case 518 -> CompareOp.LESS_THAN_OR_EQUAL;
         default -> CompareOp.ALWAYS_PASS;
      };
      CompareOp flipped = depthFunc;
      gl(() -> com.mojang.renderpearl.backend.opengl.GlStateManager._depthFunc(glCompare(flipped)));
   }

   public static void depthMask(boolean mask) {
      depthMask = mask;
      gl(() -> com.mojang.renderpearl.backend.opengl.GlStateManager._depthMask(mask));
   }

   public static void enableCull() {
      cull = true;
      gl(com.mojang.renderpearl.backend.opengl.GlStateManager::_enableCull);
   }

   public static void disableCull() {
      cull = false;
      gl(com.mojang.renderpearl.backend.opengl.GlStateManager::_disableCull);
   }

   public static void lineWidth(float width) {
      lineWidth = width;
      gl(() -> org.lwjgl.opengl.GL11.glLineWidth(Math.max(1.0F, width)));
   }

   public static void setShader(CoreShaders s) {
      shader = s;
      custom = null;
   }

   public static ShaderProgram setShader(ShaderProgram program) {
      custom = program;
      return program;
   }

   /** While deferring, draws are recorded and replayed at the end of the main level pass. */
   public static void beginDeferWorld() {
      deferWorld = true;
   }

   public static void endDeferWorld() {
      deferWorld = false;
   }

   public static Matrix4f getModelViewMatrix() {
      return com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrixCopy();
   }

   /** The current projection only exists as a GPU uniform; the level context's matrix is tracked here instead. */
   public static Matrix4f getProjectionMatrix() {
      return new Matrix4f(lastProjection);
   }

   public static void setLastProjection(Matrix4f projection) {
      lastProjection.set(projection);
   }

   private static final Matrix4f lastProjection = new Matrix4f();

   public static boolean isOnRenderThread() {
      return com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread();
   }

   public static void assertOnRenderThread() {
      com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThread();
   }

   public static void activeTexture(int unit) {
   }

   /** Raw GL texture names cannot be bound through a pipeline; the call is kept so state-restore code compiles. */
   public static void setShaderTexture(int slot, int glId) {
   }

   public static void setShaderColor(float r, float g, float b, float a) {
      shaderColor.set(r, g, b, a);
   }

   public static void setShaderTexture(int slot, Identifier id) {
      AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(id);
      texViews[slot] = t.getTextureView();
      texSamplers[slot] = t.getSampler();
   }

   public static void setShaderTexture(int slot, GpuTextureView view) {
      setShaderTexture(slot, view, com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getClampToEdge(com.mojang.renderpearl.api.textures.FilterMode.LINEAR));
   }

   public static void setShaderTexture(int slot, GpuTextureView view, GpuSampler sampler) {
      texViews[slot] = view;
      texSamplers[slot] = sampler;
   }

   public static void draw(MeshData mesh) {
      try (MeshData m = mesh) {
         MeshData.DrawState ds = m.drawState();
         if (ds.indexCount() <= 0) {
            return;
         }

         boolean useCustom = custom != null;
         List<String> samplerNames = useCustom ? custom.samplers() : (shader.textured ? List.of("Sampler0") : List.of());
         for (int i = 0; i < samplerNames.size(); i++) {
            if (texViews[i] == null) {
               return;
            }
         }

         RenderPipeline pipeline = pipeline(shader, custom, ds.primitiveTopology(), ds.format(), samplerNames.size());
         var device = com.mojang.blaze3d.systems.RenderSystem.getDevice();
         GpuBuffer vbo = device.createBuffer(() -> "AstolfoVisuals legacy vbo", 32, m.vertexBuffer());
         GpuBuffer ibo;
         com.mojang.renderpearl.api.pipeline.IndexType indexType;
         GpuBuffer ownedIbo = null;
         if (m.indexBuffer() != null) {
            ownedIbo = ibo = device.createBuffer(() -> "AstolfoVisuals legacy ibo", 64, m.indexBuffer());
            indexType = ds.indexType();
         } else {
            var seq = com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(ds.primitiveTopology());
            ibo = seq.getBuffer(ds.indexCount());
            indexType = seq.type();
         }

         GpuBuffer ubo = null;
         if (useCustom && custom.hasUniforms()) {
            int size = custom.blockSize();
            try (MemoryStack stack = MemoryStack.stackPush()) {
               Std140Builder b = Std140Builder.onStack(stack, size);
               custom.write(b);
               ubo = device.createBuffer(() -> "AstolfoVisuals legacy params", 128 | 8, b.get());
            }
         }

         GpuTextureView[] views = new GpuTextureView[samplerNames.size()];
         GpuSampler[] samplers = new GpuSampler[samplerNames.size()];
         for (int i = 0; i < views.length; i++) {
            views[i] = texViews[i];
            samplers[i] = texSamplers[i];
         }

         Deferred d = new Deferred(pipeline, vbo, ownedIbo, ibo, indexType, ds.indexCount(), ubo, samplerNames, views, samplers,
            new Vector4f(shaderColor), com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrixCopy());
         if (deferWorld) {
            DEFERRED.add(d);
         } else {
            execute(d, null);
         }
      }
   }

   /** Replays draws recorded with {@link #beginDeferWorld()}; {@code view} is the camera rotation. */
   public static void flushDeferred(Matrix4f view) {
      if (DEFERRED.isEmpty()) {
         return;
      }

      List<Deferred> copy = new java.util.ArrayList<>(DEFERRED);
      DEFERRED.clear();
      for (Deferred d : copy) {
         execute(d, view);
      }
   }

   private static void execute(Deferred d, Matrix4f view) {
      var device = com.mojang.blaze3d.systems.RenderSystem.getDevice();
      try {
         RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
         GpuTextureView color = target.getColorTextureView();
         GpuTextureView depth = null;
         if (d.pipeline.wantsDepthTexture() && target.hasDepth()) {
            depth = target.getDepthTextureView();
         }

         Matrix4f modelView = view != null ? new Matrix4f(view) : new Matrix4f(d.modelView);
         GpuBufferSlice transforms = com.mojang.blaze3d.systems.RenderSystem.getDynamicUniforms().writeTransform(modelView, d.color);
         try (RenderPass pass = device.createCommandEncoder()
               .createRenderPass(() -> "AstolfoVisuals legacy draw", color, Optional.empty(), depth, OptionalDouble.empty())) {
            pass.setPipeline(com.mojang.blaze3d.systems.RenderSystem.getCompiledPipeline(d.pipeline));
            com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            if (d.ubo != null) {
               pass.setUniform("CustomUniforms", d.ubo);
            }

            pass.setVertexBuffer(0, d.vbo.slice());
            for (int i = 0; i < d.views.length; i++) {
               pass.setUniform(d.samplerNames.get(i), d.views[i], d.samplers[i]);
            }

            pass.setIndexBuffer(d.ibo, d.indexType);
            pass.drawIndexed(d.indexCount, 1, 0, 0, 0);
         }
      } finally {
         d.vbo.close();
         if (d.ownedIbo != null) {
            d.ownedIbo.close();
         }

         if (d.ubo != null) {
            d.ubo.close();
         }
      }
   }

   private static RenderPipeline pipeline(CoreShaders s, ShaderProgram custom, PrimitiveTopology topology, VertexFormat format, int samplerCount) {
      PipelineKey key = new PipelineKey(s, custom, topology, format, blend, srcRgb, dstRgb, srcA, dstA, depthTest, depthFunc, depthMask, cull);
      return PIPELINES.computeIfAbsent(key, k -> {
         Identifier shaderId = custom != null ? custom.shaderId() : Identifier.withDefaultNamespace(s.shader);
         RenderPipeline.Builder b = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("astolfoclient", "pipeline/legacy_" + pipelineCounter++))
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withVertexShader(shaderId)
            .withFragmentShader(shaderId)
            .withVertexBinding(0, format)
            .withPrimitiveTopology(topology)
            .withCull(cull)
            .withColorTargetState(k.blend ? new ColorTargetState(new BlendFunction(k.srcRgb, k.dstRgb, k.srcA, k.dstA)) : ColorTargetState.DEFAULT);
         if (custom != null && custom.hasUniforms()) {
            b.withBindGroupLayout(CUSTOM_LAYOUT);
         }

         if (samplerCount > 0) {
            BindGroupLayout.Builder lb = BindGroupLayout.builder();
            if (custom != null) {
               for (String name : custom.samplers()) {
                  lb.withUniform(name, UniformType.COMBINED_IMAGE_SAMPLER);
               }
            } else {
               lb.withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER);
            }

            b.withBindGroupLayout(lb.build());
         }

         if (k.depthTest) {
            b.withDepthStencilState(new DepthStencilState(k.depthFunc, k.depthMask));
         } else {
            b.withDepthStencilState(Optional.empty());
         }

         return b.build();
      });
   }

   /** Builds (and caches) the pipeline a custom shader would use with default state; used by the startup self-test. */
   public static RenderPipeline pipelineFor(ShaderProgram program) {
      return pipeline(CoreShaders.POSITION_COLOR, program, PrimitiveTopology.QUADS, program.format, program.samplers().size());
   }

   private static final BindGroupLayout CUSTOM_LAYOUT = BindGroupLayout.builder().withUniform("CustomUniforms", UniformType.UNIFORM_BUFFER).build();

   private record Deferred(
      RenderPipeline pipeline, GpuBuffer vbo, GpuBuffer ownedIbo, GpuBuffer ibo, com.mojang.renderpearl.api.pipeline.IndexType indexType, int indexCount,
      GpuBuffer ubo, List<String> samplerNames, GpuTextureView[] views, GpuSampler[] samplers, Vector4f color, Matrix4f modelView
   ) {
   }

   private record PipelineKey(
      CoreShaders shader, ShaderProgram custom, PrimitiveTopology topology, VertexFormat format, boolean blend, BlendFactor srcRgb, BlendFactor dstRgb,
      BlendFactor srcA, BlendFactor dstA, boolean depthTest, CompareOp depthFunc, boolean depthMask, boolean cull
   ) {
   }
}
