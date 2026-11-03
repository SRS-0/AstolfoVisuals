package xyz.angames.astolfoclient.client.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;
@Mixin(AvatarRenderer.class) public class PlayerEntityRendererMixin {
 @Inject(method="submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("HEAD"))
 private void astolfo$protectName(AvatarRenderState state,PoseStack matrices,SubmitNodeCollector queue,CameraRenderState camera,CallbackInfo ci) {if(state.nameTag!=null)state.nameTag=NameProtectModule.getProtectedText(state.nameTag);}
}
