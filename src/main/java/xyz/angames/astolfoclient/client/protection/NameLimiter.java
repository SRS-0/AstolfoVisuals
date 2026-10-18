package xyz.angames.astolfoclient.client.protection;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class NameLimiter {
   public static final int MAX_LENGTH = 64;

   private NameLimiter() {
   }

   public static Component truncate(Component name) {
      if (name == null) {
         return null;
      }

      String text = name.getString();
      if (text.length() <= 64) {
         return name;
      }

      int cutIndex = 64;
      if (Character.isHighSurrogate(text.charAt(cutIndex - 1))) {
         cutIndex--;
      }

      String cut = text.substring(0, cutIndex) + "...";
      return Component.literal(cut);
   }
}
