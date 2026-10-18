package xyz.angames.astolfoclient.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.model.player.PlayerModel;
import xyz.angames.astolfoclient.client.render.models.CowModel;
import xyz.angames.astolfoclient.client.render.models.RabbitModel;

@Environment(EnvType.CLIENT)
public class CustomModelRenderer {
   private final CowModel cowModel = new CowModel();
   private static final Identifier RABBIT_TEXTURE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/models/rabbit.png");
   private static final Identifier AMOGUS_TEXTURE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/models/amogus.png");

   public void render(AvatarRenderState state, PoseStack matrices, SubmitNodeCollector collector, int light, String mode, PlayerModel baseModel) {
      if (mode.equals("Rabbit")) {
         // The geometry callback runs after submission, so each submit owns its posed model.
         RabbitModel posed = new RabbitModel();
         posed.setAngles(state, baseModel);
         collector.submitCustomGeometry(matrices, RenderTypes.entityTranslucent(RABBIT_TEXTURE), (pose, buffer) -> {
            PoseStack ps = new PoseStack();
            ps.last().set(pose);
            posed.render(ps, buffer, light);
         });
      } else if (mode.equals("Cow")) {
         this.cowModel.render(matrices, collector, state, light);
      } else if (mode.equals("Amogus")) {
      }
   }
}
