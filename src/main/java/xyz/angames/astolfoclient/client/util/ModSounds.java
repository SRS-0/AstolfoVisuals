package xyz.angames.astolfoclient.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import xyz.angames.astolfoclient.client.config.SoundSettings;

@Environment(value=EnvType.CLIENT)
public class ModSounds {
    public static final Identifier ENABLE_ID = Identifier.of((String)"astolfoclient", (String)"module_enable");
    public static final SoundEvent ENABLE_SOUND = SoundEvent.of((Identifier)ENABLE_ID);
    public static final Identifier DISABLE_ID = Identifier.of((String)"astolfoclient", (String)"module_disable");
    public static final SoundEvent DISABLE_SOUND = SoundEvent.of((Identifier)DISABLE_ID);
    public static final Identifier CRASH_DETECTION_ID = Identifier.of((String)"astolfoclient", (String)"crash_detection");
    public static final SoundEvent CRASH_DETECTION_SOUND = SoundEvent.of((Identifier)CRASH_DETECTION_ID);
    public static final Identifier CRASH_DETECTION_HYPHEN_ID = Identifier.of((String)"astolfoclient", (String)"crash-detection");
    public static final SoundEvent CRASH_DETECTION_HYPHEN_SOUND = SoundEvent.of((Identifier)CRASH_DETECTION_HYPHEN_ID);
    public static final Identifier GUI_OPEN_ID = Identifier.of((String)"astolfoclient", (String)"clickgui_open");
    public static final SoundEvent GUI_OPEN_SOUND = SoundEvent.of((Identifier)GUI_OPEN_ID);
    public static final Identifier CATEGORY_ID = Identifier.of((String)"astolfoclient", (String)"clickgui_category");
    public static final SoundEvent CATEGORY_SOUND = SoundEvent.of((Identifier)CATEGORY_ID);
    public static final Identifier MODULE_SELECT_ID = Identifier.of((String)"astolfoclient", (String)"clickgui_module");
    public static final SoundEvent MODULE_SELECT_SOUND = SoundEvent.of((Identifier)MODULE_SELECT_ID);
    public static final Identifier SLIDER_MOVING_ID = Identifier.of((String)"astolfoclient", (String)"clickgui_slider");
    public static final SoundEvent SLIDER_MOVING_SOUND = SoundEvent.of((Identifier)SLIDER_MOVING_ID);
    public static final Identifier SEARCH_CLICK_ID = Identifier.of((String)"astolfoclient", (String)"clickgui_search");
    public static final SoundEvent SEARCH_CLICK_SOUND = SoundEvent.of((Identifier)SEARCH_CLICK_ID);
    public static final Identifier MODE_OPEN_ID = Identifier.of((String)"astolfoclient", (String)"clickgui_mode_open");
    public static final SoundEvent MODE_OPEN_SOUND = SoundEvent.of((Identifier)MODE_OPEN_ID);
    private static long lastSliderSoundTime = 0L;

    public static void register() {
        ModSounds.registerSound(ENABLE_ID, ENABLE_SOUND);
        ModSounds.registerSound(DISABLE_ID, DISABLE_SOUND);
        ModSounds.registerSound(CRASH_DETECTION_ID, CRASH_DETECTION_SOUND);
        ModSounds.registerSound(CRASH_DETECTION_HYPHEN_ID, CRASH_DETECTION_HYPHEN_SOUND);
        ModSounds.registerSound(GUI_OPEN_ID, GUI_OPEN_SOUND);
        ModSounds.registerSound(CATEGORY_ID, CATEGORY_SOUND);
        ModSounds.registerSound(MODULE_SELECT_ID, MODULE_SELECT_SOUND);
        ModSounds.registerSound(SLIDER_MOVING_ID, SLIDER_MOVING_SOUND);
        ModSounds.registerSound(SEARCH_CLICK_ID, SEARCH_CLICK_SOUND);
        ModSounds.registerSound(MODE_OPEN_ID, MODE_OPEN_SOUND);
    }

    private static void registerSound(Identifier id, SoundEvent sound) {
        if (!Registries.SOUND_EVENT.containsId(id)) {
            Registry.register((Registry)Registries.SOUND_EVENT, (Identifier)id, (Object)sound);
        }
    }

    public static void playSound(SoundEvent sound, float volume) {
        if (!SoundSettings.isSoundEnabled()) {
            return;
        }
        float master = SoundSettings.getMasterVolume() / 100.0f;
        float vol = volume / 100.0f * master;
        if (vol <= 0.001f) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        mc.execute(() -> {
            try {
                if (mc.getSoundManager() != null) {
                    mc.getSoundManager().play((SoundInstance)PositionedSoundInstance.ui((SoundEvent)sound, (float)1.0f, (float)vol));
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
    }

    public static void playGuiOpen() {
        ModSounds.playSound(GUI_OPEN_SOUND, SoundSettings.getGuiOpenVolume());
    }

    public static void playCategoryChange() {
        ModSounds.playSound(CATEGORY_SOUND, SoundSettings.getCategoryVolume());
    }

    public static void playModuleSelect() {
        ModSounds.playSound(MODULE_SELECT_SOUND, SoundSettings.getModuleSelectVolume());
    }

    public static void playSliderMove() {
        long now = System.currentTimeMillis();
        if (now - lastSliderSoundTime < 65L) {
            return;
        }
        lastSliderSoundTime = now;
        ModSounds.playSound(SLIDER_MOVING_SOUND, SoundSettings.getSliderVolume());
    }

    public static void playSearchClick() {
        ModSounds.playSound(SEARCH_CLICK_SOUND, SoundSettings.getSearchVolume());
    }

    public static void playModeOpen() {
        ModSounds.playSound(MODE_OPEN_SOUND, SoundSettings.getModeOpenVolume());
    }

    public static void playEnable() {
        ModSounds.playSound(ENABLE_SOUND, SoundSettings.getModuleToggleVolume());
    }

    public static void playDisable() {
        ModSounds.playSound(DISABLE_SOUND, SoundSettings.getModuleToggleVolume());
    }

    public static void playCrashDetectionSound() {
        ModSounds.playSound(CRASH_DETECTION_SOUND, 100.0f);
    }
}

