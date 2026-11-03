package xyz.angames.astolfoclient.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import xyz.angames.astolfoclient.client.render.models.CowModel;
import xyz.angames.astolfoclient.client.render.models.RabbitModel;

@Environment(value=EnvType.CLIENT)
public class CustomModelRenderer {
    private final RabbitModel rabbitModel = new RabbitModel();
    private final CowModel cowModel = new CowModel();
    private static final Identifier RABBIT_TEXTURE = Identifier.of((String)"astolfoclient", (String)"textures/models/rabbit.png");
    private static final Identifier AMOGUS_TEXTURE = Identifier.of((String)"astolfoclient", (String)"textures/models/amogus.png");

    private void renderNow(PlayerEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, String mode, PlayerEntityModel baseModel) {
        if (mode.equals("Rabbit")) {
            this.rabbitModel.setAngles(state, baseModel);
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayers.entityTranslucent((Identifier)RABBIT_TEXTURE));
            this.rabbitModel.render(matrices, buffer, light);
        } else if (mode.equals("Cow")) {
            this.cowModel.render(matrices, vertexConsumers, state, light);
        } else if (mode.equals("Amogus")) {
            // empty if block
        }
    }
 public void render(PlayerEntityRenderState state,MatrixStack matrices,net.minecraft.client.render.command.OrderedRenderCommandQueue queue,int light,String mode,PlayerEntityModel baseModel) {
 var texture=mode.equals("Cow")?Identifier.of("astolfoclient","textures/models/cow.png"):RABBIT_TEXTURE;
 queue.submitCustom(matrices,RenderLayers.entityTranslucent(texture),(entry,vertices)->{var pose=new MatrixStack();pose.peek().getPositionMatrix().set(entry.getPositionMatrix());pose.peek().getNormalMatrix().set(entry.getNormalMatrix());renderNow(state,pose,layer->vertices,light,mode,baseModel);});
 }
}
