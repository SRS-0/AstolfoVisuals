package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;
import xyz.angames.astolfoclient.client.protection.NameLimiter;
@Mixin(EntityRenderer.class) public abstract class EntityRendererMixin {
 @Inject(method="renderLabelIfPresent",at=@At("HEAD"))
 private void astolfo$label(EntityRenderState state,MatrixStack matrices,OrderedRenderCommandQueue queue,CameraRenderState camera,CallbackInfo ci){if(state.displayName!=null)state.displayName=NameProtectModule.getProtectedText(NameLimiter.truncate(state.displayName));}
}
