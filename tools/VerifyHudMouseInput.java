import xyz.angames.astolfoclient.client.util.HudMouseInput;

class VerifyHudMouseInput {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Object editor = new Object();
        HudMouseInput.beginFrame(editor, true);
        HudMouseInput.onButton(editor, true, 0, 1);
        HudMouseInput.onButton(editor, true, 0, 0);
        HudMouseInput.beginFrame(editor, true);
        check(HudMouseInput.isLeftPressed(), "Fast panel click was lost");
        check(HudMouseInput.isLeftPressed(), "A second HUD consumer lost the click");
        HudMouseInput.beginFrame(editor, true);
        check(!HudMouseInput.isLeftPressed(), "Click repeated in next frame");

        HudMouseInput.onButton(editor, true, 1, 1);
        HudMouseInput.beginFrame(editor, true);
        check(HudMouseInput.isRightPressed(), "Right click was lost");
        HudMouseInput.beginFrame(editor, true);
        check(HudMouseInput.isRightPressed(), "Held button lost its state");
        HudMouseInput.onButton(editor, true, 1, 0);
        HudMouseInput.beginFrame(editor, true);
        check(!HudMouseInput.isRightPressed(), "Release was lost");

        HudMouseInput.onButton(editor, true, 0, 1);
        HudMouseInput.beginFrame(new Object(), true);
        check(!HudMouseInput.isLeftPressed(), "Input leaked into another screen");
        HudMouseInput.onButton(editor, true, 0, 1);
        HudMouseInput.beginFrame(editor, false);
        HudMouseInput.beginFrame(editor, true);
        check(!HudMouseInput.isLeftPressed(), "Button stuck after focus loss");
        HudMouseInput.onButton(null, true, 0, 1);
        HudMouseInput.beginFrame(null, true);
        check(!HudMouseInput.isLeftPressed(), "Gameplay input leaked into HUD controls");
        System.out.println("PASS fast clicks, shared frame state, held buttons, releases, screen changes and focus loss");
    }
}
