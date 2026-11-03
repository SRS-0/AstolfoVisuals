package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;
@Mixin(PlayerEntityRenderer.class) public class PlayerEntityRendererMixin {
 @Inject(method="renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",at=@At("HEAD"))
 private void astolfo$protectName(PlayerEntityRenderState state,MatrixStack matrices,OrderedRenderCommandQueue queue,CameraRenderState camera,CallbackInfo ci) {if(state.displayName!=null)state.displayName=NameProtectModule.getProtectedText(state.displayName);}
}
