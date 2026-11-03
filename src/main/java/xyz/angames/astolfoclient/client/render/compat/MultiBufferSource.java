package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Routes recovered custom-model geometry into a submit callback's native vertex consumer. */
@FunctionalInterface
public interface MultiBufferSource {
    VertexConsumer getBuffer(RenderType type);
}
