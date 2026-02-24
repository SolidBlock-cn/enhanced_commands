package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgument;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.BlockableEventLoopExtension;
import pers.solid.ecmd.history.History;

import java.util.Deque;

import static net.minecraft.commands.Commands.argument;
import static pers.solid.ecmd.argument.KeywordArgsArgument.getKeywordArgs;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum UndoCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final SimpleCommandExceptionType NO_UNDOABLE_HISTORY = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.commands.undo.no_history"));
  public static final SimpleCommandExceptionType NO_REDOABLE_HISTORY = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.commands.redo.no_history"));
  public static final SimpleCommandExceptionType CONFLICT_ARGUMENT = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.commands.undo.conflict_argument", "target", "target-server"));
  public static final KeywordArgsArgument KEYWORD_ARGS = KeywordArgsArgument.builder()
      .addOptionalArg("target", EntityArgument.player(), null)
      .addOptionalArg("target-server", BoolArgumentType.bool(), false)
      .addOptionalArg("immediately", BoolArgumentType.bool(), false)
      .addOptionalArg("undoable", BoolArgumentType.bool(), true)
      .build();

  private static int executeUndo(CommandSourceStack source, KeywordArgs keywordArgs, boolean inverse) throws CommandSyntaxException {
    final HistoryHolder holder = HistoryCommand.getHistoryHolderFromArgs(source, keywordArgs);
    return executeUndo(source, holder, keywordArgs.getBoolean("immediately"), keywordArgs.getBoolean("undoable"), inverse);
  }

  private static int executeUndo(CommandSourceStack source, @Nullable HistoryHolder historyHolder, boolean immediately, boolean undoable, boolean inverse) throws CommandSyntaxException {
    if (historyHolder == null) {
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
        ((BlockableEventLoopExtension) source.getServer()).addIteratorTask$ec(reverse.getFirst());
      }
      if (reverse.getSecond() != null) {
        historyHolder.addRedoableHistory$ec(reverse.getSecond());
      }
    }
    return 1;
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final LiteralArgumentBuilder<CommandSourceStack> historyLiteral = literalR2("history");
    final Command<CommandSourceStack> undoExecution, redoExecution;
    final LiteralCommandNode<CommandSourceStack> undoNode = literalR2("undo")
        .executes(undoExecution = context -> executeUndo(context.getSource(), HistoryHolder.fromSource(context.getSource()), false, true, false))
        .then(argument("keyword_args", KEYWORD_ARGS)
            .executes(context -> executeUndo(context.getSource(), getKeywordArgs(context, "keyword_args"), false))).build();
    final LiteralCommandNode<CommandSourceStack> redoNode = literalR2("redo")
        .executes(redoExecution = context -> executeUndo(context.getSource(), HistoryHolder.fromSource(context.getSource()), false, true, true))
        .then(argument("keyword_args", KEYWORD_ARGS)
            .executes(context -> executeUndo(context.getSource(), getKeywordArgs(context, "keyword_args"), true))).build();
    dispatcher.register(historyLiteral.then(undoNode).then(redoNode));
    dispatcher.register(literalR2("undo").redirect(undoNode).executes(undoExecution));
    dispatcher.register(literalR2("redo").redirect(redoNode).executes(redoExecution));
  }
}
