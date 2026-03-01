package pers.solid.ecmd.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;

import java.util.Collection;
import java.util.Collections;

public enum DebugDeOpCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;
  private static final SimpleCommandExceptionType ALREADY_DEOPPED_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("commands.deop.failed"));

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(
        Commands.literal("debug:deop")
            .executes(context -> deop(context.getSource(), Collections.singleton(context.getSource().getPlayerOrException().getGameProfile())))
            .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getOpNames(), builder))
                .executes(context -> deop(context.getSource(), GameProfileArgument.getGameProfiles(context, "targets")))
            )
    );
  }

  private static int deop(CommandSourceStack source, Collection<GameProfile> targets) throws CommandSyntaxException {
    PlayerList playerManager = source.getServer().getPlayerList();
    int i = 0;

    for (GameProfile gameProfile : targets) {
      if (playerManager.isOp(gameProfile)) {
        playerManager.deop(gameProfile);
        ++i;
        source.sendFeedback$ecBridge(() -> Component.translatable("commands.deop.success", targets.iterator().next().getName()), true);
      }
    }

    if (i == 0) {
      throw ALREADY_DEOPPED_EXCEPTION.create();
    } else {
      source.getServer().kickUnlistedPlayers(source);
      return i;
    }
  }
}
