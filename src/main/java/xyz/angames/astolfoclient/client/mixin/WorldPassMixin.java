package xyz.angames.astolfoclient.client.mixin;

import com.mojang.renderpearl.api.commands.*;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import java.util.*;
import java.util.function.Supplier;
import org.joml.Vector4fc;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import xyz.angames.astolfoclient.client.render.compat.WorldPass;

@Mixin(LevelRenderer.class)
public class WorldPassMixin {
    @Redirect(method="lambda$addMainPass$0",at=@At(value="INVOKE",target="Lcom/mojang/renderpearl/api/commands/CommandEncoder;createRenderPass(Ljava/util/function/Supplier;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/Optional;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Ljava/util/OptionalDouble;)Lcom/mojang/renderpearl/api/commands/RenderPass;"))
    private RenderPass astolfo$worldPass(CommandEncoder encoder,Supplier<String> label,GpuTextureView color,Optional<Vector4fc> clearColor,GpuTextureView depth,OptionalDouble clearDepth) {
        return WorldPass.wrap(encoder,label,color,clearColor,depth,clearDepth);
    }
}
