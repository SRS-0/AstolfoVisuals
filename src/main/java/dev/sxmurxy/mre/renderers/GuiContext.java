package dev.sxmurxy.mre.renderers;

import java.util.ArrayDeque;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/** Tracks the current extraction context; GPU work happens in GUI preparation. */
public final class GuiContext {
    private static GuiGraphicsExtractor current;
    private static final ArrayDeque<ScreenRectangle> clips=new ArrayDeque<>();
    private static boolean needsScene;
    private GuiContext() {}
    public static void begin(GuiGraphicsExtractor context){current=context;clips.clear();}
    public static GuiGraphicsExtractor current(){return current;}
    public static ScreenRectangle clip(){return clips.peek();}
    public static void requestScene(){needsScene=true;}
    public static void pushClip(GuiGraphicsExtractor context,int x1,int y1,int x2,int y2){
        if(current!=context)return;
        var rect=new ScreenRectangle(x1,y1,Math.max(0,x2-x1),Math.max(0,y2-y1)).transformMaxBounds(context.pose());
        if(!clips.isEmpty()){var intersection=rect.intersection(clips.peek());rect=intersection==null?ScreenRectangle.empty():intersection;}
        clips.push(rect);
    }
    public static void popClip(GuiGraphicsExtractor context){if(current==context&&!clips.isEmpty())clips.pop();}
    public static void endExtraction(){current=null;clips.clear();}
    public static void finish(){current=null;clips.clear();if(needsScene){needsScene=false;GpuSceneCopy.capture();}}
}
