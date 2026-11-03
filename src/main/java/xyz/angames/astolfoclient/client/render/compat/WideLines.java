package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Expands line segments into triangles so widths work on both graphics backends. */
final class WideLines {
    static boolean draw(BuiltBuffer source) {
        var state=source.getDrawParameters();
        boolean strip=state.mode()==VertexFormat.DrawMode.DEBUG_LINE_STRIP;
        if(!strip&&state.mode()!=VertexFormat.DrawMode.DEBUG_LINES)return false;
        try(source;var allocator=new BufferAllocator(Math.max(256,state.vertexCount()*56))) {
            var builder=new BufferBuilder(allocator,VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_COLOR);
            var format=state.format();var bytes=source.getBuffer().order(java.nio.ByteOrder.nativeOrder());
            int stride=format.getVertexSize(),position=format.getOffset(VertexFormatElement.POSITION),color=format.getOffset(VertexFormatElement.COLOR);
            var transform=new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix());
            var inverse=new Matrix4f(transform).invert();
            var window=MinecraftClient.getInstance().getWindow();
            float width=window.getFramebufferWidth(),height=window.getFramebufferHeight();
            for(int i=0;i+1<state.vertexCount();i+=strip?1:2) {
                int first=i*stride,second=(i+1)*stride;
                var a=transform.transform(new Vector4f(bytes.getFloat(first+position),bytes.getFloat(first+position+4),bytes.getFloat(first+position+8),1));
                var b=transform.transform(new Vector4f(bytes.getFloat(second+position),bytes.getFloat(second+position+4),bytes.getFloat(second+position+8),1));
                // Clip against the near plane before dividing by W.
                float da=a.z+a.w,db=b.z+b.w;
                if(da<=0&&db<=0)continue;
                if(da<=0)a.lerp(b,da/(da-db));
                else if(db<=0)b.lerp(a,db/(db-da));
                if(a.w<=0||b.w<=0)continue;
                float dx=(b.x/b.w-a.x/a.w)*width,dy=(b.y/b.w-a.y/a.w)*height;
                float length=(float)Math.sqrt(dx*dx+dy*dy);if(length<0.0001f)continue;
                float ox=-dy/length*Math.max(0.1f,RenderSystem.lineWidth)/width;
                float oy=dx/length*Math.max(0.1f,RenderSystem.lineWidth)/height;
                int ca=rgba(bytes,first+color),cb=rgba(bytes,second+color);
                vertex(builder,inverse,a,ox,oy,ca);vertex(builder,inverse,b,ox,oy,cb);
                vertex(builder,inverse,b,-ox,-oy,cb);vertex(builder,inverse,a,-ox,-oy,ca);
            }
            var result=builder.endNullable();if(result!=null)BufferRenderer.drawWithGlobalProgram(result);
        }
        return true;
    }
    private static int rgba(java.nio.ByteBuffer bytes,int offset) {
        return ((bytes.get(offset+3)&255)<<24)|((bytes.get(offset)&255)<<16)|((bytes.get(offset+1)&255)<<8)|(bytes.get(offset+2)&255);
    }
    private static void vertex(BufferBuilder builder,Matrix4f inverse,Vector4f point,float x,float y,int color) {
        var p=inverse.transform(new Vector4f(point.x+x*point.w,point.y+y*point.w,point.z,point.w));
        builder.vertex(p.x/p.w,p.y/p.w,p.z/p.w).color(color);
    }
}
