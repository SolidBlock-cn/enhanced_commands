package pers.solid.ecmd.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;
import java.util.Collections;

public enum DebugOpCommand implements CommandRegistrationCallback {
  INSTANCE;

  private static final SimpleCommandExceptionType ALREADY_OPPED_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.op.failed"));

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(
        Commands.literal("debug:op")
            .executes(context -> op(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException().getGameProfile())))
            .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                .suggests((context, builder) -> {
                      PlayerList playerManager = context.getSource().getServer().getPlayerList();
                      return SharedSuggestionProvider.suggest(
                          playerManager.getPlayers()
                              .stream()
                              .filter(player -> !playerManager.isOp(player.getGameProfile()))
                              .map(player -> player.getGameProfile().getName()),
                          builder
                      );
                    }
                )
                .executes(context -> op(context.getSource(), GameProfileArgument.getGameProfiles(context, "targets")))
            )
    );
  }

  private static int op(CommandSourceStack source, Collection<GameProfile> targets) throws CommandSyntaxException {
    PlayerList playerManager = source.getServer().getPlayerList();
    int i = 0;
    for (GameProfile gameProfile : targets) {
      if (!playerManager.isOp(gameProfile)) {
        playerManager.op(gameProfile);
        ++i;
        source.sendFeedback$ecBridge(() -> Component.translatable("commands.op.success", targets.iterator().next().getName()), true);
      }
    }
    if (i == 0) {
      throw ALREADY_OPPED_EXCEPTION.create();
    } else {
      return i;
    }
  }
}
