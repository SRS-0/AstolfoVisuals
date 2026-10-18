package xyz.angames.astolfoclient.client.command.commands;

import com.mojang.authlib.GameProfile;
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

@Environment(EnvType.CLIENT)
public class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", "Manage your friends list", "$friend <add | remove | list | clear> [name]");
   }

   @Override
   public void execute(String[] args) {
      if (args.length < 1) {
         this.sendSyntax();
      } else {
         String action = args[0].toLowerCase();
         switch (action) {
            case "add":
               if (args.length != 2) {
                  this.sendError("Usage: $friend add <name>");
                  return;
               }

               String nameToAdd = args[1];
               if (FriendManager.isFriend(nameToAdd)) {
                  this.sendError(nameToAdd + " is already in your friends list!");
               } else {
                  FriendManager.addFriend(nameToAdd);
                  AstolfoclientClient.configManager.saveFriends();
                  sendMessage(ChatFormatting.GREEN + "Added " + ChatFormatting.AQUA + nameToAdd + ChatFormatting.GREEN + " to friends list.");
               }
               break;
            case "remove":
               if (args.length != 2) {
                  this.sendError("Usage: $friend remove <name>");
                  return;
               }

               String nameToRemove = args[1];
               if (!FriendManager.isFriend(nameToRemove)) {
                  this.sendError(nameToRemove + " is not in your friends list!");
               } else {
                  FriendManager.removeFriend(nameToRemove);
                  AstolfoclientClient.configManager.saveFriends();
                  sendMessage(ChatFormatting.RED + "Removed " + ChatFormatting.AQUA + nameToRemove + ChatFormatting.RED + " from friends list.");
               }
               break;
            case "list":
               if (FriendManager.getFriends().isEmpty()) {
                  sendMessage(ChatFormatting.GRAY + "Your friends list is currently empty.");
               } else {
                  sendMessage(ChatFormatting.GOLD + "--- Friends List ---");

                  for (String friend : FriendManager.getFriends()) {
                     sendMessage(ChatFormatting.GRAY + "- " + ChatFormatting.AQUA + friend);
                  }
               }
               break;
            case "clear":
               FriendManager.clearFriends();
               AstolfoclientClient.configManager.saveFriends();
               sendMessage(ChatFormatting.GREEN + "Cleared all friends from the list.");
               break;
            default:
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
               return this.mc
                  .getConnection()
                  .getOnlinePlayers()
                  .stream()
                  .<GameProfile>map(PlayerInfo::getProfile)
                  .map(profile -> profile.name())
                  .filter(name -> !FriendManager.isFriend(name))
                  .collect(Collectors.toList());
            }
         } else if (args[0].equalsIgnoreCase("remove")) {
            return new ArrayList<>(FriendManager.getFriends());
         }
      }

      return super.suggest(args);
   }
}
