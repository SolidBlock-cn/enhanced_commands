package pers.solid.ecmd.util;

import jdk.jfr.Experimental;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

@Experimental
public interface CommandSourceWrapper {
  void sendFeedback(Text message, boolean broadcastToOps);

  PlayerEntity player();

  CommandSource forward();

  static ClientCommandSourceWrapper of(ClientCommandSource clientCommandSource) {
    return new ClientCommandSourceWrapper(clientCommandSource);
  }

  static ServerCommandSourceWrapper of(ServerCommandSource serverCommandSource) {
    return new ServerCommandSourceWrapper(serverCommandSource);
  }

  record ClientCommandSourceWrapper(ClientCommandSource forward) implements CommandSourceWrapper {

    @Override
    public void sendFeedback(Text message, boolean broadcastToOps) {
      MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
    }

    @Override
    public ClientPlayerEntity player() {
      return MinecraftClient.getInstance().player;
    }
  }

  record ServerCommandSourceWrapper(ServerCommandSource forward) implements CommandSourceWrapper {
    @Override
    public void sendFeedback(Text message, boolean broadcastToOps) {
      forward.sendFeedback(message, broadcastToOps);
    }

    @Override
    public PlayerEntity player() {
      return forward.getPlayer();
    }
  }
}
