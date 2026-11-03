package xyz.angames.astolfoclient.client.render.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

@Environment(value=EnvType.CLIENT)
public class RabbitModel {
    private final ModelPart root;
    private final ModelPart rabbitBone;
    private final ModelPart rabbitHead;
    private final ModelPart rabbitLarm;
    private final ModelPart rabbitRarm;
    private final ModelPart rabbitLleg;
    private final ModelPart rabbitRleg;

    public RabbitModel() {
        ModelPart modelRoot;
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition rootData = modelData.getRoot();
        PartDefinition boneData = rootData.addOrReplaceChild("rabbitBone", CubeListBuilder.create().texOffs(28, 45).addBox(-5.0f, -13.0f, -5.0f, 10.0f, 11.0f, 8.0f), PartPose.offset((float)0.0f, (float)24.0f, (float)0.0f));
        boneData.addOrReplaceChild("rabbitHead", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, 0.0f, -4.0f, 6.0f, 1.0f, 6.0f).texOffs(56, 0).addBox(-5.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f).texOffs(56, 0).addBox(3.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f).texOffs(0, 45).addBox(-4.0f, -11.0f, -4.0f, 8.0f, 11.0f, 8.0f).texOffs(46, 0).addBox(1.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f).texOffs(46, 0).addBox(-4.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f), PartPose.offset((float)0.0f, (float)-14.0f, (float)-1.0f));
        boneData.addOrReplaceChild("rabbitLarm", CubeListBuilder.create().texOffs(0, 0).addBox(0.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f), PartPose.offset((float)5.0f, (float)-13.0f, (float)-1.0f));
        boneData.addOrReplaceChild("rabbitRarm", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f), PartPose.offset((float)-5.0f, (float)-13.0f, (float)-1.0f));
        boneData.addOrReplaceChild("rabbitLleg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f), PartPose.offset((float)3.0f, (float)-2.0f, (float)-1.0f));
        boneData.addOrReplaceChild("rabbitRleg", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f), PartPose.offset((float)-3.0f, (float)-2.0f, (float)-1.0f));
        this.root = modelRoot = LayerDefinition.create((MeshDefinition)modelData, (int)64, (int)64).bakeRoot();
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

