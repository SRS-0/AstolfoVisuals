package xyz.angames.astolfoclient.client.command.commands;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.command.Command;

@Environment(EnvType.CLIENT)
public class ConfigCommand extends Command {
   public ConfigCommand() {
      super("config", "Manage local config profiles", "$config <save | load | list | delete> [args]", "cfg");
   }

   @Override
   public void execute(String[] args) {
      if (args.length < 1) {
         this.sendSyntax();
      } else {
         String action = args[0].toLowerCase();
         switch (action) {
            case "save":
               if (args.length < 2) {
                  this.sendError("Usage: $config save <name>");
                  return;
               }

               String saveName = args[1];
               sendMessage(ChatFormatting.GRAY + "Saving local config...");
               new Thread(() -> {
                  String error = AstolfoclientClient.configManager.saveConfig(saveName);
                  if (error == null) {
                     sendMessage(ChatFormatting.GREEN + "Successfully saved config: " + ChatFormatting.AQUA + saveName);
                  } else {
                     this.sendError("Failed to save config: " + error);
                  }
               }).start();
               break;
            case "load":
               if (args.length < 2) {
                  this.sendError("Usage: $config load <name>");
                  return;
               }

               String loadName = args[1];
               sendMessage(ChatFormatting.GRAY + "Loading local config...");
               new Thread(() -> {
                  if (AstolfoclientClient.configManager.loadConfig(loadName)) {
                     sendMessage(ChatFormatting.GREEN + "Successfully loaded config: " + ChatFormatting.AQUA + loadName);
                  } else {
                     this.sendError("Could not find a config named: " + loadName);
                  }
               }).start();
               break;
            case "list":
               sendMessage(ChatFormatting.GRAY + "Fetching your local configs...");
               new Thread(() -> {
                  List<String> configs = AstolfoclientClient.configManager.getLocalConfigs();
                  if (configs.isEmpty()) {
                     sendMessage(ChatFormatting.GRAY + "You have no saved configs.");
                  } else {
                     sendMessage(ChatFormatting.GOLD + "--- Your Configs ---");

                     for (String cfg : configs) {
                        sendMessage(ChatFormatting.GRAY + "- " + ChatFormatting.AQUA + cfg);
                     }
                  }
               }).start();
               break;
            case "delete":
               if (args.length < 2) {
                  this.sendError("Usage: $config delete <name>");
                  return;
               }

               String deleteName = args[1];
               if (deleteName.equalsIgnoreCase("default")) {
                  this.sendError("Cannot delete the default config profile.");
                  return;
               }

               sendMessage(ChatFormatting.GRAY + "Deleting config: " + deleteName + "...");
               new Thread(() -> {
                  if (AstolfoclientClient.configManager.deleteConfig(deleteName)) {
                     sendMessage(ChatFormatting.GREEN + "Successfully deleted config: " + ChatFormatting.AQUA + deleteName);
                  } else {
                     this.sendError("Failed to delete config: " + deleteName + ". Ensure it exists.");
                  }
               }).start();
               break;
            default:
               this.sendError("Unknown action. Use save, load, list, or delete.");
         }
      }
   }

   @Override
   public List<String> suggest(String[] args) {
      if (args.length == 1) {
         return List.of("save", "load", "list", "delete");
      } else {
         return args.length != 2 || !args[0].equalsIgnoreCase("load") && !args[0].equalsIgnoreCase("delete")
            ? super.suggest(args)
            : AstolfoclientClient.configManager.getLocalConfigs();
      }
   }
}
