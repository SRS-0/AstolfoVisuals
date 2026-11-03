package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

/** Builds 26.2 pipeline layouts with an explicit color and depth attachment contract. */
public final class PipelineBuilder {
    private final RenderPipeline.Builder builder=RenderPipeline.builder();
    private final BindGroupLayout.Builder layout=BindGroupLayout.builder();
    private CompareOp depth=CompareOp.GREATER_THAN_OR_EQUAL;
    private boolean writeDepth=true,hasBindings;
    private BlendFunction blend;
    public static PipelineBuilder builder(){return new PipelineBuilder();}
    public PipelineBuilder withLocation(Identifier id){builder.withLocation(id);return this;}
    public PipelineBuilder withVertexShader(Identifier id){builder.withVertexShader(id);return this;}
    public PipelineBuilder withFragmentShader(Identifier id){builder.withFragmentShader(id);return this;}
    public PipelineBuilder withVertexFormat(VertexFormat format,PrimitiveTopology topology){builder.withVertexBinding(0,format).withPrimitiveTopology(topology);return this;}
    public PipelineBuilder withUniform(String name,UniformType type){layout.withUniform(name,type);hasBindings=true;return this;}
    public PipelineBuilder withSampler(String name){layout.withSampler(name);hasBindings=true;return this;}
    public PipelineBuilder withBlend(BlendFunction value){blend=value;return this;}
    public PipelineBuilder withoutBlend(){blend=null;return this;}
    public PipelineBuilder withCull(boolean value){builder.withCull(value);return this;}
    public PipelineBuilder withDepthWrite(boolean value){writeDepth=value;return this;}
    public PipelineBuilder withDepthTestFunction(CompareOp value){depth=value;return this;}
    public RenderPipeline build(){
        if(hasBindings)builder.withBindGroupLayout(layout.build());
        builder.withColorTargetState(blend==null?ColorTargetState.DEFAULT:new ColorTargetState(blend));
        builder.withDepthStencilState(depth==CompareOp.ALWAYS_PASS&&!writeDepth?java.util.Optional.empty():java.util.Optional.of(new DepthStencilState(depth,writeDepth)));
        return builder.build();
    }
}
