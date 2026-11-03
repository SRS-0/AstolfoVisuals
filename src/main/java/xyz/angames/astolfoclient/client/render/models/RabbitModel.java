package xyz.angames.astolfoclient.client.render.models;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

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
        ModelData modelData = new ModelData();
        ModelPartData rootData = modelData.getRoot();
        ModelPartData boneData = rootData.addChild("rabbitBone", ModelPartBuilder.create().uv(28, 45).cuboid(-5.0f, -13.0f, -5.0f, 10.0f, 11.0f, 8.0f), ModelTransform.origin((float)0.0f, (float)24.0f, (float)0.0f));
        boneData.addChild("rabbitHead", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0f, 0.0f, -4.0f, 6.0f, 1.0f, 6.0f).uv(56, 0).cuboid(-5.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f).uv(56, 0).cuboid(3.0f, -9.0f, -5.0f, 2.0f, 3.0f, 2.0f).uv(0, 45).cuboid(-4.0f, -11.0f, -4.0f, 8.0f, 11.0f, 8.0f).uv(46, 0).cuboid(1.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f).uv(46, 0).cuboid(-4.0f, -20.0f, 0.0f, 3.0f, 9.0f, 1.0f), ModelTransform.origin((float)0.0f, (float)-14.0f, (float)-1.0f));
        boneData.addChild("rabbitLarm", ModelPartBuilder.create().uv(0, 0).cuboid(0.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f), ModelTransform.origin((float)5.0f, (float)-13.0f, (float)-1.0f));
        boneData.addChild("rabbitRarm", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0f, 0.0f, -2.0f, 2.0f, 8.0f, 4.0f), ModelTransform.origin((float)-5.0f, (float)-13.0f, (float)-1.0f));
        boneData.addChild("rabbitLleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f), ModelTransform.origin((float)3.0f, (float)-2.0f, (float)-1.0f));
        boneData.addChild("rabbitRleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0f, 0.0f, -2.0f, 4.0f, 2.0f, 4.0f), ModelTransform.origin((float)-3.0f, (float)-2.0f, (float)-1.0f));
        this.root = modelRoot = TexturedModelData.of((ModelData)modelData, (int)64, (int)64).createModel();
        this.rabbitBone = modelRoot.getChild("rabbitBone");
        this.rabbitHead = this.rabbitBone.getChild("rabbitHead");
        this.rabbitLarm = this.rabbitBone.getChild("rabbitLarm");
        this.rabbitRarm = this.rabbitBone.getChild("rabbitRarm");
        this.rabbitLleg = this.rabbitBone.getChild("rabbitLleg");
        this.rabbitRleg = this.rabbitBone.getChild("rabbitRleg");
    }

    public void setAngles(PlayerEntityRenderState state, PlayerEntityModel baseModel) {
        this.rabbitHead.pitch = baseModel.head.pitch;
        this.rabbitHead.yaw = baseModel.head.yaw;
        this.rabbitHead.roll = baseModel.head.roll;
        this.rabbitLarm.pitch = baseModel.leftArm.pitch;
        this.rabbitLarm.yaw = baseModel.leftArm.yaw;
        this.rabbitLarm.roll = baseModel.leftArm.roll;
        this.rabbitRarm.pitch = baseModel.rightArm.pitch;
        this.rabbitRarm.yaw = baseModel.rightArm.yaw;
        this.rabbitRarm.roll = baseModel.rightArm.roll;
        this.rabbitLleg.pitch = baseModel.leftLeg.pitch;
        this.rabbitLleg.yaw = baseModel.leftLeg.yaw;
        this.rabbitLleg.roll = baseModel.leftLeg.roll;
        this.rabbitRleg.pitch = baseModel.rightLeg.pitch;
        this.rabbitRleg.yaw = baseModel.rightLeg.yaw;
        this.rabbitRleg.roll = baseModel.rightLeg.roll;
    }

    public void render(MatrixStack matrices, VertexConsumer vertices, int light) {
        this.root.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
    }
}

