package xyz.angames.astolfoclient.client.gui.clickgui;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltBlur;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;

@Environment(value=EnvType.CLIENT)
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
        this.width = 130.0f;
        this.height = 40.0f;
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (this.isDragging) {
            this.x = (float)mouseX - this.dragOffsetX;
            this.y = (float)mouseY - this.dragOffsetY;
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        ((BuiltBlur)Builder.blur().size(new SizeState(this.width, this.height)).radius(new QuadRadiusState(6.0f)).blurRadius(15.0f).color(new QuadColorState(new Color(0, 0, 0, 240))).build()).render(matrix, this.x, this.y);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(this.width, this.height)).radius(new QuadRadiusState(6.0f)).color(new QuadColorState(new Color(16, 16, 16, 240))).build()).render(matrix, this.x, this.y);
        ((BuiltText)Builder.text().font((MsdfFont)FONT.get()).text("Theme").color(Color.WHITE).size(12.0f).build()).render(matrix, this.x + 5.0f, this.y + 5.0f);
        Color themeCol = ThemeManager.getThemeColor();
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(12.0f, 12.0f)).radius(new QuadRadiusState(6.0f)).color(new QuadColorState(themeCol)).build()).render(matrix, this.x + 10.0f, this.y + 20.0f);
        ((BuiltText)Builder.text().font((MsdfFont)FONT.get()).text(ThemeManager.getCustomColor1Hex()).color(Color.WHITE).size(10.0f).build()).render(matrix, this.x + 28.0f, this.y + 21.0f);
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= (double)this.x && mouseX <= (double)(this.x + this.width) && mouseY >= (double)this.y && mouseY <= (double)(this.y + 20.0f) && button == 0) {
            this.isDragging = true;
            this.dragOffsetX = (float)(mouseX - (double)this.x);
            this.dragOffsetY = (float)(mouseY - (double)this.y);
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
    }
}

