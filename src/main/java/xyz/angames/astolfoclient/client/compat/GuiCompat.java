package xyz.angames.astolfoclient.client.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;

/** Bridges the 2D GUI pose ({@link Matrix3x2f}) to the 4x4 matrices the immediate renderers expect. */
public final class GuiCompat {
   private GuiCompat() {
   }

   public static Matrix4f matrix4f(GuiGraphicsExtractor graphics) {
      return matrix4f(graphics.pose());
   }

   public static Matrix4f matrix4f(Matrix3x2f m) {
      return new Matrix4f(
         m.m00, m.m01, 0.0F, 0.0F,
         m.m10, m.m11, 0.0F, 0.0F,
         0.0F, 0.0F, 1.0F, 0.0F,
         m.m20, m.m21, 0.0F, 1.0F
      );
   }

   public static boolean shiftDown() {
      long window = net.minecraft.client.Minecraft.getInstance().getWindow().handle();
      return GLFW.glfwGetKey(window, 340) == 1 || GLFW.glfwGetKey(window, 344) == 1;
   }

   public static boolean controlDown() {
      long window = net.minecraft.client.Minecraft.getInstance().getWindow().handle();
      return GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1;
   }
}
