package xyz.angames.astolfoclient.client.util;

import java.util.Arrays;

/** Screen mouse state, independent of Minecraft's gameplay-only button flags. */
public final class HudMouseInput {
    private static final boolean[] down = new boolean[3];
    private static final boolean[] pressed = new boolean[3];
    private static final boolean[] frameDown = new boolean[3];
    private static Object screen;

    private HudMouseInput() {}

    private static void scope(Object currentScreen, boolean focused) {
        if (screen != currentScreen || !focused) {
            Arrays.fill(down, false);
            Arrays.fill(pressed, false);
            Arrays.fill(frameDown, false);
            screen = currentScreen;
        }
    }

    public static void onButton(Object currentScreen, boolean focused, int button, int action) {
        scope(currentScreen, focused);
        if (!focused || currentScreen == null || button < 0 || button >= down.length) return;
        if (action == 1) {
            if (!down[button]) pressed[button] = true;
            down[button] = true;
        } else if (action == 0) {
            down[button] = false;
        }
    }

    public static void beginFrame(Object currentScreen, boolean focused) {
        scope(currentScreen, focused);
        for (int button = 0; button < down.length; button++) {
            // Preserve clicks whose press and release both arrived between frames.
            frameDown[button] = down[button] || pressed[button];
            pressed[button] = false;
        }
    }

    public static boolean isLeftPressed() { return frameDown[0]; }
    public static boolean isRightPressed() { return frameDown[1]; }
}
