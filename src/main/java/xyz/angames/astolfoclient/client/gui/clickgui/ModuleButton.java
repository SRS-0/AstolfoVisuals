package xyz.angames.astolfoclient.client.gui.clickgui;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import com.google.common.base.Supplier;
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
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.util.KeyUtils;

@Environment(EnvType.CLIENT)
public class ModuleButton {
   public Module module;
   public float x;
   public float y;
   public float width;
   public float height;
   public boolean isVisible = false;
   public boolean isBinding = false;
   public float renderX = -9999.0F;
   public float renderY = -9999.0F;
   public float renderAlpha = 0.0F;
   public float renderScale = 0.94F;
   private float toggleAnim = 0.0F;
   private float hoverAnim = 0.0F;
   public final UniversalSettingsPanel settingsPanel;
   public static final float HEADER_HEIGHT = 24.0F;
   private static final Supplier<MsdfFont> SEMIBOLD_FONT = ClickGuiIcons.SEMIBOLD_FONT;
   private static final Supplier<MsdfFont> MEDIUM_FONT = ClickGuiIcons.MEDIUM_FONT;
   private static final Color C_CARD_BG = new Color(6, 6, 8, 255);
   private static final Color C_CARD_BG_HOV = new Color(10, 10, 14, 255);
   private static final Color C_SWITCH_OFF = new Color(20, 20, 26, 255);

   public ModuleButton(Module module, float width) {
      this.module = module;
      this.width = width;
      this.settingsPanel = new UniversalSettingsPanel(module);
      this.toggleAnim = module.isEnabled() ? 1.0F : 0.0F;
      this.height = 24.0F;
   }

   public float calculateHeight() {
      this.settingsPanel.updateRows();
      return 24.0F + this.settingsPanel.getTotalHeight();
   }

   public void render(GuiGraphicsExtractor context, ClickGuiScreen parentGui, int mouseX, int mouseY, float alpha, float deltaTime) {
      float effectiveAlpha = alpha * this.renderAlpha;
      if (!(effectiveAlpha <= 0.02F)) {
         this.settingsPanel.updateRows();
         float settingsHeight = this.settingsPanel.getTotalHeight();
         this.height = 24.0F + settingsHeight;
         boolean en = this.module.isEnabled();
         this.toggleAnim = GuiUtils.animate(this.toggleAnim, en ? 1.0F : 0.0F, 16.0F, deltaTime);
         boolean hov = GuiUtils.isMouseOver(mouseX, mouseY, this.x, this.y, this.width, 24.0F);
         this.hoverAnim = GuiUtils.animate(this.hoverAnim, hov ? 1.0F : 0.0F, 14.0F, deltaTime);
         Color themeColor = new Color(ThemeManager.getThemedColor(0L));
         context.pose().pushMatrix();
         if (this.renderScale < 0.999F) {
            float cx = this.x + this.width / 2.0F;
            float cy = this.y + this.height / 2.0F;
            context.pose().translate(cx, cy);
            context.pose().scale(this.renderScale, this.renderScale);
            context.pose().translate(-cx, -cy);
         }

         Matrix4f mx = GuiCompat.matrix4f(context);
         Color cardBg = GuiUtils.interpolateColor(C_CARD_BG, C_CARD_BG_HOV, this.hoverAnim);
         Builder.rectangle()
            .size(new SizeState(this.width, this.height))
            .radius(new QuadRadiusState(7.0F))
            .color(new QuadColorState(GuiUtils.withAlpha(cardBg, effectiveAlpha)))
            .build()
            .render(mx, this.x, this.y);
         float cardCenterY = this.y + 12.0F;
         float nameY = cardCenterY - 3.5F;
         GuiUtils.renderTextSafely(mx, this.module.getName(), this.x + 10.0F, nameY, GuiUtils.withAlpha(Color.WHITE, effectiveAlpha), 9.5F);
         float nameW = ((MsdfFont)SEMIBOLD_FONT.get()).getWidth(this.module.getName(), 9.5F);
         String keyText = "";
         if (this.isBinding) {
            keyText = "[...]";
         } else if (this.module.getKeyCode() != -1) {
            String kn = KeyUtils.getKeyName(this.module.getKeyCode());
            if (kn != null && !kn.isEmpty() && !kn.equalsIgnoreCase("UNKNOWN")) {
               keyText = "[" + kn + "]";
            }
         }

         if (!keyText.isEmpty()) {
            Color keyCol = this.isBinding ? new Color(255, 180, 50) : new Color(130, 130, 150);
            float keyX = this.x + 10.0F + nameW + 5.0F;
            GuiUtils.renderTextSafely(mx, keyText, keyX, nameY + 0.5F, GuiUtils.withAlpha(keyCol, effectiveAlpha), 8.0F);
         }

         if (ModuleManager.isBeta(this.module)) {
            float badgeX = this.x + 10.0F + nameW + (keyText.isEmpty() ? 0.0F : ((MsdfFont)MEDIUM_FONT.get()).getWidth(keyText, 8.0F) + 5.0F) + 5.0F;
            float betaW = 20.0F;
            float betaH = 9.0F;
            Builder.rectangle()
               .size(new SizeState(betaW, betaH))
               .radius(new QuadRadiusState(2.0F))
               .color(new QuadColorState(GuiUtils.withAlpha(new Color(255, 180, 0), effectiveAlpha * 0.85F)))
               .build()
               .render(mx, badgeX, cardCenterY - 4.5F);
            GuiUtils.renderTextSafely(mx, "BETA", badgeX + 2.5F, cardCenterY - 3.5F, GuiUtils.withAlpha(Color.BLACK, effectiveAlpha), 6.0F);
         }

         float switchW = 22.0F;
         float switchH = 11.0F;
         float switchX = this.x + this.width - switchW - 10.0F;
         float switchY = cardCenterY - 5.5F;
         Color switchTrackColor = GuiUtils.interpolateColor(C_SWITCH_OFF, themeColor, this.toggleAnim);
         Builder.rectangle()
            .size(new SizeState(switchW, switchH))
            .radius(new QuadRadiusState(5.5F))
            .color(new QuadColorState(GuiUtils.withAlpha(switchTrackColor, effectiveAlpha)))
            .build()
            .render(mx, switchX, switchY);
         float knobSize = 8.0F;
         float knobX = switchX + 1.5F + (switchW - knobSize - 3.0F) * this.toggleAnim;
         float knobY = switchY + 1.5F;
         Builder.rectangle()
            .size(new SizeState(knobSize, knobSize))
            .radius(new QuadRadiusState(4.0F))
            .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, effectiveAlpha)))
            .build()
            .render(mx, knobX, knobY);
         if (settingsHeight > 0.0F) {
            this.settingsPanel.render(context, this.x, this.y + 24.0F, this.width, effectiveAlpha, mouseX, mouseY, deltaTime);
         }

         context.pose().popMatrix();
      }
   }

   public boolean mouseClicked(float mouseX, float mouseY, int button) {
      if (GuiUtils.isMouseOver(mouseX, mouseY, this.x, this.y, this.width, 24.0F)) {
         if (button == 0) {
            this.module.toggle();
            return true;
         }

         if (button == 1 || button == 2) {
            this.isBinding = !this.isBinding;
            return true;
         }
      }

      return this.settingsPanel.hasSettings() && GuiUtils.isMouseOver(mouseX, mouseY, this.x, this.y + 24.0F, this.width, this.settingsPanel.getTotalHeight())
         ? this.settingsPanel.mouseClicked(mouseX, mouseY, button, this.x, this.y + 24.0F, this.width)
         : false;
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      return this.settingsPanel.hasSettings() ? this.settingsPanel.mouseDragged(mouseX, mouseY, this.x, this.width) : false;
   }

   public void mouseReleased() {
      if (this.settingsPanel.hasSettings()) {
         this.settingsPanel.mouseReleased();
      }
   }

   public void keyPressed(int key) {
      if (this.settingsPanel.hasSettings()) {
         this.settingsPanel.keyPressed(key);
      }
   }
}
