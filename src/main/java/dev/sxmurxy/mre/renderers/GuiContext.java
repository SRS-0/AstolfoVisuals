package dev.sxmurxy.mre.renderers;

import java.util.ArrayDeque;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;

/** Tracks the current extraction context; GPU work happens in GUI preparation. */
public final class GuiContext {
    private static DrawContext current;
    private static final ArrayDeque<ScreenRect> clips=new ArrayDeque<>();
    private static boolean needsScene;
    private GuiContext() {}
    public static void begin(DrawContext context){current=context;clips.clear();needsScene=false;}
    public static DrawContext current(){return current;}
    public static ScreenRect clip(){return clips.peek();}
    public static void requestScene(){needsScene=true;}
    public static void pushClip(DrawContext context,int x1,int y1,int x2,int y2){
        if(current!=context)return;
        var rect=new ScreenRect(x1,y1,Math.max(0,x2-x1),Math.max(0,y2-y1)).transformEachVertex(context.getMatrices());
        if(!clips.isEmpty()){var intersection=rect.intersection(clips.peek());rect=intersection==null?ScreenRect.empty():intersection;}
        clips.push(rect);
    }
    public static void popClip(DrawContext context){if(current==context&&!clips.isEmpty())clips.pop();}
    public static void finish(){current=null;clips.clear();if(needsScene){needsScene=false;GpuSceneCopy.capture();}}
}
