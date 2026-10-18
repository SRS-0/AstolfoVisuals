package xyz.angames.astolfoclient.client.module.modules.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(EnvType.CLIENT)
public class NameProtectModule extends Module {
   public static final String REPLACEMENT = "astolfoclient.top";
   public static final String FRIEND_REPLACEMENT = "astolfoclient.top";

   public NameProtectModule() {
      super("NameProtect", "Replaces your name and friends' names", Module.Category.MISC);
   }

   public static String getProtectedName(String text) {
      if (text == null || text.isEmpty()) {
         return text;
      }

      if (AstolfoclientClient.moduleManager == null) {
         return text;
      }

      Module module = AstolfoclientClient.moduleManager.getModuleByName("NameProtect");
      if (module != null && module.isEnabled()) {
         try {
            return replaceTargets(text);
         } catch (Exception e) {
            return text;
         }
      } else {
         return text;
      }
   }

   public static Component getProtectedText(Component original) {
      if (original == null) {
         return null;
      }

      if (AstolfoclientClient.moduleManager == null) {
         return original;
      }

      Module module = AstolfoclientClient.moduleManager.getModuleByName("NameProtect");
      if (module != null && module.isEnabled()) {
         try {
            return protectTree(original);
         } catch (Exception e) {
            return original;
         }
      } else {
         return original;
      }
   }

   private static Component protectTree(Component text) {
      if (text == null) {
         return null;
      }

      ComponentContents content = text.getContents();
      ComponentContents newContent = content;
      boolean contentChanged = false;
      if (content instanceof LiteralContents literal) {
         String str = literal.text();
         String replaced = replaceTargets(str);
         if (!replaced.equals(str)) {
            newContent = Component.literal(replaced).getContents();
            contentChanged = true;
         }
      } else if (content instanceof TranslatableContents translatableContent) {
         Object[] args = translatableContent.getArgs();
         Object[] newArgs = new Object[args.length];
         boolean argsChanged = false;

         for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof Component argText) {
               Component protectedArg = protectTree(argText);
               newArgs[i] = protectedArg;
               if (protectedArg != argText) {
                  argsChanged = true;
               }
            } else if (arg instanceof String argStr) {
               String protectedArg = replaceTargets(argStr);
               newArgs[i] = protectedArg;
               if (!protectedArg.equals(argStr)) {
                  argsChanged = true;
               }
            } else {
               newArgs[i] = arg;
            }
         }

         if (argsChanged) {
            newContent = new TranslatableContents(translatableContent.getKey(), translatableContent.getFallback(), newArgs);
            contentChanged = true;
         }
      }

      List<Component> siblings = text.getSiblings();
      List<Component> newSiblings = new ArrayList<>(siblings.size());
      boolean siblingsChanged = false;

      for (Component sibling : siblings) {
         Component protectedSibling = protectTree(sibling);
         newSiblings.add(protectedSibling);
         if (protectedSibling != sibling) {
            siblingsChanged = true;
         }
      }

      if (!contentChanged && !siblingsChanged) {
         return text;
      }

      MutableComponent result = MutableComponent.create(newContent).setStyle(text.getStyle());

      for (Component sibling : newSiblings) {
         result.append(sibling);
      }

      return result;
   }

   private static List<NameProtectModule.TargetEntry> getActiveTargets() {
      List<NameProtectModule.TargetEntry> targets = new ArrayList<>();
      Minecraft client = Minecraft.getInstance();
      if (client.getUser() != null && client.getUser().getName() != null) {
         String myName = client.getUser().getName().trim();
         if (!myName.isEmpty() && !myName.equalsIgnoreCase("astolfoclient.top")) {
            targets.add(new NameProtectModule.TargetEntry(myName, "astolfoclient.top"));
         }
      }

      for (String friend : FriendManager.getFriends()) {
         if (friend != null) {
            String f = friend.trim();
            if (!f.isEmpty() && !f.equalsIgnoreCase("astolfoclient.top")) {
               boolean duplicate = false;

               for (NameProtectModule.TargetEntry existing : targets) {
                  if (existing.target.equalsIgnoreCase(f)) {
                     duplicate = true;
                     break;
                  }
               }

               if (!duplicate) {
                  targets.add(new NameProtectModule.TargetEntry(f, "astolfoclient.top"));
               }
            }
         }
      }

      targets.sort((a, b) -> Integer.compare(b.target.length(), a.target.length()));
      return targets;
   }

   private static void findProtectedRanges(String text, String token, List<int[]> ranges) {
      if (token != null && !token.isEmpty()) {
         String lowerText = text.toLowerCase(Locale.ROOT);
         String lowerToken = token.toLowerCase(Locale.ROOT);
         int pos = 0;

         while ((pos = lowerText.indexOf(lowerToken, pos)) != -1) {
            ranges.add(new int[]{pos, pos + token.length()});
            pos += token.length();
         }
      }
   }

   private static boolean overlapsProtected(int start, int end, List<int[]> protectedRanges) {
      for (int[] r : protectedRanges) {
         if (start < r[1] && end > r[0]) {
            return true;
         }
      }

      return false;
   }

   private static boolean isWordChar(char c) {
      return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
   }

   private static boolean isBoundaryBefore(String s, int index) {
      if (index <= 0) {
         return true;
      }

      char prev = s.charAt(index - 1);
      return !isWordChar(prev) ? true : index >= 2 && s.charAt(index - 2) == 167;
   }

   private static boolean isBoundaryAfter(String s, int index) {
      if (index >= s.length()) {
         return true;
      }

      char next = s.charAt(index);
      return next == 167 ? true : !isWordChar(next);
   }

   public static String replaceTargets(String text) {
      if (text != null && !text.isEmpty()) {
         List<NameProtectModule.TargetEntry> targets = getActiveTargets();
         if (targets.isEmpty()) {
            return text;
         }

         List<int[]> protectedRanges = new ArrayList<>();
         findProtectedRanges(text, "astolfoclient.top", protectedRanges);
         if (!"astolfoclient.top".equalsIgnoreCase("astolfoclient.top")) {
            findProtectedRanges(text, "astolfoclient.top", protectedRanges);
         }

         String lowerText = text.toLowerCase(Locale.ROOT);
         List<NameProtectModule.FoundMatch> matches = new ArrayList<>();
         int searchPos = 0;

         while (searchPos < text.length()) {
            int earliestStart = -1;
            int earliestEnd = -1;
            NameProtectModule.TargetEntry bestTarget = null;

            for (NameProtectModule.TargetEntry entry : targets) {
               int idx = lowerText.indexOf(entry.lowerTarget, searchPos);
               if (idx != -1) {
                  int end = idx + entry.target.length();
                  boolean validBefore = !isWordChar(entry.target.charAt(0)) || isBoundaryBefore(text, idx);
                  boolean validAfter = !isWordChar(entry.target.charAt(entry.target.length() - 1)) || isBoundaryAfter(text, end);
                  if (validBefore && validAfter && !overlapsProtected(idx, end, protectedRanges) && (earliestStart == -1 || idx < earliestStart)) {
                     earliestStart = idx;
                     earliestEnd = end;
                     bestTarget = entry;
                  }
               }
            }

            if (earliestStart == -1 || bestTarget == null) {
               break;
            }

            matches.add(new NameProtectModule.FoundMatch(earliestStart, earliestEnd, bestTarget.replacement));
            searchPos = earliestEnd;
         }

         if (matches.isEmpty()) {
            return text;
         }

         StringBuilder sb = new StringBuilder(text.length() + matches.size() * 16);
         int lastPos = 0;

         for (NameProtectModule.FoundMatch m : matches) {
            sb.append(text, lastPos, m.start);
            sb.append(m.replacement);
            lastPos = m.end;
         }

         sb.append(text, lastPos, text.length());
         return sb.toString();
      } else {
         return text;
      }
   }

   @Environment(EnvType.CLIENT)
   private static class FoundMatch {
      final int start;
      final int end;
      final String replacement;

      FoundMatch(int start, int end, String replacement) {
         this.start = start;
         this.end = end;
         this.replacement = replacement;
      }
   }

   @Environment(EnvType.CLIENT)
   private static class TargetEntry {
      final String target;
      final String replacement;
      final String lowerTarget;

      TargetEntry(String target, String replacement) {
         this.target = target;
         this.replacement = replacement;
         this.lowerTarget = target.toLowerCase(Locale.ROOT);
      }
   }
}
