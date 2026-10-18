package xyz.angames.astolfoclient.client.command.commands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.command.Command;

@Environment(EnvType.CLIENT)
public class HelpCommand extends Command {
   public HelpCommand() {
      super("help", "Lists all commands", "$help");
   }

   @Override
   public void execute(String[] args) {
      sendMessage(ChatFormatting.BOLD + "--- Available Commands ---");

      for (Command c : AstolfoclientClient.commandManager.getCommands()) {
         sendMessage(ChatFormatting.AQUA + c.getName() + ChatFormatting.GRAY + ": " + c.getDescription());
         sendMessage(ChatFormatting.DARK_GRAY + "  " + c.getSyntax());
      }
   }
}
