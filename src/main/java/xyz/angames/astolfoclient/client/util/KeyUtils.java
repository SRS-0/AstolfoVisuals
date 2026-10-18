package xyz.angames.astolfoclient.client.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;

@Environment(EnvType.CLIENT)
public class KeyUtils {
   private static final Map<String, Integer> keyNameToCode = new HashMap<>();
   private static final Map<Integer, String> keyCodeToName = new HashMap<>();

   public static int getKeyCode(String keyName) {
      return keyNameToCode.getOrDefault(keyName.toUpperCase(), -1);
   }

   public static String getKeyName(int keyCode) {
      return keyCodeToName.getOrDefault(keyCode, "UNKNOWN");
   }

   public static Collection<String> getKeyNames() {
      return keyNameToCode.keySet();
   }

   static {
      for (int i = 0; i <= 350; i++) {
         String genericName = "KEY_" + i;
         keyNameToCode.put(genericName, i);
         keyCodeToName.put(i, genericName);
      }

      for (int i = 0; i < 350; i++) {
         try {
            Key key = Type.KEYBOARD.getOrCreate(xyz.angames.astolfoclient.client.compat.KeyMap.glfwToSdl(i));
            String name = key.getName();
            if (name != null && !name.isEmpty() && !name.contains("unknown")) {
               String simpleName = name.replace("key.keyboard.", "").toUpperCase();
               keyNameToCode.put(simpleName, i);
               keyCodeToName.put(i, simpleName);
            }
         } catch (Exception var4) {
         }
      }

      keyNameToCode.put("RSHIFT", 344);
      keyCodeToName.put(344, "RSHIFT");
      keyNameToCode.put("LSHIFT", 340);
      keyCodeToName.put(340, "LSHIFT");
      keyNameToCode.put("RCTRL", 345);
      keyCodeToName.put(345, "RCTRL");
      keyNameToCode.put("LCTRL", 341);
      keyCodeToName.put(341, "LCTRL");
      keyNameToCode.put("LCONTROL", 341);
      keyNameToCode.put("RCONTROL", 345);
      keyNameToCode.put("LEFT_CONTROL", 341);
      keyNameToCode.put("RIGHT_CONTROL", 345);
      keyNameToCode.put("LEFT_SHIFT", 340);
      keyNameToCode.put("RIGHT_SHIFT", 344);
      keyNameToCode.put("LALT", 342);
      keyCodeToName.put(342, "LALT");
      keyNameToCode.put("RALT", 346);
      keyCodeToName.put(346, "RALT");
      keyNameToCode.put("LEFT_ALT", 342);
      keyNameToCode.put("RIGHT_ALT", 346);

      for (int button = 0; button < 8; button++) {
         String name = "M" + (button + 1);
         int code = -(button + 100);
         keyNameToCode.put(name, code);
         keyCodeToName.put(code, name);
      }
   }
}
