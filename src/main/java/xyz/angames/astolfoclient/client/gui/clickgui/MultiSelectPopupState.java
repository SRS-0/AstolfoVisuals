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
import xyz.angames.astolfoclient.client.module.setting.MultiSelectSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;

@Environment(value=EnvType.CLIENT)
public class MultiSelectPopupState {
    public static MultiSelectPopupState ACTIVE = null;
    public static final int MAX_VISIBLE_ITEMS = 10;
    public static final float ITEM_ROW_H = 16.0f;
    public final MultiSelectSetting setting;
    public final String title;
    public final List<BooleanSetting> options;
    public float triggerX;
    public float triggerY;
    public float triggerW;
    public float triggerH;
    public float anim = 0.0f;
    public boolean isClosing = false;
    public float scrollY = 0.0f;
    public float targetScrollY = 0.0f;
    private final Map<String, Float> itemHoverAnims = new HashMap<String, Float>();

    public MultiSelectPopupState(MultiSelectSetting setting, String title, List<BooleanSetting> options, float triggerX, float triggerY, float triggerW, float triggerH) {
        this.setting = setting;
        this.title = title;
        this.options = new ArrayList<BooleanSetting>(options);
        this.triggerX = triggerX;
        this.triggerY = triggerY;
        this.triggerW = triggerW;
        this.triggerH = triggerH;
    }

    public static void open(MultiSelectSetting setting, String title, List<BooleanSetting> options, float tx, float ty, float tw, float th) {
        if (ACTIVE != null && MultiSelectPopupState.ACTIVE.setting == setting) {
            MultiSelectPopupState.ACTIVE.isClosing = true;
            return;
        }
        ACTIVE = new MultiSelectPopupState(setting, title, options, tx, ty, tw, th);
    }

    public static void close() {
        if (ACTIVE != null) {
            MultiSelectPopupState.ACTIVE.isClosing = true;
        }
    }

    public static boolean isOpen() {
        return ACTIVE != null && MultiSelectPopupState.ACTIVE.anim > 0.005f;
    }

    public static boolean isSettingOpen(Setting s) {
        return ACTIVE != null && MultiSelectPopupState.ACTIVE.setting == s && !MultiSelectPopupState.ACTIVE.isClosing;
    }

    public static float calculateSubWidth() {
        if (ACTIVE == null) {
            return 96.0f;
        }
        MsdfFont medFont = null;
        try {
            medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
        }
        catch (Exception exception) {
            // empty catch block
        }
        float maxTextW = 60.0f;
        if (medFont != null) {
            for (BooleanSetting opt : MultiSelectPopupState.ACTIVE.options) {
                float w = medFont.getWidth(opt.getName(), 7.5f);
                if (!(w > maxTextW)) continue;
                maxTextW = w;
            }
        }
        boolean hasScroll = MultiSelectPopupState.ACTIVE.options.size() > 10;
        return Math.max(maxTextW + (hasScroll ? 42.0f : 36.0f), 96.0f);
    }

    public static float calculatePopupX(float winX, float winW, float subW) {
        if (ACTIVE == null) {
            return winX;
        }
        float sX = MultiSelectPopupState.ACTIVE.triggerX + MultiSelectPopupState.ACTIVE.triggerW + 4.0f;
        if (sX + subW > winX + winW - 6.0f) {
            sX = MultiSelectPopupState.ACTIVE.triggerX - subW - 4.0f;
        }
        if (sX < winX + 10.0f) {
            sX = MultiSelectPopupState.ACTIVE.triggerX + MultiSelectPopupState.ACTIVE.triggerW - subW;
        }
        return sX;
    }

    public static float calculatePopupY(float winY, float winH, float subH) {
        if (ACTIVE == null) {
            return winY;
        }
        float sY = MultiSelectPopupState.ACTIVE.triggerY - 2.0f;
        return MathHelper.clamp((float)sY, (float)(winY + 38.0f), (float)(winY + winH - subH - 8.0f));
    }

    public static void renderActive(DrawContext context, float winX, float winY, float winW, float winH, int mouseX, int mouseY, float deltaTime, float masterAlpha, Color themeColor) {
        if (ACTIVE == null) {
            return;
        }
        MultiSelectPopupState.ACTIVE.anim = GuiUtils.animate(MultiSelectPopupState.ACTIVE.anim, MultiSelectPopupState.ACTIVE.isClosing ? 0.0f : 1.0f, 18.0f, deltaTime);
        if (MultiSelectPopupState.ACTIVE.anim <= 0.005f && MultiSelectPopupState.ACTIVE.isClosing) {
            ACTIVE = null;
            return;
        }
        float ease = 1.0f - (float)Math.pow(1.0f - MultiSelectPopupState.ACTIVE.anim, 3.0);
        float scale = 0.9f + 0.1f * ease;
        float alpha = masterAlpha * ease;
        MsdfFont medFont = null;
        MsdfFont semiboldFont = null;
        MsdfFont spFont = null;
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
        try {
            spFont = (MsdfFont)ClickGuiIcons.SP_FONT.get();
        }
        catch (Exception exception) {
            // empty catch block
        }
        int count = MultiSelectPopupState.ACTIVE.options.size();
        int visibleCount = Math.min(count, 10);
        float totalContentH = (float)count * 16.0f;
        float maxScroll = Math.max(0.0f, totalContentH - (float)visibleCount * 16.0f);
        MultiSelectPopupState.ACTIVE.scrollY = GuiUtils.animate(MultiSelectPopupState.ACTIVE.scrollY, MultiSelectPopupState.ACTIVE.targetScrollY, 20.0f, deltaTime);
        float subW = MultiSelectPopupState.calculateSubWidth();
        float subH = (float)visibleCount * 16.0f + 8.0f;
        float sX = MultiSelectPopupState.calculatePopupX(winX, winW, subW);
        float sY = MultiSelectPopupState.calculatePopupY(winY, winH, subH);
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
            int sAlpha = (int)(65.0f * (1.0f - progress) * (1.0f - progress) * alpha);
            if (sAlpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(subW + spread * 2.0f, subH + spread * 2.0f)).radius(new QuadRadiusState(8.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, sAlpha))).build()).render(matrix, sX - spread, sY - spread);
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(subW, subH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * alpha)))).build()).render(matrix, sX, sY);
        float contentTop = sY + 4.0f;
        float contentBottom = sY + subH - 4.0f;
        float contentH = contentBottom - contentTop;
        context.enableScissor((int)sX, (int)contentTop, (int)(sX + subW), (int)contentBottom);
        float itemAreaW = subW - (maxScroll > 0.5f ? 9.0f : 6.0f);
        for (int i = 0; i < MultiSelectPopupState.ACTIVE.options.size(); ++i) {
            BooleanSetting opt = MultiSelectPopupState.ACTIVE.options.get(i);
            float rY = contentTop + (float)i * 16.0f + MultiSelectPopupState.ACTIVE.scrollY;
            if (rY + 16.0f < contentTop - 2.0f || rY > contentBottom + 2.0f) continue;
            boolean isSel = opt.get();
            boolean rHov = (float)mouseX >= sX + 3.0f && (float)mouseX <= sX + 3.0f + itemAreaW && (float)mouseY >= rY && (float)mouseY <= rY + 16.0f - 1.0f && (float)mouseY >= contentTop && (float)mouseY <= contentBottom;
            float hovA = MultiSelectPopupState.ACTIVE.itemHoverAnims.getOrDefault(opt.getName(), Float.valueOf(0.0f)).floatValue();
            hovA = GuiUtils.animate(hovA, rHov ? 1.0f : 0.0f, 18.0f, deltaTime);
            MultiSelectPopupState.ACTIVE.itemHoverAnims.put(opt.getName(), Float.valueOf(hovA));
            if (hovA > 0.01f) {
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(itemAreaW, 15.0f)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(new Color(25, 25, 36, (int)(220.0f * alpha * hovA)))).build()).render(matrix, sX + 3.0f, rY);
            }
            if (isSel) {
                if (spFont != null) {
                    try {
                        ((BuiltText)Builder.text().font(spFont).text("D").size(6.5f).color(new Color(255, 255, 255, (int)(255.0f * alpha))).build()).render(matrix, sX + 6.0f, rY + 4.0f);
                    }
                    catch (Exception e) {
                        GuiUtils.renderTextSafely(matrix, "\u2713", sX + 6.0f, rY + 4.0f, GuiUtils.withAlpha(Color.WHITE, alpha), 7.5f);
                    }
                } else {
                    GuiUtils.renderTextSafely(matrix, "\u2713", sX + 6.0f, rY + 4.0f, GuiUtils.withAlpha(Color.WHITE, alpha), 7.5f);
                }
            }
            Color textCol = isSel ? new Color(255, 255, 255, (int)(255.0f * alpha)) : (hovA > 0.01f ? GuiUtils.withAlpha(new Color(220, 220, 240), alpha) : GuiUtils.withAlpha(new Color(135, 135, 150), alpha));
            float textStartX = sX + 18.0f;
            if (isSel && semiboldFont != null) {
                try {
                    ((BuiltText)Builder.text().font(semiboldFont).text(opt.getName()).size(7.5f).color(textCol).build()).render(matrix, textStartX, rY + 4.5f);
                }
                catch (Exception e) {
                    GuiUtils.renderTextSafely(matrix, opt.getName(), textStartX, rY + 4.5f, textCol, 7.5f);
                }
                continue;
            }
            if (medFont != null) {
                try {
                    ((BuiltText)Builder.text().font(medFont).text(opt.getName()).size(7.5f).color(textCol).build()).render(matrix, textStartX, rY + 4.5f);
                }
                catch (Exception e) {
                    GuiUtils.renderTextSafely(matrix, opt.getName(), textStartX, rY + 4.5f, textCol, 7.5f);
                }
                continue;
            }
            GuiUtils.renderTextSafely(matrix, opt.getName(), textStartX, rY + 4.5f, textCol, 7.5f);
        }
        context.disableScissor();
        if (maxScroll > 0.5f) {
            float scrollTrackH = subH - 12.0f;
            float scrollThumbH = Math.max(14.0f, contentH / totalContentH * scrollTrackH);
            float scrollProgress = MathHelper.clamp((float)(-MultiSelectPopupState.ACTIVE.scrollY / maxScroll), (float)0.0f, (float)1.0f);
            float scrollThumbY = sY + 6.0f + scrollProgress * (scrollTrackH - scrollThumbH);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(2.5f, scrollThumbH)).radius(new QuadRadiusState(1.25f)).color(new QuadColorState(new Color(70, 70, 90, (int)(220.0f * alpha)))).build()).render(matrix, sX + subW - 4.5f, scrollThumbY);
        }
        context.getMatrices().popMatrix();
    }

    public static boolean mouseClicked(double mouseX, double mouseY, int button, float winX, float winY, float winW, float winH) {
        if (ACTIVE == null || MultiSelectPopupState.ACTIVE.anim < 0.05f || MultiSelectPopupState.ACTIVE.isClosing) {
            return false;
        }
        int count = MultiSelectPopupState.ACTIVE.options.size();
        int visibleCount = Math.min(count, 10);
        float subW = MultiSelectPopupState.calculateSubWidth();
        float subH = (float)visibleCount * 16.0f + 8.0f;
        float sX = MultiSelectPopupState.calculatePopupX(winX, winW, subW);
        float sY = MultiSelectPopupState.calculatePopupY(winY, winH, subH);
        float totalContentH = (float)count * 16.0f;
        float maxScroll = Math.max(0.0f, totalContentH - (float)visibleCount * 16.0f);
        if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX, sY, subW, subH)) {
            float contentTop = sY + 4.0f;
            float contentBottom = sY + subH - 4.0f;
            if (mouseY >= (double)contentTop && mouseY <= (double)contentBottom) {
                float itemAreaW = subW - (maxScroll > 0.5f ? 9.0f : 6.0f);
                for (int i = 0; i < MultiSelectPopupState.ACTIVE.options.size(); ++i) {
                    float rY = contentTop + (float)i * 16.0f + MultiSelectPopupState.ACTIVE.scrollY;
                    if (!GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX + 3.0f, rY, itemAreaW, 15.0f)) continue;
                    BooleanSetting opt = MultiSelectPopupState.ACTIVE.options.get(i);
                    opt.toggle();
                    return true;
                }
            }
            return true;
        }
        MultiSelectPopupState.close();
        return true;
    }

    public static boolean mouseScrolled(double mouseX, double mouseY, double amount, float winX, float winY, float winW, float winH) {
        if (ACTIVE == null || MultiSelectPopupState.ACTIVE.anim < 0.05f || MultiSelectPopupState.ACTIVE.isClosing) {
            return false;
        }
        int count = MultiSelectPopupState.ACTIVE.options.size();
        int visibleCount = Math.min(count, 10);
        float subW = MultiSelectPopupState.calculateSubWidth();
        float subH = (float)visibleCount * 16.0f + 8.0f;
        float sX = MultiSelectPopupState.calculatePopupX(winX, winW, subW);
        float sY = MultiSelectPopupState.calculatePopupY(winY, winH, subH);
        float totalContentH = (float)count * 16.0f;
        float maxScroll = Math.max(0.0f, totalContentH - (float)visibleCount * 16.0f);
        if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX, sY, subW, subH)) {
            if (maxScroll > 0.5f) {
                MultiSelectPopupState.ACTIVE.targetScrollY = MathHelper.clamp((float)(MultiSelectPopupState.ACTIVE.targetScrollY + (float)amount * 18.0f), (float)(-maxScroll), (float)0.0f);
            }
            return true;
        }
        return false;
    }
}

