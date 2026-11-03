package xyz.angames.astolfoclient.client.command.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.command.Command;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(value=EnvType.CLIENT)
public class FriendCommand
extends Command {
    public FriendCommand() {
        super("friend", "Manage your friends list", "$friend <add | remove | list | clear> [name]", new String[0]);
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
                if (args.length != 2) {
                    this.sendError("Usage: $friend add <name>");
                    return;
                }
                String nameToAdd = args[1];
                if (FriendManager.isFriend(nameToAdd)) {
                    this.sendError(nameToAdd + " is already in your friends list!");
                    break;
                }
                FriendManager.addFriend(nameToAdd);
                AstolfoclientClient.configManager.saveFriends();
                FriendCommand.sendMessage(String.valueOf(ChatFormatting.GREEN) + "Added " + String.valueOf(ChatFormatting.AQUA) + nameToAdd + String.valueOf(ChatFormatting.GREEN) + " to friends list.");
                break;
            }
            case "remove": {
                if (args.length != 2) {
                    this.sendError("Usage: $friend remove <name>");
                    return;
                }
                String nameToRemove = args[1];
                if (!FriendManager.isFriend(nameToRemove)) {
                    this.sendError(nameToRemove + " is not in your friends list!");
                    break;
                }
                FriendManager.removeFriend(nameToRemove);
                AstolfoclientClient.configManager.saveFriends();
                FriendCommand.sendMessage(String.valueOf(ChatFormatting.RED) + "Removed " + String.valueOf(ChatFormatting.AQUA) + nameToRemove + String.valueOf(ChatFormatting.RED) + " from friends list.");
                break;
            }
            case "list": {
                if (FriendManager.getFriends().isEmpty()) {
                    FriendCommand.sendMessage(String.valueOf(ChatFormatting.GRAY) + "Your friends list is currently empty.");
                    break;
                }
                FriendCommand.sendMessage(String.valueOf(ChatFormatting.GOLD) + "--- Friends List ---");
                for (String friend : FriendManager.getFriends()) {
                    FriendCommand.sendMessage(String.valueOf(ChatFormatting.GRAY) + "- " + String.valueOf(ChatFormatting.AQUA) + friend);
                }
                break;
            }
            case "clear": {
                FriendManager.clearFriends();
                AstolfoclientClient.configManager.saveFriends();
                FriendCommand.sendMessage(String.valueOf(ChatFormatting.GREEN) + "Cleared all friends from the list.");
                break;
            }
            default: {
                this.sendError("Unknown action: " + action);
            }
        }
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return List.of("add", "remove", "list", "clear");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add")) {
                if (this.mc.getConnection() != null) {
                    return this.mc.getConnection().getOnlinePlayers().stream().map(PlayerInfo::getProfile).map(profile -> profile.name()).filter(name -> !FriendManager.isFriend(name)).collect(Collectors.toList());
                }
            } else if (args[0].equalsIgnoreCase("remove")) {
                return new ArrayList<String>(FriendManager.getFriends());
            }
        }
        return super.suggest(args);
    }
}

