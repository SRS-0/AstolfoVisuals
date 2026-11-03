package xyz.angames.astolfoclient.client.gui.clickgui;

import com.google.common.base.Supplier;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.ClickGuiIcons;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.gui.clickgui.UniversalSettingsPanel;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.util.KeyUtils;

@Environment(value=EnvType.CLIENT)
public class ModuleButton {
    public Module module;
    public float x;
    public float y;
    public float width;
    public float height;
    public boolean isVisible = false;
    public boolean isBinding = false;
    public float renderX = -9999.0f;
    public float renderY = -9999.0f;
    public float renderAlpha = 0.0f;
    public float renderScale = 0.94f;
    private float toggleAnim = 0.0f;
    private float hoverAnim = 0.0f;
    public final UniversalSettingsPanel settingsPanel;
    public static final float HEADER_HEIGHT = 24.0f;
    private static final Supplier<MsdfFont> SEMIBOLD_FONT = ClickGuiIcons.SEMIBOLD_FONT;
    private static final Supplier<MsdfFont> MEDIUM_FONT = ClickGuiIcons.MEDIUM_FONT;
    private static final Color C_CARD_BG = new Color(6, 6, 8, 255);
    private static final Color C_CARD_BG_HOV = new Color(10, 10, 14, 255);
    private static final Color C_SWITCH_OFF = new Color(20, 20, 26, 255);

    public ModuleButton(Module module, float width) {
        this.module = module;
        this.width = width;
        this.settingsPanel = new UniversalSettingsPanel(module);
        this.toggleAnim = module.isEnabled() ? 1.0f : 0.0f;
        this.height = 24.0f;
    }

    public float calculateHeight() {
        this.settingsPanel.updateRows();
        return 24.0f + this.settingsPanel.getTotalHeight();
    }

    public void render(GuiGraphicsExtractor context, ClickGuiScreen parentGui, int mouseX, int mouseY, float alpha, float deltaTime) {
        String kn;
        float effectiveAlpha = alpha * this.renderAlpha;
        if (effectiveAlpha <= 0.02f) {
            return;
        }
        this.settingsPanel.updateRows();
        float settingsHeight = this.settingsPanel.getTotalHeight();
        this.height = 24.0f + settingsHeight;
        boolean en = this.module.isEnabled();
        this.toggleAnim = GuiUtils.animate(this.toggleAnim, en ? 1.0f : 0.0f, 16.0f, deltaTime);
        boolean hov = GuiUtils.isMouseOver(mouseX, mouseY, this.x, this.y, this.width, 24.0f);
        this.hoverAnim = GuiUtils.animate(this.hoverAnim, hov ? 1.0f : 0.0f, 14.0f, deltaTime);
        Color themeColor = new Color(ThemeManager.getThemedColor(0L));
        context.pose().pushMatrix();
        if (this.renderScale < 0.999f) {
            float cx = this.x + this.width / 2.0f;
            float cy = this.y + this.height / 2.0f;
            context.pose().translate(cx, cy);
            context.pose().scale(this.renderScale, this.renderScale);
            context.pose().translate(-cx, -cy);
        }
        Matrix4f mx = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        Color cardBg = GuiUtils.interpolateColor(C_CARD_BG, C_CARD_BG_HOV, this.hoverAnim);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(this.width, this.height)).radius(new QuadRadiusState(7.0f)).color(new QuadColorState(GuiUtils.withAlpha(cardBg, effectiveAlpha))).build()).render(mx, this.x, this.y);
        float cardCenterY = this.y + 12.0f;
        float nameY = cardCenterY - 3.5f;
        GuiUtils.renderTextSafely(mx, this.module.getName(), this.x + 10.0f, nameY, GuiUtils.withAlpha(Color.WHITE, effectiveAlpha), 9.5f);
        float nameW = ((MsdfFont)SEMIBOLD_FONT.get()).getWidth(this.module.getName(), 9.5f);
        Object keyText = "";
        if (this.isBinding) {
            keyText = "[...]";
        } else if (this.module.getKeyCode() != -1 && (kn = KeyUtils.getKeyName(this.module.getKeyCode())) != null && !kn.isEmpty() && !kn.equalsIgnoreCase("UNKNOWN")) {
            keyText = "[" + kn + "]";
        }
        if (!((String)keyText).isEmpty()) {
            Color keyCol = this.isBinding ? new Color(255, 180, 50) : new Color(130, 130, 150);
            float keyX = this.x + 10.0f + nameW + 5.0f;
            GuiUtils.renderTextSafely(mx, (String)keyText, keyX, nameY + 0.5f, GuiUtils.withAlpha(keyCol, effectiveAlpha), 8.0f);
        }
        if (ModuleManager.isBeta(this.module)) {
            float badgeX = this.x + 10.0f + nameW + (((String)keyText).isEmpty() ? 0.0f : ((MsdfFont)MEDIUM_FONT.get()).getWidth((String)keyText, 8.0f) + 5.0f) + 5.0f;
            float betaW = 20.0f;
            float betaH = 9.0f;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(betaW, betaH)).radius(new QuadRadiusState(2.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(255, 180, 0), effectiveAlpha * 0.85f))).build()).render(mx, badgeX, cardCenterY - 4.5f);
            GuiUtils.renderTextSafely(mx, "BETA", badgeX + 2.5f, cardCenterY - 3.5f, GuiUtils.withAlpha(Color.BLACK, effectiveAlpha), 6.0f);
        }
        float switchW = 22.0f;
        float switchH = 11.0f;
        float switchX = this.x + this.width - switchW - 10.0f;
        float switchY = cardCenterY - 5.5f;
        Color switchTrackColor = GuiUtils.interpolateColor(C_SWITCH_OFF, themeColor, this.toggleAnim);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(switchW, switchH)).radius(new QuadRadiusState(5.5f)).color(new QuadColorState(GuiUtils.withAlpha(switchTrackColor, effectiveAlpha))).build()).render(mx, switchX, switchY);
        float knobSize = 8.0f;
        float knobX = switchX + 1.5f + (switchW - knobSize - 3.0f) * this.toggleAnim;
        float knobY = switchY + 1.5f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(knobSize, knobSize)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, effectiveAlpha))).build()).render(mx, knobX, knobY);
        if (settingsHeight > 0.0f) {
            this.settingsPanel.render(context, this.x, this.y + 24.0f, this.width, effectiveAlpha, mouseX, mouseY, deltaTime);
        }
        context.pose().popMatrix();
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (GuiUtils.isMouseOver(mouseX, mouseY, this.x, this.y, this.width, 24.0f)) {
            if (button == 0) {
                this.module.toggle();
                return true;
            }
            if (button == 1 || button == 2) {
                this.isBinding = !this.isBinding;
                return true;
            }
        }
        if (this.settingsPanel.hasSettings() && GuiUtils.isMouseOver(mouseX, mouseY, this.x, this.y + 24.0f, this.width, this.settingsPanel.getTotalHeight())) {
            return this.settingsPanel.mouseClicked(mouseX, mouseY, button, this.x, this.y + 24.0f, this.width);
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.settingsPanel.hasSettings()) {
            return this.settingsPanel.mouseDragged(mouseX, mouseY, this.x, this.width);
        }
        return false;
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

