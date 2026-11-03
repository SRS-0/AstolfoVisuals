package xyz.angames.astolfoclient.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.player.PlayerModel;
import xyz.angames.astolfoclient.client.render.compat.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import xyz.angames.astolfoclient.client.render.models.CowModel;
import xyz.angames.astolfoclient.client.render.models.RabbitModel;

@Environment(value=EnvType.CLIENT)
public class CustomModelRenderer {
    private final RabbitModel rabbitModel = new RabbitModel();
    private final CowModel cowModel = new CowModel();
    private static final Identifier RABBIT_TEXTURE = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/models/rabbit.png");
    private static final Identifier AMOGUS_TEXTURE = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/models/amogus.png");

    private void renderNow(AvatarRenderState state, PoseStack matrices, MultiBufferSource vertexConsumers, int light, String mode, PlayerModel baseModel) {
        if (mode.equals("Rabbit")) {
            this.rabbitModel.setAngles(state, baseModel);
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderTypes.entityTranslucent((Identifier)RABBIT_TEXTURE));
            this.rabbitModel.render(matrices, buffer, light);
        } else if (mode.equals("Cow")) {
            this.cowModel.render(matrices, vertexConsumers, state, light);
        } else if (mode.equals("Amogus")) {
            // empty if block
        }
    }
 public void render(AvatarRenderState state,PoseStack matrices,net.minecraft.client.renderer.SubmitNodeCollector queue,int light,String mode,PlayerModel baseModel) {
 var texture=mode.equals("Cow")?Identifier.fromNamespaceAndPath("astolfoclient","textures/models/cow.png"):RABBIT_TEXTURE;
 queue.submitCustomGeometry(matrices,RenderTypes.entityTranslucent(texture),(entry,vertices)->{var pose=new PoseStack();pose.last().pose().set(entry.pose());pose.last().normal().set(entry.normal());renderNow(state,pose,layer->vertices,light,mode,baseModel);});
 }
}
