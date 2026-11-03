package xyz.angames.astolfoclient.client.command.commands;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Formatting;
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
        HelpCommand.sendMessage(String.valueOf(Formatting.BOLD) + "--- Available Commands ---");
        for (Command c : AstolfoclientClient.commandManager.getCommands()) {
            HelpCommand.sendMessage(String.valueOf(Formatting.AQUA) + c.getName() + String.valueOf(Formatting.GRAY) + ": " + c.getDescription());
            HelpCommand.sendMessage(String.valueOf(Formatting.DARK_GRAY) + "  " + c.getSyntax());
        }
    }
}

