package xyz.angames.astolfoclient.client.command.commands;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity.RemovalReason;
import xyz.angames.astolfoclient.client.command.Command;
import xyz.angames.astolfoclient.client.util.FakePlayerEntity;

@Environment(EnvType.CLIENT)
public class FakePlayerCommand extends Command {
   public FakePlayerCommand() {
      super("fakeplayer", "Spawns/Despawns a client-side fake player for module testing", "$fakeplayer [spawn <name> | despawn]");
   }

   @Override
   public void execute(String[] args) {
      if (this.mc.player == null || this.mc.level == null) {
         this.sendError("You must be in-game to use this command.");
      } else if (args.length == 0) {
         if (FakePlayerEntity.instance != null) {
            this.despawn();
         } else {
            this.spawn("FakePlayer");
         }
      } else {
         String action = args[0].toLowerCase();
         if (action.equals("spawn")) {
            String name = "FakePlayer";
            if (args.length > 1 && !args[1].trim().isEmpty()) {
               name = args[1];
            }

            this.spawn(name);
         } else if (!action.equals("despawn") && !action.equals("remove") && !action.equals("clear") && !action.equals("off")) {
            this.sendError("Unknown action. Usage: " + this.getSyntax());
         } else {
            this.despawn();
         }
      }
   }

   private void spawn(String name) {
      if (FakePlayerEntity.instance != null) {
         this.despawn();
      }

      GameProfile profile = new GameProfile(UUID.randomUUID(), name);
      FakePlayerEntity fakePlayer = new FakePlayerEntity(this.mc.level, profile);
      fakePlayer.copyPosition(this.mc.player);
      fakePlayer.yHeadRot = this.mc.player.yHeadRot;
      fakePlayer.yBodyRot = this.mc.player.yBodyRot;
      fakePlayer.getInventory().replaceWith(this.mc.player.getInventory());
      int fakeId = -987654;
      fakePlayer.setId(fakeId);
      this.mc.level.addEntity(fakePlayer);
      FakePlayerEntity.instance = fakePlayer;
      sendMessage("Spawned fake player: " + ChatFormatting.AQUA + name + ChatFormatting.GRAY + " (ID: " + fakeId + ")");
   }

   private void despawn() {
      if (FakePlayerEntity.instance != null) {
         int id = FakePlayerEntity.instance.getId();
         FakePlayerEntity.instance.discard();
         this.mc.level.removeEntity(id, RemovalReason.DISCARDED);
         FakePlayerEntity.instance = null;
         sendMessage("Despawned fake player.");
      } else {
         this.sendError("No fake player currently spawned.");
      }
   }
}
