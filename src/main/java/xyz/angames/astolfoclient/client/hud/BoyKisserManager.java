package xyz.angames.astolfoclient.client.hud;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.GuiCompat;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class BoyKisserManager {
   private final Minecraft client = Minecraft.getInstance();
   private final List<Identifier> frames = new ArrayList<>();
   private int currentFrame = 0;
   private long lastFrameTime = 0L;
   private final int frameDelay = 100;
   private double x = 10.0;
   private double y = 50.0;
   private final double width = 64.0;
   private final double height = 64.0;
   private boolean isDragging = false;
   private double dragX;
   private double dragY;

   public BoyKisserManager() {
      int frameCount = 52;

      for (int i = 0; i < frameCount; i++) {
         this.frames.add(Identifier.fromNamespaceAndPath("astolfoclient", "textures/gui/boikiser/boykisser_" + i + ".png"));
      }
   }

   public void render(GuiGraphicsExtractor context, float delta) {
      long now = (long)(System.nanoTime() / 1000000.0);
      if (now - this.lastFrameTime > 100L) {
         this.currentFrame = (this.currentFrame + 1) % this.frames.size();
         this.lastFrameTime = now;
      }

      Identifier currentTexture = this.frames.get(this.currentFrame);
      context.pose().pushMatrix();
      float scaleModifier = this.getScaleModifier();
      context.pose().translate((float)this.x, (float)this.y);
      context.pose().scale(scaleModifier, scaleModifier);
      context.pose().translate((float)(-this.x), (float)(-this.y));
      Matrix4f matrix = GuiCompat.matrix4f(context);
      Builder.texture()
         .size(new SizeState(64.0F, 64.0F))
         .radius(new QuadRadiusState(0.0F))
         .texture(0.0F, 0.0F, 1.0F, 1.0F, this.client.getTextureManager().getTexture(currentTexture))
         .color(QuadColorState.WHITE)
         .build()
         .render(matrix, (float)this.x, (float)this.y);
      context.pose().popMatrix();
   }

   public float getScaleModifier() {
      Minecraft mc = Minecraft.getInstance();
      double currentGuiScale = mc.getWindow().getGuiScale();
      if (currentGuiScale <= 0.0) {
         currentGuiScale = 2.0;
      }

      return (float)(2.0 / currentGuiScale);
   }

   public boolean onMouseClicked(double mouseX, double mouseY, int button) {
      float scaleModifier = this.getScaleModifier();
      double effectiveW = 64.0 * scaleModifier;
      double effectiveH = 64.0 * scaleModifier;
      if (button == 0 && mouseX >= this.x && mouseX <= this.x + effectiveW && mouseY >= this.y && mouseY <= this.y + effectiveH) {
         this.isDragging = true;
         this.dragX = mouseX - this.x;
         this.dragY = mouseY - this.y;
         return true;
      } else {
         return false;
      }
   }

   public void onMouseDragged(double mouseX, double mouseY, int button) {
      if (button == 0 && this.isDragging) {
         float scaleModifier = this.getScaleModifier();
         float screenW = this.client.getWindow().getGuiScaledWidth();
         float screenH = this.client.getWindow().getGuiScaledHeight();
         float effectiveW = (float)(64.0 * scaleModifier);
         float effectiveH = (float)(64.0 * scaleModifier);
         this.x = Math.max(0.0, Math.min(Math.max(0.0F, screenW - effectiveW), mouseX - this.dragX));
         this.y = Math.max(0.0, Math.min(Math.max(0.0F, screenH - effectiveH), mouseY - this.dragY));
      }
   }

   public void onMouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.isDragging = false;
      }
   }

   public void onMouseReleased(int button) {
      if (button == 0) {
         this.isDragging = false;
      }
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public void setX(double x) {
      this.x = x;
   }

   public void setY(double y) {
      this.y = y;
   }

   public double getWidth() {
      return 64.0;
   }

   public double getHeight() {
      return 64.0;
   }
}
