package xyz.angames.astolfoclient.client.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class SoundSettings {
    private static boolean soundEnabled = true;
    private static float masterVolume = 100.0f;
    private static float guiOpenVolume = 100.0f;
    private static float categoryVolume = 100.0f;
    private static float moduleSelectVolume = 100.0f;
    private static float sliderVolume = 70.0f;
    private static float searchVolume = 100.0f;
    private static float modeOpenVolume = 100.0f;
    private static float moduleToggleVolume = 100.0f;

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static float getMasterVolume() {
        return masterVolume;
    }

    public static void setMasterVolume(float volume) {
        masterVolume = MathHelper.clamp((float)volume, (float)0.0f, (float)100.0f);
    }

    public static float getGuiOpenVolume() {
        return guiOpenVolume;
    }

    public static void setGuiOpenVolume(float volume) {
        guiOpenVolume = MathHelper.clamp((float)volume, (float)0.0f, (float)100.0f);
    }

    public static float getCategoryVolume() {
        return categoryVolume;
    }

    public static void setCategoryVolume(float volume) {
        categoryVolume = MathHelper.clamp((float)volume, (float)0.0f, (float)100.0f);
    }

    public static float getModuleSelectVolume() {
        return moduleSelectVolume;
    }

    public static void setModuleSelectVolume(float volume) {
        moduleSelectVolume = MathHelper.clamp((float)volume, (float)0.0f, (float)100.0f);
    }

    public static float getSliderVolume() {
        return sliderVolume;
    }

    public static void setSliderVolume(float volume) {
        sliderVolume = MathHelper.clamp((float)volume, (float)0.0f, (float)100.0f);
    }

    public static float getSearchVolume() {
        return searchVolume;
    }

    public static void setSearchVolume(float volume) {
        searchVolume = MathHelper.clamp((float)volume, (float)0.0f, (float)100.0f);
    }

    public static float getModeOpenVolume() {
        return modeOpenVolume;
    }

    public static void setModeOpenVolume(float volume) {
        modeOpenVolume = MathHelper.clamp((float)volume, (float)0.0f, (float)100.0f);
    }

    public static float getModuleToggleVolume() {
        return moduleToggleVolume;
    }

    public static void setModuleToggleVolume(float volume) {
        moduleToggleVolume = MathHelper.clamp((float)volume, (float)0.0f, (float)100.0f);
    }
}

