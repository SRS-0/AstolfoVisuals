package xyz.angames.astolfoclient.client.mixin;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BufferBuilder.class)
public interface BufferBuilderGpuAccessor {
    @Accessor("vertexPointer") long astolfo$vertexPointer();
}
