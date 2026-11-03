package xyz.angames.astolfoclient.client.hud;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;

/** Draws editor settings after HUD previews and blocks clicks through open panels. */
public final class HudPanelLayer {
    private record Panel(Matrix3x2f pose, Runnable draw) {}
    private static final List<Panel> pending = new ArrayList<>();
    private static final List<ScreenRectangle> bounds = new ArrayList<>();
    private static GuiGraphicsExtractor collecting;

    private HudPanelLayer() {}

    public static void begin(GuiGraphicsExtractor context) {
        collecting = context;
        pending.clear();
        bounds.clear();
    }

    public static void draw(GuiGraphicsExtractor context, Runnable draw) {
        if (context == collecting) pending.add(new Panel(new Matrix3x2f(context.pose()), draw));
        else draw.run();
    }

    public static void flush(GuiGraphicsExtractor context) {
        collecting = null;
        context.nextStratum();
        try {
            for (Panel panel : pending) {
                context.pose().pushMatrix();
                try {
                    context.pose().set(panel.pose());
                    panel.draw().run();
                } finally {
                    context.pose().popMatrix();
                }
            }
        } finally {
            pending.clear();
        }
    }

    public static void capture(GuiGraphicsExtractor context, float x, float y, float width, float height) {
        bounds.add(new ScreenRectangle((int)x, (int)y, (int)Math.ceil(width), (int)Math.ceil(height))
            .transformMaxBounds(context.pose()));
    }

    public static boolean contains(double x, double y) {
        for (ScreenRectangle rect : bounds) {
            if (x >= rect.left() && x < rect.right() && y >= rect.top() && y < rect.bottom()) return true;
        }
        return false;
    }

    public static void clear() { collecting = null; pending.clear(); bounds.clear(); }
}
