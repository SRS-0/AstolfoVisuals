package xyz.angames.astolfoclient.client.gui.clickgui;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import com.google.common.base.Supplier;
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
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.ActionSetting;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ConfigureSetting;
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.KeybindSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.MultiSelectSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(EnvType.CLIENT)
public class UniversalSettingsPanel {
   private final Module module;
   private final List<Object> rows = new ArrayList<>();
   private static final float SLIDER_H = 23.0F;
   private static final float BOOL_H = 18.0F;
   private static final float MODE_H = 22.0F;
   private static final float MODE_ITEM_H = 15.0F;
   private static final float KEYBIND_H = 20.0F;
   private static final float ACTION_H = 22.0F;
   private static final Color C_LABEL_OFF = new Color(145, 145, 160, 255);
   private static final Color C_LABEL_ON = new Color(255, 255, 255, 255);
   private static final Color C_TRACK_BG = new Color(14, 14, 18, 255);
   private static final Color C_BOX_BG = new Color(10, 10, 14, 255);
   private static final Color C_SWITCH_OFF = new Color(20, 20, 26, 255);
   private static final Color C_POPUP_BG = new Color(6, 6, 8, 250);
   private static final Supplier<MsdfFont> MEDIUM_FONT = ClickGuiIcons.MEDIUM_FONT;
   private int draggingSlider = -1;
   private boolean isListening = false;
   private KeybindSetting listeningSetting = null;
   private final Map<BooleanSetting, Float> boolAnimMap = new HashMap<>();
   private final Map<NumberSetting, Float> sliderRatioAnimMap = new HashMap<>();
   private final Map<NumberSetting, Float> sliderHoverAnimMap = new HashMap<>();
   private final Map<ActionSetting, Float> actionHoverMap = new HashMap<>();

   public UniversalSettingsPanel(Module module) {
      this.module = module;
   }

   public void updateRows() {
      this.rows.clear();

      for (Setting baseSetting : this.module.getSettings()) {
         if (baseSetting != null && baseSetting.isVisible()) {
            this.rows.add(baseSetting);
         }
      }
   }

   public boolean hasSettings() {
      for (Object row : this.rows) {
         if (row instanceof Setting) {
            return true;
         }
      }

      return false;
   }

   public float getTotalHeight() {
      if (this.rows.isEmpty()) {
         return 0.0F;
      }

      float h = 4.0F;

      for (Object r : this.rows) {
         h += this.rowH(r);
      }

      return h + 4.0F;
   }

   public void render(GuiGraphicsExtractor context, float x, float y, float width, float alpha, int mouseX, int mouseY, float deltaTime) {
      if (!(alpha <= 0.05F) && !this.rows.isEmpty()) {
         Matrix4f mx = GuiCompat.matrix4f(context);
         Color themeColor = new Color(ThemeManager.getThemedColor(0L));

         for (Object row : this.rows) {
            if (row instanceof BooleanSetting bs) {
               float cur = this.boolAnimMap.getOrDefault(bs, bs.get() ? 1.0F : 0.0F);
               this.boolAnimMap.put(bs, GuiUtils.animate(cur, bs.get() ? 1.0F : 0.0F, 18.0F, deltaTime));
            }
         }

         Builder.rectangle()
            .size(new SizeState(width - 20.0F, 1.0F))
            .radius(new QuadRadiusState(0.0F))
            .color(new QuadColorState(GuiUtils.withAlpha(new Color(24, 24, 32), alpha * 0.8F)))
            .build()
            .render(mx, x + 10.0F, y + 1.0F);
         float rowY = y + 5.0F;

         for (int i = 0; i < this.rows.size(); i++) {
            Object row = this.rows.get(i);
            if (row instanceof NumberSetting ns) {
               this.renderSlider(mx, rowY, ns, themeColor, x, width, alpha, mouseX, mouseY, deltaTime, i == this.draggingSlider);
            } else if (row instanceof BooleanSetting bs) {
               this.renderBool(mx, rowY, bs, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
            } else if (row instanceof ModeSetting ms) {
               this.renderMode(mx, rowY, ms, ms.getName(), ms.get(), ms.getModes(), themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
            } else if (!(row instanceof EnumSetting<?> es)) {
               if (row instanceof KeybindSetting ks) {
                  this.renderKeybind(mx, rowY, ks, themeColor, x, width, alpha, mouseX, mouseY);
               } else if (row instanceof ActionSetting as) {
                  this.renderAction(mx, rowY, as, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
               } else if (row instanceof ConfigureSetting cs) {
                  this.renderConfigure(mx, rowY, cs, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
               } else if (row instanceof MultiSelectSetting mss) {
                  this.renderMultiSelect(mx, rowY, mss, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
               }
            } else {
               List<String> enumModes = new ArrayList<>();

               for (Enum<?> e : es.getValues()) {
                  enumModes.add(e.name());
               }

               this.renderMode(mx, rowY, es, es.getName(), es.getValue().name(), enumModes, themeColor, x, width, alpha, mouseX, mouseY, deltaTime);
            }

            rowY += this.rowH(row);
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
      boolean isHov = isDragging || GuiUtils.isMouseOver(mouseX, mouseY, sx, rowY, sw, 23.0F);
      float hovAnim = this.sliderHoverAnimMap.getOrDefault(ns, 0.0F);
      hovAnim = GuiUtils.animate(hovAnim, isHov ? 1.0F : 0.0F, 14.0F, deltaTime);
      this.sliderHoverAnimMap.put(ns, hovAnim);
      String valStr = String.format(ns.getIncrement() > 0.0 && ns.getIncrement() < 1.0 ? "%.2f" : "%.1f", val);
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      float textY = rowY + 1.5F;
      Color labelCol = GuiUtils.interpolateColor(C_LABEL_OFF, Color.WHITE, hovAnim * 0.4F);
      this.renderMediumText(mx, ns.getName(), sx, textY, GuiUtils.withAlpha(labelCol, alpha), 8.5F);
      float valW = medFont.getWidth(valStr, 8.5F);
      this.renderMediumText(mx, valStr, sx + sw - valW, textY, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5F);
      float trackY = rowY + 13.5F;
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
      this.renderMediumText(mx, bs.getName(), sx, rowY + 3.5F, GuiUtils.withAlpha(labelColor, alpha), 8.5F);
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

   private void renderMode(
      Matrix4f mx,
      float rowY,
      Setting setting,
      String name,
      String cur,
      List<String> modes,
      Color themeColor,
      float x,
      float w,
      float alpha,
      int mouseX,
      int mouseY,
      float deltaTime
   ) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float boxH = 15.0F;
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      this.renderMediumText(mx, name, sx, rowY + 3.5F, GuiUtils.withAlpha(C_LABEL_OFF, alpha), 8.5F);
      boolean isOpen = ModePopupState.isSettingOpen(setting);
      String displayStr = cur + " >";
      float strW = medFont != null ? medFont.getWidth(displayStr, 7.5F) : 30.0F;
      float pillW = Math.max(strW + 10.0F, 38.0F);
      float pillX = sx + sw - pillW;
      float pillY = rowY + 2.5F;
      boolean pillHov = GuiUtils.isMouseOver(mouseX, mouseY, pillX, pillY, pillW, boxH);
      Color pillBg = isOpen ? new Color(28, 28, 38, 255) : (pillHov ? new Color(20, 20, 26, 255) : C_BOX_BG);
      Builder.rectangle()
         .size(new SizeState(pillW, boxH))
         .radius(new QuadRadiusState(3.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(pillBg, alpha)))
         .build()
         .render(mx, pillX, pillY);
      Color textColor = isOpen ? themeColor : (pillHov ? themeColor : new Color(220, 220, 235));
      float textX = pillX + (pillW - strW) / 2.0F;
      this.renderMediumText(mx, displayStr, textX, pillY + 3.5F, GuiUtils.withAlpha(textColor, alpha), 7.5F);
   }

   private void renderKeybind(Matrix4f mx, float rowY, KeybindSetting ks, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float boxH = 15.0F;
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      this.renderMediumText(mx, ks.getName(), sx, rowY + 3.5F, GuiUtils.withAlpha(C_LABEL_OFF, alpha), 8.5F);
      boolean listening = this.isListening && this.listeningSetting == ks;
      String cur = listening ? "..." : (ks.getKey() == 0 ? "NONE" : ks.getKeyName());
      float curW = medFont.getWidth(cur, 8.0F);
      float pillW = Math.max(curW + 10.0F, 30.0F);
      float pillX = sx + sw - pillW;
      float pillY = rowY + 2.5F;
      boolean hov = GuiUtils.isMouseOver(mouseX, mouseY, pillX, pillY, pillW, boxH);
      Color pillBg = listening ? new Color(50, 25, 20, 255) : (hov ? new Color(24, 24, 32, 255) : C_BOX_BG);
      Color textColor = listening ? new Color(255, 200, 80) : Color.WHITE;
      Builder.rectangle()
         .size(new SizeState(pillW, boxH))
         .radius(new QuadRadiusState(3.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(pillBg, alpha)))
         .build()
         .render(mx, pillX, pillY);
      float textX = pillX + (pillW - curW) / 2.0F;
      this.renderMediumText(mx, cur, textX, pillY + 3.5F, GuiUtils.withAlpha(textColor, alpha), 8.0F);
   }

   private void renderAction(
      Matrix4f mx, float rowY, ActionSetting as, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime
   ) {
      float bx = x + 10.0F;
      float bw = w - 20.0F;
      float bh = 18.0F;
      boolean hov = GuiUtils.isMouseOver(mouseX, mouseY, bx, rowY + 2.0F, bw, bh);
      float hovA = this.actionHoverMap.getOrDefault(as, 0.0F);
      hovA = GuiUtils.animate(hovA, hov ? 1.0F : 0.0F, 15.0F, deltaTime);
      this.actionHoverMap.put(as, hovA);
      Color btnBg = GuiUtils.interpolateColor(C_BOX_BG, new Color(26, 26, 36), hovA);
      Builder.rectangle()
         .size(new SizeState(bw, bh))
         .radius(new QuadRadiusState(4.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(btnBg, alpha)))
         .build()
         .render(mx, bx, rowY + 2.0F);
      String label = as.getName();
      float tw = ((MsdfFont)MEDIUM_FONT.get()).getWidth(label, 8.5F);
      float tx = bx + (bw - tw) / 2.0F;
      this.renderMediumText(mx, label, tx, rowY + 5.5F, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5F);
   }

   private void renderConfigure(
      Matrix4f mx, float rowY, ConfigureSetting cs, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime
   ) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float boxH = 15.0F;
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      this.renderMediumText(mx, cs.getName(), sx, rowY + 3.5F, GuiUtils.withAlpha(C_LABEL_OFF, alpha), 8.5F);
      boolean isOpen = SubSettingsPopupState.isSettingOpen(cs);
      String displayStr = cs.getButtonText() + " >";
      float strW = medFont != null ? medFont.getWidth(displayStr, 7.5F) : 38.0F;
      float pillW = Math.max(strW + 10.0F, 44.0F);
      float pillX = sx + sw - pillW;
      float pillY = rowY + 2.5F;
      boolean pillHov = GuiUtils.isMouseOver(mouseX, mouseY, pillX, pillY, pillW, boxH);
      Color pillBg = isOpen ? new Color(28, 28, 38, 255) : (pillHov ? new Color(20, 20, 26, 255) : C_BOX_BG);
      Builder.rectangle()
         .size(new SizeState(pillW, boxH))
         .radius(new QuadRadiusState(3.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(pillBg, alpha)))
         .build()
         .render(mx, pillX, pillY);
      Color textColor = isOpen ? themeColor : (pillHov ? themeColor : new Color(220, 220, 235));
      float textX = pillX + (pillW - strW) / 2.0F;
      this.renderMediumText(mx, displayStr, textX, pillY + 3.5F, GuiUtils.withAlpha(textColor, alpha), 7.5F);
   }

   private void renderMultiSelect(
      Matrix4f mx, float rowY, MultiSelectSetting mss, Color themeColor, float x, float w, float alpha, int mouseX, int mouseY, float deltaTime
   ) {
      float sx = x + 10.0F;
      float sw = w - 20.0F;
      float boxH = 15.0F;
      MsdfFont medFont = (MsdfFont)MEDIUM_FONT.get();
      this.renderMediumText(mx, mss.getName(), sx, rowY + 3.5F, GuiUtils.withAlpha(C_LABEL_OFF, alpha), 8.5F);
      boolean isOpen = MultiSelectPopupState.isSettingOpen(mss);
      String displayStr = mss.getButtonText() + " >";
      float strW = medFont != null ? medFont.getWidth(displayStr, 7.5F) : 38.0F;
      float pillW = Math.max(strW + 10.0F, 42.0F);
      float pillX = sx + sw - pillW;
      float pillY = rowY + 2.5F;
      boolean pillHov = GuiUtils.isMouseOver(mouseX, mouseY, pillX, pillY, pillW, boxH);
      Color pillBg = isOpen ? new Color(28, 28, 38, 255) : (pillHov ? new Color(20, 20, 26, 255) : C_BOX_BG);
      Builder.rectangle()
         .size(new SizeState(pillW, boxH))
         .radius(new QuadRadiusState(3.5F))
         .color(new QuadColorState(GuiUtils.withAlpha(pillBg, alpha)))
         .build()
         .render(mx, pillX, pillY);
      Color textColor = isOpen ? themeColor : (pillHov ? themeColor : new Color(220, 220, 235));
      float textX = pillX + (pillW - strW) / 2.0F;
      this.renderMediumText(mx, displayStr, textX, pillY + 3.5F, GuiUtils.withAlpha(textColor, alpha), 7.5F);
   }

   private void renderMediumText(Matrix4f matrix, String text, float tx, float ty, Color color, float size) {
      if (text != null && !text.isEmpty() && color.getAlpha() > 4) {
         try {
            Builder.text().font((MsdfFont)MEDIUM_FONT.get()).text(text).color(color).size(size).build().render(matrix, tx, ty);
         } catch (Exception var8) {
         }
      }
   }

   public boolean mouseClicked(double mx, double my, int button, float startX, float startY, float w) {
      if (this.isListening) {
         if (this.listeningSetting != null) {
            this.listeningSetting.setKey(-(button + 100));
         }

         this.isListening = false;
         this.listeningSetting = null;
         return true;
      } else {
         float rowY = startY + 5.0F;

         for (int i = 0; i < this.rows.size(); i++) {
            Object row = this.rows.get(i);
            float rh = this.rowH(row);
            if (row instanceof NumberSetting ns) {
               if (GuiUtils.isMouseOver((float)mx, (float)my, startX + 10.0F, rowY, w - 20.0F, 23.0F)) {
                  this.draggingSlider = i;
                  this.applySlider(mx, startX, w, ns);
                  return true;
               }
            } else if (row instanceof BooleanSetting bs) {
               if (GuiUtils.isMouseOver((float)mx, (float)my, startX + 10.0F, rowY, w - 20.0F, 18.0F)) {
                  bs.toggle();
                  return true;
               }
            } else if (row instanceof ModeSetting ms) {
               float sx = startX + 10.0F;
               float sw = w - 20.0F;
               String displayStr = ms.get() + " >";
               float strW = MEDIUM_FONT.get() != null ? ((MsdfFont)MEDIUM_FONT.get()).getWidth(displayStr, 7.5F) : 30.0F;
               float pillW = Math.max(strW + 10.0F, 38.0F);
               float pillX = sx + sw - pillW;
               if (GuiUtils.isMouseOver((float)mx, (float)my, pillX, rowY + 2.5F, pillW, 15.0F)) {
                  ModSounds.playModeOpen();
                  ModePopupState.open(ms, ms.getName(), ms.getModes(), pillX, rowY + 2.5F, pillW, 15.0F);
                  return true;
               }
            } else if (row instanceof EnumSetting<?> es) {
               float sx = startX + 10.0F;
               float sw = w - 20.0F;
               String displayStr = es.getValue().name() + " >";
               float strW = MEDIUM_FONT.get() != null ? ((MsdfFont)MEDIUM_FONT.get()).getWidth(displayStr, 7.5F) : 30.0F;
               float pillW = Math.max(strW + 10.0F, 38.0F);
               float pillX = sx + sw - pillW;
               if (GuiUtils.isMouseOver((float)mx, (float)my, pillX, rowY + 2.5F, pillW, 15.0F)) {
                  List<String> enumModes = new ArrayList<>();

                  for (Enum<?> e : es.getValues()) {
                     enumModes.add(e.name());
                  }

                  ModSounds.playModeOpen();
                  ModePopupState.open(es, es.getName(), enumModes, pillX, rowY + 2.5F, pillW, 15.0F);
                  return true;
               }
            } else if (row instanceof KeybindSetting ks) {
               if (GuiUtils.isMouseOver((float)mx, (float)my, startX + 10.0F, rowY, w - 20.0F, 20.0F)) {
                  if (button != 1 && button != 2) {
                     this.isListening = true;
                     this.listeningSetting = ks;
                  } else {
                     ks.setKey(0);
                  }

                  return true;
               }
            } else if (row instanceof ActionSetting as) {
               if (GuiUtils.isMouseOver((float)mx, (float)my, startX + 10.0F, rowY + 2.0F, w - 20.0F, 18.0F)) {
                  as.run();
                  return true;
               }
            } else if (row instanceof ConfigureSetting cs) {
               float sx = startX + 10.0F;
               float sw = w - 20.0F;
               String displayStr = cs.getButtonText() + " >";
               float strW = MEDIUM_FONT.get() != null ? ((MsdfFont)MEDIUM_FONT.get()).getWidth(displayStr, 7.5F) : 38.0F;
               float pillW = Math.max(strW + 10.0F, 44.0F);
               float pillX = sx + sw - pillW;
               if (GuiUtils.isMouseOver((float)mx, (float)my, pillX, rowY + 2.5F, pillW, 15.0F)) {
                  ModSounds.playModeOpen();
                  SubSettingsPopupState.open(cs, cs.getName(), cs.getSubSettings(), pillX, rowY + 2.5F, pillW, 15.0F);
                  return true;
               }
            } else if (row instanceof MultiSelectSetting mss) {
               float sx = startX + 10.0F;
               float sw = w - 20.0F;
               String displayStr = mss.getButtonText() + " >";
               float strW = MEDIUM_FONT.get() != null ? ((MsdfFont)MEDIUM_FONT.get()).getWidth(displayStr, 7.5F) : 38.0F;
               float pillW = Math.max(strW + 10.0F, 42.0F);
               float pillX = sx + sw - pillW;
               if (GuiUtils.isMouseOver((float)mx, (float)my, pillX, rowY + 2.5F, pillW, 15.0F)) {
                  ModSounds.playModeOpen();
                  MultiSelectPopupState.open(mss, mss.getName(), mss.getOptions(), pillX, rowY + 2.5F, pillW, 15.0F);
                  return true;
               }
            }

            rowY += rh;
         }

         return false;
      }
   }

   public boolean mouseDragged(double mx, double my, float startX, float w) {
      if (this.draggingSlider >= 0 && this.draggingSlider < this.rows.size() && this.rows.get(this.draggingSlider) instanceof NumberSetting ns) {
         this.applySlider(mx, startX, w, ns);
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased() {
      this.draggingSlider = -1;
   }

   public void keyPressed(int key) {
      if (this.isListening && this.listeningSetting != null) {
         this.listeningSetting.setKey(key != 256 && key != 261 && key != 259 ? key : 0);
         this.isListening = false;
         this.listeningSetting = null;
      }
   }

   private float rowH(Object row) {
      if (row instanceof NumberSetting) {
         return 23.0F;
      } else if (row instanceof BooleanSetting) {
         return 18.0F;
      } else if (row instanceof KeybindSetting) {
         return 20.0F;
      } else if (row instanceof ActionSetting) {
         return 22.0F;
      } else if (row instanceof ConfigureSetting) {
         return 22.0F;
      } else if (row instanceof MultiSelectSetting) {
         return 22.0F;
      } else {
         return !(row instanceof ModeSetting) && !(row instanceof EnumSetting) ? 22.0F : 22.0F;
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
