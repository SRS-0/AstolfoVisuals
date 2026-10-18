package xyz.angames.astolfoclient.client.module;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.module.setting.Setting;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(EnvType.CLIENT)
public abstract class Module {
   private String name;
   private String description;
   private boolean enabled;
   private int keyCode = -1;
   private final Module.Category category;
   private final List<Setting> settings = new ArrayList<>();

   public Module(String name, Module.Category category) {
      this(name, "", category);
   }

   public Module(String name, String description, Module.Category category) {
      this.name = name;
      this.description = description;
      this.category = category;
      this.registerSettings();
   }

   protected void registerSettings() {
      for (Field field : this.getClass().getDeclaredFields()) {
         try {
            field.setAccessible(true);
            Object obj = field.get(this);
            if (obj instanceof Setting) {
               this.addSetting((Setting)obj);
            }
         } catch (Exception var6) {
         }
      }
   }

   public void addSetting(Setting setting) {
      if (setting != null && !this.settings.contains(setting)) {
         this.settings.add(setting);
      }
   }

   public void addSettings(Setting... settings) {
      for (Setting s : settings) {
         this.addSetting(s);
      }
   }

   public List<Setting> getSettings() {
      this.registerSettings();
      return this.settings;
   }

   public String getName() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public Module.Category getCategory() {
      return this.category;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         Minecraft mc = Minecraft.getInstance();
         if (this.enabled) {
            this.onEnable();
            ModSounds.playEnable();
         } else {
            this.onDisable();
            ModSounds.playDisable();
         }
      }
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void onTick() {
   }

   public void onRender() {
   }

   public int getKeyCode() {
      return this.keyCode;
   }

   public void setKeyCode(int keyCode) {
      this.keyCode = keyCode;
   }

   @Environment(EnvType.CLIENT)
   public enum Category {
      RENDER,
      MISC;
   }
}
