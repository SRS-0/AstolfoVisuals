package xyz.angames.astolfoclient.client.compat;

import java.util.HashMap;
import java.util.Map;

/**
 * 26.3 switched from GLFW to SDL3. The mod stores keybinds as GLFW key codes (configs, GUI, module binds), so key
 * events are translated SDL scancode -> GLFW key code at the input entry points and back for key-state queries.
 */
public final class KeyMap {
   private static final Map<Integer, Integer> GLFW_TO_SDL = new HashMap<>();
   private static final Map<Integer, Integer> SDL_TO_GLFW = new HashMap<>();

   private KeyMap() {
   }

   private static void put(int glfw, int sdl) {
      GLFW_TO_SDL.put(glfw, sdl);
      SDL_TO_GLFW.put(sdl, glfw);
   }

   static {
      for (int i = 0; i < 26; i++) {
         put(65 + i, 4 + i);
      }

      for (int i = 1; i <= 9; i++) {
         put(48 + i, 29 + i);
      }

      put(48, 39);
      put(257, 40);
      put(256, 41);
      put(259, 42);
      put(258, 43);
      put(32, 44);
      put(45, 45);
      put(61, 46);
      put(91, 47);
      put(93, 48);
      put(92, 49);
      put(59, 51);
      put(39, 52);
      put(96, 53);
      put(44, 54);
      put(46, 55);
      put(47, 56);
      put(280, 57);
      for (int i = 0; i < 12; i++) {
         put(290 + i, 58 + i);
      }

      for (int i = 0; i < 12; i++) {
         put(302 + i, 104 + i);
      }

      put(283, 70);
      put(281, 71);
      put(284, 72);
      put(260, 73);
      put(268, 74);
      put(266, 75);
      put(261, 76);
      put(269, 77);
      put(267, 78);
      put(262, 79);
      put(263, 80);
      put(264, 81);
      put(265, 82);
      put(282, 83);
      put(331, 84);
      put(332, 85);
      put(333, 86);
      put(334, 87);
      put(335, 88);
      for (int i = 1; i <= 9; i++) {
         put(320 + i, 88 + i);
      }

      put(320, 98);
      put(330, 99);
      put(340, 225);
      put(341, 224);
      put(342, 226);
      put(343, 227);
      put(344, 229);
      put(345, 228);
      put(346, 230);
      put(347, 231);
   }

   public static int glfwToSdl(int glfw) {
      return GLFW_TO_SDL.getOrDefault(glfw, glfw);
   }

   public static int sdlToGlfw(int sdl) {
      return SDL_TO_GLFW.getOrDefault(sdl, sdl);
   }

   /** SDL mouse buttons are 1-based, the mod's are 0-based GLFW buttons. */
   public static int sdlButtonToGlfw(int sdlButton) {
      return sdlButton - 1;
   }

   /** GLFW-style modifier bits (shift 1, ctrl 2, alt 4, super 8) from an input event. */
   public static int glfwMods(net.minecraft.client.input.InputWithModifiers input) {
      return (input.hasShiftDown() ? 1 : 0) | (input.hasControlDown() ? 2 : 0) | (input.hasAltDown() ? 4 : 0);
   }
}
