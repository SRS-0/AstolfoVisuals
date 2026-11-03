import java.lang.reflect.*;
import java.util.*;
import com.mojang.renderpearl.api.commands.*;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import xyz.angames.astolfoclient.client.render.compat.WorldPass;
import xyz.angames.astolfoclient.client.util.LegacyInput;

/** Exercises pass interruption/restoration without opening a graphics device. */
public class VerifyWorldPass {
    static final List<MockPass> passes=new ArrayList<>();
    static class MockPass implements InvocationHandler {
        boolean closed,uniform,scissor,pipeline;int groups;
        public Object invoke(Object proxy,Method method,Object[] args) {
            String name=method.getName();
            if(name.equals("toString"))return "MockPass";
            if(closed)throw new AssertionError("Commands after pass closure");
            switch(name){case "close" -> {if(groups!=0)throw new AssertionError("Unbalanced groups");closed=true;}
                case "pushDebugGroup" -> groups++;
                case "popDebugGroup" -> {if(--groups<0)throw new AssertionError("Group underflow");}
                case "setUniform" -> uniform=true;
                case "setPipeline" -> pipeline=true;
                case "enableScissor" -> scissor=true;}
            return null;
        }
    }
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args) {
        CommandEncoder encoder=(CommandEncoder)Proxy.newProxyInstance(CommandEncoder.class.getClassLoader(),new Class[]{CommandEncoder.class},(p,m,a)->{
            if(!m.getName().equals("createRenderPass"))throw new AssertionError(m);
            var state=new MockPass();passes.add(state);
            return Proxy.newProxyInstance(RenderPass.class.getClassLoader(),new Class[]{RenderPass.class},state);
        });
        var pass=WorldPass.wrap(encoder,()->"test",null,Optional.empty(),null,OptionalDouble.empty());
        pass.setPipeline(null);pass.setUniform("Projection",(GpuBuffer)null);pass.enableScissor(1,2,3,4);pass.pushDebugGroup(()->"group");
        check(WorldPass.outside(()->{check(!WorldPass.isActive());check(passes.getFirst().closed);return 42;})==42);
        check(passes.size()==2&&passes.getLast().uniform&&passes.getLast().pipeline&&passes.getLast().scissor&&passes.getLast().groups==1);
        try {WorldPass.outside(()->{throw new IllegalArgumentException("expected");});throw new AssertionError();}
        catch(IllegalArgumentException expected){check(WorldPass.isActive());check(passes.size()==3&&passes.getLast().groups==1);}
        pass.popDebugGroup();pass.close();check(!WorldPass.isActive());
        for(int old:new int[]{65,90,48,57,256,257,259,260,290,301,344,341})check(LegacyInput.legacyKey(LegacyInput.nativeKey(old))==old);
        check(LegacyInput.legacyButton(1)==0&&LegacyInput.legacyButton(3)==1&&LegacyInput.legacyButton(2)==2);
        check(LegacyInput.legacyModifiers(192|3)==3);
        System.out.println("PASS pass closure, state restoration, debug groups, exception recovery and SDL keybind translation");
    }
}
