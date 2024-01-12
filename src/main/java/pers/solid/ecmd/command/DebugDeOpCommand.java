package pers.solid.ecmd.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Collection;
import java.util.Collections;

public enum DebugDeOpCommand implements CommandRegistrationCallback {
  INSTANCE;
  private static final SimpleCommandExceptionType ALREADY_DEOPPED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.deop.failed"));

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(
        CommandManager.literal("debug:deop")
            .executes(context -> deop(context.getSource(), Collections.singleton(context.getSource().getPlayerOrThrow().getGameProfile())))
            .then(CommandManager.argument("targets", GameProfileArgumentType.gameProfile())
                .suggests((context, builder) -> CommandSource.suggestMatching(context.getSource().getServer().getPlayerManager().getOpNames(), builder))
                .executes(context -> deop(context.getSource(), GameProfileArgumentType.getProfileArgument(context, "targets")))
            )
    );
  }

  private static int deop(ServerCommandSource source, Collection<GameProfile> targets) throws CommandSyntaxException {
    PlayerManager playerManager = source.getServer().getPlayerManager();
    int i = 0;

    for (GameProfile gameProfile : targets) {
      if (playerManager.isOperator(gameProfile)) {
        playerManager.removeFromOperators(gameProfile);
        ++i;
        source.sendFeedback(Text.translatable("commands.deop.success", ((GameProfile) targets.iterator().next()).getName()), true);
      }
    }

    if (i == 0) {
      throw ALREADY_DEOPPED_EXCEPTION.create();
    } else {
      source.getServer().kickNonWhitelistedPlayers(source);
      return i;
    }
  }
}
