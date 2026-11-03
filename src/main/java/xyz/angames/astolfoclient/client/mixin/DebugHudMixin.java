package xyz.angames.astolfoclient.client.mixin;
import java.util.List;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.angames.astolfoclient.client.module.modules.misc.CoordsHiderModule;
@Mixin(DebugHud.class) public class DebugHudMixin {
 @ModifyVariable(method="drawText",at=@At("HEAD"),argsOnly=true)
 private List<String> astolfo$hideCoordinates(List<String> lines){return CoordsHiderModule.isF3Hidden()?lines.stream().map(CoordsHiderModule::filterF3Line).toList():lines;}
}
