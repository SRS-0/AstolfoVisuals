package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={AbstractBlock.AbstractBlockState.class})
public abstract class AbstractBlockStateMixin {
    @Inject(method={"getRenderType"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetRenderType(CallbackInfoReturnable<BlockRenderType> cir) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.grass.get() && NoRenderModule.isGrass((BlockState)(Object)this)) {
            cir.setReturnValue(BlockRenderType.INVISIBLE);
        }
    }
}

