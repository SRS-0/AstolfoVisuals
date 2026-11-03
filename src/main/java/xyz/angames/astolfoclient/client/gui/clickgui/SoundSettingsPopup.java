package xyz.angames.astolfoclient.client.gui.clickgui;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.SoundSettings;
import xyz.angames.astolfoclient.client.gui.clickgui.ClickGuiIcons;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(value=EnvType.CLIENT)
public class SoundSettingsPopup {
    private static final List<SoundEntry> ENTRIES = new ArrayList<SoundEntry>();
    private static int draggingIndex = -1;

    public static void render(DrawContext context, float x, float y, float w, float h, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        MsdfFont medFont = null;
        try {
            medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
        }
        catch (Exception exception) {
            // empty catch block
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        GuiUtils.renderTextSafely(matrix, "Client Sounds", x + 10.0f, y + 8.0f, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5f);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(w - 20.0f, 1.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(30, 30, 40), alpha))).build()).render(matrix, x + 10.0f, y + 21.0f);
        float startY = y + 26.0f;
        float rowH = 19.5f;
        float trackW = w - 20.0f;
        float trackH = 3.5f;
        for (int i = 0; i < ENTRIES.size(); ++i) {
            boolean isHov;
            SoundEntry entry = ENTRIES.get(i);
            float curVal = entry.getter.get().floatValue();
            entry.animVal = GuiUtils.animate(entry.animVal, curVal, 20.0f, deltaTime);
            float rY = startY + (float)i * rowH;
            float tX = x + 10.0f;
            float tY = rY + 11.0f;
            boolean isDraggingThis = draggingIndex == i;
            boolean bl = isHov = isDraggingThis || GuiUtils.isMouseOver(mouseX, mouseY, tX - 4.0f, rY, trackW + 8.0f, rowH);
            if (isHov) {
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(trackW + 8.0f, rowH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(new Color(25, 25, 36, (int)(160.0f * alpha)))).build()).render(matrix, tX - 4.0f, rY);
            }
            Color nameCol = isHov ? Color.WHITE : new Color(200, 200, 215);
            GuiUtils.renderTextSafely(matrix, entry.name, tX, rY + 1.0f, GuiUtils.withAlpha(nameCol, alpha), 7.0f);
            String valStr = Math.round(curVal) + "%";
            float valW = medFont != null ? medFont.getWidth(valStr, 7.0f) : 18.0f;
            GuiUtils.renderTextSafely(matrix, valStr, tX + trackW - valW, rY + 1.0f, GuiUtils.withAlpha(themeColor, alpha), 7.0f);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(trackW, trackH)).radius(new QuadRadiusState(trackH / 2.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(22, 22, 30), alpha))).build()).render(matrix, tX, tY);
            float fillW = entry.animVal / 100.0f * trackW;
            if (fillW > 1.0f) {
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(fillW, trackH)).radius(new QuadRadiusState(trackH / 2.0f)).color(new QuadColorState(GuiUtils.withAlpha(themeColor, alpha))).build()).render(matrix, tX, tY);
            }
            float knobSize = isHov ? 7.5f : 6.0f;
            float knobX = tX + fillW;
            float knobY = tY + trackH / 2.0f;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(knobSize, knobSize)).radius(new QuadRadiusState(knobSize / 2.0f)).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha))).build()).render(matrix, knobX - knobSize / 2.0f, knobY - knobSize / 2.0f);
        }
    }

    public static boolean mouseClicked(double mx, double my, int button, float x, float y, float w, float h) {
        float startY = y + 26.0f;
        float rowH = 19.5f;
        float trackW = w - 20.0f;
        for (int i = 0; i < ENTRIES.size(); ++i) {
            float tX = x + 10.0f;
            float rY = startY + (float)i * rowH;
            if (!GuiUtils.isMouseOver((float)mx, (float)my, tX - 4.0f, rY, trackW + 8.0f, rowH)) continue;
            draggingIndex = i;
            SoundSettingsPopup.updateDrag((float)mx, tX, trackW);
            return true;
        }
        return false;
    }

    public static boolean isDragging() {
        return draggingIndex >= 0 && draggingIndex < ENTRIES.size();
    }

    public static void mouseDragged(double mx, float x, float w) {
        if (!SoundSettingsPopup.isDragging()) {
            return;
        }
        float tX = x + 10.0f;
        float trackW = w - 20.0f;
        SoundSettingsPopup.updateDrag((float)mx, tX, trackW);
    }

    private static void updateDrag(float mx, float tX, float trackW) {
        if (draggingIndex < 0 || draggingIndex >= ENTRIES.size()) {
            return;
        }
        SoundEntry entry = ENTRIES.get(draggingIndex);
        float progress = MathHelper.clamp((float)((mx - tX) / trackW), (float)0.0f, (float)1.0f);
        float newVal = Math.round(progress * 100.0f);
        entry.setter.accept(Float.valueOf(newVal));
        if (draggingIndex == 4) {
            ModSounds.playSliderMove();
        }
    }

    public static void mouseReleased() {
        if (draggingIndex >= 0 && draggingIndex < ENTRIES.size()) {
            SoundEntry entry = ENTRIES.get(draggingIndex);
            if (entry.soundSample != null && draggingIndex != 4) {
                entry.soundSample.run();
            }
        }
        draggingIndex = -1;
    }

    static {
        ENTRIES.add(new SoundEntry("Master Volume", SoundSettings::getMasterVolume, SoundSettings::setMasterVolume, ModSounds::playGuiOpen));
        ENTRIES.add(new SoundEntry("GUI Open", SoundSettings::getGuiOpenVolume, SoundSettings::setGuiOpenVolume, ModSounds::playGuiOpen));
        ENTRIES.add(new SoundEntry("Change Category", SoundSettings::getCategoryVolume, SoundSettings::setCategoryVolume, ModSounds::playCategoryChange));
        ENTRIES.add(new SoundEntry("Hover SFX", SoundSettings::getModuleSelectVolume, SoundSettings::setModuleSelectVolume, ModSounds::playModuleSelect));
        ENTRIES.add(new SoundEntry("Slider SFX", SoundSettings::getSliderVolume, SoundSettings::setSliderVolume, ModSounds::playSliderMove));
        ENTRIES.add(new SoundEntry("Search SFX", SoundSettings::getSearchVolume, SoundSettings::setSearchVolume, ModSounds::playSearchClick));
        ENTRIES.add(new SoundEntry("Mode Open", SoundSettings::getModeOpenVolume, SoundSettings::setModeOpenVolume, ModSounds::playModeOpen));
        ENTRIES.add(new SoundEntry("Module Toggles", SoundSettings::getModuleToggleVolume, SoundSettings::setModuleToggleVolume, ModSounds::playEnable));
    }

    @Environment(value=EnvType.CLIENT)
    public static class SoundEntry {
        public final String name;
        public final Supplier<Float> getter;
        public final Consumer<Float> setter;
        public final Runnable soundSample;
        public float animVal = 100.0f;

        public SoundEntry(String name, Supplier<Float> getter, Consumer<Float> setter, Runnable soundSample) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.soundSample = soundSample;
            this.animVal = getter.get().floatValue();
        }
    }
}

