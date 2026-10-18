package xyz.angames.astolfoclient.client.gui.clickgui;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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

@Environment(EnvType.CLIENT)
public class ThemeSelectorPanel {
   public float x;
   public float y;
   public float width;
   public float height;
   private boolean isDragging = false;
   private float dragOffsetX;
   private float dragOffsetY;
   private static final Supplier<MsdfFont> FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());

   public ThemeSelectorPanel(float x, float y) {
      this.x = x;
      this.y = y;
      this.width = 130.0F;
      this.height = 40.0F;
   }

   public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      if (this.isDragging) {
         this.x = mouseX - this.dragOffsetX;
         this.y = mouseY - this.dragOffsetY;
      }

      Matrix4f matrix = GuiCompat.matrix4f(context);
      Builder.blur()
         .size(new SizeState(this.width, this.height))
         .radius(new QuadRadiusState(6.0F))
         .blurRadius(15.0F)
         .color(new QuadColorState(new Color(0, 0, 0, 240)))
         .build()
         .render(matrix, this.x, this.y);
      Builder.rectangle()
         .size(new SizeState(this.width, this.height))
         .radius(new QuadRadiusState(6.0F))
         .color(new QuadColorState(new Color(16, 16, 16, 240)))
         .build()
         .render(matrix, this.x, this.y);
      Builder.text().font((MsdfFont)FONT.get()).text("Theme").color(Color.WHITE).size(12.0F).build().render(matrix, this.x + 5.0F, this.y + 5.0F);
      Color themeCol = ThemeManager.getThemeColor();
      Builder.rectangle()
         .size(new SizeState(12.0F, 12.0F))
         .radius(new QuadRadiusState(6.0F))
         .color(new QuadColorState(themeCol))
         .build()
         .render(matrix, this.x + 10.0F, this.y + 20.0F);
      Builder.text()
         .font((MsdfFont)FONT.get())
         .text(ThemeManager.getCustomColor1Hex())
         .color(Color.WHITE)
         .size(10.0F)
         .build()
         .render(matrix, this.x + 28.0F, this.y + 21.0F);
   }

   public void mouseClicked(double mouseX, double mouseY, int button) {
      if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + 20.0F && button == 0) {
         this.isDragging = true;
         this.dragOffsetX = (float)(mouseX - this.x);
         this.dragOffsetY = (float)(mouseY - this.y);
      }
   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.isDragging = false;
      }
   }
}
