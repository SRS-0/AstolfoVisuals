package xyz.angames.astolfoclient.client.gui.clickgui;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.GuiScaleSettings;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(EnvType.CLIENT)
public class ScaleSettingsPopup {
   public static final String[] DISPLAY_NAMES = new String[]{"35%", "50%", "75%", "100% (Def)", "125%", "150%", "200%"};

   public static void render(GuiGraphicsExtractor context, float x, float y, float w, float h, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
      MsdfFont medFont = null;

      try {
         medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
      } catch (Exception var26) {
      }

      Matrix4f matrix = GuiCompat.matrix4f(context);
      GuiUtils.renderTextSafely(matrix, "GUI Scale", x + 10.0F, y + 8.0F, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5F);
      Builder.rectangle()
         .size(new SizeState(w - 20.0F, 1.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(new Color(30, 30, 40), alpha)))
         .build()
         .render(matrix, x + 10.0F, y + 21.0F);
      float startY = y + 26.0F;
      float itemH = 16.5F;
      float itemW = w - 12.0F;
      float itemX = x + 6.0F;
      float currentScale = GuiScaleSettings.getScale();

      for (int i = 0; i < GuiScaleSettings.SCALES.length; i++) {
         float s = GuiScaleSettings.SCALES[i];
         boolean isSelected = Math.abs(currentScale - s) < 0.01F;
         float rY = startY + i * (itemH + 2.0F);
         boolean isHov = GuiUtils.isMouseOver(mouseX, mouseY, itemX, rY, itemW, itemH);
         if (isSelected) {
            Color selBg = new Color(
               (int)(themeColor.getRed() * 0.25F), (int)(themeColor.getGreen() * 0.25F), (int)(themeColor.getBlue() * 0.25F), (int)(220.0F * alpha)
            );
            Builder.rectangle()
               .size(new SizeState(itemW, itemH))
               .radius(new QuadRadiusState(4.0F))
               .color(new QuadColorState(selBg))
               .build()
               .render(matrix, itemX, rY);
         } else if (isHov) {
            Builder.rectangle()
               .size(new SizeState(itemW, itemH))
               .radius(new QuadRadiusState(4.0F))
               .color(new QuadColorState(new Color(28, 28, 38, (int)(200.0F * alpha))))
               .build()
               .render(matrix, itemX, rY);
         }

         Color textCol = isSelected ? themeColor : (isHov ? Color.WHITE : new Color(200, 200, 215));
         String name = DISPLAY_NAMES[i];
         float tw = medFont != null ? medFont.getWidth(name, 7.5F) : 24.0F;
         float tx = itemX + (itemW - tw) / 2.0F;
         GuiUtils.renderTextSafely(matrix, name, tx, rY + 4.5F, GuiUtils.withAlpha(textCol, alpha), 7.5F);
      }
   }

   public static boolean mouseClicked(double mx, double my, int button, float x, float y, float w, float h) {
      float startY = y + 26.0F;
      float itemH = 16.5F;
      float itemW = w - 12.0F;
      float itemX = x + 6.0F;

      for (int i = 0; i < GuiScaleSettings.SCALES.length; i++) {
         float rY = startY + i * (itemH + 2.0F);
         if (GuiUtils.isMouseOver((float)mx, (float)my, itemX, rY, itemW, itemH)) {
            GuiScaleSettings.setScale(GuiScaleSettings.SCALES[i]);
            ModSounds.playModuleSelect();
            return true;
         }
      }

      return false;
   }
}
