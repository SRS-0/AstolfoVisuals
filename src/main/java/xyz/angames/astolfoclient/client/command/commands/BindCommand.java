package xyz.angames.astolfoclient.client.command.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Formatting;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.command.Command;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.util.KeyUtils;

@Environment(value=EnvType.CLIENT)
public class BindCommand
extends Command {
    public BindCommand() {
        super("bind", "Manage keybinds for modules", "$bind <add | list | reset | clear> ...", new String[0]);
    }

    @Override
    public void execute(String[] args) {
        String action;
        if (args.length < 1) {
            this.sendSyntax();
            return;
        }
        switch (action = args[0].toLowerCase()) {
            case "add": {
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
            }
            case "list": {
                BindCommand.sendMessage(String.valueOf(Formatting.GOLD) + "--- Key Binds ---");
                if (AstolfoclientClient.clickGuiKeyCode != -1) {
                    BindCommand.sendMessage(String.valueOf(Formatting.GRAY) + "ClickGUI: " + String.valueOf(Formatting.AQUA) + KeyUtils.getKeyName(AstolfoclientClient.clickGuiKeyCode));
                }
                if (AstolfoclientClient.hudEditorKeyCode != -1) {
                    BindCommand.sendMessage(String.valueOf(Formatting.GRAY) + "HudEditor: " + String.valueOf(Formatting.AQUA) + KeyUtils.getKeyName(AstolfoclientClient.hudEditorKeyCode));
                }
                AstolfoclientClient.moduleManager.getModules().stream().filter(m -> m.getKeyCode() != -1).forEach(m -> BindCommand.sendMessage(String.valueOf(Formatting.GRAY) + m.getName() + ": " + String.valueOf(Formatting.AQUA) + KeyUtils.getKeyName(m.getKeyCode())));
                break;
            }
            case "reset": {
                if (args.length != 2) {
                    this.sendError("Usage: $bind reset <module | clickgui | hudeditor>");
                    return;
                }
                this.setBind(args[1], -1, "NONE");
                break;
            }
            case "clear": {
                AstolfoclientClient.moduleManager.getModules().forEach(m -> m.setKeyCode(-1));
                BindCommand.sendMessage(String.valueOf(Formatting.GREEN) + "Cleared all module keybinds! (ClickGUI and HudEditor were kept safe)");
                break;
            }
            default: {
                this.sendError("Unknown action: " + action);
            }
        }
    }

    private void setBind(String name, int key, String keyName) {
        boolean found = false;
        if (name.equalsIgnoreCase("clickgui")) {
            AstolfoclientClient.clickGuiKeyCode = key;
            BindCommand.sendMessage(String.valueOf(Formatting.GREEN) + "Bound ClickGUI to " + (key == -1 ? "NONE" : keyName));
            found = true;
        } else if (name.equalsIgnoreCase("hudeditor")) {
            AstolfoclientClient.hudEditorKeyCode = key;
            BindCommand.sendMessage(String.valueOf(Formatting.GREEN) + "Bound HudEditor to " + (key == -1 ? "NONE" : keyName));
            found = true;
        } else {
            Module module = AstolfoclientClient.moduleManager.getModuleByName(name);
            if (module != null) {
                module.setKeyCode(key);
                BindCommand.sendMessage(String.valueOf(Formatting.GREEN) + "Bound " + module.getName() + " to " + (key == -1 ? "NONE" : keyName));
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
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("reset"))) {
            ArrayList<String> suggestions = new ArrayList<String>();
            suggestions.add("clickgui");
            suggestions.add("hudeditor");
            suggestions.addAll(AstolfoclientClient.moduleManager.getModules().stream().map(Module::getName).map(String::toLowerCase).collect(Collectors.toList()));
            return suggestions;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            String prefix = args[2].toLowerCase();
            return KeyUtils.getKeyNames().stream().map(String::toLowerCase).filter(name -> name.startsWith(prefix) || name.contains(prefix)).sorted((a, b) -> {
                boolean aStarts = a.startsWith(prefix);
                boolean bStarts = b.startsWith(prefix);
                if (aStarts && !bStarts) {
                    return -1;
                }
                if (!aStarts && bStarts) {
                    return 1;
                }
                return a.compareTo((String)b);
            }).collect(Collectors.toList());
        }
        return super.suggest(args);
    }
}

