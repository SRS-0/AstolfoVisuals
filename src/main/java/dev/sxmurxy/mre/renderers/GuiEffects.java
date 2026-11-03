package dev.sxmurxy.mre.renderers;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.blaze3d.vertex.*;
import com.mojang.renderpearl.api.vertex.*;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.lwjgl.system.MemoryUtil;
import xyz.angames.astolfoclient.client.mixin.BufferBuilderGpuAccessor;

/** GUI effect parameters travel with vertices, allowing Minecraft to batch and order them. */
public final class GuiEffects {
    private static final Map<String,RenderPipeline> PIPELINES=new HashMap<>();
    private GuiEffects() {}
    private static RenderPipeline pipeline(String name,boolean textured,int count){
        return PIPELINES.computeIfAbsent(name,key->{
            var format=VertexFormat.builder(0).addAttribute("Position",com.mojang.renderpearl.api.GpuFormat.RGB32_FLOAT).addAttribute("Color",com.mojang.renderpearl.api.GpuFormat.RGBA8_UNORM);
            if(textured)format.addAttribute("UV0",com.mojang.renderpearl.api.GpuFormat.RG32_FLOAT);
            format.addAttribute("EffectLocal",com.mojang.renderpearl.api.GpuFormat.RG32_FLOAT);
            for(int i=0;i<count;i++)format.addAttribute("Effect"+i,com.mojang.renderpearl.api.GpuFormat.RGBA32_FLOAT);
            var builder=xyz.angames.astolfoclient.client.render.compat.PipelineBuilder.builder().withLocation(Identifier.fromNamespaceAndPath("mre","pipeline/"+name+"_gui"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("mre","core/"+name+"_gui")).withFragmentShader(Identifier.fromNamespaceAndPath("mre","core/"+name+"_gui"))
                    .withVertexFormat(format.build(),com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS)
                    .withUniform("Projection",UniformType.UNIFORM_BUFFER).withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT).withCull(false).withDepthWrite(false).withDepthTestFunction(CompareOp.ALWAYS_PASS);
            if(!name.equals("rectangle")&&!name.equals("border"))builder.withSampler("Sampler0");
            return builder.build();
        });
    }
    static boolean enqueue(String name,MeshData mesh,float[] uniforms,GpuTextureView texture,GpuSampler sampler){
        var context=GuiContext.current();if(context==null)return false;
        try(mesh){
            var state=mesh.drawState();var format=state.format();var data=mesh.vertexBuffer().order(java.nio.ByteOrder.nativeOrder());int count=state.vertexCount(),stride=format.getVertexSize();
            float[] positions=new float[count*3],uv=new float[count*2];int[] colors=new int[count];
            int positionOffset=format.getElement("Position").offset(),colorOffset=format.getElement("Color").offset();
            boolean textured=format.contains("UV0");int textureOffset=textured?format.getElement("UV0").offset():0;
            float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
            for(int i=0;i<count;i++){
                int offset=i*stride;float x=data.getFloat(offset+positionOffset),y=data.getFloat(offset+positionOffset+4),z=data.getFloat(offset+positionOffset+8);
                positions[i*3]=x;positions[i*3+1]=y;positions[i*3+2]=z;
                minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
                int r=data.get(offset+colorOffset)&255,g=data.get(offset+colorOffset+1)&255,b=data.get(offset+colorOffset+2)&255,a=data.get(offset+colorOffset+3)&255;
                colors[i]=(a<<24)|(r<<16)|(g<<8)|b;
                if(textured){uv[i*2]=data.getFloat(offset+textureOffset);uv[i*2+1]=data.getFloat(offset+textureOffset+4);}
            }
            if(count==0)return true;
            var bounds=new ScreenRectangle((int)Math.floor(minX),(int)Math.floor(minY),(int)Math.ceil(maxX)-(int)Math.floor(minX),(int)Math.ceil(maxY)-(int)Math.floor(minY));
            var clip=GuiContext.clip();if(clip!=null){bounds=bounds.intersection(clip);if(bounds==null||bounds.width()==0||bounds.height()==0)return true;}
            var setup=texture==null?TextureSetup.noTexture():TextureSetup.singleTexture(texture,sampler);
            context.guiRenderState.addGuiElement(new Element(pipeline(name,textured,uniforms.length/4),setup,clip,bounds,positions,uv,colors,uniforms.clone()));
            return true;
        }
    }
    private record Element(RenderPipeline pipeline,TextureSetup textureSetup,ScreenRectangle scissorArea,ScreenRectangle bounds,
                           float[] positions,float[] uv,int[] colors,float[] parameters) implements GuiElementRenderState {
        public void buildVertices(VertexConsumer consumer){
            var attributes=(BufferBuilderGpuAccessor)consumer;
            for(int vertex=0;vertex<colors.length;vertex++){
                consumer.addVertex(positions[vertex*3],positions[vertex*3+1],positions[vertex*3+2]).setColor(colors[vertex]);
                if(pipeline.getVertexFormatBinding(0).contains("UV0"))consumer.setUv(uv[vertex*2],uv[vertex*2+1]);
                // GUI batches may start at any vertex offset; gl_VertexID is not quad-local.
                long local=attributes.astolfo$vertexPointer()+pipeline.getVertexFormatBinding(0).getElement("EffectLocal").offset();
                MemoryUtil.memPutFloat(local,(vertex%4>=2)?1f:0f);
                MemoryUtil.memPutFloat(local+4,(vertex%4==1||vertex%4==2)?1f:0f);
                for(int element=0;element<parameters.length/4;element++){
                    long address=attributes.astolfo$vertexPointer()+pipeline.getVertexFormatBinding(0).getElement("Effect"+element).offset();
                    if(address==0)throw new IllegalStateException("GUI effect attribute missing");
                    for(int component=0;component<4;component++)MemoryUtil.memPutFloat(address+component*4L,parameters[element*4+component]);
                }
            }
        }
    }
}
