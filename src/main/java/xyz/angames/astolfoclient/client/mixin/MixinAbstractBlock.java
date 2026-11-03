package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
@Mixin(value={AbstractBlock.class})
public class MixinAbstractBlock {
    @Inject(method={"getCollisionShape"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (AstolfoclientClient.moduleManager == null) {
            return;
        }
        Module noClip = AstolfoclientClient.moduleManager.getModuleByName("NoClip");
        if (noClip != null && noClip.isEnabled()) {
            cir.setReturnValue(VoxelShapes.empty());
        }
    }
}

