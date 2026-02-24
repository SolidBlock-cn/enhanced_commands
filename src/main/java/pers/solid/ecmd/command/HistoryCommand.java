package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgument;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.history.History;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.argument.KeywordArgsArgument.getKeywordArgs;
import static pers.solid.ecmd.argument.StringEnumArgument.stringEnum;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum HistoryCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final KeywordArgsArgument CLEAR_KEYWORD_ARGS = KeywordArgsArgument.builder()
      .addOptionalArg("target", EntityArgument.player(), null)
      .addOptionalArg("target-server", bool(), false)
      .addOptionalArg("type", stringEnum("undo", "redo"), "undo")
      .build();

  public static final KeywordArgsArgument LIST_KEYWORD_ARGS = KeywordArgsArgument.builder()
      .addAll(CLEAR_KEYWORD_ARGS)
      .addOptionalArg("from", integer(0), 0)
      .addOptionalArg("limit", integer(0, 50), 7)
      .addOptionalArg("sort", stringEnum("latest", "oldest"), "latest")
      .build();

  public static @NotNull HistoryHolder getHistoryHolderFromArgs(CommandSourceStack source, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final EntitySelector selector = keywordArgs.getArg("target");
    if (selector == null) {
      if (keywordArgs.getBoolean("target-server")) {
        return ((HistoryHolder) source.getServer());
      } else {
        return fromSourceOrThrow(source);
      }
    } else if (keywordArgs.getBoolean("target-server")) {
      throw UndoCommand.CONFLICT_ARGUMENT.create();
    }
    final ServerPlayer player = selector.findSinglePlayer(source);
    if (player instanceof HistoryHolder holder) {
      return holder;
    } else {
      throw NOT_SUPPORTED_HISTORY.create(targetName(player));
    }
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(literalR2("history")
        .executes(context -> executeList(context.getSource()))
        .then(literal("clear")
            .executes(context -> executeClear(context.getSource(), fromSourceOrThrow(context.getSource()), false))
            .then(argument("keyword_args", CLEAR_KEYWORD_ARGS)
                .executes(context -> executeClear(context.getSource(), getKeywordArgs(context, "keyword_args")))))
        .then(literal("list")
            .executes(context -> executeList(context.getSource()))
            .then(argument("keyword_args", LIST_KEYWORD_ARGS)
                .executes(context -> executeList(context.getSource(), getKeywordArgs(context, "keyword_args"))))));
  }

  private static Component targetName(Object object) {
    if (object instanceof Entity entity) {
      return TextUtil.styled(entity.getDisplayName(), Styles.TARGET);
    } else if (object instanceof MinecraftServer server) {
      return Component.literal(server.name()).withStyle(Styles.TARGET);
    } else {
      return Component.literal("<unknown source>").withStyle(Styles.TARGET);
    }
  }

  public static final DynamicCommandExceptionType NOT_SUPPORTED_HISTORY = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.commands.history.not_supported", o));

  private static int executeClear(CommandSourceStack source, KeywordArgs keywordArgs) throws CommandSyntaxException {
    HistoryHolder holder = getHistoryHolderFromArgs(source, keywordArgs);
    return executeClear(source, holder, "redo".equals(keywordArgs.getArg("type")));
  }

  private static int executeClear(CommandSourceStack source, @NotNull HistoryHolder historyHolder, boolean redo) {
    final Component targetName = targetName(historyHolder);
    if (redo) {
      historyHolder = historyHolder.inverse();
    }
    Deque<History> histories = historyHolder.getUndoableHistories$ec();
    final int size = histories.size();
    histories.clear();
    source.sendSuccess(() -> Component.translatable("enhanced_commands.commands.history.cleared", targetName, size), true);
    return 1;
  }

  private static int executeList(CommandSourceStack source, KeywordArgs keywordArgs) throws CommandSyntaxException {
    HistoryHolder holder = getHistoryHolderFromArgs(source, keywordArgs);
    return executeList(source, holder, keywordArgs.getInt("from"), keywordArgs.getInt("limit"), "latest".equals(keywordArgs.getArg("sort")), "redo".equals(keywordArgs.getArg("type")));
  }

  private static int executeList(CommandSourceStack source) throws CommandSyntaxException {
    return executeList(source, fromSourceOrThrow(source), 0, 7, true, false);
  }

  private static @NotNull HistoryHolder fromSourceOrThrow(CommandSourceStack source) throws CommandSyntaxException {
    final HistoryHolder holder = HistoryHolder.fromSource(source);
    if (holder != null) {
      return holder;
    }
    if (source.getEntity() != null) {
      throw NOT_SUPPORTED_HISTORY.create(targetName(source.getEntity()));
    } else if (source.getServer() != null) {
      throw NOT_SUPPORTED_HISTORY.create(targetName(source.getServer()));
    } else {
      throw NOT_SUPPORTED_HISTORY.create(targetName(null));
    }
  }

  private static int executeList(CommandSourceStack source, @NotNull HistoryHolder historyHolder, int from, int limit, boolean latestFirst, boolean redo) {
    final Component targetName = targetName(historyHolder);
    if (redo) {
      historyHolder = historyHolder.inverse();
    }
    Deque<History> histories = historyHolder.getUndoableHistories$ec();
    if (latestFirst) {
      histories = histories.reversed();
    }


    final int size = histories.size();
    Iterable<History> iterable = Iterables.limit(Iterables.skip(histories, from), limit);
    @NotNull HistoryHolder finalHistoryHolder = historyHolder;
    source.sendFeedback$ecBridge(() -> {
      if (size == 0) {
        return Component.translatable("enhanced_commands.commands.history.no_history", targetName);
      }
      final Component sizeText = TextUtil.literal(size).withStyle(Styles.RESULT);
      List<Component> messages = new ArrayList<>();

      if (redo) {
        messages.add(Component.translatable("enhanced_commands.commands.history.caption.redo", targetName, sizeText).enhanced$$());
      } else {
        messages.add(Component.translatable("enhanced_commands.commands.history.caption.undo", targetName, sizeText).enhanced$$());
      }

      int i = from + 1;
      for (History history : iterable) {
        messages.add(Component.literal(" #" + i + " - ").withStyle(ChatFormatting.GRAY)
            .append(history.getName()));
        i++;
      }

      List<Component> buttons = new ArrayList<>(3);

      final StringBuilder sb = new StringBuilder();
      if (finalHistoryHolder instanceof MinecraftServer) {
        sb.append(" target-server=true");
      } else if (finalHistoryHolder instanceof ServerPlayer player) {
        sb.append(" target=").append(player.getGameProfile().getName());
      } else {
        // 无法描述对象（通常不是这样的情况，但是需要考虑）
        buttons = null;
      }
      if (redo) {
        sb.append(" type=redo");
      }
      if (!latestFirst) {
        sb.append(" sort=oldest");
      }

      if (buttons != null) {
        if (from > 0) {
          buttons.add(Component.translatable("enhanced_commands.commands.history.previous").withStyle(style -> style
              .withColor(ChatFormatting.YELLOW)
              .withUnderlined(true)
              .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history list" + sb +
                  " from=" + Math.max(0, from - limit) +
                  " limit=" + Math.min(from, limit)))
              .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.history.previous.tooltip", Math.min(from, limit)).enhanced$$()))));
        }
        buttons.add(Component.translatable("enhanced_commands.commands.history.clear").withStyle(style -> style
            .withColor(ChatFormatting.RED)
            .withUnderlined(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history clear" + sb))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.history.clear.tooltip", size).enhanced$$()))));
        if (from + limit < size) {
          buttons.add(Component.translatable("enhanced_commands.commands.history.next").withStyle(style -> style
              .withColor(ChatFormatting.YELLOW)
              .withUnderlined(true)
              .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history list" + sb +
                  " from=" + (from + limit) +
                  " limit=" + limit))
              .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.history.next.tooltip", Math.min(size - from - limit, limit)).enhanced$$()))));
        }
        messages.add(Component.literal("[").withStyle(ChatFormatting.GRAY).append(ComponentUtils.formatList(buttons, Component.literal(" | "))).append("]"));
      } // end if buttons != null
      return CommonComponents.joinLines(messages);
    }, true);
    return size;
  }
}
