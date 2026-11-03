package xyz.angames.astolfoclient.client.mixin;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(BufferBuilder.class)
public interface BufferBuilderGpuAccessor {
    @Invoker("beginElement") long astolfo$beginElement(VertexFormatElement element);
}
