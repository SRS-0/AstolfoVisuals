package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.renderpearl.api.commands.*;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;
import org.joml.Vector4fc;

/** Splits the game's main pass around effects that copy or sample its attachments. */
public final class WorldPass implements InvocationHandler {
    private static WorldPass current;
    private final CommandEncoder encoder;
    private final Supplier<String> label;
    private final GpuTextureView color, depth;
    private final Map<String, State> state = new LinkedHashMap<>();
    private final List<Supplier<String>> debugGroups = new ArrayList<>();
    private RenderPass delegate;
    private boolean paused;
    private record State(Method method, Object[] arguments) {}

    private WorldPass(CommandEncoder encoder, Supplier<String> label, GpuTextureView color,
                      Optional<Vector4fc> clearColor, GpuTextureView depth, OptionalDouble clearDepth) {
        this.encoder=encoder;this.label=label;this.color=color;this.depth=depth;
        delegate=encoder.createRenderPass(label,color,clearColor,depth,clearDepth);
    }
    public static RenderPass wrap(CommandEncoder encoder, Supplier<String> label, GpuTextureView color,
                                  Optional<Vector4fc> clearColor, GpuTextureView depth, OptionalDouble clearDepth) {
        current=new WorldPass(encoder,label,color,clearColor,depth,clearDepth);
        return (RenderPass)Proxy.newProxyInstance(RenderPass.class.getClassLoader(),new Class<?>[]{RenderPass.class},current);
    }
    public static boolean isActive() { return current!=null&&!current.paused; }
    public static <T> T outside(Supplier<T> effect) {
        if(!isActive())return effect.get();
        var pass=current;
        var previous=BufferRenderer.currentPass;
        for(int i=0;i<pass.debugGroups.size();i++)pass.delegate.popDebugGroup();
        pass.delegate.close();pass.paused=true;BufferRenderer.currentPass=null;
        try {return effect.get();}
        finally {
            pass.delegate=pass.encoder.createRenderPass(pass.label,pass.color,Optional.empty(),pass.depth,OptionalDouble.empty());
            try {for(var saved:pass.state.values())saved.method.invoke(pass.delegate,saved.arguments);for(var group:pass.debugGroups)pass.delegate.pushDebugGroup(group);}
            catch(ReflectiveOperationException ex){throw new IllegalStateException("Restoring world render state",ex);}
            finally {pass.paused=false;BufferRenderer.currentPass=previous;}
        }
    }
    @Override public Object invoke(Object proxy,Method method,Object[] arguments) throws Throwable {
        if(method.getName().equals("close")) {
            try{return method.invoke(delegate,arguments);}
            catch(InvocationTargetException ex){throw ex.getCause();}
            finally {if(current==this)current=null;}
        }
        var name=method.getName();
        if(name.equals("pushDebugGroup"))debugGroups.add((Supplier<String>)arguments[0]);
        else if(name.equals("popDebugGroup")&&!debugGroups.isEmpty())debugGroups.removeLast();
        if(name.startsWith("set")||name.equals("pushConstants")||name.equals("enableScissor")||name.equals("disableScissor")) {
            Object[] saved=arguments==null?new Object[0]:arguments.clone();
            if(name.equals("pushConstants")){var bytes=((ByteBuffer)saved[0]).duplicate();var copy=ByteBuffer.allocateDirect(bytes.remaining());copy.put(bytes).flip();saved[0]=copy;}
            var key=name.equals("setUniform")||name.equals("setVertexBuffer")?name+saved[0]:name.endsWith("Scissor")?"scissor":name;
            state.put(key,new State(method,saved));
        }
        try{return method.invoke(delegate,arguments);}
        catch(InvocationTargetException ex){throw ex.getCause();}
    }
}
