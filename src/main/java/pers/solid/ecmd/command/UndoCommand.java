package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.vehicle.CommandBlockMinecartEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.history.History;
import pers.solid.ecmd.mixins.accessor.ServerCommandSourceAccessor;

import java.util.Deque;

import static net.minecraft.server.command.CommandManager.argument;
import static pers.solid.ecmd.argument.KeywordArgsArgumentType.getKeywordArgs;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum UndoCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final SimpleCommandExceptionType NO_UNDOABLE_HISTORY = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.commands.undo.no_history"));
  public static final SimpleCommandExceptionType NO_REDOABLE_HISTORY = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.commands.redo.no_history"));
  public static final SimpleCommandExceptionType CONFLICT_ARGUMENT = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.commands.undo.conflict_argument", "target", "target-server"));
  public static final KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("target", EntityArgumentType.player(), null)
      .addOptionalArg("target-server", BoolArgumentType.bool(), false)
      .addOptionalArg("immediately", BoolArgumentType.bool(), false)
      .addOptionalArg("undoable", BoolArgumentType.bool(), true)
      .build();

  private static int executeUndo(ServerCommandSource source, KeywordArgs keywordArgs, boolean inverse) throws CommandSyntaxException {
    final CommandOutput target = getTargetFromArgs(source, keywordArgs);
    return executeUndo(source, target, keywordArgs.getBoolean("immediately"), keywordArgs.getBoolean("undoable"), inverse);
  }

  static CommandOutput getTargetFromArgs(ServerCommandSource source, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final EntitySelector selector = keywordArgs.getArg("target");
    if (selector == null) {
      if (keywordArgs.getBoolean("target-server")) {
        return source.getServer();
      } else {
        return ((ServerCommandSourceAccessor) source).getOutput();
      }
    } else if (keywordArgs.getBoolean("target-server")) {
      throw CONFLICT_ARGUMENT.create();
    }
    return selector.getPlayer(source);
  }

  private static int executeUndo(ServerCommandSource source, @NotNull CommandOutput target, boolean immediately, boolean undoable, boolean inverse) throws CommandSyntaxException {
    if (target instanceof CommandBlockBlockEntity || target instanceof CommandBlockMinecartEntity) {
      target = source.getServer();
    }
    if (!(target instanceof HistoryHolder historyHolder)) {
      if (inverse) {
        throw NO_REDOABLE_HISTORY.create();
      } else {
        throw NO_UNDOABLE_HISTORY.create();
      }
    }
    if (inverse) {
      historyHolder = historyHolder.inverse();
    }
    final Deque<History> histories = historyHolder.getUndoableHistories$ec();
    final History poll = histories.pollLast();
    if (poll == null) {
      if (inverse) {
        throw NO_REDOABLE_HISTORY.create();
      } else {
        throw NO_UNDOABLE_HISTORY.create();
      }
    } else {
      final Pair<? extends @Nullable IteratorTask<?>, ? extends @Nullable History> reverse = poll.undo(source, immediately, undoable);
      if (reverse.getFirst() != null) {
        ((ThreadExecutorExtension) source.getServer()).addIteratorTask$ec(reverse.getFirst());
      }
      if (reverse.getSecond() != null) {
        historyHolder.addRedoableHistory$ec(reverse.getSecond());
      }
    }
    return 1;
  }

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literalR2("undo")
        .executes(context -> executeUndo(context.getSource(), ((ServerCommandSourceAccessor) context.getSource()).getOutput(), false, true, false))
        .then(argument("keyword_args", KEYWORD_ARGS)
            .executes(context -> executeUndo(context.getSource(), getKeywordArgs(context, "keyword_args"), false))));
    dispatcher.register(literalR2("redo")
        .executes(context -> executeUndo(context.getSource(), ((ServerCommandSourceAccessor) context.getSource()).getOutput(), false, true, true))
        .then(argument("keyword_args", KEYWORD_ARGS)
            .executes(context -> executeUndo(context.getSource(), getKeywordArgs(context, "keyword_args"), true))));
  }
}
