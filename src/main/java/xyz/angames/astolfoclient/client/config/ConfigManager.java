package xyz.angames.astolfoclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.GuiScaleSettings;
import xyz.angames.astolfoclient.client.config.SoundSettings;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.hud.ArmorHudManager;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.EnumSetting;
import xyz.angames.astolfoclient.client.module.setting.KeybindSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;
import xyz.angames.astolfoclient.client.module.setting.Setting;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(value=EnvType.CLIENT)
public class ConfigManager {
    private final Path mainDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ConfigManager() {
        this.mainDir = FabricLoader.getInstance().getConfigDir().resolve("astolfovisuals");
    }

    public ClientConfig buildCurrentConfig() {
        ClientConfig config = new ClientConfig();
        for (Module module : AstolfoclientClient.moduleManager.getModules()) {
            ModuleData data = new ModuleData();
            data.enabled = module.isEnabled();
            data.keyCode = module.getKeyCode();
            for (Setting setting : module.getSettings()) {
                Object valueToSave = this.getSettingValue(setting);
                if (valueToSave == null) continue;
                data.settings.put(setting.getName(), valueToSave);
            }
            config.modules.put(module.getName(), data);
        }
        config.specialBinds.put("clickgui", AstolfoclientClient.clickGuiKeyCode);
        config.specialBinds.put("hudeditor", AstolfoclientClient.hudEditorKeyCode);
        this.saveHudPositions(config);
        config.theme = ThemeManager.getCurrentTheme().name();
        config.customColor1 = ThemeManager.getCustomColor1Hex();
        config.customColor2 = ThemeManager.getCustomColor2Hex();
        config.watermarkPosition = LogoRenderer.watermarkPosition;
        config.watermarkShowAvatar = LogoRenderer.showAvatar;
        ArrayList<String> enabledNames = new ArrayList<String>();
        for (LogoRenderer.SectionType s : LogoRenderer.enabledSections) {
            enabledNames.add(s.name());
        }
        config.watermarkEnabledSections = enabledNames;
        ArrayList<String> arrayList = new ArrayList<String>();
        for (LogoRenderer.SectionType s : LogoRenderer.getSectionOrder()) {
            arrayList.add(s.name());
        }
        config.watermarkSectionOrder = arrayList;
        config.armorHudLayout = ArmorHudManager.layout;
        config.armorHudWarningGlow = ArmorHudManager.warningGlow;
        config.soundMasterVolume = Float.valueOf(SoundSettings.getMasterVolume());
        config.soundGuiOpenVolume = Float.valueOf(SoundSettings.getGuiOpenVolume());
        config.soundCategoryVolume = Float.valueOf(SoundSettings.getCategoryVolume());
        config.soundModuleSelectVolume = Float.valueOf(SoundSettings.getModuleSelectVolume());
        config.soundSliderVolume = Float.valueOf(SoundSettings.getSliderVolume());
        config.soundSearchVolume = Float.valueOf(SoundSettings.getSearchVolume());
        config.soundModeOpenVolume = Float.valueOf(SoundSettings.getModeOpenVolume());
        config.soundModuleToggleVolume = Float.valueOf(SoundSettings.getModuleToggleVolume());
        config.guiScale = Float.valueOf(GuiScaleSettings.getScale());
        return config;
    }

    public String serializeCurrentConfig() {
        return this.gson.toJson((Object)this.buildCurrentConfig());
    }

    public String saveConfig(String configName) {
        String jsonOutput = this.serializeCurrentConfig();
        try {
            Path configsDir = this.mainDir.resolve("configs");
            if (!Files.exists(configsDir, new LinkOption[0])) {
                Files.createDirectories(configsDir, new FileAttribute[0]);
            }
            File configFile = configsDir.resolve(configName + ".json").toFile();
            try (FileWriter writer = new FileWriter(configFile);){
                writer.write(jsonOutput);
            }
            return null;
        }
        catch (Exception e) {
            return e.getMessage() != null ? e.getMessage() : "Unknown error occurred while saving config.";
        }
    }

    public boolean loadConfig(String configName) {
        try {
            Path configsDir = this.mainDir.resolve("configs");
            File configFile = configsDir.resolve(configName + ".json").toFile();
            if (!configFile.exists()) {
                return false;
            }
            String jsonInput = Files.readString(configFile.toPath());
            if (jsonInput == null || jsonInput.isEmpty()) {
                return false;
            }
            Type type = new TypeToken<ClientConfig>(){}.getType();
            ClientConfig config = (ClientConfig)this.gson.fromJson(jsonInput, type);
            if (config == null) {
                return false;
            }
            MinecraftClient.getInstance().execute(() -> {
                this.applyModuleData(config);
                if (config.specialBinds != null) {
                    AstolfoclientClient.clickGuiKeyCode = config.specialBinds.getOrDefault("clickgui", 260);
                    AstolfoclientClient.hudEditorKeyCode = config.specialBinds.getOrDefault("hudeditor", 79);
                }
                this.applyHudPositions(config);
                if (config.theme != null) {
                    try {
                        ThemeManager.setCurrentTheme(ThemeManager.Theme.valueOf(config.theme));
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (config.customColor1 != null && config.customColor2 != null) {
                    ThemeManager.setCustomColors(config.customColor1, config.customColor2);
                }
                if (config.watermarkPosition != null) {
                    LogoRenderer.watermarkPosition = config.watermarkPosition;
                }
                LogoRenderer.showAvatar = config.watermarkShowAvatar;
                if (config.watermarkEnabledSections != null && !config.watermarkEnabledSections.isEmpty()) {
                    LogoRenderer.enabledSections.clear();
                    LogoRenderer.enabledSections.add(LogoRenderer.SectionType.BRAND);
                    for (String sName : config.watermarkEnabledSections) {
                        try {
                            LogoRenderer.enabledSections.add(LogoRenderer.SectionType.valueOf(sName));
                        }
                        catch (Exception exception) {}
                    }
                }
                if (config.watermarkSectionOrder != null && !config.watermarkSectionOrder.isEmpty()) {
                    ArrayList<LogoRenderer.SectionType> loadedOrder = new ArrayList<LogoRenderer.SectionType>();
                    for (String sName : config.watermarkSectionOrder) {
                        try {
                            loadedOrder.add(LogoRenderer.SectionType.valueOf(sName));
                        }
                        catch (Exception exception) {}
                    }
                    if (!loadedOrder.contains((Object)LogoRenderer.SectionType.BRAND)) {
                        loadedOrder.add(0, LogoRenderer.SectionType.BRAND);
                    }
                    for (LogoRenderer.SectionType secType : LogoRenderer.SectionType.values()) {
                        if (loadedOrder.contains((Object)secType)) continue;
                        loadedOrder.add(secType);
                    }
                    LogoRenderer.setSectionOrder(loadedOrder);
                }
                if (config.armorHudLayout != null) {
                    ArmorHudManager.layout = config.armorHudLayout;
                }
                if (config.armorHudWarningGlow != null) {
                    ArmorHudManager.warningGlow = config.armorHudWarningGlow;
                }
                if (config.soundMasterVolume != null) {
                    SoundSettings.setMasterVolume(config.soundMasterVolume.floatValue());
                }
                if (config.soundGuiOpenVolume != null) {
                    SoundSettings.setGuiOpenVolume(config.soundGuiOpenVolume.floatValue());
                }
                if (config.soundCategoryVolume != null) {
                    SoundSettings.setCategoryVolume(config.soundCategoryVolume.floatValue());
                }
                if (config.soundModuleSelectVolume != null) {
                    SoundSettings.setModuleSelectVolume(config.soundModuleSelectVolume.floatValue());
                }
                if (config.soundSliderVolume != null) {
                    SoundSettings.setSliderVolume(config.soundSliderVolume.floatValue());
                }
                if (config.soundSearchVolume != null) {
                    SoundSettings.setSearchVolume(config.soundSearchVolume.floatValue());
                }
                if (config.soundModeOpenVolume != null) {
                    SoundSettings.setModeOpenVolume(config.soundModeOpenVolume.floatValue());
                }
                if (config.soundModuleToggleVolume != null) {
                    SoundSettings.setModuleToggleVolume(config.soundModuleToggleVolume.floatValue());
                }
                if (config.guiScale != null) {
                    GuiScaleSettings.setScale(config.guiScale.floatValue());
                }
            });
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getCloudConfigs() {
        return this.getLocalConfigs();
    }

    public List<String> getLocalConfigs() {
        ArrayList<String> list;
        block8: {
            list = new ArrayList<String>();
            try {
                Path configsDir = this.mainDir.resolve("configs");
                if (!Files.exists(configsDir, new LinkOption[0])) break block8;
                try (Stream<Path> stream = Files.list(configsDir);){
                    stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                        String name = p.getFileName().toString();
                        list.add(name.substring(0, name.length() - 5));
                    });
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return list;
    }

    public boolean deleteConfig(String configName) {
        try {
            Path configsDir = this.mainDir.resolve("configs");
            File configFile = configsDir.resolve(configName + ".json").toFile();
            if (configFile.exists()) {
                return configFile.delete();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    private Object getSettingValue(Setting setting) {
        if (setting instanceof BooleanSetting) {
            BooleanSetting s = (BooleanSetting)setting;
            return s.get();
        }
        if (setting instanceof NumberSetting) {
            NumberSetting s = (NumberSetting)setting;
            return s.get();
        }
        if (setting instanceof ModeSetting) {
            ModeSetting s = (ModeSetting)setting;
            return s.get();
        }
        if (setting instanceof EnumSetting) {
            EnumSetting s = (EnumSetting)setting;
            return ((Enum)s.getValue()).name();
        }
        if (setting instanceof KeybindSetting) {
            KeybindSetting s = (KeybindSetting)setting;
            return s.getKey();
        }
        return null;
    }

    private void saveHudPositions(ClientConfig config) {
        for (Field field : AstolfoclientClient.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            try {
                Object instance = field.get(null);
                if (instance == null) continue;
                Field xF = instance.getClass().getDeclaredField("x");
                Field yF = instance.getClass().getDeclaredField("y");
                config.hudPositions.put(field.getName() + "_x", xF.getDouble(instance));
                config.hudPositions.put(field.getName() + "_y", yF.getDouble(instance));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void applyModuleData(ClientConfig config) {
        if (config.modules == null) {
            return;
        }
        for (Map.Entry<String, ModuleData> entry : config.modules.entrySet()) {
            Module module = AstolfoclientClient.moduleManager.getModuleByName(entry.getKey());
            if (module == null) continue;
            ModuleData data = entry.getValue();
            module.setEnabled(data.enabled);
            module.setKeyCode(data.keyCode);
            for (Setting setting : module.getSettings()) {
                if (!data.settings.containsKey(setting.getName())) continue;
                String valStr = String.valueOf(data.settings.get(setting.getName()));
                try {
                    if (setting instanceof BooleanSetting) {
                        ((BooleanSetting)setting).set(Boolean.parseBoolean(valStr));
                        continue;
                    }
                    if (setting instanceof NumberSetting) {
                        ((NumberSetting)setting).set(Double.parseDouble(valStr));
                        continue;
                    }
                    if (setting instanceof ModeSetting) {
                        ((ModeSetting)setting).set(valStr);
                        continue;
                    }
                    if (setting instanceof EnumSetting) {
                        ((EnumSetting)setting).setByName(valStr);
                        continue;
                    }
                    if (!(setting instanceof KeybindSetting)) continue;
                    ((KeybindSetting)setting).setKey((int)Double.parseDouble(valStr));
                }
                catch (Exception exception) {}
            }
        }
    }

    private void applyHudPositions(ClientConfig config) {
        if (config.hudPositions == null) {
            return;
        }
        for (Field field : AstolfoclientClient.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            try {
                Object instance = field.get(null);
                if (instance == null) continue;
                String xKey = field.getName() + "_x";
                String yKey = field.getName() + "_y";
                if (config.hudPositions.containsKey(xKey)) {
                    float valX = config.hudPositions.get(xKey).floatValue();
                    try {
                        instance.getClass().getField("x").setFloat(instance, valX);
                    }
                    catch (Exception e) {
                        instance.getClass().getMethod("setX", Float.TYPE).invoke(instance, Float.valueOf(valX));
                    }
                }
                if (!config.hudPositions.containsKey(yKey)) continue;
                float valY = config.hudPositions.get(yKey).floatValue();
                try {
                    instance.getClass().getField("y").setFloat(instance, valY);
                }
                catch (Exception e) {
                    instance.getClass().getMethod("setY", Float.TYPE).invoke(instance, Float.valueOf(valY));
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    public void saveFriends() {
        try {
            if (!Files.exists(this.mainDir, new LinkOption[0])) {
                Files.createDirectories(this.mainDir, new FileAttribute[0]);
            }
            File friendsFile = this.mainDir.resolve("friends.json").toFile();
            try (FileWriter writer = new FileWriter(friendsFile);){
                this.gson.toJson(FriendManager.getFriends(), (Appendable)writer);
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public void loadFriends() {
        try {
            File friendsFile = this.mainDir.resolve("friends.json").toFile();
            if (!friendsFile.exists()) {
                return;
            }
            try (FileReader reader = new FileReader(friendsFile);){
                List<String> loaded = (List)this.gson.fromJson((Reader)reader, new TypeToken<List<String>>(){}.getType());
                if (loaded != null) {
                    FriendManager.clearFriends();
                    loaded.forEach(FriendManager::addFriend);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ClientConfig {
        Map<String, ModuleData> modules = new HashMap<String, ModuleData>();
        Map<String, Integer> specialBinds = new HashMap<String, Integer>();
        Map<String, Double> hudPositions = new HashMap<String, Double>();
        String theme;
        String customColor1;
        String customColor2;
        String watermarkPosition = "TOP_LEFT";
        boolean watermarkShowAvatar = true;
        List<String> watermarkEnabledSections = new ArrayList<String>();
        List<String> watermarkSectionOrder = new ArrayList<String>();
        String armorHudLayout = "VERTICAL";
        Boolean armorHudWarningGlow = true;
        Float soundMasterVolume;
        Float soundGuiOpenVolume;
        Float soundCategoryVolume;
        Float soundModuleSelectVolume;
        Float soundSliderVolume;
        Float soundSearchVolume;
        Float soundModeOpenVolume;
        Float soundModuleToggleVolume;
        Float guiScale;

        private ClientConfig() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ModuleData {
        boolean enabled;
        int keyCode;
        Map<String, Object> settings = new HashMap<String, Object>();

        private ModuleData() {
        }
    }
}

