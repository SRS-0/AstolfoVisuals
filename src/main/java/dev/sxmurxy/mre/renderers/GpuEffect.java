package dev.sxmurxy.mre.renderers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/** Owns no OpenGL handles. Geometry, effect parameters and shading use Blaze3D. */
public final class GpuEffect {
    private final RenderPipeline pipeline;
    private final String[] names;
    private final boolean linearSampling;
    private final String name;

    public GpuEffect(String name, VertexFormat format) {
        this.name=name;
        linearSampling = name.equals("msdf_font");
        names = switch (name) {
            case "rectangle", "texture" -> new String[]{"Size", "Radius", "Smoothness"};
            case "border" -> new String[]{"Size", "Radius", "Thickness", "Smoothness"};
            case "blur" -> new String[]{"Size", "Radius", "Smoothness", "BlurRadius"};
            case "msdf_font" -> new String[]{"Range", "Thickness", "Smoothness", "Outline", "OutlineThickness", "OutlineColor"};
            case "liquidglass" -> new String[]{"Size", "Radius", "Smoothness", "CornerSmoothness", "GlobalAlpha", "FresnelPower", "FresnelColor", "FresnelAlpha", "BaseAlpha", "FresnelInvert", "FresnelMix", "DistortStrength"};
            default -> throw new IllegalArgumentException("Unknown effect: " + name);
        };
        var builder = xyz.angames.astolfoclient.client.render.compat.PipelineBuilder.builder()
                .withLocation(Identifier.fromNamespaceAndPath("mre", "pipeline/" + name))
                .withVertexShader(Identifier.fromNamespaceAndPath("mre", "core/" + name))
                .withFragmentShader(Identifier.fromNamespaceAndPath("mre", "core/" + name))
                .withVertexFormat(format, com.mojang.blaze3d.PrimitiveTopology.QUADS)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("EffectData", UniformType.UNIFORM_BUFFER)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(CompareOp.ALWAYS_PASS)
                .withDepthWrite(false).withCull(false);
        if (!name.equals("rectangle") && !name.equals("border")) builder.withSampler("Sampler0");
        pipeline = builder.build();
    }

    public Uniforms uniforms() { return new Uniforms(); }

    public final class Uniforms {
        private final float[] values = new float[names.length * 4];
        public Uniform getUniform(String name) {
            int index = Arrays.asList(names).indexOf(name);
            if (index < 0) throw new IllegalArgumentException("Unknown uniform: " + name);
            return new Uniform(index * 4);
        }
        public final class Uniform {
            private final int offset;
            private Uniform(int offset) { this.offset = offset; }
            public void set(float... components) {
                if (components.length > 4) throw new IllegalArgumentException("At most four components");
                System.arraycopy(components, 0, values, offset, components.length);
            }
        }
    }

    public void draw(MeshData mesh, Uniforms uniforms, AbstractTexture texture) {
        draw(mesh, uniforms, texture == null ? null : texture.getTextureView(), texture == null ? null :
                linearSampling ? RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR) : texture.getSampler());
    }

    public void draw(MeshData mesh, Uniforms uniforms, GpuTextureView texture, GpuSampler sampler) {
        RenderSystem.assertOnRenderThread();
        if(GuiEffects.enqueue(name,mesh,uniforms.values,texture,sampler))return;
        ByteBuffer data = MemoryUtil.memAlloc(uniforms.values.length * Float.BYTES);
        try (mesh) {
            data.asFloatBuffer().put(uniforms.values);
            var device = RenderSystem.getDevice();
            try (GpuBuffer parameters = device.createBuffer(() -> "Astolfo effect parameters", GpuBuffer.USAGE_UNIFORM, data);
                 GpuBuffer vertices = device.createBuffer(() -> "Astolfo effect vertices", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())) {
                var state = mesh.drawState();
                var indices = RenderSystem.getSequentialBuffer(com.mojang.blaze3d.PrimitiveTopology.QUADS);
                var indexBuffer = indices.getBuffer(state.indexCount());
                var transforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(), new Vector4f(1), new Vector3f(), new Matrix4f());
                var target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
                var color = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
                try (var pass = device.createCommandEncoder().createRenderPass(() -> "Astolfo GUI effect", color, java.util.Optional.empty())) {
                    pass.setPipeline(pipeline);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", transforms);
                    pass.setUniform("EffectData", parameters);
                    if (!com.mojang.blaze3d.pipeline.BindGroupLayout.flattenSamplers(pipeline.getBindGroupLayouts()).isEmpty()) {
                        if (texture == null || sampler == null) throw new IllegalStateException("Textured effect needs a texture and sampler");
                        pass.bindTexture("Sampler0", texture, sampler);
                    }
                    pass.setVertexBuffer(0,vertices.slice());
                    pass.setIndexBuffer(indexBuffer, indices.type());
                    pass.drawIndexed(state.indexCount(),1,0,0,0);
                }
            }
        } finally { MemoryUtil.memFree(data); }
    }
}
