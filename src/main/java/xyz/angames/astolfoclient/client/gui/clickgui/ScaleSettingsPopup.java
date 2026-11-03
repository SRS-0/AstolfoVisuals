package xyz.angames.astolfoclient.client.gui.clickgui;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.GuiScaleSettings;
import xyz.angames.astolfoclient.client.gui.clickgui.ClickGuiIcons;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(value=EnvType.CLIENT)
public class ScaleSettingsPopup {
    public static final String[] DISPLAY_NAMES = new String[]{"35%", "50%", "75%", "100% (Def)", "125%", "150%", "200%"};

    public static void render(GuiGraphicsExtractor context, float x, float y, float w, float h, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        MsdfFont medFont = null;
        try {
            medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
        }
        catch (Exception exception) {
            // empty catch block
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        GuiUtils.renderTextSafely(matrix, "GUI Scale", x + 10.0f, y + 8.0f, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5f);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(w - 20.0f, 1.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(30, 30, 40), alpha))).build()).render(matrix, x + 10.0f, y + 21.0f);
        float startY = y + 26.0f;
        float itemH = 16.5f;
        float itemW = w - 12.0f;
        float itemX = x + 6.0f;
        float currentScale = GuiScaleSettings.getScale();
        for (int i = 0; i < GuiScaleSettings.SCALES.length; ++i) {
            float s = GuiScaleSettings.SCALES[i];
            boolean isSelected = Math.abs(currentScale - s) < 0.01f;
            float rY = startY + (float)i * (itemH + 2.0f);
            boolean isHov = GuiUtils.isMouseOver(mouseX, mouseY, itemX, rY, itemW, itemH);
            if (isSelected) {
                Color selBg = new Color((int)((float)themeColor.getRed() * 0.25f), (int)((float)themeColor.getGreen() * 0.25f), (int)((float)themeColor.getBlue() * 0.25f), (int)(220.0f * alpha));
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(itemW, itemH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(selBg)).build()).render(matrix, itemX, rY);
            } else if (isHov) {
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(itemW, itemH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(new Color(28, 28, 38, (int)(200.0f * alpha)))).build()).render(matrix, itemX, rY);
            }
            Color textCol = isSelected ? themeColor : (isHov ? Color.WHITE : new Color(200, 200, 215));
            String name = DISPLAY_NAMES[i];
            float tw = medFont != null ? medFont.getWidth(name, 7.5f) : 24.0f;
            float tx = itemX + (itemW - tw) / 2.0f;
            GuiUtils.renderTextSafely(matrix, name, tx, rY + 4.5f, GuiUtils.withAlpha(textCol, alpha), 7.5f);
        }
    }

    public static boolean mouseClicked(double mx, double my, int button, float x, float y, float w, float h) {
        float startY = y + 26.0f;
        float itemH = 16.5f;
        float itemW = w - 12.0f;
        float itemX = x + 6.0f;
        for (int i = 0; i < GuiScaleSettings.SCALES.length; ++i) {
            float rY = startY + (float)i * (itemH + 2.0f);
            if (!GuiUtils.isMouseOver((float)mx, (float)my, itemX, rY, itemW, itemH)) continue;
            GuiScaleSettings.setScale(GuiScaleSettings.SCALES[i]);
            ModSounds.playModuleSelect();
            return true;
        }
        return false;
    }
}

