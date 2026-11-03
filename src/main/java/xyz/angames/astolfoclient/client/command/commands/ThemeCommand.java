package xyz.angames.astolfoclient.client.command.commands;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import xyz.angames.astolfoclient.client.command.Command;
import xyz.angames.astolfoclient.client.config.ThemeManager;

@Environment(value=EnvType.CLIENT)
public class ThemeCommand
extends Command {
    public ThemeCommand() {
        super("theme", "Change client color", "$theme <color | set <#hex>>", new String[0]);
    }

    @Override
    public void execute(String[] args) {
        String action;
        if (args.length < 1) {
            this.sendSyntax();
            return;
        }
        switch (action = args[0].toLowerCase()) {
            case "get": 
            case "color": {
                ThemeCommand.sendMessage(String.valueOf(ChatFormatting.GOLD) + "Current Theme Color: " + String.valueOf(ChatFormatting.WHITE) + ThemeManager.getCustomColor1Hex());
                break;
            }
            case "set": {
                if (args.length != 2) {
                    this.sendError("Usage: $theme set <#hex>");
                    return;
                }
                try {
                    ThemeManager.setCustomColor(args[1]);
                    ThemeCommand.sendMessage(String.valueOf(ChatFormatting.GREEN) + "Theme color set to " + args[1]);
                }
                catch (Exception e) {
                    this.sendError("Invalid hex color: " + args[1]);
                }
                break;
            }
            default: {
                if (args[0].startsWith("#")) {
                    try {
                        ThemeManager.setCustomColor(args[0]);
                        ThemeCommand.sendMessage(String.valueOf(ChatFormatting.GREEN) + "Theme color set to " + args[0]);
                    }
                    catch (Exception e) {
                        this.sendError("Invalid hex color: " + args[0]);
                    }
                    break;
                }
                this.sendError("Usage: $theme set <#hex>");
            }
        }
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return List.of("set", "color");
        }
        return super.suggest(args);
    }
}

