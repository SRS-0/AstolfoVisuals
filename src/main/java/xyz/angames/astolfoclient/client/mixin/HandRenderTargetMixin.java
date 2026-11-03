package xyz.angames.astolfoclient.client.mixin;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.render.ShaderHand;
@Mixin(GameRenderer.class)
public abstract class HandRenderTargetMixin {
 private static final String PASS="Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;";
 @Inject(method="renderItemInHand",at=@At(value="INVOKE",target=PASS))
 private void astolfo$begin(CallbackInfo ci){var mod=ShaderHand.getInstance();if(mod!=null&&mod.shouldRender())mod.beginRender();}
 @ModifyArg(method="renderItemInHand",at=@At(value="INVOKE",target=PASS),index=1)
 private GpuTextureView astolfo$color(GpuTextureView original){return ShaderHand.rendering?ShaderHand.getInstance().getHandsBuffer().getColorTextureView():original;}
 @ModifyArg(method="renderItemInHand",at=@At(value="INVOKE",target=PASS),index=3)
 private GpuTextureView astolfo$depth(GpuTextureView original){return ShaderHand.rendering?ShaderHand.getInstance().getHandsBuffer().getDepthTextureView():original;}
 @Inject(method="renderItemInHand",at=@At("RETURN"))
 private void astolfo$composite(CallbackInfo ci){var mod=ShaderHand.getInstance();if(mod!=null&&ShaderHand.rendering){mod.restoreTarget();ShaderHand.rendering=false;mod.draw();}}
}
