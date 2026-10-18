package xyz.angames.astolfoclient.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
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

@Environment(EnvType.CLIENT)
public class ConfigManager {
   private final Path mainDir;
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

   public ConfigManager() {
      this.mainDir = FabricLoader.getInstance().getConfigDir().resolve("astolfovisuals");
   }

   public ConfigManager.ClientConfig buildCurrentConfig() {
      ConfigManager.ClientConfig config = new ConfigManager.ClientConfig();

      for (Module module : AstolfoclientClient.moduleManager.getModules()) {
         ConfigManager.ModuleData data = new ConfigManager.ModuleData();
         data.enabled = module.isEnabled();
         data.keyCode = module.getKeyCode();

         for (Setting setting : module.getSettings()) {
            Object valueToSave = this.getSettingValue(setting);
            if (valueToSave != null) {
               data.settings.put(setting.getName(), valueToSave);
            }
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
      List<String> enabledNames = new ArrayList<>();

      for (LogoRenderer.SectionType s : LogoRenderer.enabledSections) {
         enabledNames.add(s.name());
      }

      config.watermarkEnabledSections = enabledNames;
      List<String> orderNames = new ArrayList<>();

      for (LogoRenderer.SectionType s : LogoRenderer.getSectionOrder()) {
         orderNames.add(s.name());
      }

      config.watermarkSectionOrder = orderNames;
      config.armorHudLayout = ArmorHudManager.layout;
      config.armorHudWarningGlow = ArmorHudManager.warningGlow;
      config.soundMasterVolume = SoundSettings.getMasterVolume();
      config.soundGuiOpenVolume = SoundSettings.getGuiOpenVolume();
      config.soundCategoryVolume = SoundSettings.getCategoryVolume();
      config.soundModuleSelectVolume = SoundSettings.getModuleSelectVolume();
      config.soundSliderVolume = SoundSettings.getSliderVolume();
      config.soundSearchVolume = SoundSettings.getSearchVolume();
      config.soundModeOpenVolume = SoundSettings.getModeOpenVolume();
      config.soundModuleToggleVolume = SoundSettings.getModuleToggleVolume();
      config.guiScale = GuiScaleSettings.getScale();
      return config;
   }

   public String serializeCurrentConfig() {
      return this.gson.toJson(this.buildCurrentConfig());
   }

   public String saveConfig(String configName) {
      String jsonOutput = this.serializeCurrentConfig();

      try {
         Path configsDir = this.mainDir.resolve("configs");
         if (!Files.exists(configsDir)) {
            Files.createDirectories(configsDir);
         }

         File configFile = configsDir.resolve(configName + ".json").toFile();

         try (FileWriter writer = new FileWriter(configFile)) {
            writer.write(jsonOutput);
         }

         return null;
      } catch (Exception e) {
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
         if (jsonInput != null && !jsonInput.isEmpty()) {
            Type type = (new TypeToken<ConfigManager.ClientConfig>() {}).getType();
            ConfigManager.ClientConfig config = (ConfigManager.ClientConfig)this.gson.fromJson(jsonInput, type);
            if (config == null) {
               return false;
            }

            Minecraft.getInstance().execute(() -> {
               this.applyModuleData(config);
               if (config.specialBinds != null) {
                  AstolfoclientClient.clickGuiKeyCode = config.specialBinds.getOrDefault("clickgui", 260);
                  AstolfoclientClient.hudEditorKeyCode = config.specialBinds.getOrDefault("hudeditor", 79);
               }

               this.applyHudPositions(config);
               if (config.theme != null) {
                  try {
                     ThemeManager.setCurrentTheme(ThemeManager.Theme.valueOf(config.theme));
                  } catch (Exception var9) {
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
                     } catch (Exception var8) {
                     }
                  }
               }

               if (config.watermarkSectionOrder != null && !config.watermarkSectionOrder.isEmpty()) {
                  List<LogoRenderer.SectionType> loadedOrder = new ArrayList<>();

                  for (String sName : config.watermarkSectionOrder) {
                     try {
                        loadedOrder.add(LogoRenderer.SectionType.valueOf(sName));
                     } catch (Exception var7x) {
                     }
                  }

                  if (!loadedOrder.contains(LogoRenderer.SectionType.BRAND)) {
                     loadedOrder.add(0, LogoRenderer.SectionType.BRAND);
                  }

                  for (LogoRenderer.SectionType secType : LogoRenderer.SectionType.values()) {
                     if (!loadedOrder.contains(secType)) {
                        loadedOrder.add(secType);
                     }
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
                  SoundSettings.setMasterVolume(config.soundMasterVolume);
               }

               if (config.soundGuiOpenVolume != null) {
                  SoundSettings.setGuiOpenVolume(config.soundGuiOpenVolume);
               }

               if (config.soundCategoryVolume != null) {
                  SoundSettings.setCategoryVolume(config.soundCategoryVolume);
               }

               if (config.soundModuleSelectVolume != null) {
                  SoundSettings.setModuleSelectVolume(config.soundModuleSelectVolume);
               }

               if (config.soundSliderVolume != null) {
                  SoundSettings.setSliderVolume(config.soundSliderVolume);
               }

               if (config.soundSearchVolume != null) {
                  SoundSettings.setSearchVolume(config.soundSearchVolume);
               }

               if (config.soundModeOpenVolume != null) {
                  SoundSettings.setModeOpenVolume(config.soundModeOpenVolume);
               }

               if (config.soundModuleToggleVolume != null) {
                  SoundSettings.setModuleToggleVolume(config.soundModuleToggleVolume);
               }

               if (config.guiScale != null) {
                  GuiScaleSettings.setScale(config.guiScale);
               }
            });
            return true;
         } else {
            return false;
         }
      } catch (Exception e) {
         e.printStackTrace();
         return false;
      }
   }

   public List<String> getCloudConfigs() {
      return this.getLocalConfigs();
   }

   public List<String> getLocalConfigs() {
      List<String> list = new ArrayList<>();

      try {
         Path configsDir = this.mainDir.resolve("configs");
         if (Files.exists(configsDir)) {
            try (Stream<Path> stream = Files.list(configsDir)) {
               stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                  String name = p.getFileName().toString();
                  list.add(name.substring(0, name.length() - 5));
               });
            }
         }
      } catch (Exception var8) {
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
      } catch (Exception var4) {
      }

      return false;
   }

   private Object getSettingValue(Setting setting) {
      if (setting instanceof BooleanSetting s) {
         return s.get();
      } else if (setting instanceof NumberSetting s) {
         return s.get();
      } else if (setting instanceof ModeSetting s) {
         return s.get();
      } else if (setting instanceof EnumSetting<?> s) {
         return s.getValue().name();
      } else {
         return setting instanceof KeybindSetting s ? s.getKey() : null;
      }
   }

   private void saveHudPositions(ConfigManager.ClientConfig config) {
      for (Field field : AstolfoclientClient.class.getDeclaredFields()) {
         if (Modifier.isStatic(field.getModifiers())) {
            try {
               Object instance = field.get(null);
               if (instance != null) {
                  Field xF = instance.getClass().getDeclaredField("x");
                  Field yF = instance.getClass().getDeclaredField("y");
                  config.hudPositions.put(field.getName() + "_x", xF.getDouble(instance));
                  config.hudPositions.put(field.getName() + "_y", yF.getDouble(instance));
               }
            } catch (Exception var9) {
            }
         }
      }
   }

   private void applyModuleData(ConfigManager.ClientConfig config) {
      if (config.modules != null) {
         for (Entry<String, ConfigManager.ModuleData> entry : config.modules.entrySet()) {
            Module module = AstolfoclientClient.moduleManager.getModuleByName(entry.getKey());
            if (module != null) {
               ConfigManager.ModuleData data = entry.getValue();
               module.setEnabled(data.enabled);
               module.setKeyCode(data.keyCode);

               for (Setting setting : module.getSettings()) {
                  if (data.settings.containsKey(setting.getName())) {
                     String valStr = String.valueOf(data.settings.get(setting.getName()));

                     try {
                        if (setting instanceof BooleanSetting) {
                           ((BooleanSetting)setting).set(Boolean.parseBoolean(valStr));
                        } else if (setting instanceof NumberSetting) {
                           ((NumberSetting)setting).set(Double.parseDouble(valStr));
                        } else if (setting instanceof ModeSetting) {
                           ((ModeSetting)setting).set(valStr);
                        } else if (setting instanceof EnumSetting) {
                           ((EnumSetting)setting).setByName(valStr);
                        } else if (setting instanceof KeybindSetting) {
                           ((KeybindSetting)setting).setKey((int)Double.parseDouble(valStr));
                        }
                     } catch (Exception var10) {
                     }
                  }
               }
            }
         }
      }
   }

   private void applyHudPositions(ConfigManager.ClientConfig config) {
      if (config.hudPositions != null) {
         for (Field field : AstolfoclientClient.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
               try {
                  Object instance = field.get(null);
                  if (instance != null) {
                     String xKey = field.getName() + "_x";
                     String yKey = field.getName() + "_y";
                     if (config.hudPositions.containsKey(xKey)) {
                        float valX = config.hudPositions.get(xKey).floatValue();

                        try {
                           instance.getClass().getField("x").setFloat(instance, valX);
                        } catch (Exception e) {
                           instance.getClass().getMethod("setX", float.class).invoke(instance, valX);
                        }
                     }

                     if (config.hudPositions.containsKey(yKey)) {
                        float valY = config.hudPositions.get(yKey).floatValue();

                        try {
                           instance.getClass().getField("y").setFloat(instance, valY);
                        } catch (Exception e) {
                           instance.getClass().getMethod("setY", float.class).invoke(instance, valY);
                        }
                     }
                  }
               } catch (Exception var13) {
               }
            }
         }
      }
   }

   public void saveFriends() {
      try {
         if (!Files.exists(this.mainDir)) {
            Files.createDirectories(this.mainDir);
         }

         File friendsFile = this.mainDir.resolve("friends.json").toFile();

         try (FileWriter writer = new FileWriter(friendsFile)) {
            this.gson.toJson(FriendManager.getFriends(), writer);
         }
      } catch (IOException var7) {
      }
   }

   public void loadFriends() {
      try {
         File friendsFile = this.mainDir.resolve("friends.json").toFile();
         if (!friendsFile.exists()) {
            return;
         }

         try (FileReader reader = new FileReader(friendsFile)) {
            List<String> loaded = (List<String>)this.gson.fromJson(reader, (new TypeToken<List<String>>() {}).getType());
            if (loaded != null) {
               FriendManager.clearFriends();
               loaded.forEach(FriendManager::addFriend);
            }
         }
      } catch (Exception var7) {
      }
   }

   @Environment(EnvType.CLIENT)
   private static class ClientConfig {
      Map<String, ConfigManager.ModuleData> modules = new HashMap<>();
      Map<String, Integer> specialBinds = new HashMap<>();
      Map<String, Double> hudPositions = new HashMap<>();
      String theme;
      String customColor1;
      String customColor2;
      String watermarkPosition = "TOP_LEFT";
      boolean watermarkShowAvatar = true;
      List<String> watermarkEnabledSections = new ArrayList<>();
      List<String> watermarkSectionOrder = new ArrayList<>();
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
   }

   @Environment(EnvType.CLIENT)
   private static class ModuleData {
      boolean enabled;
      int keyCode;
      Map<String, Object> settings = new HashMap<>();
   }
}
