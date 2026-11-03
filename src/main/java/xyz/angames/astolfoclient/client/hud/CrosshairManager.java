package xyz.angames.astolfoclient.client.hud;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.CrosshairModule;

@Environment(value=EnvType.CLIENT)
public class CrosshairManager {
    private final Minecraft client = Minecraft.getInstance();
    private final Color entityColor = new Color(255, 50, 50);

    public void render(GuiGraphicsExtractor context) {
        if (this.client.player == null || this.client.options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        CrosshairModule module = (CrosshairModule)AstolfoclientClient.moduleManager.getModuleByName("Crosshair");
        if (module == null || !module.isEnabled()) {
            return;
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        float x = (float)this.client.getWindow().getGuiScaledWidth() / 2.0f;
        float y = (float)this.client.getWindow().getGuiScaledHeight() / 2.0f;
        float gap = module.getGap();
        if (module.hasDynamicGap()) {
            float cooldown = 1.0f - this.client.player.getAttackStrengthScale(0.0f);
            gap += 8.0f * cooldown * cooldown;
        }
        float thickness = module.getThickness();
        float length = module.getLength();
        Color color = module.usesEntityColor() && this.client.hitResult != null && this.client.hitResult.getType() == HitResult.Type.ENTITY ? this.entityColor : Color.WHITE;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(thickness, length)).color(new QuadColorState(color)).build()).render(matrix, x - thickness / 2.0f, y - gap - length);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(thickness, length)).color(new QuadColorState(color)).build()).render(matrix, x - thickness / 2.0f, y + gap);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(length, thickness)).color(new QuadColorState(color)).build()).render(matrix, x - gap - length, y - thickness / 2.0f);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(length, thickness)).color(new QuadColorState(color)).build()).render(matrix, x + gap, y - thickness / 2.0f);
    }
}

