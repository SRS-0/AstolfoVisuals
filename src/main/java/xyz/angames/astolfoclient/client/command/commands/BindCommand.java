package xyz.angames.astolfoclient.client.command.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.command.Command;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.util.KeyUtils;

@Environment(EnvType.CLIENT)
public class BindCommand extends Command {
   public BindCommand() {
      super("bind", "Manage keybinds for modules", "$bind <add | list | reset | clear> ...");
   }

   @Override
   public void execute(String[] args) {
      if (args.length < 1) {
         this.sendSyntax();
      } else {
         String action = args[0].toLowerCase();
         switch (action) {
            case "add":
               if (args.length != 3) {
                  this.sendError("Usage: $bind add <module | clickgui | hudeditor> <key>");
                  return;
               }

               String moduleName = args[1];
               String keyName = args[2].toUpperCase();
               int keyCode = KeyUtils.getKeyCode(keyName);
               if (keyCode == -1) {
                  this.sendError("Unknown key: " + keyName);
                  return;
               }

               this.setBind(moduleName, keyCode, keyName);
               break;
            case "list":
               sendMessage(ChatFormatting.GOLD + "--- Key Binds ---");
               if (AstolfoclientClient.clickGuiKeyCode != -1) {
                  sendMessage(ChatFormatting.GRAY + "ClickGUI: " + ChatFormatting.AQUA + KeyUtils.getKeyName(AstolfoclientClient.clickGuiKeyCode));
               }

               if (AstolfoclientClient.hudEditorKeyCode != -1) {
                  sendMessage(ChatFormatting.GRAY + "HudEditor: " + ChatFormatting.AQUA + KeyUtils.getKeyName(AstolfoclientClient.hudEditorKeyCode));
               }

               AstolfoclientClient.moduleManager
                  .getModules()
                  .stream()
                  .filter(m -> m.getKeyCode() != -1)
                  .forEach(m -> sendMessage(ChatFormatting.GRAY + m.getName() + ": " + ChatFormatting.AQUA + KeyUtils.getKeyName(m.getKeyCode())));
               break;
            case "reset":
               if (args.length != 2) {
                  this.sendError("Usage: $bind reset <module | clickgui | hudeditor>");
                  return;
               }

               this.setBind(args[1], -1, "NONE");
               break;
            case "clear":
               AstolfoclientClient.moduleManager.getModules().forEach(m -> m.setKeyCode(-1));
               sendMessage(ChatFormatting.GREEN + "Cleared all module keybinds! (ClickGUI and HudEditor were kept safe)");
               break;
            default:
               this.sendError("Unknown action: " + action);
         }
      }
   }

   private void setBind(String name, int key, String keyName) {
      boolean found = false;
      if (name.equalsIgnoreCase("clickgui")) {
         AstolfoclientClient.clickGuiKeyCode = key;
         sendMessage(ChatFormatting.GREEN + "Bound ClickGUI to " + (key == -1 ? "NONE" : keyName));
         found = true;
      } else if (name.equalsIgnoreCase("hudeditor")) {
         AstolfoclientClient.hudEditorKeyCode = key;
         sendMessage(ChatFormatting.GREEN + "Bound HudEditor to " + (key == -1 ? "NONE" : keyName));
         found = true;
      } else {
         Module module = AstolfoclientClient.moduleManager.getModuleByName(name);
         if (module != null) {
            module.setKeyCode(key);
            sendMessage(ChatFormatting.GREEN + "Bound " + module.getName() + " to " + (key == -1 ? "NONE" : keyName));
            found = true;
         } else {
            this.sendError("Module not found: " + name);
         }
      }
   }

   @Override
   public List<String> suggest(String[] args) {
      if (args.length == 1) {
         return List.of("add", "list", "reset", "clear");
      }

      if (args.length != 2 || !args[0].equalsIgnoreCase("add") && !args[0].equalsIgnoreCase("reset")) {
         if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            String prefix = args[2].toLowerCase();
            return KeyUtils.getKeyNames().stream().map(String::toLowerCase).filter(name -> name.startsWith(prefix) || name.contains(prefix)).sorted((a, b) -> {
               boolean aStarts = a.startsWith(prefix);
               boolean bStarts = b.startsWith(prefix);
               if (aStarts && !bStarts) {
                  return -1;
               } else {
                  return !aStarts && bStarts ? 1 : a.compareTo(b);
               }
            }).collect(Collectors.toList());
         } else {
            return super.suggest(args);
         }
      } else {
         List<String> suggestions = new ArrayList<>();
         suggestions.add("clickgui");
         suggestions.add("hudeditor");
         suggestions.addAll(AstolfoclientClient.moduleManager.getModules().stream().map(Module::getName).map(String::toLowerCase).collect(Collectors.toList()));
         return suggestions;
      }
   }
}
