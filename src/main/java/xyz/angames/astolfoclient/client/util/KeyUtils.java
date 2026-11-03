package xyz.angames.astolfoclient.client.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.InputUtil;

@Environment(value=EnvType.CLIENT)
public class KeyUtils {
    private static final Map<String, Integer> keyNameToCode;
    private static final Map<Integer, String> keyCodeToName;

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
        int i;
        keyNameToCode = new HashMap<String, Integer>();
        keyCodeToName = new HashMap<Integer, String>();
        for (i = 0; i <= 350; ++i) {
            String genericName = "KEY_" + i;
            keyNameToCode.put(genericName, i);
            keyCodeToName.put(i, genericName);
        }
        for (i = 0; i < 350; ++i) {
            try {
                InputUtil.Key key = InputUtil.Type.KEYSYM.createFromCode(i);
                String name = key.getTranslationKey();
                if (name == null || name.isEmpty() || name.contains("unknown")) continue;
                String simpleName = name.replace("key.keyboard.", "").toUpperCase();
                keyNameToCode.put(simpleName, i);
                keyCodeToName.put(i, simpleName);
                continue;
            }
            catch (Exception key) {
                // empty catch block
            }
        }
        keyNameToCode.put("RSHIFT", 344);
        // Names must also work when this class initializes before GLFW's window exists.
        for (int code = 65; code <= 90; code++) {
            String name = Character.toString((char)code);
            keyNameToCode.put(name, code);
            keyCodeToName.put(code, name);
        }
        for (int code = 48; code <= 57; code++) {
            String name = Character.toString((char)code);
            keyNameToCode.put(name, code);
            keyCodeToName.put(code, name);
        }
        for (int number = 1; number <= 25; number++) {
            keyNameToCode.put("F" + number, 289 + number);
            keyCodeToName.put(289 + number, "F" + number);
        }
        keyNameToCode.put("INSERT", 260);
        keyCodeToName.put(260, "INSERT");
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
        for (int button = 0; button < 8; ++button) {
            String name = "M" + (button + 1);
            int code = -(button + 100);
            keyNameToCode.put(name, code);
            keyCodeToName.put(code, name);
        }
    }
}

