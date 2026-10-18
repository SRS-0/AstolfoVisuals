package xyz.angames.astolfoclient.client.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.command.commands.BindCommand;
import xyz.angames.astolfoclient.client.command.commands.ConfigCommand;
import xyz.angames.astolfoclient.client.command.commands.FakePlayerCommand;
import xyz.angames.astolfoclient.client.command.commands.FriendCommand;
import xyz.angames.astolfoclient.client.command.commands.GpsCommand;
import xyz.angames.astolfoclient.client.command.commands.HelpCommand;
import xyz.angames.astolfoclient.client.command.commands.ThemeCommand;

@Environment(EnvType.CLIENT)
public class CommandManager {
   private final List<Command> commands = new ArrayList<>();
   private final String PREFIX = "$";

   public CommandManager() {
      this.addCommand(new BindCommand());
      this.addCommand(new ThemeCommand());
      this.addCommand(new HelpCommand());
      this.addCommand(new GpsCommand());
      this.addCommand(new FriendCommand());
      this.addCommand(new ConfigCommand());
      this.addCommand(new FakePlayerCommand());
   }

   private void addCommand(Command command) {
      this.commands.add(command);
   }

   public List<Command> getCommands() {
      return this.commands == null ? Collections.emptyList() : this.commands;
   }

   public boolean handleCommand(String message) {
      if (!message.startsWith("$")) {
         return false;
      }

      String[] args = message.substring("$".length()).split("\\s+");
      if (args.length == 0) {
         return false;
      }

      String commandName = args[0];
      String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

      for (Command command : this.commands) {
         if (command.getName().equalsIgnoreCase(commandName)) {
            command.execute(commandArgs);
            return true;
         }

         for (String alias : command.getAliases()) {
            if (alias.equalsIgnoreCase(commandName)) {
               command.execute(commandArgs);
               return true;
            }
         }
      }

      if (Minecraft.getInstance().gui != null) {
         Minecraft.getInstance()
            .gui
            .hud.getChat()
            .addClientSystemMessage(Component.literal("§d[Astolfo] " + ChatFormatting.RED + "Unknown command: " + commandName + ". Try $help."));
      }

      return true;
   }

   public List<String> getSuggestions(String text) {
      if (!text.startsWith("$")) {
         return new ArrayList<>();
      }

      String raw = text.substring("$".length());
      String[] args = raw.split(" ", -1);
      if (args.length != 0 && (args.length != 1 || !args[0].isEmpty())) {
         String commandName = args[0];
         if (args.length == 1) {
            return this.commands
               .stream()
               .map(Command::getName)
               .filter(name -> name.toLowerCase().startsWith(commandName.toLowerCase()))
               .collect(Collectors.toList());
         }

         Command targetCommand = this.commands.stream().filter(c -> c.getName().equalsIgnoreCase(commandName)).findFirst().orElse(null);
         if (targetCommand == null) {
            return new ArrayList<>();
         }

         String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
         String currentArg = commandArgs[commandArgs.length - 1].toLowerCase();
         return targetCommand.suggest(commandArgs).stream().filter(s -> s.toLowerCase().startsWith(currentArg)).collect(Collectors.toList());
      } else {
         return this.commands.stream().map(Command::getName).collect(Collectors.toList());
      }
   }
}
