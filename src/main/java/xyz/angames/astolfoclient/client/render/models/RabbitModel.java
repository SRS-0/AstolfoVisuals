package xyz.angames.astolfoclient.client.render.models;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

@Environment(EnvType.CLIENT)
public class RabbitModel {
   private final ModelPart root;
   private final ModelPart rabbitBone;
   private final ModelPart rabbitHead;
   private final ModelPart rabbitLarm;
   private final ModelPart rabbitRarm;
   private final ModelPart rabbitLleg;
   private final ModelPart rabbitRleg;

   public RabbitModel() {
      MeshDefinition modelData = new MeshDefinition();
      PartDefinition rootData = modelData.getRoot();
      PartDefinition boneData = rootData.addOrReplaceChild(
         "rabbitBone",
         CubeListBuilder.create().texOffs(28, 45).addBox(-5.0F, -13.0F, -5.0F, 10.0F, 11.0F, 8.0F),
         PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      boneData.addOrReplaceChild(
         "rabbitHead",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-3.0F, 0.0F, -4.0F, 6.0F, 1.0F, 6.0F)
            .texOffs(56, 0)
            .addBox(-5.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F)
            .texOffs(56, 0)
            .addBox(3.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F)
            .texOffs(0, 45)
            .addBox(-4.0F, -11.0F, -4.0F, 8.0F, 11.0F, 8.0F)
            .texOffs(46, 0)
            .addBox(1.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F)
            .texOffs(46, 0)
            .addBox(-4.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F),
         PartPose.offset(0.0F, -14.0F, -1.0F)
      );
      boneData.addOrReplaceChild(
         "rabbitLarm",
         CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F),
         PartPose.offset(5.0F, -13.0F, -1.0F)
      );
      boneData.addOrReplaceChild(
         "rabbitRarm",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F),
         PartPose.offset(-5.0F, -13.0F, -1.0F)
      );
      boneData.addOrReplaceChild(
         "rabbitLleg",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F),
         PartPose.offset(3.0F, -2.0F, -1.0F)
      );
      boneData.addOrReplaceChild(
         "rabbitRleg",
         CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F),
         PartPose.offset(-3.0F, -2.0F, -1.0F)
      );
      ModelPart modelRoot = LayerDefinition.create(modelData, 64, 64).bakeRoot();
      this.root = modelRoot;
      this.rabbitBone = modelRoot.getChild("rabbitBone");
      this.rabbitHead = this.rabbitBone.getChild("rabbitHead");
      this.rabbitLarm = this.rabbitBone.getChild("rabbitLarm");
      this.rabbitRarm = this.rabbitBone.getChild("rabbitRarm");
      this.rabbitLleg = this.rabbitBone.getChild("rabbitLleg");
      this.rabbitRleg = this.rabbitBone.getChild("rabbitRleg");
   }

   public void setAngles(AvatarRenderState state, PlayerModel baseModel) {
      this.rabbitHead.xRot = baseModel.head.xRot;
      this.rabbitHead.yRot = baseModel.head.yRot;
      this.rabbitHead.zRot = baseModel.head.zRot;
      this.rabbitLarm.xRot = baseModel.leftArm.xRot;
      this.rabbitLarm.yRot = baseModel.leftArm.yRot;
      this.rabbitLarm.zRot = baseModel.leftArm.zRot;
      this.rabbitRarm.xRot = baseModel.rightArm.xRot;
      this.rabbitRarm.yRot = baseModel.rightArm.yRot;
      this.rabbitRarm.zRot = baseModel.rightArm.zRot;
      this.rabbitLleg.xRot = baseModel.leftLeg.xRot;
      this.rabbitLleg.yRot = baseModel.leftLeg.yRot;
      this.rabbitLleg.zRot = baseModel.leftLeg.zRot;
      this.rabbitRleg.xRot = baseModel.rightLeg.xRot;
      this.rabbitRleg.yRot = baseModel.rightLeg.yRot;
      this.rabbitRleg.zRot = baseModel.rightLeg.zRot;
   }

   public void render(PoseStack matrices, VertexConsumer vertices, int light) {
      this.root.render(matrices, vertices, light, OverlayTexture.NO_OVERLAY);
   }
}
