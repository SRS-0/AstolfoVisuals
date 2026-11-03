package xyz.angames.astolfoclient.client.config;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class ThemeManager {
    public static Color themeColor;
    public static Color customColor1;
    public static Color customColor2;

    public static int getThemedColor() {
        return themeColor.getRGB();
    }

    public static int getThemedColor(long offset) {
        return themeColor.getRGB();
    }

    public static void setThemedColor(int rgb) {
        ThemeManager.setThemeColor(new Color(rgb));
    }

    public static void setThemeColor(Color c) {
        if (c != null) {
            themeColor = c;
            customColor1 = c;
            customColor2 = c;
        }
    }

    public static Color getThemeColor() {
        return themeColor;
    }

    public static Color getCustomColor1() {
        return themeColor;
    }

    public static void setCustomColor1(Color c) {
        ThemeManager.setThemeColor(c);
    }

    public static Color getCustomColor2() {
        return themeColor;
    }

    public static void setCustomColor2(Color c) {
        ThemeManager.setThemeColor(c);
    }

    public static Theme getCurrentTheme() {
        return Theme.CUSTOM;
    }

    public static void setCurrentTheme(Theme theme) {
    }

    public static void cycleTheme() {
    }

    public static String getCustomColor1Hex() {
        return String.format("#%06X", 0xFFFFFF & themeColor.getRGB());
    }

    public static String getCustomColor2Hex() {
        return ThemeManager.getCustomColor1Hex();
    }

    public static void setCustomColors(String hex1, String hex2) {
        try {
            ThemeManager.setThemeColor(Color.decode(hex1));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void setCustomColor(String hex) {
        try {
            ThemeManager.setThemeColor(Color.decode(hex));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    static {
        customColor1 = themeColor = new Color(16738740);
        customColor2 = themeColor;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum Theme {
        CUSTOM;

    }
}

