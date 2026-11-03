package dev.sxmurxy.mre.renderers;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
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
        var builder = RenderPipeline.builder()
                .withLocation(Identifier.of("mre", "pipeline/" + name))
                .withVertexShader(Identifier.of("mre", "core/" + name))
                .withFragmentShader(Identifier.of("mre", "core/" + name))
                .withVertexFormat(format, VertexFormat.DrawMode.QUADS)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("EffectData", UniformType.UNIFORM_BUFFER)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
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

    public void draw(BuiltBuffer mesh, Uniforms uniforms, AbstractTexture texture) {
        draw(mesh, uniforms, texture == null ? null : texture.getGlTextureView(), texture == null ? null :
                linearSampling ? RenderSystem.getSamplerCache().get(com.mojang.blaze3d.textures.FilterMode.LINEAR) : texture.getSampler());
    }

    public void draw(BuiltBuffer mesh, Uniforms uniforms, GpuTextureView texture, GpuSampler sampler) {
        RenderSystem.assertOnRenderThread();
        if(GuiEffects.enqueue(name,mesh,uniforms.values,texture,sampler))return;
        ByteBuffer data = MemoryUtil.memAlloc(uniforms.values.length * Float.BYTES);
        try (mesh) {
            data.asFloatBuffer().put(uniforms.values);
            var device = RenderSystem.getDevice();
            try (GpuBuffer parameters = device.createBuffer(() -> "Astolfo effect parameters", GpuBuffer.USAGE_UNIFORM, data);
                 GpuBuffer vertices = device.createBuffer(() -> "Astolfo effect vertices", GpuBuffer.USAGE_VERTEX, mesh.getBuffer())) {
                var state = mesh.getDrawParameters();
                var indices = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
                var indexBuffer = indices.getIndexBuffer(state.indexCount());
                var transforms = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), new Vector4f(1), new Vector3f(), new Matrix4f());
                var target = MinecraftClient.getInstance().getFramebuffer();
                var color = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : target.getColorAttachmentView();
                try (var pass = device.createCommandEncoder().createRenderPass(() -> "Astolfo GUI effect", color, OptionalInt.empty())) {
                    pass.setPipeline(pipeline);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", transforms);
                    pass.setUniform("EffectData", parameters);
                    if (!pipeline.getSamplers().isEmpty()) {
                        if (texture == null || sampler == null) throw new IllegalStateException("Textured effect needs a texture and sampler");
                        pass.bindTexture("Sampler0", texture, sampler);
                    }
                    pass.setVertexBuffer(0, vertices);
                    pass.setIndexBuffer(indexBuffer, indices.getIndexType());
                    pass.drawIndexed(0, 0, state.indexCount(), 1);
                }
            }
        } finally { MemoryUtil.memFree(data); }
    }
}
