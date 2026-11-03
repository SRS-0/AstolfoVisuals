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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
public class ScoreboardManager {
    public float x = 0.0f;
    public float y = 0.0f;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
    private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
    private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());

    public void render(DrawContext context) {
        float titleWidth;
        Module module = AstolfoclientClient.moduleManager.getModuleByName("Scoreboard");
        if (module == null || !module.isEnabled()) {
            return;
        }
        if (this.client.world == null || this.client.player == null) {
            return;
        }
        Scoreboard scoreboard = this.client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return;
        }
        ArrayList<String> lines = new ArrayList<String>();
        Collection<ScoreboardEntry> scores = scoreboard.getScoreboardEntries(objective);
        List<ScoreboardEntry> list = scores.stream().filter(score -> score.owner() != null && !score.owner().startsWith("#")).sorted((s1, s2) -> Integer.compare(s2.value(), s1.value())).limit(15L).collect(Collectors.toList());
        String title = objective.getDisplayName().getString();
        for (ScoreboardEntry score2 : list) {
            Team team = scoreboard.getScoreHolderTeam(score2.owner());
            MutableText text = Team.decorateName((AbstractTeam)team, (Text)Text.literal((String)score2.owner()));
            lines.add(text.getString());
        }
        MsdfFont bold = (MsdfFont)BOLD_FONT.get();
        MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
        MsdfFont medium = (MsdfFont)MEDIUM_FONT.get();
        float maxWidth = titleWidth = bold != null ? bold.getWidth(title, 8.5f) : 40.0f;
        for (String line : lines) {
            float f = semibold != null ? semibold.getWidth(line, 7.5f) : 30.0f;
            float w = f;
            if (!(w > maxWidth)) continue;
            maxWidth = w;
        }
        float padding = 7.5f;
        float width = maxWidth + padding * 2.0f;
        float headerHeight = 18.0f;
        float lineHeight = 11.0f;
        float totalHeight = headerHeight + (float)lines.size() * lineHeight + 6.0f;
        double currentGuiScale = this.client.getWindow().getScaleFactor();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        float scaleModifier = (float)(2.0 / currentGuiScale);
        this.x = (float)this.client.getWindow().getScaledWidth() - width * scaleModifier - 6.0f;
        this.y = ((float)this.client.getWindow().getScaledHeight() - totalHeight * scaleModifier) / 2.0f;
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.x, this.y);
        context.getMatrices().scale(scaleModifier, scaleModifier);
        context.getMatrices().translate(-this.x, -this.y);
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        long now = System.currentTimeMillis();
        Color themeColor = new Color(ThemeManager.getThemedColor(now / 10L));
        this.renderShadow(matrix, this.x, this.y, width, totalHeight, 6.5f, 1.0f);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(width, totalHeight)).radius(new QuadRadiusState(6.5f)).color(new QuadColorState(new Color(0, 0, 0, 255))).build()).render(matrix, this.x, this.y);
        if (bold != null) {
            float titleX = this.x + width / 2.0f - titleWidth / 2.0f;
            ((BuiltText)Builder.text().font(bold).text(title).color(Color.WHITE).size(8.5f).build()).render(matrix, titleX, this.y + 4.5f);
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(width - 12.0f, 1.0f)).color(new QuadColorState(new Color(25, 25, 30, 200))).build()).render(matrix, this.x + 6.0f, this.y + 15.5f);
        float currentY = this.y + headerHeight + 2.0f;
        for (String line : lines) {
            if (semibold != null) {
                ((BuiltText)Builder.text().font(semibold).text(line).color(new Color(200, 200, 205)).size(7.5f).build()).render(matrix, this.x + padding, currentY);
            }
            currentY += lineHeight;
        }
        context.getMatrices().popMatrix();
    }

    private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
        int layers = 12;
        float maxSpread = 8.0f;
        for (int i = layers - 1; i >= 0; --i) {
            float progress = (float)i / (float)layers;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0f - progress) * (1.0f - progress);
            int alpha = (int)(102.0f * alphaFactor * masterAlpha);
            if (alpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(w + spread * 2.0f, h + spread * 2.0f)).radius(new QuadRadiusState(radius + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, x - spread, y - spread + 1.0f);
        }
    }
}

