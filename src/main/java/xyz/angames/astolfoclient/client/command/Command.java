package xyz.angames.astolfoclient.client.command;

import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Environment(value=EnvType.CLIENT)
public abstract class Command {
    private final String name;
    private final String description;
    private final String syntax;
    private final String[] aliases;
    protected final MinecraftClient mc = MinecraftClient.getInstance();

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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud != null && mc.inGameHud.getChatHud() != null) {
            mc.inGameHud.getChatHud().addMessage((Text)Text.literal((String)("\u00a7d[Astolfo] \u00a77" + message)));
        }
    }

    protected void sendError(String message) {
        Command.sendMessage(String.valueOf(Formatting.RED) + message);
    }

    protected void sendSyntax() {
        Command.sendMessage(String.valueOf(Formatting.RED) + "Usage: " + this.getSyntax());
    }
}

