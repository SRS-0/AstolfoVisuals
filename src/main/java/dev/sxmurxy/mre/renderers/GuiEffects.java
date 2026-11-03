package dev.sxmurxy.mre.renderers;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.lwjgl.system.MemoryUtil;
import xyz.angames.astolfoclient.client.mixin.BufferBuilderGpuAccessor;

/** GUI effect parameters travel with vertices, allowing Minecraft to batch and order them. */
public final class GuiEffects {
    private static final VertexFormatElement[] ELEMENTS=new VertexFormatElement[12];
    private static final Map<String,RenderPipeline> PIPELINES=new HashMap<>();
    static {
        int next=0;
        for(int i=0;i<ELEMENTS.length;i++){
            while(next<VertexFormatElement.MAX_COUNT&&VertexFormatElement.byId(next)!=null)next++;
            if(next==VertexFormatElement.MAX_COUNT)throw new IllegalStateException("Not enough vertex element IDs for Astolfo GUI effects");
            ELEMENTS[i]=VertexFormatElement.register(next++,0,VertexFormatElement.Type.FLOAT,VertexFormatElement.Usage.GENERIC,4);
        }
    }
    private GuiEffects() {}
    private static RenderPipeline pipeline(String name,boolean textured,int count){
        return PIPELINES.computeIfAbsent(name,key->{
            var format=VertexFormat.builder().add("Position",VertexFormatElement.POSITION).add("Color",VertexFormatElement.COLOR);
            if(textured)format.add("UV0",VertexFormatElement.UV0);
            for(int i=0;i<count;i++)format.add("Effect"+i,ELEMENTS[i]);
            var builder=RenderPipeline.builder().withLocation(Identifier.of("mre","pipeline/"+name+"_gui"))
                    .withVertexShader(Identifier.of("mre","core/"+name+"_gui")).withFragmentShader(Identifier.of("mre","core/"+name+"_gui"))
                    .withVertexFormat(format.build(),VertexFormat.DrawMode.QUADS)
                    .withUniform("Projection",UniformType.UNIFORM_BUFFER).withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER)
                    .withBlend(BlendFunction.TRANSLUCENT).withCull(false).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
            if(!name.equals("rectangle")&&!name.equals("border"))builder.withSampler("Sampler0");
            return builder.build();
        });
    }
    static boolean enqueue(String name,BuiltBuffer mesh,float[] uniforms,GpuTextureView texture,GpuSampler sampler){
        var context=GuiContext.current();if(context==null)return false;
        try(mesh){
            var state=mesh.getDrawParameters();var format=state.format();var data=mesh.getBuffer().order(java.nio.ByteOrder.nativeOrder());int count=state.vertexCount(),stride=format.getVertexSize();
            float[] positions=new float[count*3],uv=new float[count*2];int[] colors=new int[count];
            int positionOffset=format.getOffset(VertexFormatElement.POSITION),colorOffset=format.getOffset(VertexFormatElement.COLOR);
            boolean textured=format.contains(VertexFormatElement.UV0);int textureOffset=textured?format.getOffset(VertexFormatElement.UV0):0;
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
            var bounds=new ScreenRect((int)Math.floor(minX),(int)Math.floor(minY),(int)Math.ceil(maxX)-(int)Math.floor(minX),(int)Math.ceil(maxY)-(int)Math.floor(minY));
            var clip=GuiContext.clip();if(clip!=null){bounds=bounds.intersection(clip);if(bounds==null||bounds.width()==0||bounds.height()==0)return true;}
            var setup=texture==null?TextureSetup.empty():TextureSetup.of(texture,sampler);
            context.state.addSimpleElement(new Element(pipeline(name,textured,uniforms.length/4),setup,clip,bounds,positions,uv,colors,uniforms.clone()));
            return true;
        }
    }
    private record Element(RenderPipeline pipeline,TextureSetup textureSetup,ScreenRect scissorArea,ScreenRect bounds,
                           float[] positions,float[] uv,int[] colors,float[] parameters) implements SimpleGuiElementRenderState {
        public void setupVertices(VertexConsumer consumer){
            var attributes=(BufferBuilderGpuAccessor)consumer;
            for(int vertex=0;vertex<colors.length;vertex++){
                consumer.vertex(positions[vertex*3],positions[vertex*3+1],positions[vertex*3+2]).color(colors[vertex]);
                if(pipeline.getVertexFormat().contains(VertexFormatElement.UV0))consumer.texture(uv[vertex*2],uv[vertex*2+1]);
                for(int element=0;element<parameters.length/4;element++){
                    long address=attributes.astolfo$beginElement(ELEMENTS[element]);
                    if(address==0)throw new IllegalStateException("GUI effect attribute missing");
                    for(int component=0;component<4;component++)MemoryUtil.memPutFloat(address+component*4L,parameters[element*4+component]);
                }
            }
        }
    }
}
