package xyz.angames.astolfoclient.client.compat;

import java.nio.DoubleBuffer;
import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDLVideo;

/** The handful of GLFW queries the mod made, answered through the 26.3 SDL-based window/input layer. */
public final class GLFW {
   private GLFW() {
   }

   public static int glfwGetMouseButton(long window, int glfwButton) {
      var mouse = Minecraft.getInstance().mouseHandler;
      boolean down = switch (glfwButton) {
         case 0 -> mouse.isLeftPressed();
         case 1 -> mouse.isRightPressed();
         case 2 -> mouse.isMiddlePressed();
         default -> false;
      };
      return down ? 1 : 0;
   }

   public static int glfwGetKey(long window, int glfwKey) {
      return com.mojang.blaze3d.platform.InputConstants.isKeyDown(KeyMap.glfwToSdl(glfwKey)) ? 1 : 0;
   }

   public static void glfwGetCursorPos(long window, DoubleBuffer x, DoubleBuffer y) {
      var mouse = Minecraft.getInstance().mouseHandler;
      if (x != null) {
         x.put(0, mouse.xpos());
      }

      if (y != null) {
         y.put(0, mouse.ypos());
      }
   }

   public static void glfwGetCursorPos(long window, double[] x, double[] y) {
      var mouse = Minecraft.getInstance().mouseHandler;
      if (x != null) {
         x[0] = mouse.xpos();
      }

      if (y != null) {
         y[0] = mouse.ypos();
      }
   }

   public static long glfwGetCurrentContext() {
      return SDLVideo.SDL_GL_GetCurrentContext();
   }
}
