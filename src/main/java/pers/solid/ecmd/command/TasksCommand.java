package pers.solid.ecmd.command;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.lang.ref.WeakReference;
import java.util.*;

public enum TasksCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final SuggestionProvider<ServerCommandSource> taskUuidSuggestion = (context, builder) -> {
      final Map<UUID, WeakReference<IteratorTask<?>>> uuidToTasks = ((ThreadExecutorExtension) context.getSource().getServer()).ec_getUUIDToIteratorTasks();
      return CommandSource.suggestMatching(uuidToTasks.keySet().stream().map(UUID::toString), builder);
    };
    dispatcher.register(ModCommands.literalR2("tasks")
        .executes(context -> executeListTasks(context.getSource().getServer(), context, 10))
        .then(CommandManager.literal("count")
            .executes(context -> executeCountTasks(context.getSource().getServer(), context)))
        .then(CommandManager.literal("remove")
            .then(CommandManager.argument("uuid", UuidArgumentType.uuid()).suggests(taskUuidSuggestion)
                .executes(context -> executeRemoveTask(context.getSource().getServer(), context, UuidArgumentType.getUuid(context, "uuid")))))
        .then(CommandManager.literal("suspend")
            .then(CommandManager.argument("uuid", UuidArgumentType.uuid()).suggests(taskUuidSuggestion)
                .executes(context -> executeSetTaskSuspension(context.getSource().getServer(), context, UuidArgumentType.getUuid(context, "uuid"), true))))
        .then(CommandManager.literal("continue")
            .then(CommandManager.argument("uuid", UuidArgumentType.uuid()).suggests(taskUuidSuggestion)
                .executes(context -> executeSetTaskSuspension(context.getSource().getServer(), context, UuidArgumentType.getUuid(context, "uuid"), false))))
        .then(CommandManager.literal("clear")
            .executes(context -> executeClearTasks(context.getSource().getServer(), context)))
        .then(CommandManager.literal("exhaust")
            .then(CommandManager.argument("uuid", UuidArgumentType.uuid()).suggests(taskUuidSuggestion)
                .executes(context -> executeExhaustTask(context.getSource().getServer(), context, UuidArgumentType.getUuid(context, "uuid"), 0))
                .then(CommandManager.argument("limit", IntegerArgumentType.integer(1))
                    .executes(context -> executeExhaustTask(context.getSource().getServer(), context, UuidArgumentType.getUuid(context, "uuid"), IntegerArgumentType.getInteger(context, "limit"))))))
        .then(CommandManager.literal("list")
            .executes(context -> executeListTasks(context.getSource().getServer(), context, 10))
            .then(CommandManager.argument("limit", IntegerArgumentType.integer(1, 30))
                .executes(context -> executeListTasks(context.getSource().getServer(), context, IntegerArgumentType.getInteger(context, "limit")))))
    );
  }

  private static int executeCountTasks(MinecraftServer server, CommandContext<ServerCommandSource> context) {
    final Queue<IteratorTask<?>> iteratorTasks = ((ThreadExecutorExtension) server).ec_getIteratorTasks();
    final int size = iteratorTasks.size();
    if (size == 0) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.tasks.count.none", size), true);
    } else {
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhancedCommands.commands.tasks.count", size), true);
    }
    return size;
  }

  private static int executeClearTasks(MinecraftServer server, CommandContext<ServerCommandSource> context) {
    final Queue<IteratorTask<?>> iteratorTasks = ((ThreadExecutorExtension) server).ec_getIteratorTasks();
    final int size = iteratorTasks.size();
    iteratorTasks.clear();
    if (size == 0) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.tasks.clear.none", size), true);
    } else {
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhancedCommands.commands.tasks.clear", size), true);
    }
    return size;
  }

  private static final DynamicCommandExceptionType TASK_UUID_DOES_NOT_EXIST = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.commands.tasks.not_exist", o));

  private static int executeRemoveTask(MinecraftServer server, CommandContext<ServerCommandSource> context, UUID uuid) throws CommandSyntaxException {
    final Map<UUID, WeakReference<IteratorTask<?>>> uuidToTasks = ((ThreadExecutorExtension) server).ec_getUUIDToIteratorTasks();
    if (uuidToTasks.containsKey(uuid)) {
      final IteratorTask<?> remove = uuidToTasks.remove(uuid).get();
      ((ThreadExecutorExtension) server).ec_getIteratorTasks().remove(remove);
      if (remove != null) {
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.tasks.remove.success", remove.name), true);
        return 1;
      } else {
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.tasks.remove.collected").formatted(Formatting.YELLOW), true);
        return 0;
      }
    }
    throw TASK_UUID_DOES_NOT_EXIST.create(uuid);
  }

  private static MutableText createSuspendButton(UUID uuid) {
    return Text.translatable("enhancedCommands.commands.tasks.suspend").styled(style -> style.withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tasks suspend " + uuid.toString())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhancedCommands.commands.tasks.suspend.tooltip"))));
  }

  private static MutableText createContinueButton(UUID uuid) {
    return Text.translatable("enhancedCommands.commands.tasks.continue").styled(style -> style.withUnderline(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tasks continue " + uuid.toString())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhancedCommands.commands.tasks.continue.tooltip"))));
  }

  private static MutableText createExhaustButton(UUID uuid) {
    return Text.translatable("enhancedCommands.commands.tasks.exhaust").styled(style -> style.withUnderline(true).withColor(Formatting.YELLOW).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tasks exhaust " + uuid.toString())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhancedCommands.commands.tasks.exhaust.tooltip"))));
  }

  private static MutableText createRemoveButton(UUID uuid) {
    return Text.translatable("enhancedCommands.commands.tasks.remove").styled(style -> style.withUnderline(true).withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tasks remove " + uuid.toString())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("enhancedCommands.commands.tasks.remove.tooltip"))));
  }

  private static int executeSetTaskSuspension(MinecraftServer server, CommandContext<ServerCommandSource> context, UUID uuid, boolean suspension) throws CommandSyntaxException {
    final Map<UUID, WeakReference<IteratorTask<?>>> uuidToTasks = ((ThreadExecutorExtension) server).ec_getUUIDToIteratorTasks();
    if (uuidToTasks.containsKey(uuid)) {
      final IteratorTask<?> iteratorTask = uuidToTasks.get(uuid).get();
      if (iteratorTask != null) {
        if (suspension) {
          if (iteratorTask.suspended) {
            throw new CommandSyntaxException(null, Text.translatable("enhancedCommands.commands.tasks.suspend.already_suspended", iteratorTask.name));
          } else {
            iteratorTask.suspended = true;
            CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.tasks.suspend.success", iteratorTask.name).append("  ").append(Text.translatable("enhancedCommands.commands.tasks.buttons", Texts.join(List.of(createContinueButton(uuid), createRemoveButton(uuid)), Text.literal("|"))).formatted(Formatting.GRAY)), true);
            return 2;
          }
        } else {
          if (iteratorTask.suspended) {
            iteratorTask.suspended = false;
            CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.tasks.continue.success", iteratorTask.name).append("  ").append(Text.translatable("enhancedCommands.commands.tasks.buttons", Texts.join(List.of(createSuspendButton(uuid), createRemoveButton(uuid)), Text.literal("|"))).formatted(Formatting.GRAY)), true);
            return 1;
          } else {
            throw new CommandSyntaxException(null, Text.translatable("enhancedCommands.commands.tasks.continue.not_suspended", iteratorTask.name));
          }
        }
      } else {
        uuidToTasks.remove(uuid);
      }
    }
    throw TASK_UUID_DOES_NOT_EXIST.create(uuid);
  }

  private static int executeExhaustTask(MinecraftServer server, CommandContext<ServerCommandSource> context, UUID uuid, int limit) throws CommandSyntaxException {
    final Map<UUID, WeakReference<IteratorTask<?>>> uuidToTasks = ((ThreadExecutorExtension) server).ec_getUUIDToIteratorTasks();
    if (uuidToTasks.containsKey(uuid)) {
      final IteratorTask<?> iteratorTask = uuidToTasks.get(uuid).get();
      if (iteratorTask != null) {
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.tasks.exhaust.start", iteratorTask.name), true);
        if (limit <= 0) {
          IterateUtils.exhaust(iteratorTask);
        } else {
          IterateUtils.exhaust(Iterators.limit(iteratorTask, limit));
        }
        if (!iteratorTask.hasNext()) {
          uuidToTasks.remove(iteratorTask.uuid);
          ((ThreadExecutorExtension) server).ec_getIteratorTasks().remove(iteratorTask);
        }
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.tasks.exhaust.success", iteratorTask.name), true);
        return 1;
      } else {
        uuidToTasks.remove(uuid);
      }
    }

    throw TASK_UUID_DOES_NOT_EXIST.create(uuid);
  }

  private static int executeListTasks(MinecraftServer server, CommandContext<ServerCommandSource> context, int limit) {
    final Queue<IteratorTask<?>> iteratorTasks = ((ThreadExecutorExtension) server).ec_getIteratorTasks();
    final int size = iteratorTasks.size();

    if (size == 0) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.tasks.list.none").formatted(Formatting.RED), true);
      return 0;
    }
    CommandBridge.sendFeedback(context.getSource(), () -> {
      final MutableText message = TextUtil.enhancedTranslatable("enhancedCommands.commands.tasks.list.summary", Integer.toString(size));
      for (IteratorTask<?> iteratorTask : Iterables.limit(iteratorTasks, limit)) {
        final List<Text> list = new ArrayList<>();
        if (iteratorTask.suspended) {
          list.add(Text.translatable("enhancedCommands.commands.tasks.buttons.suspended").formatted(Formatting.LIGHT_PURPLE));
          list.add(createContinueButton(iteratorTask.uuid));
        } else {
          list.add(createSuspendButton(iteratorTask.uuid));
        }
        list.add(createExhaustButton(iteratorTask.uuid));
        list.add(createRemoveButton(iteratorTask.uuid));
        message.append(ScreenTexts.LINE_BREAK).append(Text.literal(" - ").formatted(Formatting.GRAY).append(Text.translatable("enhancedCommands.commands.tasks.buttons", Texts.join(list, Text.literal("|")))).append(ScreenTexts.SPACE).append(iteratorTask.name));
      }
      if (size > limit) {
        message.append(ScreenTexts.LINE_BREAK).append(Text.translatable("enhancedCommands.commands.tasks.list.limit_note").styled(style -> style.withColor(0xffa960)));
      }
      return message;
    }, true);
    return size;
  }
}
