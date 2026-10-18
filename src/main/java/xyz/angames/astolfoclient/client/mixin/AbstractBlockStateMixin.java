package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;

@Environment(EnvType.CLIENT)
@Mixin(BlockStateBase.class)
public abstract class AbstractBlockStateMixin {
   @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
   private void onGetRenderType(CallbackInfoReturnable<RenderShape> cir) {
      NoRenderModule noRender = NoRenderModule.getInstance();
      if (noRender != null && noRender.isEnabled() && noRender.grass.get() && NoRenderModule.isGrass((BlockState)(Object)this)) {
         cir.setReturnValue(RenderShape.INVISIBLE);
      }
   }
}
