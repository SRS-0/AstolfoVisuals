package xyz.angames.astolfoclient.client.hud;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;
import xyz.angames.astolfoclient.client.util.KeyUtils;

@Environment(EnvType.CLIENT)
public class ActiveBindsManager {
   private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
   private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
   private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
   private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(
      () -> MsdfFont.builder()
         .name("interface_icons_activebinds")
         .data(Identifier.fromNamespaceAndPath("mre", "icons/interface/interface.json"))
         .atlas(Identifier.fromNamespaceAndPath("mre", "icons/interface/interface.png"))
         .glyphMapper(g -> {
            int idx = g.index();
            if (idx == 3) {
               return 75;
            } else if (idx == 4) {
               return 83;
            } else if (idx == 5) {
               return 67;
            } else {
               return idx == 6 ? 66 : idx;
            }
         })
         .build()
   );
   public float x = 10.0F;
   public float y = 100.0F;
   private boolean dragging = false;
   private float dragOffsetX;
   private float dragOffsetY;
   private final Map<String, Float> animationMap = new HashMap<>();
   private float currentWidth = 90.0F;
   private float currentHeight = 22.0F;
   private float masterAlpha = 0.0F;
   private long lastUpdateTimeNs = -1L;

   public void render(GuiGraphicsExtractor context, float tickDelta) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null) {
         boolean isEditing = mc.gui.screen() instanceof HudEditorScreen;
         InterfaceModule interfaceMod = (InterfaceModule)(
            AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Interface") : null
         );
         boolean isModuleOn = interfaceMod != null && interfaceMod.isEnabled() && interfaceMod.activeBinds.get();
         long nowNs = System.nanoTime();
         if (this.lastUpdateTimeNs == -1L) {
            this.lastUpdateTimeNs = nowNs;
         }

         long elapsedNs = nowNs - this.lastUpdateTimeNs;
         this.lastUpdateTimeNs = nowNs;
         double deltaSeconds = elapsedNs / 1.0E9;
         if (deltaSeconds > 0.1) {
            deltaSeconds = 0.1;
         }

         long now = (long)(nowNs / 1000000.0);
         Color themeColor = new Color(ThemeManager.getThemedColor(now / 10L));
         List<ActiveBindsManager.BindData> activeList = new ArrayList<>();
         List<Module> allModules = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModules() : List.of();
         if (isEditing && allModules.stream().noneMatch(m -> m.isEnabled() && m.getKeyCode() != -1)) {
            activeList.add(new ActiveBindsManager.BindData("KillAura", "R", 1.0F));
            activeList.add(new ActiveBindsManager.BindData("TargetStrafe", "V", 1.0F));
            activeList.add(new ActiveBindsManager.BindData("Velocity", "H", 1.0F));
         } else {
            for (Module mod : allModules) {
               if (mod.getKeyCode() != -1) {
                  boolean isOn = mod.isEnabled();
                  float currentProgress = this.animationMap.getOrDefault(mod.getName(), 0.0F);
                  float interpolationSpeed = 16.0F;
                  currentProgress += ((isOn ? 1.0F : 0.0F) - currentProgress) * (float)(1.0 - Math.exp(-interpolationSpeed * deltaSeconds));
                  if (currentProgress > 0.005F) {
                     this.animationMap.put(mod.getName(), currentProgress);
                     activeList.add(new ActiveBindsManager.BindData(mod.getName(), KeyUtils.getKeyName(mod.getKeyCode()), currentProgress));
                  } else {
                     this.animationMap.remove(mod.getName());
                  }
               }
            }
         }

         boolean shouldShow = !activeList.isEmpty() && isModuleOn || isEditing;
         float targetAlpha = shouldShow ? 1.0F : 0.0F;
         float fadeSpeed = 12.0F;
         this.masterAlpha = this.masterAlpha + (targetAlpha - this.masterAlpha) * (float)(1.0 - Math.exp(-fadeSpeed * deltaSeconds));
         if (!(this.masterAlpha < 0.005F) || shouldShow) {
            float headerHeight = 20.0F;
            float headerNameSize = 8.0F;
            float headerIconSize = 8.0F;
            float headerPaddingX = 8.0F;
            float headerIconGap = 4.5F;
            float moduleNameSize = 7.0F;
            float rowHeight = 14.0F;
            float paddingX = 8.0F;
            float paddingBottom = 5.0F;
            float minColumnGap = 16.0F;
            float radius = 6.0F;
            Color white = new Color(255, 255, 255, 255);
            Color lightGrayText = new Color(220, 220, 225, 255);
            MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
            MsdfFont medium = (MsdfFont)MEDIUM_FONT.get();
            MsdfFont iconFont = (MsdfFont)ICON_FONT.get();
            String iconChar = "K";
            String headerText = "Keybinds";
            float headerIconW = iconFont != null ? iconFont.getWidth(iconChar, headerIconSize) : 8.0F;
            float headerTextW = semibold != null ? semibold.getWidth(headerText, headerNameSize) : 38.0F;
            float headerContentW = headerIconW + headerIconGap + headerTextW;
            float maxRowW = headerContentW;

            for (ActiveBindsManager.BindData bind : activeList) {
               float nameW = medium != null ? medium.getWidth(bind.name, moduleNameSize) : 35.0F;
               float keyW = medium != null ? medium.getWidth(bind.key, moduleNameSize) : 10.0F;
               float rowW = nameW + minColumnGap + keyW;
               if (rowW > maxRowW) {
                  maxRowW = rowW;
               }
            }

            float targetWidth = Math.max(88.0F, paddingX + maxRowW + paddingX);
            float targetHeight = activeList.isEmpty() ? headerHeight : headerHeight + 2.0F + activeList.size() * rowHeight + paddingBottom;
            this.currentWidth = this.currentWidth + (targetWidth - this.currentWidth) * (float)(1.0 - Math.exp(-15.0 * deltaSeconds));
            this.currentHeight = this.currentHeight + (targetHeight - this.currentHeight) * (float)(1.0 - Math.exp(-15.0 * deltaSeconds));
            float scaleModifier = this.getScaleModifier();
            context.pose().pushMatrix();
            context.pose().translate(this.x, this.y);
            context.pose().scale(scaleModifier, scaleModifier);
            context.pose().translate(-this.x, -this.y);
            Matrix4f baseMatrix = GuiCompat.matrix4f(context);
            this.renderShadow(baseMatrix, this.x, this.y, this.currentWidth, this.currentHeight, radius, this.masterAlpha);
            Builder.rectangle()
               .size(new SizeState(this.currentWidth, this.currentHeight))
               .radius(new QuadRadiusState(radius))
               .color(new QuadColorState(new Color(0, 0, 0, (int)(255.0F * this.masterAlpha))))
               .build()
               .render(baseMatrix, this.x, this.y);
            float headerCenterY = this.y + headerHeight / 2.0F;
            float hX = this.x + headerPaddingX;
            float iconCX = hX + headerIconW / 2.0F;
            this.drawIconGlowShadow(baseMatrix, iconCX, headerCenterY, headerIconSize / 2.0F, themeColor, 0.12F * this.masterAlpha);
            if (iconFont != null) {
               Builder.text()
                  .font(iconFont)
                  .text(iconChar)
                  .color(GuiUtils.withAlpha(themeColor, this.masterAlpha))
                  .size(headerIconSize)
                  .build()
                  .render(baseMatrix, hX, headerCenterY - 2.9F);
            }

            hX += headerIconW + headerIconGap;
            if (semibold != null) {
               Builder.text()
                  .font(semibold)
                  .text(headerText)
                  .color(GuiUtils.withAlpha(white, this.masterAlpha))
                  .size(headerNameSize)
                  .build()
                  .render(baseMatrix, hX, headerCenterY - 2.9F);
            }

            if (!activeList.isEmpty()) {
               float divY = this.y + headerHeight;
               Builder.rectangle()
                  .size(new SizeState(this.currentWidth - paddingX * 2.0F, 1.0F))
                  .radius(new QuadRadiusState(0.5F))
                  .color(new QuadColorState(new Color(22, 22, 28, (int)(200.0F * this.masterAlpha))))
                  .build()
                  .render(baseMatrix, this.x + paddingX, divY);
            }

            float rowStartY = this.y + headerHeight + 3.5F;

            for (int i = 0; i < activeList.size(); i++) {
               ActiveBindsManager.BindData bind = activeList.get(i);
               float rowY = rowStartY + i * rowHeight;
               float rowCenterY = rowY + rowHeight / 2.0F;
               float totalRowAlpha = bind.progress * this.masterAlpha;
               Color rowWhite = GuiUtils.withAlpha(lightGrayText, totalRowAlpha);
               Color rowDim = GuiUtils.withAlpha(themeColor, totalRowAlpha);
               if (medium != null) {
                  Builder.text()
                     .font(medium)
                     .text(bind.name)
                     .color(rowWhite)
                     .size(moduleNameSize)
                     .build()
                     .render(baseMatrix, this.x + paddingX, rowCenterY - 2.5F);
               }

               if (medium != null) {
                  float keyW = medium.getWidth(bind.key, moduleNameSize);
                  float rightX = this.x + this.currentWidth - paddingX - keyW;
                  Builder.text().font(medium).text(bind.key).color(rowDim).size(moduleNameSize).build().render(baseMatrix, rightX, rowCenterY - 2.5F);
               }
            }

            context.pose().popMatrix();
         }
      }
   }

   private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
      int layers = 12;
      float maxSpread = 7.5F;

      for (int i = layers - 1; i >= 0; i--) {
         float progress = (float)i / layers;
         float spread = progress * maxSpread;
         float alphaFactor = (1.0F - progress) * (1.0F - progress);
         int alpha = (int)(102.0F * alphaFactor * masterAlpha);
         if (alpha > 0) {
            Builder.rectangle()
               .size(new SizeState(w + spread * 2.0F, h + spread * 2.0F))
               .radius(new QuadRadiusState(radius + spread))
               .color(new QuadColorState(new Color(0, 0, 0, alpha)))
               .build()
               .render(matrix, x - spread, y - spread + 0.8F);
         }
      }
   }

   private void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float radius, Color color, float masterAlpha) {
      if (!(masterAlpha <= 0.001F)) {
         int steps = 10;
         float maxSpread = 7.0F;
         int cr = color.getRed();
         int cg = color.getGreen();
         int cb = color.getBlue();

         for (int i = steps - 1; i >= 0; i--) {
            float progress = (float)i / steps;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0F - progress) * (1.0F - progress);
            int alpha = (int)(255.0F * alphaFactor * masterAlpha);
            if (alpha > 0) {
               float curR = radius + spread;
               Builder.rectangle()
                  .size(new SizeState(curR * 2.0F, curR * 2.0F))
                  .radius(new QuadRadiusState(curR))
                  .color(new QuadColorState(new Color(cr, cg, cb, alpha)))
                  .build()
                  .render(matrix, cx - curR, cy - curR);
            }
         }
      }
   }

   private float getScaleModifier() {
      Minecraft mc = Minecraft.getInstance();
      double currentGuiScale = mc.getWindow().getGuiScale();
      if (currentGuiScale <= 0.0) {
         currentGuiScale = 2.0;
      }

      return (float)(2.0 / currentGuiScale);
   }

   public boolean onMouseClicked(double mouseX, double mouseY, int button) {
      Minecraft mc = Minecraft.getInstance();
      boolean isEditing = mc.gui.screen() instanceof HudEditorScreen;
      InterfaceModule interfaceMod = (InterfaceModule)(
         AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Interface") : null
      );
      boolean isSettingEnabled = interfaceMod != null && interfaceMod.activeBinds.get();
      if (isEditing || interfaceMod != null && isSettingEnabled && interfaceMod.isEnabled()) {
         float scaleModifier = this.getScaleModifier();
         float effectiveW = this.currentWidth * scaleModifier;
         float effectiveH = this.currentHeight * scaleModifier;
         if (button == 0 && mouseX >= this.x && mouseX <= this.x + effectiveW && mouseY >= this.y && mouseY <= this.y + effectiveH) {
            this.dragging = true;
            this.dragOffsetX = (float)(mouseX - this.x);
            this.dragOffsetY = (float)(mouseY - this.y);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean onMouseDragged(double mouseX, double mouseY, int button) {
      if (this.dragging && button == 0) {
         Minecraft mc = Minecraft.getInstance();
         float scaleModifier = this.getScaleModifier();
         float screenW = mc.getWindow().getGuiScaledWidth();
         float screenH = mc.getWindow().getGuiScaledHeight();
         float effectiveW = this.currentWidth * scaleModifier;
         float effectiveH = this.currentHeight * scaleModifier;
         float targetX = (float)(mouseX - this.dragOffsetX);
         float targetY = (float)(mouseY - this.dragOffsetY);
         this.x = Math.max(0.0F, Math.min(Math.max(0.0F, screenW - effectiveW), targetX));
         this.y = Math.max(0.0F, Math.min(Math.max(0.0F, screenH - effectiveH), targetY));
         return true;
      } else {
         return false;
      }
   }

   public boolean onMouseReleased(double mouseX, double mouseY, int button) {
      if (this.dragging && button == 0) {
         this.dragging = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean onMouseReleased(int button) {
      if (this.dragging && button == 0) {
         this.dragging = false;
         return true;
      } else {
         return false;
      }
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public void setX(float x) {
      this.x = x;
   }

   public void setY(float y) {
      this.y = y;
   }

   public float getWidth() {
      return this.currentWidth;
   }

   public float getHeight() {
      return this.currentHeight;
   }

   @Environment(EnvType.CLIENT)
   private static class BindData {
      String name;
      String key;
      float progress;

      BindData(String name, String key, float progress) {
         this.name = name;
         this.key = key;
         this.progress = progress;
      }
   }
}
