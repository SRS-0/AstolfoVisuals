package xyz.angames.astolfoclient.client.gui.clickgui;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.SoundSettings;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(EnvType.CLIENT)
public class SoundSettingsPopup {
   private static final List<SoundSettingsPopup.SoundEntry> ENTRIES = new ArrayList<>();
   private static int draggingIndex = -1;

   public static void render(GuiGraphicsExtractor context, float x, float y, float w, float h, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
      MsdfFont medFont = null;

      try {
         medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
      } catch (Exception var31) {
      }

      Matrix4f matrix = GuiCompat.matrix4f(context);
      GuiUtils.renderTextSafely(matrix, "Client Sounds", x + 10.0F, y + 8.0F, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5F);
      Builder.rectangle()
         .size(new SizeState(w - 20.0F, 1.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(new Color(30, 30, 40), alpha)))
         .build()
         .render(matrix, x + 10.0F, y + 21.0F);
      float startY = y + 26.0F;
      float rowH = 19.5F;
      float trackW = w - 20.0F;
      float trackH = 3.5F;

      for (int i = 0; i < ENTRIES.size(); i++) {
         SoundSettingsPopup.SoundEntry entry = ENTRIES.get(i);
         float curVal = entry.getter.get();
         entry.animVal = GuiUtils.animate(entry.animVal, curVal, 20.0F, deltaTime);
         float rY = startY + i * rowH;
         float tX = x + 10.0F;
         float tY = rY + 11.0F;
         boolean isDraggingThis = draggingIndex == i;
         boolean isHov = isDraggingThis || GuiUtils.isMouseOver(mouseX, mouseY, tX - 4.0F, rY, trackW + 8.0F, rowH);
         if (isHov) {
            Builder.rectangle()
               .size(new SizeState(trackW + 8.0F, rowH))
               .radius(new QuadRadiusState(4.0F))
               .color(new QuadColorState(new Color(25, 25, 36, (int)(160.0F * alpha))))
               .build()
               .render(matrix, tX - 4.0F, rY);
         }

         Color nameCol = isHov ? Color.WHITE : new Color(200, 200, 215);
         GuiUtils.renderTextSafely(matrix, entry.name, tX, rY + 1.0F, GuiUtils.withAlpha(nameCol, alpha), 7.0F);
         String valStr = Math.round(curVal) + "%";
         float valW = medFont != null ? medFont.getWidth(valStr, 7.0F) : 18.0F;
         GuiUtils.renderTextSafely(matrix, valStr, tX + trackW - valW, rY + 1.0F, GuiUtils.withAlpha(themeColor, alpha), 7.0F);
         Builder.rectangle()
            .size(new SizeState(trackW, trackH))
            .radius(new QuadRadiusState(trackH / 2.0F))
            .color(new QuadColorState(GuiUtils.withAlpha(new Color(22, 22, 30), alpha)))
            .build()
            .render(matrix, tX, tY);
         float fillW = entry.animVal / 100.0F * trackW;
         if (fillW > 1.0F) {
            Builder.rectangle()
               .size(new SizeState(fillW, trackH))
               .radius(new QuadRadiusState(trackH / 2.0F))
               .color(new QuadColorState(GuiUtils.withAlpha(themeColor, alpha)))
               .build()
               .render(matrix, tX, tY);
         }

         float knobSize = isHov ? 7.5F : 6.0F;
         float knobX = tX + fillW;
         float knobY = tY + trackH / 2.0F;
         Builder.rectangle()
            .size(new SizeState(knobSize, knobSize))
            .radius(new QuadRadiusState(knobSize / 2.0F))
            .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
            .build()
            .render(matrix, knobX - knobSize / 2.0F, knobY - knobSize / 2.0F);
      }
   }

   public static boolean mouseClicked(double mx, double my, int button, float x, float y, float w, float h) {
      float startY = y + 26.0F;
      float rowH = 19.5F;
      float trackW = w - 20.0F;

      for (int i = 0; i < ENTRIES.size(); i++) {
         float rY = startY + i * rowH;
         float tX = x + 10.0F;
         if (GuiUtils.isMouseOver((float)mx, (float)my, tX - 4.0F, rY, trackW + 8.0F, rowH)) {
            draggingIndex = i;
            updateDrag((float)mx, tX, trackW);
            return true;
         }
      }

      return false;
   }

   public static boolean isDragging() {
      return draggingIndex >= 0 && draggingIndex < ENTRIES.size();
   }

   public static void mouseDragged(double mx, float x, float w) {
      if (isDragging()) {
         float tX = x + 10.0F;
         float trackW = w - 20.0F;
         updateDrag((float)mx, tX, trackW);
      }
   }

   private static void updateDrag(float mx, float tX, float trackW) {
      if (draggingIndex >= 0 && draggingIndex < ENTRIES.size()) {
         SoundSettingsPopup.SoundEntry entry = ENTRIES.get(draggingIndex);
         float progress = Mth.clamp((mx - tX) / trackW, 0.0F, 1.0F);
         float newVal = Math.round(progress * 100.0F);
         entry.setter.accept(newVal);
         if (draggingIndex == 4) {
            ModSounds.playSliderMove();
         }
      }
   }

   public static void mouseReleased() {
      if (draggingIndex >= 0 && draggingIndex < ENTRIES.size()) {
         SoundSettingsPopup.SoundEntry entry = ENTRIES.get(draggingIndex);
         if (entry.soundSample != null && draggingIndex != 4) {
            entry.soundSample.run();
         }
      }

      draggingIndex = -1;
   }

   static {
      ENTRIES.add(new SoundSettingsPopup.SoundEntry("Master Volume", SoundSettings::getMasterVolume, SoundSettings::setMasterVolume, ModSounds::playGuiOpen));
      ENTRIES.add(new SoundSettingsPopup.SoundEntry("GUI Open", SoundSettings::getGuiOpenVolume, SoundSettings::setGuiOpenVolume, ModSounds::playGuiOpen));
      ENTRIES.add(
         new SoundSettingsPopup.SoundEntry("Change Category", SoundSettings::getCategoryVolume, SoundSettings::setCategoryVolume, ModSounds::playCategoryChange)
      );
      ENTRIES.add(
         new SoundSettingsPopup.SoundEntry("Hover SFX", SoundSettings::getModuleSelectVolume, SoundSettings::setModuleSelectVolume, ModSounds::playModuleSelect)
      );
      ENTRIES.add(new SoundSettingsPopup.SoundEntry("Slider SFX", SoundSettings::getSliderVolume, SoundSettings::setSliderVolume, ModSounds::playSliderMove));
      ENTRIES.add(new SoundSettingsPopup.SoundEntry("Search SFX", SoundSettings::getSearchVolume, SoundSettings::setSearchVolume, ModSounds::playSearchClick));
      ENTRIES.add(new SoundSettingsPopup.SoundEntry("Mode Open", SoundSettings::getModeOpenVolume, SoundSettings::setModeOpenVolume, ModSounds::playModeOpen));
      ENTRIES.add(
         new SoundSettingsPopup.SoundEntry("Module Toggles", SoundSettings::getModuleToggleVolume, SoundSettings::setModuleToggleVolume, ModSounds::playEnable)
      );
   }

   @Environment(EnvType.CLIENT)
   public static class SoundEntry {
      public final String name;
      public final Supplier<Float> getter;
      public final Consumer<Float> setter;
      public final Runnable soundSample;
      public float animVal = 100.0F;

      public SoundEntry(String name, Supplier<Float> getter, Consumer<Float> setter, Runnable soundSample) {
         this.name = name;
         this.getter = getter;
         this.setter = setter;
         this.soundSample = soundSample;
         this.animVal = getter.get();
      }
   }
}
