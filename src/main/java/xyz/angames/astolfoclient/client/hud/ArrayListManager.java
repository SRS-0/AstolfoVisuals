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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(EnvType.CLIENT)
public class ArrayListManager {
   private final Minecraft client = Minecraft.getInstance();
   private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());

   public void render(GuiGraphicsExtractor context) {
      MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
      if (semibold != null) {
         double currentGuiScale = this.client.getWindow().getGuiScale();
         if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
         }

         float scaleModifier = (float)(2.0 / currentGuiScale);
         context.pose().pushMatrix();
         context.pose().scale(scaleModifier, scaleModifier);
         Matrix4f matrix = GuiCompat.matrix4f(context);
         float guiWidth = this.client.getWindow().getGuiScaledWidth() / scaleModifier;
         List<Module> enabledModules = AstolfoclientClient.moduleManager
            .getModules()
            .stream()
            .filter(Module::isEnabled)
            .filter(modulex -> !(modulex instanceof InterfaceModule))
            .sorted(Comparator.<Module>comparingDouble(m -> semibold.getWidth(m.getName(), 7.5F)).reversed())
            .collect(Collectors.toList());
         float y = 3.0F;
         long timeOffset = 0L;
         float textSize = 7.5F;
         float height = 14.0F;
         float paddingX = 5.5F;

         for (Module module : enabledModules) {
            String name = module.getName();
            float textW = semibold.getWidth(name, textSize);
            float pillW = paddingX + textW + paddingX + 2.0F;
            float pillX = guiWidth - pillW - 3.0F;
            Color accentColor = new Color(ThemeManager.getThemedColor(timeOffset));

            for (int i = 5; i > 0; i--) {
               float progress = i / 5.0F;
               float spread = progress * 3.5F;
               int alpha = (int)(60.0F * (1.0F - progress) * (1.0F - progress));
               if (alpha > 0) {
                  Builder.rectangle()
                     .size(new SizeState(pillW + spread * 2.0F, height + spread * 2.0F))
                     .radius(new QuadRadiusState(4.0F + spread))
                     .color(new QuadColorState(new Color(0, 0, 0, alpha)))
                     .build()
                     .render(matrix, pillX - spread, y - spread + 0.5F);
               }
            }

            Builder.rectangle()
               .size(new SizeState(pillW, height))
               .radius(new QuadRadiusState(4.0F))
               .color(new QuadColorState(new Color(0, 0, 0, 240)))
               .build()
               .render(matrix, pillX, y);
            Builder.rectangle()
               .size(new SizeState(1.5F, height - 4.0F))
               .radius(new QuadRadiusState(0.75F))
               .color(new QuadColorState(accentColor))
               .build()
               .render(matrix, pillX + pillW - 2.5F, y + 2.0F);
            float textY = y + height / 2.0F - 2.7F;
            Builder.text().font(semibold).text(name).color(accentColor).size(textSize).build().render(matrix, pillX + paddingX, textY);
            y += height + 2.5F;
            timeOffset += 180L;
         }

         context.pose().popMatrix();
      }
   }
}
