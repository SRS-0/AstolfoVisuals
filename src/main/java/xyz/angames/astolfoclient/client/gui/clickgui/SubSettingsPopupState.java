package xyz.angames.astolfoclient.client.gui.clickgui;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(EnvType.CLIENT)
public class SubSettingsPopupState {
   public static SubSettingsPopupState ACTIVE = null;
   public final Setting triggerSetting;
   public final String title;
   public final List<Setting> settings;
   public float triggerX;
   public float triggerY;
   public float triggerW;
   public float triggerH;
   public float anim = 0.0F;
   public boolean isClosing = false;
   public float scrollY = 0.0F;
   public float targetScrollY = 0.0F;
   private int draggingSlider = -1;
   private final Map<BooleanSetting, Float> boolAnimMap = new HashMap<>();
   private final Map<NumberSetting, Float> sliderRatioAnimMap = new HashMap<>();
   private final Map<NumberSetting, Float> sliderHoverAnimMap = new HashMap<>();
   public static final float POPUP_WIDTH = 220.0F;
   public static final float MAX_POPUP_HEIGHT = 260.0F;
   public static final float HEADER_HEIGHT = 24.0F;
   public static final float SLIDER_H = 22.0F;
   public static final float BOOL_H = 18.0F;
   private static final Color C_LABEL_OFF = new Color(150, 150, 165, 255);
   private static final Color C_LABEL_ON = new Color(255, 255, 255, 255);
   private static final Color C_TRACK_BG = new Color(20, 20, 28, 255);
   private static final Color C_SWITCH_OFF = new Color(24, 24, 32, 255);

   public SubSettingsPopupState(Setting triggerSetting, String title, List<Setting> settings, float triggerX, float triggerY, float triggerW, float triggerH) {
      this.triggerSetting = triggerSetting;
      this.title = title;
      this.settings = new ArrayList<>(settings);
      this.triggerX = triggerX;
      this.triggerY = triggerY;
      this.triggerW = triggerW;
      this.triggerH = triggerH;
   }

   public static void open(Setting triggerSetting, String title, List<Setting> settings, float tx, float ty, float tw, float th) {
      if (ACTIVE != null && ACTIVE.triggerSetting == triggerSetting) {
         ACTIVE.isClosing = true;
      } else {
         ACTIVE = new SubSettingsPopupState(triggerSetting, title, settings, tx, ty, tw, th);
      }
   }

   public static void close() {
      if (ACTIVE != null) {
         ACTIVE.isClosing = true;
      }
   }

   public static boolean isOpen() {
      return ACTIVE != null && ACTIVE.anim > 0.005F;
   }

   public static boolean isSettingOpen(Setting s) {
      return ACTIVE != null && ACTIVE.triggerSetting == s && !ACTIVE.isClosing;
   }

   public float calculateTotalContentHeight() {
      float total = 0.0F;

      for (Setting s : this.settings) {
         if (s instanceof NumberSetting) {
            total += 22.0F;
         } else if (s instanceof BooleanSetting) {
            total += 18.0F;
         } else {
            total += 20.0F;
         }
      }

      return total;
   }

   public static float[] getPopupBounds(float winX, float winY, float winW, float winH) {
      if (ACTIVE == null) {
         return new float[]{0.0F, 0.0F, 220.0F, 100.0F, 100.0F, 100.0F};
      }

      float subW = 220.0F;
      float totalContentH = ACTIVE.calculateTotalContentHeight();
      float bodyH = Math.min(totalContentH + 8.0F, 236.0F);
      float subH = 24.0F + bodyH;
      float sX = ACTIVE.triggerX + ACTIVE.triggerW + 4.0F;
      if (sX + subW > winX + winW - 6.0F) {
         sX = ACTIVE.triggerX - subW - 4.0F;
      }

      if (sX < winX + 10.0F) {
         sX = ACTIVE.triggerX + ACTIVE.triggerW - subW;
      }

      float sY = ACTIVE.triggerY - 2.0F;
      sY = Mth.clamp(sY, winY + 38.0F, winY + winH - subH - 8.0F);
      return new float[]{sX, sY, subW, subH, bodyH, totalContentH};
   }

   public static void renderActive(
      GuiGraphicsExtractor context, float winX, float winY, float winW, float winH, int mouseX, int mouseY, float deltaTime, float masterAlpha, Color themeColor
   ) {
      if (ACTIVE != null) {
         ACTIVE.anim = GuiUtils.animate(ACTIVE.anim, ACTIVE.isClosing ? 0.0F : 1.0F, 18.0F, deltaTime);
         if (ACTIVE.anim <= 0.005F && ACTIVE.isClosing) {
            ACTIVE = null;
         } else {
            float ease = 1.0F - (float)Math.pow(1.0F - ACTIVE.anim, 3.0);
            float scale = 0.9F + 0.1F * ease;
            float alpha = masterAlpha * ease;
            MsdfFont medFont = null;
            MsdfFont semiboldFont = null;

            try {
               medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
            } catch (Exception var35) {
            }

            try {
               semiboldFont = (MsdfFont)ClickGuiIcons.SEMIBOLD_FONT.get();
            } catch (Exception var34) {
            }

            float[] bounds = getPopupBounds(winX, winY, winW, winH);
            float sX = bounds[0];
            float sY = bounds[1];
            float subW = bounds[2];
            float subH = bounds[3];
            float bodyH = bounds[4];
            float totalContentH = bounds[5];
            float maxScroll = Math.max(0.0F, totalContentH + 8.0F - bodyH);
            ACTIVE.targetScrollY = Mth.clamp(ACTIVE.targetScrollY, -maxScroll, 0.0F);
            ACTIVE.scrollY = GuiUtils.animate(ACTIVE.scrollY, ACTIVE.targetScrollY, 20.0F, deltaTime);
            context.pose().pushMatrix();
            if (scale < 0.999F) {
               float scx = sX + subW / 2.0F;
               float scy = sY + subH / 2.0F;
               context.pose().translate(scx, scy);
               context.pose().scale(scale, scale);
               context.pose().translate(-scx, -scy);
            }

            Matrix4f matrix = GuiCompat.matrix4f(context);

            for (int i = 6; i >= 0; i--) {
               float progress = i / 6.0F;
               float spread = progress * 4.0F;
               int sAlpha = (int)(70.0F * (1.0F - progress) * (1.0F - progress) * alpha);
               if (sAlpha > 0) {
                  Builder.rectangle()
                     .size(new SizeState(subW + spread * 2.0F, subH + spread * 2.0F))
                     .radius(new QuadRadiusState(8.0F + spread))
                     .color(new QuadColorState(new Color(0, 0, 0, sAlpha)))
                     .build()
                     .render(matrix, sX - spread, sY - spread);
               }
            }

            Builder.rectangle()
               .size(new SizeState(subW, subH))
               .radius(new QuadRadiusState(8.0F))
               .color(new QuadColorState(new Color(0, 0, 0, (int)(255.0F * alpha))))
               .build()
               .render(matrix, sX, sY);
            if (semiboldFont != null) {
               try {
                  Builder.text()
                     .font(semiboldFont)
                     .text(ACTIVE.title)
                     .size(8.5F)
                     .color(new Color(255, 255, 255, (int)(255.0F * alpha)))
                     .build()
                     .render(matrix, sX + 10.0F, sY + 7.5F);
               } catch (Exception var33) {
               }
            } else {
               GuiUtils.renderTextSafely(matrix, ACTIVE.title, sX + 10.0F, sY + 7.5F, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5F);
            }

            boolean closeHov = GuiUtils.isMouseOver(mouseX, mouseY, sX + subW - 20.0F, sY + 5.0F, 15.0F, 15.0F);
            Color closeCol = closeHov ? themeColor : new Color(140, 140, 160, (int)(255.0F * alpha));
            GuiUtils.renderTextSafely(matrix, "✕", sX + subW - 16.0F, sY + 7.5F, GuiUtils.withAlpha(closeCol, alpha), 8.0F);
            Builder.rectangle()
               .size(new SizeState(subW - 16.0F, 1.0F))
               .radius(new QuadRadiusState(0.0F))
               .color(new QuadColorState(GuiUtils.withAlpha(new Color(28, 28, 36), alpha * 0.8F)))
               .build()
               .render(matrix, sX + 8.0F, sY + 24.0F);
            float contentTop = sY + 24.0F + 2.0F;
            float contentBottom = sY + subH - 3.0F;
            context.enableScissor((int)sX, (int)contentTop, (int)(sX + subW), (int)contentBottom);
            float itemY = contentTop + 2.0F + ACTIVE.scrollY;

            for (int i = 0; i < ACTIVE.settings.size(); i++) {
               Setting s = ACTIVE.settings.get(i);
               if (s instanceof NumberSetting ns) {
                  ACTIVE.renderSlider(matrix, itemY, ns, themeColor, sX, subW, alpha, mouseX, mouseY, deltaTime, i == ACTIVE.draggingSlider);
                  itemY += 22.0F;
               } else if (s instanceof BooleanSetting bs) {
                  ACTIVE.renderBool(matrix, itemY, bs, themeColor, sX, subW, alpha, mouseX, mouseY, deltaTime);
                  itemY += 18.0F;
               }
            }

            context.disableScissor();
            if (maxScroll > 0.5F) {
               float scrollTrackH = bodyH - 8.0F;
               float scrollThumbH = Math.max(16.0F, bodyH / (totalContentH + 8.0F) * scrollTrackH);
               float scrollProgress = -ACTIVE.scrollY / maxScroll;
               scrollProgress = Mth.clamp(scrollProgress, 0.0F, 1.0F);
               float scrollThumbY = contentTop + 4.0F + scrollProgress * (scrollTrackH - scrollThumbH);
               Builder.rectangle()
                  .size(new SizeState(2.5F, scrollThumbH))
                  .radius(new QuadRadiusState(1.25F))
                  .color(new QuadColorState(new Color(70, 70, 90, (int)(220.0F * alpha))))
                  .build()
                  .render(matrix, sX + subW - 5.0F, scrollThumbY);
            }

            context.pose().popMatrix();
         }
      }
   }

   private void renderSlider(
      Matrix4f mx, float rowY, NumberSetting ns, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime, boolean isDragging
   ) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float val = (float)ns.get();
      float targetRatio = Mth.clamp((val - (float)ns.getMin()) / ((float)ns.getMax() - (float)ns.getMin()), 0.0F, 1.0F);
      float animRatio = this.sliderRatioAnimMap.getOrDefault(ns, targetRatio);
      animRatio = GuiUtils.animate(animRatio, targetRatio, 20.0F, deltaTime);
      this.sliderRatioAnimMap.put(ns, animRatio);
      boolean isHov = isDragging || GuiUtils.isMouseOver(mouseX, mouseY, sx, rowY, sw, 22.0F);
      float hovAnim = this.sliderHoverAnimMap.getOrDefault(ns, 0.0F);
      hovAnim = GuiUtils.animate(hovAnim, isHov ? 1.0F : 0.0F, 14.0F, deltaTime);
      this.sliderHoverAnimMap.put(ns, hovAnim);
      String valStr = String.format(ns.getIncrement() > 0.0 && ns.getIncrement() < 1.0 ? "%.2f" : "%.1f", val);
      MsdfFont medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
      float textY = rowY + 1.5F;
      Color labelCol = GuiUtils.interpolateColor(C_LABEL_OFF, Color.WHITE, hovAnim * 0.4F);
      if (medFont != null) {
         try {
            Builder.text().font(medFont).text(ns.getName()).size(8.0F).color(GuiUtils.withAlpha(labelCol, alpha)).build().render(mx, sx, textY);
            float valW = medFont.getWidth(valStr, 8.0F);
            Builder.text().font(medFont).text(valStr).size(8.0F).color(GuiUtils.withAlpha(Color.WHITE, alpha)).build().render(mx, sx + sw - valW, textY);
         } catch (Exception e) {
            GuiUtils.renderTextSafely(mx, ns.getName(), sx, textY, GuiUtils.withAlpha(labelCol, alpha), 8.0F);
            GuiUtils.renderTextSafely(mx, valStr, sx + sw - 20.0F, textY, GuiUtils.withAlpha(Color.WHITE, alpha), 8.0F);
         }
      }

      float trackY = rowY + 12.5F;
      float trackH = 3.0F;
      Builder.rectangle()
         .size(new SizeState(sw, trackH))
         .radius(new QuadRadiusState(1.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(C_TRACK_BG, alpha)))
         .build()
         .render(mx, sx, trackY);
      if (animRatio > 0.001F) {
         Builder.rectangle()
            .size(new SizeState(sw * animRatio, trackH))
            .radius(new QuadRadiusState(1.5F))
            .color(new QuadColorState(GuiUtils.withAlpha(themeColor, alpha)))
            .build()
            .render(mx, sx, trackY);
      }

      float knobX = sx + sw * animRatio;
      float knobY = trackY + trackH / 2.0F;
      float knobSize = 6.5F + 1.5F * hovAnim;
      Builder.rectangle()
         .size(new SizeState(knobSize, knobSize))
         .radius(new QuadRadiusState(knobSize / 2.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
         .build()
         .render(mx, knobX - knobSize / 2.0F, knobY - knobSize / 2.0F);
   }

   private void renderBool(Matrix4f mx, float rowY, BooleanSetting bs, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      boolean on = bs.get();
      float anim = this.boolAnimMap.getOrDefault(bs, on ? 1.0F : 0.0F);
      anim = GuiUtils.animate(anim, on ? 1.0F : 0.0F, 18.0F, deltaTime);
      this.boolAnimMap.put(bs, anim);
      Color labelColor = GuiUtils.interpolateColor(C_LABEL_OFF, C_LABEL_ON, anim);
      GuiUtils.renderTextSafely(mx, bs.getName(), sx, rowY + 3.5F, GuiUtils.withAlpha(labelColor, alpha), 8.0F);
      float switchW = 18.0F;
      float switchH = 9.0F;
      float switchX = sx + sw - switchW;
      float switchY = rowY + 4.0F;
      Color trackColor = GuiUtils.interpolateColor(C_SWITCH_OFF, themeColor, anim);
      Builder.rectangle()
         .size(new SizeState(switchW, switchH))
         .radius(new QuadRadiusState(4.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(trackColor, alpha)))
         .build()
         .render(mx, switchX, switchY);
      float thumbSize = 7.0F;
      float thumbX = switchX + 1.0F + (switchW - thumbSize - 2.0F) * anim;
      float thumbY = switchY + 1.0F;
      Builder.rectangle()
         .size(new SizeState(thumbSize, thumbSize))
         .radius(new QuadRadiusState(3.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
         .build()
         .render(mx, thumbX, thumbY);
   }

   public static boolean mouseClicked(double mouseX, double mouseY, int button, float winX, float winY, float winW, float winH) {
      if (ACTIVE != null && !(ACTIVE.anim < 0.05F) && !ACTIVE.isClosing) {
         float[] bounds = getPopupBounds(winX, winY, winW, winH);
         float sX = bounds[0];
         float sY = bounds[1];
         float subW = bounds[2];
         float subH = bounds[3];
         if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX + subW - 20.0F, sY + 5.0F, 15.0F, 15.0F)) {
            close();
            return true;
         }

         float contentTop = sY + 24.0F + 2.0F;
         float contentBottom = sY + subH - 3.0F;
         if (!GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX, sY, subW, subH)) {
            close();
            return true;
         }

         if (mouseY >= contentTop && mouseY <= contentBottom) {
            float itemY = contentTop + 2.0F + ACTIVE.scrollY;

            for (int i = 0; i < ACTIVE.settings.size(); i++) {
               Setting s = ACTIVE.settings.get(i);
               float itemH = s instanceof NumberSetting ? 22.0F : (s instanceof BooleanSetting ? 18.0F : 20.0F);
               if (s instanceof NumberSetting ns) {
                  if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX + 10.0F, itemY, subW - 20.0F, 22.0F)) {
                     ACTIVE.draggingSlider = i;
                     ACTIVE.applySlider(mouseX, sX, subW, ns);
                     return true;
                  }
               } else if (s instanceof BooleanSetting bs && GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX + 10.0F, itemY, subW - 20.0F, 18.0F)) {
                  bs.toggle();
                  return true;
               }

               itemY += itemH;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean mouseDragged(double mouseX, double mouseY, int button, float winX, float winY, float winW, float winH) {
      if (ACTIVE != null && !(ACTIVE.anim < 0.05F) && !ACTIVE.isClosing) {
         if (ACTIVE.draggingSlider >= 0 && ACTIVE.draggingSlider < ACTIVE.settings.size()) {
            Setting s = ACTIVE.settings.get(ACTIVE.draggingSlider);
            if (s instanceof NumberSetting ns) {
               float[] bounds = getPopupBounds(winX, winY, winW, winH);
               float sX = bounds[0];
               float subW = bounds[2];
               ACTIVE.applySlider(mouseX, sX, subW, ns);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static void mouseReleased(double mouseX, double mouseY, int button) {
      if (ACTIVE != null) {
         ACTIVE.draggingSlider = -1;
      }
   }

   public static boolean mouseScrolled(double mouseX, double mouseY, double amount, float winX, float winY, float winW, float winH) {
      if (ACTIVE != null && !(ACTIVE.anim < 0.05F) && !ACTIVE.isClosing) {
         float[] bounds = getPopupBounds(winX, winY, winW, winH);
         float sX = bounds[0];
         float sY = bounds[1];
         float subW = bounds[2];
         float subH = bounds[3];
         float bodyH = bounds[4];
         float totalContentH = bounds[5];
         if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX, sY, subW, subH)) {
            float maxScroll = Math.max(0.0F, totalContentH + 8.0F - bodyH);
            ACTIVE.targetScrollY = Mth.clamp(ACTIVE.targetScrollY + (float)amount * 28.0F, -maxScroll, 0.0F);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void applySlider(double mx, float startX, float w, NumberSetting ns) {
      float sw = w - 20.0F;
      float ratio = (float)Mth.clamp((mx - (startX + 10.0F)) / sw, 0.0, 1.0);
      double range = ns.getMax() - ns.getMin();
      double rawVal = ns.getMin() + ratio * range;
      double inc = ns.getIncrement();
      if (inc > 0.0) {
         rawVal = Math.round(rawVal / inc) * inc;
      }

      double oldVal = ns.get();
      double newVal = Mth.clamp(rawVal, ns.getMin(), ns.getMax());
      if (Double.compare(oldVal, newVal) != 0) {
         ns.set(newVal);
         ModSounds.playSliderMove();
      }
   }
}
