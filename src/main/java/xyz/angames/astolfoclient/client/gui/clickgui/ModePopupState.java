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
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;

@Environment(EnvType.CLIENT)
public class ModePopupState {
   public static ModePopupState ACTIVE = null;
   public static final int MAX_VISIBLE_ITEMS = 10;
   public static final float ITEM_ROW_H = 16.0F;
   public final Setting setting;
   public final String title;
   public final List<String> options;
   public float triggerX;
   public float triggerY;
   public float triggerW;
   public float triggerH;
   public float anim = 0.0F;
   public boolean isClosing = false;
   public float scrollY = 0.0F;
   public float targetScrollY = 0.0F;
   private final Map<String, Float> itemHoverAnims = new HashMap<>();

   public ModePopupState(Setting setting, String title, List<String> options, float triggerX, float triggerY, float triggerW, float triggerH) {
      this.setting = setting;
      this.title = title;
      this.options = new ArrayList<>(options);
      this.triggerX = triggerX;
      this.triggerY = triggerY;
      this.triggerW = triggerW;
      this.triggerH = triggerH;
      String curMode = "";
      if (setting instanceof ModeSetting ms) {
         curMode = ms.get();
      } else if (setting instanceof EnumSetting<?> es) {
         curMode = es.getValue().name();
      }

      int selIdx = -1;

      for (int i = 0; i < this.options.size(); i++) {
         if (this.options.get(i).equalsIgnoreCase(curMode)) {
            selIdx = i;
            break;
         }
      }

      int count = this.options.size();
      int visibleCount = Math.min(count, 10);
      float totalContentH = count * 16.0F;
      float maxScroll = Math.max(0.0F, totalContentH - visibleCount * 16.0F);
      if (selIdx >= 10 && maxScroll > 0.0F) {
         this.targetScrollY = -Math.min(maxScroll, (selIdx - 4) * 16.0F);
         this.scrollY = this.targetScrollY;
      }
   }

   public static void open(Setting setting, String title, List<String> options, float tx, float ty, float tw, float th) {
      if (ACTIVE != null && ACTIVE.setting == setting) {
         ACTIVE.isClosing = true;
      } else {
         ACTIVE = new ModePopupState(setting, title, options, tx, ty, tw, th);
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
      return ACTIVE != null && ACTIVE.setting == s && !ACTIVE.isClosing;
   }

   public static float calculateSubWidth() {
      if (ACTIVE == null) {
         return 84.0F;
      }

      MsdfFont medFont = null;

      try {
         medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
      } catch (Exception var5) {
      }

      float maxTextW = 50.0F;
      if (medFont != null) {
         for (String opt : ACTIVE.options) {
            float w = medFont.getWidth(opt, 7.5F);
            if (w > maxTextW) {
               maxTextW = w;
            }
         }
      }

      boolean hasScroll = ACTIVE.options.size() > 10;
      return Math.max(maxTextW + (hasScroll ? 42.0F : 36.0F), 84.0F);
   }

   public static float calculatePopupX(float winX, float winW, float subW) {
      if (ACTIVE == null) {
         return winX;
      }

      float sX = ACTIVE.triggerX + ACTIVE.triggerW + 4.0F;
      if (sX + subW > winX + winW - 6.0F) {
         sX = ACTIVE.triggerX - subW - 4.0F;
      }

      if (sX < winX + 10.0F) {
         sX = ACTIVE.triggerX + ACTIVE.triggerW - subW;
      }

      return sX;
   }

   public static float calculatePopupY(float winY, float winH, float subH) {
      if (ACTIVE == null) {
         return winY;
      }

      float sY = ACTIVE.triggerY - 2.0F;
      return Mth.clamp(sY, winY + 38.0F, winY + winH - subH - 8.0F);
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
            MsdfFont spFont = null;

            try {
               medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
            } catch (Exception var44) {
            }

            try {
               semiboldFont = (MsdfFont)ClickGuiIcons.SEMIBOLD_FONT.get();
            } catch (Exception var43) {
            }

            try {
               spFont = (MsdfFont)ClickGuiIcons.SP_FONT.get();
            } catch (Exception var42) {
            }

            int count = ACTIVE.options.size();
            int visibleCount = Math.min(count, 10);
            float totalContentH = count * 16.0F;
            float maxScroll = Math.max(0.0F, totalContentH - visibleCount * 16.0F);
            ACTIVE.scrollY = GuiUtils.animate(ACTIVE.scrollY, ACTIVE.targetScrollY, 20.0F, deltaTime);
            float subW = calculateSubWidth();
            float subH = visibleCount * 16.0F + 8.0F;
            float sX = calculatePopupX(winX, winW, subW);
            float sY = calculatePopupY(winY, winH, subH);
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
               int sAlpha = (int)(65.0F * (1.0F - progress) * (1.0F - progress) * alpha);
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
            float contentTop = sY + 4.0F;
            float contentBottom = sY + subH - 4.0F;
            float contentH = contentBottom - contentTop;
            context.enableScissor((int)sX, (int)contentTop, (int)(sX + subW), (int)contentBottom);
            String curMode = "";
            if (ACTIVE.setting instanceof ModeSetting ms) {
               curMode = ms.get();
            } else if (ACTIVE.setting instanceof EnumSetting<?> es) {
               curMode = es.getValue().name();
            }

            float itemAreaW = subW - (maxScroll > 0.5F ? 9.0F : 6.0F);

            for (int i = 0; i < ACTIVE.options.size(); i++) {
               String opt = ACTIVE.options.get(i);
               float rY = contentTop + i * 16.0F + ACTIVE.scrollY;
               if (!(rY + 16.0F < contentTop - 2.0F) && !(rY > contentBottom + 2.0F)) {
                  boolean isSel = opt.equalsIgnoreCase(curMode);
                  boolean rHov = mouseX >= sX + 3.0F
                     && mouseX <= sX + 3.0F + itemAreaW
                     && mouseY >= rY
                     && mouseY <= rY + 16.0F - 1.0F
                     && mouseY >= contentTop
                     && mouseY <= contentBottom;
                  float hovA = ACTIVE.itemHoverAnims.getOrDefault(opt, 0.0F);
                  hovA = GuiUtils.animate(hovA, rHov ? 1.0F : 0.0F, 18.0F, deltaTime);
                  ACTIVE.itemHoverAnims.put(opt, hovA);
                  if (hovA > 0.01F) {
                     Builder.rectangle()
                        .size(new SizeState(itemAreaW, 15.0F))
                        .radius(new QuadRadiusState(4.0F))
                        .color(new QuadColorState(new Color(25, 25, 36, (int)(220.0F * alpha * hovA))))
                        .build()
                        .render(matrix, sX + 3.0F, rY);
                  }

                  if (isSel) {
                     if (spFont != null) {
                        try {
                           Builder.text()
                              .font(spFont)
                              .text("D")
                              .size(6.5F)
                              .color(new Color(255, 255, 255, (int)(255.0F * alpha)))
                              .build()
                              .render(matrix, sX + 6.0F, rY + 4.0F);
                        } catch (Exception e) {
                           GuiUtils.renderTextSafely(matrix, "✓", sX + 6.0F, rY + 4.0F, GuiUtils.withAlpha(Color.WHITE, alpha), 7.5F);
                        }
                     } else {
                        GuiUtils.renderTextSafely(matrix, "✓", sX + 6.0F, rY + 4.0F, GuiUtils.withAlpha(Color.WHITE, alpha), 7.5F);
                     }
                  }

                  Color textCol = isSel
                     ? new Color(255, 255, 255, (int)(255.0F * alpha))
                     : (hovA > 0.01F ? GuiUtils.withAlpha(new Color(220, 220, 240), alpha) : GuiUtils.withAlpha(new Color(135, 135, 150), alpha));
                  float textStartX = sX + 18.0F;
                  if (isSel && semiboldFont != null) {
                     try {
                        Builder.text().font(semiboldFont).text(opt).size(7.5F).color(textCol).build().render(matrix, textStartX, rY + 4.5F);
                     } catch (Exception e) {
                        GuiUtils.renderTextSafely(matrix, opt, textStartX, rY + 4.5F, textCol, 7.5F);
                     }
                  } else if (medFont != null) {
                     try {
                        Builder.text().font(medFont).text(opt).size(7.5F).color(textCol).build().render(matrix, textStartX, rY + 4.5F);
                     } catch (Exception e) {
                        GuiUtils.renderTextSafely(matrix, opt, textStartX, rY + 4.5F, textCol, 7.5F);
                     }
                  } else {
                     GuiUtils.renderTextSafely(matrix, opt, textStartX, rY + 4.5F, textCol, 7.5F);
                  }
               }
            }

            context.disableScissor();
            if (maxScroll > 0.5F) {
               float scrollTrackH = subH - 12.0F;
               float scrollThumbH = Math.max(14.0F, contentH / totalContentH * scrollTrackH);
               float scrollProgress = Mth.clamp(-ACTIVE.scrollY / maxScroll, 0.0F, 1.0F);
               float scrollThumbY = sY + 6.0F + scrollProgress * (scrollTrackH - scrollThumbH);
               Builder.rectangle()
                  .size(new SizeState(2.5F, scrollThumbH))
                  .radius(new QuadRadiusState(1.25F))
                  .color(new QuadColorState(new Color(70, 70, 90, (int)(220.0F * alpha))))
                  .build()
                  .render(matrix, sX + subW - 4.5F, scrollThumbY);
            }

            context.pose().popMatrix();
         }
      }
   }

   public static boolean mouseClicked(double mouseX, double mouseY, int button, float winX, float winY, float winW, float winH) {
      if (ACTIVE != null && !(ACTIVE.anim < 0.05F) && !ACTIVE.isClosing) {
         int count = ACTIVE.options.size();
         int visibleCount = Math.min(count, 10);
         float subW = calculateSubWidth();
         float subH = visibleCount * 16.0F + 8.0F;
         float sX = calculatePopupX(winX, winW, subW);
         float sY = calculatePopupY(winY, winH, subH);
         float totalContentH = count * 16.0F;
         float maxScroll = Math.max(0.0F, totalContentH - visibleCount * 16.0F);
         if (!GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX, sY, subW, subH)) {
            close();
            return true;
         }

         float contentTop = sY + 4.0F;
         float contentBottom = sY + subH - 4.0F;
         if (mouseY >= contentTop && mouseY <= contentBottom) {
            float itemAreaW = subW - (maxScroll > 0.5F ? 9.0F : 6.0F);

            for (int i = 0; i < ACTIVE.options.size(); i++) {
               float rY = contentTop + i * 16.0F + ACTIVE.scrollY;
               if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX + 3.0F, rY, itemAreaW, 15.0F)) {
                  String chosen = ACTIVE.options.get(i);
                  if (ACTIVE.setting instanceof ModeSetting ms) {
                     ms.set(chosen);
                  } else if (ACTIVE.setting instanceof EnumSetting<?> es) {
                     es.setByName(chosen);
                  }

                  close();
                  return true;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean mouseScrolled(double mouseX, double mouseY, double amount, float winX, float winY, float winW, float winH) {
      if (ACTIVE != null && !(ACTIVE.anim < 0.05F) && !ACTIVE.isClosing) {
         int count = ACTIVE.options.size();
         int visibleCount = Math.min(count, 10);
         float subW = calculateSubWidth();
         float subH = visibleCount * 16.0F + 8.0F;
         float sX = calculatePopupX(winX, winW, subW);
         float sY = calculatePopupY(winY, winH, subH);
         float totalContentH = count * 16.0F;
         float maxScroll = Math.max(0.0F, totalContentH - visibleCount * 16.0F);
         if (GuiUtils.isMouseOver((float)mouseX, (float)mouseY, sX, sY, subW, subH)) {
            if (maxScroll > 0.5F) {
               ACTIVE.targetScrollY = Mth.clamp(ACTIVE.targetScrollY + (float)amount * 18.0F, -maxScroll, 0.0F);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
