package xyz.angames.astolfoclient.client.gui.clickgui;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.gui.clickgui.ClickGuiIcons;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(value=EnvType.CLIENT)
public class SubSettingsPopupState {
    public static SubSettingsPopupState ACTIVE = null;
    public final Setting triggerSetting;
    public final String title;
    public final List<Setting> settings;
    public float triggerX;
    public float triggerY;
    public float triggerW;
    public float triggerH;
    public float anim = 0.0f;
    public boolean isClosing = false;
    public float scrollY = 0.0f;
    public float targetScrollY = 0.0f;
    private int draggingSlider = -1;
    private final Map<BooleanSetting, Float> boolAnimMap = new HashMap<BooleanSetting, Float>();
    private final Map<NumberSetting, Float> sliderRatioAnimMap = new HashMap<NumberSetting, Float>();
    private final Map<NumberSetting, Float> sliderHoverAnimMap = new HashMap<NumberSetting, Float>();
    public static final float POPUP_WIDTH = 220.0f;
    public static final float MAX_POPUP_HEIGHT = 260.0f;
    public static final float HEADER_HEIGHT = 24.0f;
    public static final float SLIDER_H = 22.0f;
    public static final float BOOL_H = 18.0f;
    private static final Color C_LABEL_OFF = new Color(150, 150, 165, 255);
    private static final Color C_LABEL_ON = new Color(255, 255, 255, 255);
    private static final Color C_TRACK_BG = new Color(20, 20, 28, 255);
    private static final Color C_SWITCH_OFF = new Color(24, 24, 32, 255);

    public SubSettingsPopupState(Setting triggerSetting, String title, List<Setting> settings, float triggerX, float triggerY, float triggerW, float triggerH) {
        this.triggerSetting = triggerSetting;
        this.title = title;
        this.settings = new ArrayList<Setting>(settings);
        this.triggerX = triggerX;
        this.triggerY = triggerY;
        this.triggerW = triggerW;
        this.triggerH = triggerH;
    }

    public static void open(Setting triggerSetting, String title, List<Setting> settings, float tx, float ty, float tw, float th) {
        if (ACTIVE != null && SubSettingsPopupState.ACTIVE.triggerSetting == triggerSetting) {
            SubSettingsPopupState.ACTIVE.isClosing = true;
            return;
        }
        ACTIVE = new SubSettingsPopupState(triggerSetting, title, settings, tx, ty, tw, th);
    }

    public static void close() {
        if (ACTIVE != null) {
            SubSettingsPopupState.ACTIVE.isClosing = true;
        }
    }

    public static boolean isOpen() {
        return ACTIVE != null && SubSettingsPopupState.ACTIVE.anim > 0.005f;
    }

    public static boolean isSettingOpen(Setting s) {
        return ACTIVE != null && SubSettingsPopupState.ACTIVE.triggerSetting == s && !SubSettingsPopupState.ACTIVE.isClosing;
    }

    public float calculateTotalContentHeight() {
        float total = 0.0f;
        for (Setting s : this.settings) {
            if (s instanceof NumberSetting) {
                total += 22.0f;
                continue;
            }
            if (s instanceof BooleanSetting) {
                total += 18.0f;
                continue;
            }
            total += 20.0f;
        }
        return total;
    }

    public static float[] getPopupBounds(float winX, float winY, float winW, float winH) {
        if (ACTIVE == null) {
            return new float[]{0.0f, 0.0f, 220.0f, 100.0f, 100.0f, 100.0f};
        }
        float subW = 220.0f;
        float totalContentH = ACTIVE.calculateTotalContentHeight();
        float bodyH = Math.min(totalContentH + 8.0f, 236.0f);
        float subH = 24.0f + bodyH;
        float sX = SubSettingsPopupState.ACTIVE.triggerX + SubSettingsPopupState.ACTIVE.triggerW + 4.0f;
        if (sX + subW > winX + winW - 6.0f) {
            sX = SubSettingsPopupState.ACTIVE.triggerX - subW - 4.0f;
        }
        if (sX < winX + 10.0f) {
            sX = SubSettingsPopupState.ACTIVE.triggerX + SubSettingsPopupState.ACTIVE.triggerW - subW;
        }
        float sY = SubSettingsPopupState.ACTIVE.triggerY - 2.0f;
        sY = MathHelper.clamp((float)sY, (float)(winY + 38.0f), (float)(winY + winH - subH - 8.0f));
        return new float[]{sX, sY, subW, subH, bodyH, totalContentH};
    }

    public static void renderActive(DrawContext context, float winX, float winY, float winW, float winH, int mouseX, int mouseY, float deltaTime, float masterAlpha, Color themeColor) {
        if (ACTIVE == null) {
            return;
        }
        SubSettingsPopupState.ACTIVE.anim = GuiUtils.animate(SubSettingsPopupState.ACTIVE.anim, SubSettingsPopupState.ACTIVE.isClosing ? 0.0f : 1.0f, 18.0f, deltaTime);
        if (SubSettingsPopupState.ACTIVE.anim <= 0.005f && SubSettingsPopupState.ACTIVE.isClosing) {
            ACTIVE = null;
            return;
        }
        float ease = 1.0f - (float)Math.pow(1.0f - SubSettingsPopupState.ACTIVE.anim, 3.0);
        float scale = 0.9f + 0.1f * ease;
        float alpha = masterAlpha * ease;
        MsdfFont medFont = null;
        MsdfFont semiboldFont = null;
        try {
            medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            semiboldFont = (MsdfFont)ClickGuiIcons.SEMIBOLD_FONT.get();
        }
        catch (Exception exception) {
            // empty catch block
        }
        float[] bounds = SubSettingsPopupState.getPopupBounds(winX, winY, winW, winH);
        float sX = bounds[0];
        float sY = bounds[1];
        float subW = bounds[2];
        float subH = bounds[3];
        float bodyH = bounds[4];
        float totalContentH = bounds[5];
        float maxScroll = Math.max(0.0f, totalContentH + 8.0f - bodyH);
        SubSettingsPopupState.ACTIVE.targetScrollY = MathHelper.clamp((float)SubSettingsPopupState.ACTIVE.targetScrollY, (float)(-maxScroll), (float)0.0f);
        SubSettingsPopupState.ACTIVE.scrollY = GuiUtils.animate(SubSettingsPopupState.ACTIVE.scrollY, SubSettingsPopupState.ACTIVE.targetScrollY, 20.0f, deltaTime);
        context.getMatrices().pushMatrix();
        if (scale < 0.999f) {
            float scx = sX + subW / 2.0f;
            float scy = sY + subH / 2.0f;
            context.getMatrices().translate(scx, scy);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-scx, -scy);
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        for (int i = 6; i >= 0; --i) {
            float progress = (float)i / 6.0f;
            float spread = progress * 4.0f;
            int sAlpha = (int)(70.0f * (1.0f - progress) * (1.0f - progress) * alpha);
            if (sAlpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(subW + spread * 2.0f, subH + spread * 2.0f)).radius(new QuadRadiusState(8.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, sAlpha))).build()).render(matrix, sX - spread, sY - spread);
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(subW, subH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * alpha)))).build()).render(matrix, sX, sY);
        if (semiboldFont != null) {
            try {
                ((BuiltText)Builder.text().font(semiboldFont).text(SubSettingsPopupState.ACTIVE.title).size(8.5f).color(new Color(255, 255, 255, (int)(255.0f * alpha))).build()).render(matrix, sX + 10.0f, sY + 7.5f);
            }
            catch (Exception i) {}
        } else {
            GuiUtils.renderTextSafely(matrix, SubSettingsPopupState.ACTIVE.title, sX + 10.0f, sY + 7.5f, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5f);
        }
        boolean closeHov = GuiUtils.isMouseOver(mouseX, mouseY, sX + subW - 20.0f, sY + 5.0f, 15.0f, 15.0f);
        Color closeCol = closeHov ? themeColor : new Color(140, 140, 160, (int)(255.0f * alpha));
        GuiUtils.renderTextSafely(matrix, "\u2715", sX + subW - 16.0f, sY + 7.5f, GuiUtils.withAlpha(closeCol, alpha), 8.0f);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(subW - 16.0f, 1.0f)).radius(new QuadRadiusState(0.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(28, 28, 36), alpha * 0.8f))).build()).render(matrix, sX + 8.0f, sY + 24.0f);
        float contentTop = sY + 24.0f + 2.0f;
        float contentBottom = sY + subH - 3.0f;
        context.enableScissor((int)sX, (int)contentTop, (int)(sX + subW), (int)contentBottom);
        float itemY = contentTop + 2.0f + SubSettingsPopupState.ACTIVE.scrollY;
        for (int i = 0; i < SubSettingsPopupState.ACTIVE.settings.size(); ++i) {
            Setting s = SubSettingsPopupState.ACTIVE.settings.get(i);
            if (s instanceof NumberSetting) {
                NumberSetting ns = (NumberSetting)s;
                ACTIVE.renderSlider(matrix, itemY, ns, themeColor, sX, subW, alpha, mouseX, mouseY, deltaTime, i == SubSettingsPopupState.ACTIVE.draggingSlider);
                itemY += 22.0f;
                continue;
            }
            if (!(s instanceof BooleanSetting)) continue;
            BooleanSetting bs = (BooleanSetting)s;
            ACTIVE.renderBool(matrix, itemY, bs, themeColor, sX, subW, alpha, mouseX, mouseY, deltaTime);
            itemY += 18.0f;
        }
        context.disableScissor();
        if (maxScroll > 0.5f) {
            float scrollTrackH = bodyH - 8.0f;
            float scrollThumbH = Math.max(16.0f, bodyH / (totalContentH + 8.0f) * scrollTrackH);
            float scrollProgress = -SubSettingsPopupState.ACTIVE.scrollY / maxScroll;
            scrollProgress = MathHelper.clamp((float)scrollProgress, (float)0.0f, (float)1.0f);
            float scrollThumbY = contentTop + 4.0f + scrollProgress * (scrollTrackH - scrollThumbH);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(2.5f, scrollThumbH)).radius(new QuadRadiusState(1.25f)).color(new QuadColorState(new Color(70, 70, 90, (int)(220.0f * alpha)))).build()).render(matrix, sX + subW - 5.0f, scrollThumbY);
        }
        context.getMatrices().popMatrix();
    }

    private void renderSlider(Matrix4f mx, float rowY, NumberSetting ns, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime, boolean isDragging) {
        float sx = x + 10.0f;
        float sw = w - 20.0f;
        float val = (float)ns.get();
        float targetRatio = MathHelper.clamp((float)((val - (float)ns.getMin()) / ((float)ns.getMax() - (float)ns.getMin())), (float)0.0f, (float)1.0f);
        float animRatio = this.sliderRatioAnimMap.getOrDefault(ns, Float.valueOf(targetRatio)).floatValue();
        animRatio = GuiUtils.animate(animRatio, targetRatio, 20.0f, deltaTime);
        this.sliderRatioAnimMap.put(ns, Float.valueOf(animRatio));
        boolean isHov = isDragging || GuiUtils.isMouseOver(mouseX, mouseY, sx, rowY, sw, 22.0f);
        float hovAnim = this.sliderHoverAnimMap.getOrDefault(ns, Float.valueOf(0.0f)).floatValue();
        hovAnim = GuiUtils.animate(hovAnim, isHov ? 1.0f : 0.0f, 14.0f, deltaTime);
        this.sliderHoverAnimMap.put(ns, Float.valueOf(hovAnim));
        String valStr = String.format(ns.getIncrement() > 0.0 && ns.getIncrement() < 1.0 ? "%.2f" : "%.1f", Float.valueOf(val));
        MsdfFont medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
        float textY = rowY + 1.5f;
        Color labelCol = GuiUtils.interpolateColor(C_LABEL_OFF, Color.WHITE, hovAnim * 0.4f);
        if (medFont != null) {
            try {
                ((BuiltText)Builder.text().font(medFont).text(ns.getName()).size(8.0f).color(GuiUtils.withAlpha(labelCol, alpha)).build()).render(mx, sx, textY);
                float valW = medFont.getWidth(valStr, 8.0f);
                ((BuiltText)Builder.text().font(medFont).text(valStr).size(8.0f).color(GuiUtils.withAlpha(Color.WHITE, alpha)).build()).render(mx, sx + sw - valW, textY);
            }
            catch (Exception e) {
                GuiUtils.renderTextSafely(mx, ns.getName(), sx, textY, GuiUtils.withAlpha(labelCol, alpha), 8.0f);
                GuiUtils.renderTextSafely(mx, valStr, sx + sw - 20.0f, textY, GuiUtils.withAlpha(Color.WHITE, alpha), 8.0f);
            }
        }
        float trackY = rowY + 12.5f;
        float trackH = 3.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(sw, trackH)).radius(new QuadRadiusState(1.5f)).color(new QuadColorState(GuiUtils.withAlpha(C_TRACK_BG, alpha))).build()).render(mx, sx, trackY);
        if (animRatio > 0.001f) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(sw * animRatio, trackH)).radius(new QuadRadiusState(1.5f)).color(new QuadColorState(GuiUtils.withAlpha(themeColor, alpha))).build()).render(mx, sx, trackY);
        }
        float knobX = sx + sw * animRatio;
        float knobY = trackY + trackH / 2.0f;
        float knobSize = 6.5f + 1.5f * hovAnim;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(knobSize, knobSize)).radius(new QuadRadiusState(knobSize / 2.0f)).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha))).build()).render(mx, knobX - knobSize / 2.0f, knobY - knobSize / 2.0f);
    }

    private void renderBool(Matrix4f mx, float rowY, BooleanSetting bs, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime) {
        float sx = x + 10.0f;
        float sw = w - 20.0f;
        boolean on = bs.get();
        float anim = this.boolAnimMap.getOrDefault(bs, Float.valueOf(on ? 1.0f : 0.0f)).floatValue();
        anim = GuiUtils.animate(anim, on ? 1.0f : 0.0f, 18.0f, deltaTime);
        this.boolAnimMap.put(bs, Float.valueOf(anim));
        Color labelColor = GuiUtils.interpolateColor(C_LABEL_OFF, C_LABEL_ON, anim);
        GuiUtils.renderTextSafely(mx, bs.getName(), sx, rowY + 3.5f, GuiUtils.withAlpha(labelColor, alpha), 8.0f);
        float switchW = 18.0f;
        float switchH = 9.0f;
        float switchX = sx + sw - switchW;
        float switchY = rowY + 4.0f;
        Color trackColor = GuiUtils.interpolateColor(C_SWITCH_OFF, themeColor, anim);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(switchW, switchH)).radius(new QuadRadiusState(4.5f)).color(new QuadColorState(GuiUtils.withAlpha(trackColor, alpha))).build()).render(mx, switchX, switchY);
        float thumbSize = 7.0f;
        float thumbX = switchX + 1.0f + (switchW - thumbSize - 2.0f) * anim;
        float thumbY = switchY + 1.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(thumbSize, thumbSize)).radius(new QuadRadiusState(3.5f)).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha))).build()).render(mx, thumbX, thumbY);
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button, float winX, float winY, float winW, float winH) {
        if (ACTIVE == null || SubSettingsPopupState.ACTIVE.anim < 0.05f || SubSettingsPopupState.ACTIVE.isClosing) {
            return false;
        }
        float[] bounds = SubSettingsPopupState.getPopupBounds(winX, winY, winW, winH);
        float sX = bounds[0];
        float sY = bounds[1];
        float subW = bounds[2];
        float subH = bounds[3];
        if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX + subW - 20.0f, sY + 5.0f, 15.0f, 15.0f)) {
            SubSettingsPopupState.close();
            return true;
        }
        float contentTop = sY + 24.0f + 2.0f;
        float contentBottom = sY + subH - 3.0f;
        if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX, sY, subW, subH)) {
            if (mouseY >= (double)contentTop && mouseY <= (double)contentBottom) {
                float itemY = contentTop + 2.0f + SubSettingsPopupState.ACTIVE.scrollY;
                for (int i = 0; i < SubSettingsPopupState.ACTIVE.settings.size(); ++i) {
                    float itemH;
                    Setting s = SubSettingsPopupState.ACTIVE.settings.get(i);
                    itemH = s instanceof NumberSetting ? 22.0f : (s instanceof BooleanSetting ? 18.0f : 20.0f);
                    if (s instanceof NumberSetting) {
                        NumberSetting ns = (NumberSetting)s;
                        if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX + 10.0f, itemY, subW - 20.0f, 22.0f)) {
                            SubSettingsPopupState.ACTIVE.draggingSlider = i;
                            ACTIVE.applySlider(mouseX, sX, subW, ns);
                            return true;
                        }
                    } else if (s instanceof BooleanSetting) {
                        BooleanSetting bs = (BooleanSetting)s;
                        if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX + 10.0f, itemY, subW - 20.0f, 18.0f)) {
                            bs.toggle();
                            return true;
                        }
                    }
                    itemY += itemH;
                }
            }
            return true;
        }
        SubSettingsPopupState.close();
        return true;
    }

    public static boolean mouseDragged(double mouseX, double mouseY, int button, float winX, float winY, float winW, float winH) {
        Setting s;
        if (ACTIVE == null || SubSettingsPopupState.ACTIVE.anim < 0.05f || SubSettingsPopupState.ACTIVE.isClosing) {
            return false;
        }
        if (SubSettingsPopupState.ACTIVE.draggingSlider >= 0 && SubSettingsPopupState.ACTIVE.draggingSlider < SubSettingsPopupState.ACTIVE.settings.size() && (s = SubSettingsPopupState.ACTIVE.settings.get(SubSettingsPopupState.ACTIVE.draggingSlider)) instanceof NumberSetting) {
            NumberSetting ns = (NumberSetting)s;
            float[] bounds = SubSettingsPopupState.getPopupBounds(winX, winY, winW, winH);
            float sX = bounds[0];
            float subW = bounds[2];
            ACTIVE.applySlider(mouseX, sX, subW, ns);
            return true;
        }
        return false;
    }

    public static void mouseReleased(double mouseX, double mouseY, int button) {
        if (ACTIVE != null) {
            SubSettingsPopupState.ACTIVE.draggingSlider = -1;
        }
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double amount, float winX, float winY, float winW, float winH) {
        if (ACTIVE == null || SubSettingsPopupState.ACTIVE.anim < 0.05f || SubSettingsPopupState.ACTIVE.isClosing) {
            return false;
        }
        float[] bounds = SubSettingsPopupState.getPopupBounds(winX, winY, winW, winH);
        float sX = bounds[0];
        float sY = bounds[1];
        float subW = bounds[2];
        float subH = bounds[3];
        float bodyH = bounds[4];
        float totalContentH = bounds[5];
        if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX, sY, subW, subH)) {
            float maxScroll = Math.max(0.0f, totalContentH + 8.0f - bodyH);
            SubSettingsPopupState.ACTIVE.targetScrollY = MathHelper.clamp((float)(SubSettingsPopupState.ACTIVE.targetScrollY + (float)amount * 28.0f), (float)(-maxScroll), (float)0.0f);
            return true;
        }
        return false;
    }

    private void applySlider(double mx, float startX, float w, NumberSetting ns) {
        double newVal;
        double oldVal;
        float sw = w - 20.0f;
        float ratio = (float)MathHelper.clamp((double)((mx - (double)(startX + 10.0f)) / (double)sw), (double)0.0, (double)1.0);
        double range = ns.getMax() - ns.getMin();
        double rawVal = ns.getMin() + (double)ratio * range;
        double inc = ns.getIncrement();
        if (inc > 0.0) {
            rawVal = (double)Math.round(rawVal / inc) * inc;
        }
        if (Double.compare(oldVal = ns.get(), newVal = MathHelper.clamp((double)rawVal, (double)ns.getMin(), (double)ns.getMax())) != 0) {
            ns.set(newVal);
            ModSounds.playSliderMove();
        }
    }
}

