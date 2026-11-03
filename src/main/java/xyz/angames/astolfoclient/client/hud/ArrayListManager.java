package xyz.angames.astolfoclient.client.hud;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
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

@Environment(value=EnvType.CLIENT)
public class ArrayListManager {
    private final Minecraft client = Minecraft.getInstance();
    private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());

    public void render(GuiGraphicsExtractor context) {
        MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
        if (semibold == null) {
            return;
        }
        double currentGuiScale = this.client.getWindow().getGuiScale();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        float scaleModifier = (float)(2.0 / currentGuiScale);
        context.pose().pushMatrix();
        context.pose().scale(scaleModifier, scaleModifier);
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        float guiWidth = (float)this.client.getWindow().getGuiScaledWidth() / scaleModifier;
        List<Module> enabledModules = AstolfoclientClient.moduleManager.getModules().stream().filter(Module::isEnabled).filter(module -> !(module instanceof InterfaceModule)).sorted(Comparator.comparingDouble((Module m) -> semibold.getWidth(m.getName(), 7.5f)).reversed()).collect(Collectors.toList());
        float y = 3.0f;
        long timeOffset = 0L;
        float textSize = 7.5f;
        float height = 14.0f;
        float paddingX = 5.5f;
        for (Module module2 : enabledModules) {
            String name = module2.getName();
            float textW = semibold.getWidth(name, textSize);
            float pillW = paddingX + textW + paddingX + 2.0f;
            float pillX = guiWidth - pillW - 3.0f;
            Color accentColor = new Color(ThemeManager.getThemedColor(timeOffset));
            for (int i = 5; i > 0; --i) {
                float progress = (float)i / 5.0f;
                float spread = progress * 3.5f;
                int alpha = (int)(60.0f * (1.0f - progress) * (1.0f - progress));
                if (alpha <= 0) continue;
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(pillW + spread * 2.0f, height + spread * 2.0f)).radius(new QuadRadiusState(4.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, pillX - spread, y - spread + 0.5f);
            }
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(pillW, height)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(new Color(0, 0, 0, 240))).build()).render(matrix, pillX, y);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(1.5f, height - 4.0f)).radius(new QuadRadiusState(0.75f)).color(new QuadColorState(accentColor)).build()).render(matrix, pillX + pillW - 2.5f, y + 2.0f);
            float textY = y + height / 2.0f - 2.7f;
            ((BuiltText)Builder.text().font(semibold).text(name).color(accentColor).size(textSize).build()).render(matrix, pillX + paddingX, textY);
            y += height + 2.5f;
            timeOffset += 180L;
        }
        context.pose().popMatrix();
    }
}

