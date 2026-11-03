package xyz.angames.astolfoclient.client.command;

import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@Environment(value=EnvType.CLIENT)
public abstract class Command {
    private final String name;
    private final String description;
    private final String syntax;
    private final String[] aliases;
    protected final Minecraft mc = Minecraft.getInstance();

    public Command(String name, String description, String syntax, String ... aliases) {
        this.name = name;
        this.description = description;
        this.syntax = syntax;
        this.aliases = aliases;
    }

    public abstract void execute(String[] var1);

    public List<String> suggest(String[] args) {
        return Collections.emptyList();
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getSyntax() {
        return this.syntax;
    }

    public String[] getAliases() {
        return this.aliases;
    }

    public static void sendMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.hud != null && mc.gui.hud.getChat() != null) {
            mc.gui.hud.getChat().addClientSystemMessage((Component)Component.literal((String)("\u00a7d[Astolfo] \u00a77" + message)));
        }
    }

    protected void sendError(String message) {
        Command.sendMessage(String.valueOf(ChatFormatting.RED) + message);
    }

    protected void sendSyntax() {
        Command.sendMessage(String.valueOf(ChatFormatting.RED) + "Usage: " + this.getSyntax());
    }
}

