package xyz.angames.astolfoclient.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

/**
 * Stand-in for the removed Fabric {@code WorldRenderContext}, built from the 26.x {@link LevelRenderContext}.
 * Exposes an identity pose stack (camera rotation lives in the model-view matrix) and the level projection.
 */
public final class WorldRenderContext {
   private final LevelRenderContext delegate;
   private final PoseStack poseStack = new PoseStack();
   private final Matrix4f projection;

   public WorldRenderContext(LevelRenderContext delegate) {
      this.delegate = delegate;
      CameraRenderState cs = delegate.levelState().cameraRenderState;
      // The model-view stack already carries the camera rotation while the level renders, so the pose stays identity.
      this.projection = new Matrix4f(cs.projectionMatrix);
   }

   public LevelRenderContext level() {
      return this.delegate;
   }

   public Camera camera() {
      return Minecraft.getInstance().gameRenderer.mainCamera();
   }

   public PoseStack matrixStack() {
      return this.poseStack;
   }

   public Matrix4f projectionMatrix() {
      return this.projection;
   }

   public DeltaTracker tickCounter() {
      return Minecraft.getInstance().getDeltaTracker();
   }

   public CameraRenderState cameraState() {
      return this.delegate.levelState().cameraRenderState;
   }
}
