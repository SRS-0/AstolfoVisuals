package xyz.angames.astolfoclient.client.gui.clickgui;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(EnvType.CLIENT)
public class ColorPickerComponent {
   private final String title;
   public float x;
   public float y;
   public float width;
   public float height;
   private float hue = 0.0F;
   private float sat = 1.0F;
   private float bri = 1.0F;
   private float animHue = 0.0F;
   private float animSat = 1.0F;
   private float animBri = 1.0F;
   private boolean draggingPad = false;
   private boolean draggingHue = false;

   public ColorPickerComponent(String title) {
      this.title = title;
   }

   public void loadColor() {
      Color current = ThemeManager.getThemeColor();
      if (current == null) {
         current = new Color(16711680);
      }

      float[] hsb = Color.RGBtoHSB(current.getRed(), current.getGreen(), current.getBlue(), null);
      this.hue = hsb[0];
      this.sat = hsb[1];
      this.bri = hsb[2];
      this.animHue = this.hue;
      this.animSat = this.sat;
      this.animBri = this.bri;
   }

   public void animate(float deltaTime) {
      this.animHue = GuiUtils.animate(this.animHue, this.hue, 20.0F, deltaTime);
      this.animSat = GuiUtils.animate(this.animSat, this.sat, 20.0F, deltaTime);
      this.animBri = GuiUtils.animate(this.animBri, this.bri, 20.0F, deltaTime);
   }

   public void render(Matrix4f matrix, int mouseX, int mouseY, float alpha) {
      float padX = this.x + 6.0F;
      float padY = this.y + 6.0F;
      float padW = this.width - 12.0F;
      float padH = this.height - 25.0F;
      float hueBarX = padX;
      float hueBarY = padY + padH + 5.0F;
      float hueBarW = padW;
      float hueBarH = 8.0F;
      Color pureHue = Color.getHSBColor(this.animHue, 1.0F, 1.0F);
      Color cPureHue = GuiUtils.withAlpha(pureHue, alpha);
      Color cWhite = GuiUtils.withAlpha(Color.WHITE, alpha);
      Builder.rectangle()
         .size(new SizeState(padW, padH))
         .radius(new QuadRadiusState(5.0F))
         .color(new QuadColorState(cWhite, cWhite, cPureHue, cPureHue))
         .build()
         .render(matrix, padX, padY);
      Color cBlack = GuiUtils.withAlpha(Color.BLACK, alpha);
      Color cBlackTrans = new Color(0, 0, 0, 0);
      Builder.rectangle()
         .size(new SizeState(padW, padH))
         .radius(new QuadRadiusState(5.0F))
         .color(new QuadColorState(cBlackTrans, cBlack, cBlack, cBlackTrans))
         .build()
         .render(matrix, padX, padY);
      float curX = padX + Mth.clamp(this.animSat, 0.0F, 1.0F) * padW;
      float curY = padY + (1.0F - Mth.clamp(this.animBri, 0.0F, 1.0F)) * padH;
      Builder.border()
         .size(new SizeState(8.5F, 8.5F))
         .radius(new QuadRadiusState(4.25F))
         .thickness(1.6F)
         .color(new QuadColorState(GuiUtils.withAlpha(new Color(0, 0, 0, 160), alpha)))
         .build()
         .render(matrix, curX - 4.25F, curY - 4.25F);
      Builder.border()
         .size(new SizeState(7.0F, 7.0F))
         .radius(new QuadRadiusState(3.5F))
         .thickness(1.4F)
         .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
         .build()
         .render(matrix, curX - 3.5F, curY - 3.5F);
      int slices = 6;
      float sliceW = hueBarW / slices;

      for (int i = 0; i < slices; i++) {
         float h1 = (float)i / slices;
         float h2 = (float)(i + 1) / slices;
         Color c1 = GuiUtils.withAlpha(Color.getHSBColor(h1, 1.0F, 1.0F), alpha);
         Color c2 = GuiUtils.withAlpha(Color.getHSBColor(h2, 1.0F, 1.0F), alpha);
         QuadRadiusState rad = QuadRadiusState.NO_ROUND;
         if (i == 0) {
            rad = new QuadRadiusState(4.0F, 4.0F, 0.0F, 0.0F);
         } else if (i == slices - 1) {
            rad = new QuadRadiusState(0.0F, 0.0F, 4.0F, 4.0F);
         }

         Builder.rectangle()
            .size(new SizeState(sliceW + 0.5F, hueBarH))
            .radius(rad)
            .smoothness(0.0F)
            .color(new QuadColorState(c1, c1, c2, c2))
            .build()
            .render(matrix, hueBarX + i * sliceW, hueBarY);
      }

      float hueHandleX = hueBarX + Mth.clamp(this.animHue, 0.0F, 1.0F) * hueBarW;
      float hueHandleY = hueBarY + hueBarH / 2.0F;
      Builder.border()
         .size(new SizeState(8.5F, 8.5F))
         .radius(new QuadRadiusState(4.25F))
         .thickness(1.6F)
         .color(new QuadColorState(GuiUtils.withAlpha(new Color(0, 0, 0, 160), alpha)))
         .build()
         .render(matrix, hueHandleX - 4.25F, hueHandleY - 4.25F);
      Builder.border()
         .size(new SizeState(7.0F, 7.0F))
         .radius(new QuadRadiusState(3.5F))
         .thickness(1.4F)
         .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
         .build()
         .render(matrix, hueHandleX - 3.5F, hueHandleY - 3.5F);
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      float padX = this.x + 6.0F;
      float padY = this.y + 6.0F;
      float padH = this.height - 25.0F;
      float hueBarY = padY + padH + 5.0F;
      if (GuiUtils.isMouseOver((float)mx, (float)my, this.x, this.y, this.width, this.height)) {
         if (my >= hueBarY - 2.5F) {
            this.draggingHue = true;
            this.draggingPad = false;
         } else {
            this.draggingPad = true;
            this.draggingHue = false;
         }

         ModSounds.playSliderMove();
         this.update(mx, my);
         return true;
      } else {
         return false;
      }
   }

   public boolean isDragging() {
      return this.draggingPad || this.draggingHue;
   }

   public void update(double mx, double my) {
      float padX = this.x + 6.0F;
      float padY = this.y + 6.0F;
      float padW = this.width - 12.0F;
      float padH = this.height - 25.0F;
      float hueBarX = padX;
      float hueBarW = padW;
      if (this.draggingPad) {
         this.sat = Mth.clamp((float)(mx - padX) / padW, 0.0F, 1.0F);
         this.bri = 1.0F - Mth.clamp((float)(my - padY) / padH, 0.0F, 1.0F);
         this.updateTheme();
      }

      if (this.draggingHue) {
         this.hue = Mth.clamp((float)(mx - hueBarX) / hueBarW, 0.0F, 1.0F);
         this.updateTheme();
      }
   }

   public void mouseReleased() {
      this.draggingPad = false;
      this.draggingHue = false;
   }

   private void updateTheme() {
      Color c = Color.getHSBColor(this.hue, this.sat, this.bri);
      ThemeManager.setThemeColor(c);
   }
}
