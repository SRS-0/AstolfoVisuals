package xyz.angames.astolfoclient.client.command.commands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.command.Command;

@Environment(value=EnvType.CLIENT)
public class HelpCommand
extends Command {
    public HelpCommand() {
        super("help", "Lists all commands", "$help", new String[0]);
    }

    @Override
    public void execute(String[] args) {
        HelpCommand.sendMessage(String.valueOf(ChatFormatting.BOLD) + "--- Available Commands ---");
        for (Command c : AstolfoclientClient.commandManager.getCommands()) {
            HelpCommand.sendMessage(String.valueOf(ChatFormatting.AQUA) + c.getName() + String.valueOf(ChatFormatting.GRAY) + ": " + c.getDescription());
            HelpCommand.sendMessage(String.valueOf(ChatFormatting.DARK_GRAY) + "  " + c.getSyntax());
        }
    }
}

