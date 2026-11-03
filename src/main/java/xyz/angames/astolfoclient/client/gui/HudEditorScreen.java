package xyz.angames.astolfoclient.client.gui;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;

@Environment(value=EnvType.CLIENT)
public class HudEditorScreen
extends Screen {
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    private Object currentlyDraggedManager = null;
    private static boolean activeVLine = false;
    private static float activeVLineX = 0.0f;
    private static boolean activeHLine = false;
    private static float activeHLineY = 0.0f;

    public HudEditorScreen() {
        super((Component)Component.literal((String)"HUD Editor"));
    }

    protected void init() {
        super.init();
        xyz.angames.astolfoclient.client.hud.HudPanelLayer.clear();
        this.clampAllElements();
    }

    private void clampAllElements() {
        Object[] managers;
        for (Object manager : managers = new Object[]{AstolfoclientClient.targetHudManager, AstolfoclientClient.boyKisserManager, AstolfoclientClient.effectHudManager, AstolfoclientClient.armorHudManager, AstolfoclientClient.inventoryHudManager, AstolfoclientClient.activeBindsManager, AstolfoclientClient.testHudManager, AstolfoclientClient.musicHudManager, AstolfoclientClient.infoHudManager}) {
            if (manager == null) continue;
            try {
                float scale = this.getManagerScaleModifier(manager);
                float w = this.getManagerWidth(manager) * scale;
                float h = this.getManagerHeight(manager) * scale;
                float maxElemX = Math.max(0.0f, (float)this.width - w);
                float maxElemY = Math.max(0.0f, (float)this.height - h);
                float curX = 0.0f;
                try {
                    Field f = manager.getClass().getField("x");
                    curX = ((Number)f.get(manager)).floatValue();
                }
                catch (Exception e) {
                    try {
                        Method m = manager.getClass().getMethod("getX", new Class[0]);
                        curX = ((Number)m.invoke(manager, new Object[0])).floatValue();
                    }
                    catch (Exception m) {
                        // empty catch block
                    }
                }
                float curY = 0.0f;
                try {
                    Field f = manager.getClass().getField("y");
                    curY = ((Number)f.get(manager)).floatValue();
                }
                catch (Exception e) {
                    try {
                        Method m = manager.getClass().getMethod("getY", new Class[0]);
                        curY = ((Number)m.invoke(manager, new Object[0])).floatValue();
                    }
                    catch (Exception m) {
                        // empty catch block
                    }
                }
                float clampedX = Math.max(0.0f, Math.min(maxElemX, curX));
                float clampedY = Math.max(0.0f, Math.min(maxElemY, curY));
                if (clampedX == curX && clampedY == curY) continue;
                HudEditorScreen.applyPosition(manager, clampedX, clampedY);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public void extractTransparentBackground(GuiGraphicsExtractor context) {
    }

    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        xyz.angames.astolfoclient.client.hud.HudPanelLayer.begin(context);
        if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
            float gridSpacing = 10.0f;
            int gridColor = 0xDFFFFFF;
            for (float x = gridSpacing; x < (float)this.width; x += gridSpacing) {
                context.fill((int)x, 0, (int)x + 1, this.height, gridColor);
            }
            for (float y = gridSpacing; y < (float)this.height; y += gridSpacing) {
                context.fill(0, (int)y, this.width, (int)y + 1, gridColor);
            }
        }
        Minecraft mc = Minecraft.getInstance();
        double currentGuiScale = mc.getWindow().getGuiScale();
        float scaleModifier = (float)(2.0 / currentGuiScale);
        float hudWidth = (float)this.width / scaleModifier;
        float hudHeight = (float)this.height / scaleModifier;
        context.pose().pushMatrix();
        context.pose().scale(scaleModifier, scaleModifier);
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        MsdfFont bikoFont = (MsdfFont)BIKO_FONT.get();
        Color white = Color.WHITE;
        Color gray = new Color(170, 170, 170);
        String title = "HUD Editor";
        float titleSize = 16.0f;
        float titleW = bikoFont.getWidth(title, titleSize);
        float titleX = hudWidth / 2.0f - titleW / 2.0f;
        float titleY = 10.0f / scaleModifier;
        ((BuiltText)Builder.text().font(bikoFont).text(title).color(new Color(0, 0, 0, 150)).size(titleSize).build()).render(matrix, titleX + 1.0f, titleY + 1.0f);
        ((BuiltText)Builder.text().font(bikoFont).text(title).color(white).size(titleSize).build()).render(matrix, titleX, titleY);
        String desc = "Drag elements to reposition them (Hold SHIFT to snap/align)";
        float descSize = 11.5f;
        float descW = bikoFont.getWidth(desc, descSize);
        float descX = hudWidth / 2.0f - descW / 2.0f;
        float descY = 22.0f / scaleModifier;
        ((BuiltText)Builder.text().font(bikoFont).text(desc).color(new Color(0, 0, 0, 150)).size(descSize).build()).render(matrix, descX + 1.0f, descY + 1.0f);
        ((BuiltText)Builder.text().font(bikoFont).text(desc).color(gray).size(descSize).build()).render(matrix, descX, descY);
        String exit = "Press ESC to save and exit";
        float exitSize = 11.0f;
        float exitW = bikoFont.getWidth(exit, exitSize);
        float exitX = hudWidth / 2.0f - exitW / 2.0f;
        float exitY = 34.0f / scaleModifier;
        ((BuiltText)Builder.text().font(bikoFont).text(exit).color(new Color(0, 0, 0, 150)).size(exitSize).build()).render(matrix, exitX + 1.0f, exitY + 1.0f);
        ((BuiltText)Builder.text().font(bikoFont).text(exit).color(gray).size(exitSize).build()).render(matrix, exitX, exitY);
        context.pose().popMatrix();
        if (AstolfoclientClient.hudRenderer != null && AstolfoclientClient.hudRenderer.getLogoRenderer() != null) {
            AstolfoclientClient.hudRenderer.getLogoRenderer().render(context);
        }
        AstolfoclientClient.targetHudManager.render(context, delta);
        AstolfoclientClient.boyKisserManager.render(context, delta);
        AstolfoclientClient.effectHudManager.render(context);
        AstolfoclientClient.armorHudManager.render(context);
        AstolfoclientClient.inventoryHudManager.render(context);
        AstolfoclientClient.activeBindsManager.render(context, delta);
        AstolfoclientClient.testHudManager.render(context, delta);
        AstolfoclientClient.musicHudManager.render(context, delta);
        AstolfoclientClient.infoHudManager.render(context, delta);
        xyz.angames.astolfoclient.client.hud.HudPanelLayer.flush(context);
        if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
            int guideColor = -65281;
            if (activeVLine) {
                context.fill((int)activeVLineX, 0, (int)activeVLineX + 1, this.height, guideColor);
            }
            if (activeHLine) {
                context.fill(0, (int)activeHLineY, this.width, (int)activeHLineY + 1, guideColor);
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.currentlyDraggedManager = null;
        if (xyz.angames.astolfoclient.client.hud.HudPanelLayer.contains(mouseX, mouseY)) return true;
        if (AstolfoclientClient.hudRenderer != null && AstolfoclientClient.hudRenderer.getLogoRenderer().onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.hudRenderer.getLogoRenderer();
            return true;
        }
        if (AstolfoclientClient.infoHudManager != null && AstolfoclientClient.infoHudManager.onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.infoHudManager;
            return true;
        }
        if (AstolfoclientClient.musicHudManager != null && AstolfoclientClient.musicHudManager.onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.musicHudManager;
            return true;
        }
        if (AstolfoclientClient.testHudManager != null && AstolfoclientClient.testHudManager.onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.testHudManager;
            return true;
        }
        if (AstolfoclientClient.activeBindsManager != null && AstolfoclientClient.activeBindsManager.onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.activeBindsManager;
            return true;
        }
        if (AstolfoclientClient.inventoryHudManager != null && AstolfoclientClient.inventoryHudManager.onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.inventoryHudManager;
            return true;
        }
        if (AstolfoclientClient.armorHudManager != null && AstolfoclientClient.armorHudManager.onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.armorHudManager;
            return true;
        }
        if (AstolfoclientClient.effectHudManager != null && AstolfoclientClient.effectHudManager.onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.effectHudManager;
            return true;
        }
        if (AstolfoclientClient.boyKisserManager != null && AstolfoclientClient.boyKisserManager.onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.boyKisserManager;
            return true;
        }
        if (AstolfoclientClient.targetHudManager != null && AstolfoclientClient.targetHudManager.onMouseClicked(mouseX, mouseY, button)) {
            this.currentlyDraggedManager = AstolfoclientClient.targetHudManager;
            return true;
        }
        return super.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)), false);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.currentlyDraggedManager instanceof xyz.angames.astolfoclient.client.hud.LogoRenderer logo) {
            logo.onMouseDragged(mouseX);
            return true;
        }
        if (this.currentlyDraggedManager == AstolfoclientClient.infoHudManager && AstolfoclientClient.infoHudManager != null) {
            AstolfoclientClient.infoHudManager.onMouseDragged(mouseX, mouseY, button);
        } else if (this.currentlyDraggedManager == AstolfoclientClient.musicHudManager && AstolfoclientClient.musicHudManager != null) {
            AstolfoclientClient.musicHudManager.onMouseDragged(mouseX, mouseY, button);
        } else if (this.currentlyDraggedManager == AstolfoclientClient.testHudManager && AstolfoclientClient.testHudManager != null) {
            AstolfoclientClient.testHudManager.onMouseDragged(mouseX, mouseY, button);
        } else if (this.currentlyDraggedManager == AstolfoclientClient.activeBindsManager && AstolfoclientClient.activeBindsManager != null) {
            AstolfoclientClient.activeBindsManager.onMouseDragged(mouseX, mouseY, button);
        } else if (this.currentlyDraggedManager == AstolfoclientClient.inventoryHudManager && AstolfoclientClient.inventoryHudManager != null) {
            AstolfoclientClient.inventoryHudManager.onMouseDragged(mouseX, mouseY, button);
        } else if (this.currentlyDraggedManager == AstolfoclientClient.armorHudManager && AstolfoclientClient.armorHudManager != null) {
            AstolfoclientClient.armorHudManager.onMouseDragged(mouseX, mouseY, button);
        } else if (this.currentlyDraggedManager == AstolfoclientClient.effectHudManager && AstolfoclientClient.effectHudManager != null) {
            AstolfoclientClient.effectHudManager.onMouseDragged(mouseX, mouseY, button);
        } else if (this.currentlyDraggedManager == AstolfoclientClient.boyKisserManager && AstolfoclientClient.boyKisserManager != null) {
            AstolfoclientClient.boyKisserManager.onMouseDragged(mouseX, mouseY, button);
        } else if (this.currentlyDraggedManager == AstolfoclientClient.targetHudManager && AstolfoclientClient.targetHudManager != null) {
            AstolfoclientClient.targetHudManager.onMouseDragged(mouseX, mouseY, button);
        }
        DraggedElement de = this.getDraggedElement();
        if (de != null) {
            if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
                this.applySnapping(de);
            }
            float currentScale = de.scaleModifier;
            float elemW = de.width * currentScale;
            float elemH = de.height * currentScale;
            float maxElemX = Math.max(0.0f, (float)this.width - elemW);
            float maxElemY = Math.max(0.0f, (float)this.height - elemH);
            float curX = de.x;
            try {
                Field f = de.manager.getClass().getField("x");
                curX = ((Number)f.get(de.manager)).floatValue();
            }
            catch (Exception e) {
                try {
                    Method m = de.manager.getClass().getMethod("getX", new Class[0]);
                    curX = ((Number)m.invoke(de.manager, new Object[0])).floatValue();
                }
                catch (Exception m) {
                    // empty catch block
                }
            }
            float curY = de.y;
            try {
                Field f = de.manager.getClass().getField("y");
                curY = ((Number)f.get(de.manager)).floatValue();
            }
            catch (Exception e) {
                try {
                    Method m = de.manager.getClass().getMethod("getY", new Class[0]);
                    curY = ((Number)m.invoke(de.manager, new Object[0])).floatValue();
                }
                catch (Exception m) {
                    // empty catch block
                }
            }
            float clampedX = Math.max(0.0f, Math.min(maxElemX, curX));
            float clampedY = Math.max(0.0f, Math.min(maxElemY, curY));
            if (clampedX != curX || clampedY != curY) {
                de.apply(clampedX, clampedY);
            }
        } else {
            activeVLine = false;
            activeHLine = false;
        }
        return super.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)), deltaX, deltaY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.currentlyDraggedManager instanceof xyz.angames.astolfoclient.client.hud.LogoRenderer logo) {
            logo.onMouseReleased(mouseX);
        }
        activeVLine = false;
        activeHLine = false;
        if (AstolfoclientClient.targetHudManager != null) {
            AstolfoclientClient.targetHudManager.onMouseReleased(mouseX, mouseY, button);
        }
        if (AstolfoclientClient.boyKisserManager != null) {
            AstolfoclientClient.boyKisserManager.onMouseReleased(mouseX, mouseY, button);
        }
        if (AstolfoclientClient.effectHudManager != null) {
            AstolfoclientClient.effectHudManager.onMouseReleased(button);
        }
        if (AstolfoclientClient.armorHudManager != null) {
            AstolfoclientClient.armorHudManager.onMouseReleased(button);
        }
        if (AstolfoclientClient.inventoryHudManager != null) {
            AstolfoclientClient.inventoryHudManager.onMouseReleased(button);
        }
        if (AstolfoclientClient.activeBindsManager != null) {
            AstolfoclientClient.activeBindsManager.onMouseReleased(mouseX, mouseY, button);
        }
        if (AstolfoclientClient.testHudManager != null) {
            AstolfoclientClient.testHudManager.onMouseReleased(mouseX, mouseY, button);
        }
        if (AstolfoclientClient.musicHudManager != null) {
            AstolfoclientClient.musicHudManager.onMouseReleased(mouseX, mouseY, button);
        }
        if (AstolfoclientClient.infoHudManager != null) {
            AstolfoclientClient.infoHudManager.onMouseReleased(mouseX, mouseY, button);
        }
        this.currentlyDraggedManager = null;
        return super.mouseReleased(new net.minecraft.client.input.MouseButtonEvent(mouseX, mouseY, new net.minecraft.client.input.MouseButtonInfo(button, 0)));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        AstolfoclientClient.musicHudManager.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private static void applyPosition(Object manager, float newX, float newY) {
        if (manager == null) {
            return;
        }
        try {
            Field xField = manager.getClass().getField("x");
            if (xField.getType() == Float.TYPE) {
                xField.setFloat(manager, newX);
            } else if (xField.getType() == Double.TYPE) {
                xField.setDouble(manager, newX);
            }
        }
        catch (Exception e) {
            try {
                Method setX = manager.getClass().getMethod("setX", Float.TYPE);
                setX.invoke(manager, Float.valueOf(newX));
            }
            catch (Exception e2) {
                try {
                    Method setX = manager.getClass().getMethod("setX", Double.TYPE);
                    setX.invoke(manager, newX);
                }
                catch (Exception setX) {
                    // empty catch block
                }
            }
        }
        try {
            Field yField = manager.getClass().getField("y");
            if (yField.getType() == Float.TYPE) {
                yField.setFloat(manager, newY);
            } else if (yField.getType() == Double.TYPE) {
                yField.setDouble(manager, newY);
            }
        }
        catch (Exception e) {
            try {
                Method setY = manager.getClass().getMethod("setY", Float.TYPE);
                setY.invoke(manager, Float.valueOf(newY));
            }
            catch (Exception e2) {
                try {
                    Method setY = manager.getClass().getMethod("setY", Double.TYPE);
                    setY.invoke(manager, newY);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
    }

    private DraggedElement getDraggedElement() {
        Object[] managers;
        for (Object manager : managers = new Object[]{AstolfoclientClient.targetHudManager, AstolfoclientClient.boyKisserManager, AstolfoclientClient.effectHudManager, AstolfoclientClient.armorHudManager, AstolfoclientClient.inventoryHudManager, AstolfoclientClient.activeBindsManager, AstolfoclientClient.testHudManager, AstolfoclientClient.musicHudManager, AstolfoclientClient.infoHudManager}) {
            if (manager == null) continue;
            boolean dragging = false;
            try {
                Field f = manager.getClass().getDeclaredField("dragging");
                f.setAccessible(true);
                dragging = f.getBoolean(manager);
            }
            catch (Exception e) {
                try {
                    Field f = manager.getClass().getDeclaredField("isDragging");
                    f.setAccessible(true);
                    dragging = f.getBoolean(manager);
                }
                catch (Exception f) {
                    // empty catch block
                }
            }
            if (!dragging) continue;
            DraggedElement de = new DraggedElement();
            de.manager = manager;
            float xVal = 0.0f;
            try {
                Field f = manager.getClass().getField("x");
                xVal = ((Number)f.get(manager)).floatValue();
            }
            catch (Exception e) {
                try {
                    Method m = manager.getClass().getMethod("getX", new Class[0]);
                    xVal = ((Number)m.invoke(manager, new Object[0])).floatValue();
                }
                catch (Exception m) {
                    // empty catch block
                }
            }
            de.x = xVal;
            float yVal = 0.0f;
            try {
                Field f = manager.getClass().getField("y");
                yVal = ((Number)f.get(manager)).floatValue();
            }
            catch (Exception e) {
                try {
                    Method m = manager.getClass().getMethod("getY", new Class[0]);
                    yVal = ((Number)m.invoke(manager, new Object[0])).floatValue();
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            de.y = yVal;
            de.scaleModifier = this.getManagerScaleModifier(manager);
            de.width = this.getManagerWidth(manager);
            de.height = this.getManagerHeight(manager);
            return de;
        }
        return null;
    }

    private float getManagerScaleModifier(Object manager) {
        try {
            Method m = manager.getClass().getDeclaredMethod("getScaleModifier", new Class[0]);
            m.setAccessible(true);
            return ((Number)m.invoke(manager, new Object[0])).floatValue();
        }
        catch (Exception e) {
            try {
                Field f = manager.getClass().getDeclaredField("scaleModifier");
                f.setAccessible(true);
                return ((Number)f.get(manager)).floatValue();
            }
            catch (Exception exception) {
                double currentGuiScale = Minecraft.getInstance().getWindow().getGuiScale();
                return (float)(2.0 / currentGuiScale);
            }
        }
    }

    private List<ElementRect> getOtherElementRects(Object draggedManager) {
        Object[] managers;
        ArrayList<ElementRect> rects = new ArrayList<ElementRect>();
        for (Object manager : managers = new Object[]{AstolfoclientClient.targetHudManager, AstolfoclientClient.boyKisserManager, AstolfoclientClient.effectHudManager, AstolfoclientClient.armorHudManager, AstolfoclientClient.inventoryHudManager, AstolfoclientClient.activeBindsManager, AstolfoclientClient.testHudManager, AstolfoclientClient.musicHudManager, AstolfoclientClient.infoHudManager}) {
            if (manager == null || manager == draggedManager) continue;
            try {
                float xVal = 0.0f;
                try {
                    Field f = manager.getClass().getField("x");
                    xVal = ((Number)f.get(manager)).floatValue();
                }
                catch (Exception e) {
                    try {
                        Method m = manager.getClass().getMethod("getX", new Class[0]);
                        xVal = ((Number)m.invoke(manager, new Object[0])).floatValue();
                    }
                    catch (Exception ex) {
                        continue;
                    }
                }
                float yVal = 0.0f;
                try {
                    Field f = manager.getClass().getField("y");
                    yVal = ((Number)f.get(manager)).floatValue();
                }
                catch (Exception e) {
                    try {
                        Method m = manager.getClass().getMethod("getY", new Class[0]);
                        yVal = ((Number)m.invoke(manager, new Object[0])).floatValue();
                    }
                    catch (Exception ex) {
                        continue;
                    }
                }
                float scale = this.getManagerScaleModifier(manager);
                float w = this.getManagerWidth(manager);
                float h = this.getManagerHeight(manager);
                ElementRect r = new ElementRect();
                r.manager = manager;
                r.left = xVal;
                r.top = yVal;
                r.right = xVal + w * scale;
                r.bottom = yVal + h * scale;
                r.centerX = r.left + w * scale / 2.0f;
                r.centerY = r.top + h * scale / 2.0f;
                rects.add(r);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return rects;
    }

    private float getManagerWidth(Object manager) {
        if (manager == null) {
            return 100.0f;
        }
        try {
            Method m = manager.getClass().getMethod("getWidth", new Class[0]);
            return ((Number)m.invoke(manager, new Object[0])).floatValue();
        }
        catch (Exception m) {
            try {
                Field f = manager.getClass().getDeclaredField("currentWidth");
                f.setAccessible(true);
                return f.getFloat(manager);
            }
            catch (Exception f) {
                try {
                    Field f2 = manager.getClass().getDeclaredField("width");
                    f2.setAccessible(true);
                    return f2.getFloat(manager);
                }
                catch (Exception exception) {
                    return 100.0f;
                }
            }
        }
    }

    private float getManagerHeight(Object manager) {
        if (manager == null) {
            return 30.0f;
        }
        try {
            Method m = manager.getClass().getMethod("getHeight", new Class[0]);
            return ((Number)m.invoke(manager, new Object[0])).floatValue();
        }
        catch (Exception m) {
            try {
                Field f = manager.getClass().getDeclaredField("currentHeight");
                f.setAccessible(true);
                return f.getFloat(manager);
            }
            catch (Exception f) {
                try {
                    Field f2 = manager.getClass().getDeclaredField("height");
                    f2.setAccessible(true);
                    return f2.getFloat(manager);
                }
                catch (Exception exception) {
                    return 30.0f;
                }
            }
        }
    }

    private void applySnapping(DraggedElement de) {
        if (!net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
            activeVLine = false;
            activeHLine = false;
            return;
        }
        float snapThreshold = 5.0f;
        float gridSpacing = 10.0f;
        float currentScale = de.scaleModifier;
        float left = de.x;
        float top = de.y;
        float width = de.width * currentScale;
        float height = de.height * currentScale;
        float right = left + width;
        float bottom = top + height;
        float centerX = left + width / 2.0f;
        float centerY = top + height / 2.0f;
        float snappedLeft = left;
        boolean xSnapped = false;
        activeVLine = false;
        activeVLineX = 0.0f;
        float screenCenterX = (float)this.width / 2.0f;
        if (Math.abs(centerX - screenCenterX) < snapThreshold) {
            snappedLeft = screenCenterX - width / 2.0f;
            xSnapped = true;
            activeVLine = true;
            activeVLineX = screenCenterX;
        }
        List<ElementRect> others = this.getOtherElementRects(de.manager);
        if (!xSnapped) {
            for (ElementRect other : others) {
                if (Math.abs(centerX - other.centerX) < snapThreshold) {
                    snappedLeft = other.centerX - width / 2.0f;
                    xSnapped = true;
                    activeVLine = true;
                    activeVLineX = other.centerX;
                    break;
                }
                if (Math.abs(left - other.left) < snapThreshold) {
                    snappedLeft = other.left;
                    xSnapped = true;
                    activeVLine = true;
                    activeVLineX = other.left;
                    break;
                }
                if (Math.abs(right - other.right) < snapThreshold) {
                    snappedLeft = other.right - width;
                    xSnapped = true;
                    activeVLine = true;
                    activeVLineX = other.right;
                    break;
                }
                if (Math.abs(left - other.right) < snapThreshold) {
                    snappedLeft = other.right;
                    xSnapped = true;
                    activeVLine = true;
                    activeVLineX = other.right;
                    break;
                }
                if (!(Math.abs(right - other.left) < snapThreshold)) continue;
                snappedLeft = other.left - width;
                xSnapped = true;
                activeVLine = true;
                activeVLineX = other.left;
                break;
            }
        }
        if (!xSnapped) {
            float margin = 10.0f;
            if (Math.abs(left - margin) < snapThreshold) {
                snappedLeft = margin;
                xSnapped = true;
            } else if (Math.abs(right - ((float)this.width - margin)) < snapThreshold) {
                snappedLeft = (float)this.width - margin - width;
                xSnapped = true;
            }
        }
        if (!xSnapped) {
            snappedLeft = (float)Math.round(left / gridSpacing) * gridSpacing;
        }
        float snappedTop = top;
        boolean ySnapped = false;
        activeHLine = false;
        activeHLineY = 0.0f;
        float screenCenterY = (float)this.height / 2.0f;
        if (Math.abs(centerY - screenCenterY) < snapThreshold) {
            snappedTop = screenCenterY - height / 2.0f;
            ySnapped = true;
            activeHLine = true;
            activeHLineY = screenCenterY;
        }
        if (!ySnapped) {
            for (ElementRect other : others) {
                if (Math.abs(centerY - other.centerY) < snapThreshold) {
                    snappedTop = other.centerY - height / 2.0f;
                    ySnapped = true;
                    activeHLine = true;
                    activeHLineY = other.centerY;
                    break;
                }
                if (Math.abs(top - other.top) < snapThreshold) {
                    snappedTop = other.top;
                    ySnapped = true;
                    activeHLine = true;
                    activeHLineY = other.top;
                    break;
                }
                if (Math.abs(bottom - other.bottom) < snapThreshold) {
                    snappedTop = other.bottom - height;
                    ySnapped = true;
                    activeHLine = true;
                    activeHLineY = other.bottom;
                    break;
                }
                if (Math.abs(top - other.bottom) < snapThreshold) {
                    snappedTop = other.bottom;
                    ySnapped = true;
                    activeHLine = true;
                    activeHLineY = other.bottom;
                    break;
                }
                if (!(Math.abs(bottom - other.top) < snapThreshold)) continue;
                snappedTop = other.top - height;
                ySnapped = true;
                activeHLine = true;
                activeHLineY = other.top;
                break;
            }
        }
        if (!ySnapped) {
            float margin = 10.0f;
            if (Math.abs(top - margin) < snapThreshold) {
                snappedTop = margin;
                ySnapped = true;
            } else if (Math.abs(bottom - ((float)this.height - margin)) < snapThreshold) {
                snappedTop = (float)this.height - margin - height;
                ySnapped = true;
            }
        }
        if (!ySnapped) {
            snappedTop = (float)Math.round(top / gridSpacing) * gridSpacing;
        }
        float maxLeft = Math.max(0.0f, (float)this.width - width);
        float maxTop = Math.max(0.0f, (float)this.height - height);
        snappedLeft = Math.max(0.0f, Math.min(maxLeft, snappedLeft));
        snappedTop = Math.max(0.0f, Math.min(maxTop, snappedTop));
        de.apply(snappedLeft, snappedTop);
    }

    @Environment(value=EnvType.CLIENT)
    private static class DraggedElement {
        Object manager;
        float x;
        float y;
        float width;
        float height;
        float scaleModifier;

        private DraggedElement() {
        }

        void apply(float newX, float newY) {
            HudEditorScreen.applyPosition(this.manager, newX, newY);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ElementRect {
        Object manager;
        float left;
        float top;
        float right;
        float bottom;
        float centerX;
        float centerY;

        private ElementRect() {
        }
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) { return mouseClicked(click.x(), click.y(), xyz.angames.astolfoclient.client.util.LegacyInput.legacyButton(click.button())); }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent click, double dx, double dy) { return mouseDragged(click.x(), click.y(), xyz.angames.astolfoclient.client.util.LegacyInput.legacyButton(click.button()), dx, dy); }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent click) { return mouseReleased(click.x(), click.y(), xyz.angames.astolfoclient.client.util.LegacyInput.legacyButton(click.button())); }

}
