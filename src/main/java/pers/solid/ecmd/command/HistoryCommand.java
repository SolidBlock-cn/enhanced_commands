package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.history.History;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.KeywordArgsArgumentType.getKeywordArgs;
import static pers.solid.ecmd.argument.StringEnumArgumentType.stringEnum;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum HistoryCommand implements CommandRegistrationCallback {
  INSTANCE;

  public static final KeywordArgsArgumentType CLEAR_KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("target", EntityArgumentType.player(), null)
      .addOptionalArg("target-server", bool(), false)
      .addOptionalArg("type", stringEnum("undo", "redo"), "undo")
      .build();

  public static final KeywordArgsArgumentType LIST_KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addAll(CLEAR_KEYWORD_ARGS)
      .addOptionalArg("from", integer(0), 0)
      .addOptionalArg("limit", integer(0, 50), 7)
      .addOptionalArg("sort", stringEnum("latest", "oldest"), "latest")
      .build();

  public static @NotNull HistoryHolder getHistoryHolderFromArgs(ServerCommandSource source, KeywordArgs keywordArgs) throws CommandSyntaxException {
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
    final ServerPlayerEntity player = selector.getPlayer(source);
    if (player instanceof HistoryHolder holder) {
      return holder;
    } else {
      throw NOT_SUPPORTED_HISTORY.create(targetName(player));
    }
  }

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
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

  private static Text targetName(Object object) {
    if (object instanceof Entity entity) {
      return TextUtil.styled(entity.getDisplayName(), Styles.TARGET);
    } else if (object instanceof MinecraftServer server) {
      return Text.literal(server.getName()).styled(Styles.TARGET);
    } else {
      return Text.literal("<unknown source>").styled(Styles.TARGET);
    }
  }

  public static final DynamicCommandExceptionType NOT_SUPPORTED_HISTORY = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.history.not_supported", o));

  private static int executeClear(ServerCommandSource source, KeywordArgs keywordArgs) throws CommandSyntaxException {
    HistoryHolder holder = getHistoryHolderFromArgs(source, keywordArgs);
    return executeClear(source, holder, "redo".equals(keywordArgs.getArg("type")));
  }

  private static int executeClear(ServerCommandSource source, @NotNull HistoryHolder historyHolder, boolean redo) {
    final Text targetName = targetName(historyHolder);
    if (redo) {
      historyHolder = historyHolder.inverse();
    }
    Deque<History> histories = historyHolder.getUndoableHistories$ec();
    final int size = histories.size();
    histories.clear();
    source.sendFeedback(() -> Text.translatable("enhanced_commands.commands.history.cleared", targetName, size), true);
    return 1;
  }

  private static int executeList(ServerCommandSource source, KeywordArgs keywordArgs) throws CommandSyntaxException {
    HistoryHolder holder = getHistoryHolderFromArgs(source, keywordArgs);
    return executeList(source, holder, keywordArgs.getInt("from"), keywordArgs.getInt("limit"), "latest".equals(keywordArgs.getArg("sort")), "redo".equals(keywordArgs.getArg("type")));
  }

  private static int executeList(ServerCommandSource source) throws CommandSyntaxException {
    return executeList(source, fromSourceOrThrow(source), 0, 7, true, false);
  }

  private static @NotNull HistoryHolder fromSourceOrThrow(ServerCommandSource source) throws CommandSyntaxException {
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

  private static int executeList(ServerCommandSource source, @NotNull HistoryHolder historyHolder, int from, int limit, boolean latestFirst, boolean redo) {
    final Text targetName = targetName(historyHolder);
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
        return Text.translatable("enhanced_commands.commands.history.no_history", targetName);
      }
      final Text sizeText = TextUtil.literal(size).styled(Styles.RESULT);
      List<Text> messages = new ArrayList<>();

      if (redo) {
        messages.add(Text.translatable("enhanced_commands.commands.history.caption.redo", targetName, sizeText).enhanced$$());
      } else {
        messages.add(Text.translatable("enhanced_commands.commands.history.caption.undo", targetName, sizeText).enhanced$$());
      }

      int i = from + 1;
      for (History history : iterable) {
        messages.add(Text.literal(" #" + i + " - ").formatted(Formatting.GRAY)
            .append(history.getName()));
        i++;
      }

      List<Text> buttons = new ArrayList<>(3);

      final StringBuilder sb = new StringBuilder();
      if (finalHistoryHolder instanceof MinecraftServer) {
        sb.append(" target-server=true");
      } else if (finalHistoryHolder instanceof ServerPlayerEntity player) {
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
          buttons.add(Text.translatable("enhanced_commands.commands.history.previous").styled(style -> style
              .withColor(Formatting.YELLOW)
              .withUnderline(true)
              .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history list" + sb +
                  " from=" + Math.max(0, from - limit) +
                  " limit=" + Math.min(from, limit)))
              .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhanced_commands.commands.history.previous.tooltip", Math.min(from, limit)).enhanced$$()))));
        }
        buttons.add(Text.translatable("enhanced_commands.commands.history.clear").styled(style -> style
            .withColor(Formatting.RED)
            .withUnderline(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history clear" + sb))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhanced_commands.commands.history.clear.tooltip", size).enhanced$$()))));
        if (from + limit < size) {
          buttons.add(Text.translatable("enhanced_commands.commands.history.next").styled(style -> style
              .withColor(Formatting.YELLOW)
              .withUnderline(true)
              .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/history list" + sb +
                  " from=" + (from + limit) +
                  " limit=" + limit))
              .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhanced_commands.commands.history.next.tooltip", Math.min(size - from - limit, limit)).enhanced$$()))));
        }
        messages.add(Text.literal("[").formatted(Formatting.GRAY).append(Texts.join(buttons, Text.literal(" | "))).append("]"));
      } // end if buttons != null
      return ScreenTexts.joinLines(messages);
    }, true);
    return size;
  }
}
