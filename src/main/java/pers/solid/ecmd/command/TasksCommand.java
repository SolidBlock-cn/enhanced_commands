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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.extensions.IteratorTask;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.*;

public enum TasksCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    final SuggestionProvider<CommandSourceStack> taskUuidSuggestion = (context, builder) -> {
      final Map<UUID, IteratorTask<?>> uuidToTasks = ((ThreadExecutorExtension) context.getSource().getServer()).getUUIDToIteratorTasks$ec();
      return SharedSuggestionProvider.suggest(uuidToTasks.keySet().stream().map(UUID::toString), builder);
    };
    dispatcher.register(ModCommands.literalR2("tasks")
        .executes(context -> executeListTasks(context.getSource().getServer(), context, 10))
        .then(Commands.literal("count")
            .executes(context -> executeCountTasks(context.getSource().getServer(), context)))
        .then(Commands.literal("remove")
            .then(Commands.argument("uuid", UuidArgument.uuid()).suggests(taskUuidSuggestion)
                .executes(context -> executeRemoveTask(context.getSource().getServer(), context, UuidArgument.getUuid(context, "uuid")))))
        .then(Commands.literal("suspend")
            .then(Commands.argument("uuid", UuidArgument.uuid()).suggests(taskUuidSuggestion)
                .executes(context -> executeSetTaskSuspension(context.getSource().getServer(), context, UuidArgument.getUuid(context, "uuid"), true))))
        .then(Commands.literal("continue")
            .then(Commands.argument("uuid", UuidArgument.uuid()).suggests(taskUuidSuggestion)
                .executes(context -> executeSetTaskSuspension(context.getSource().getServer(), context, UuidArgument.getUuid(context, "uuid"), false))))
        .then(Commands.literal("clear")
            .executes(context -> executeClearTasks(context.getSource().getServer(), context)))
        .then(Commands.literal("sprint")
            .then(Commands.argument("uuid", UuidArgument.uuid()).suggests(taskUuidSuggestion)
                .executes(context -> executeSprintTask(context.getSource().getServer(), context, UuidArgument.getUuid(context, "uuid"), 0))
                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                    .executes(context -> executeSprintTask(context.getSource().getServer(), context, UuidArgument.getUuid(context, "uuid"), IntegerArgumentType.getInteger(context, "limit"))))))
        .then(Commands.literal("list")
            .executes(context -> executeListTasks(context.getSource().getServer(), context, 10))
            .then(Commands.argument("limit", IntegerArgumentType.integer(1, 30))
                .executes(context -> executeListTasks(context.getSource().getServer(), context, IntegerArgumentType.getInteger(context, "limit")))))
    );
  }

  private static int executeCountTasks(MinecraftServer server, CommandContext<CommandSourceStack> context) {
    final Queue<IteratorTask<?>> iteratorTasks = ((ThreadExecutorExtension) server).getIteratorTasks$ec();
    final int size = iteratorTasks.size();
    if (size == 0) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.count.none", size), false);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.count", size).enhanced$$(), false);
    }
    return size;
  }

  private static int executeClearTasks(MinecraftServer server, CommandContext<CommandSourceStack> context) {
    final Queue<IteratorTask<?>> iteratorTasks = ((ThreadExecutorExtension) server).getIteratorTasks$ec();
    final int size = iteratorTasks.size();
    iteratorTasks.clear();
    if (size == 0) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.clear.none", size), true);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.clear", size).enhanced$$(), true);
    }
    return size;
  }

  private static final DynamicCommandExceptionType TASK_UUID_DOES_NOT_EXIST = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.commands.tasks.not_exist", o));

  private static int executeRemoveTask(MinecraftServer server, CommandContext<CommandSourceStack> context, UUID uuid) throws CommandSyntaxException {
    final Map<UUID, IteratorTask<?>> uuidToTasks = ((ThreadExecutorExtension) server).getUUIDToIteratorTasks$ec();
    if (uuidToTasks.containsKey(uuid)) {
      final IteratorTask<?> remove = uuidToTasks.remove(uuid);
      ((ThreadExecutorExtension) server).getIteratorTasks$ec().remove(remove);
      if (remove != null) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.remove.success", remove.name), true);
        return 1;
      } else {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.remove.collected").withStyle(ChatFormatting.YELLOW), true);
        return 0;
      }
    }
    throw TASK_UUID_DOES_NOT_EXIST.create(uuid.toString());
  }

  private static MutableComponent createSuspendButton(UUID uuid) {
    return Component.translatable("enhanced_commands.commands.tasks.suspend").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tasks suspend " + uuid.toString())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.tasks.suspend.tooltip"))));
  }

  private static MutableComponent createContinueButton(UUID uuid) {
    return Component.translatable("enhanced_commands.commands.tasks.continue").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tasks continue " + uuid.toString())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.tasks.continue.tooltip"))));
  }

  private static MutableComponent createSprintButton(UUID uuid) {
    return Component.translatable("enhanced_commands.commands.tasks.sprint").withStyle(style -> style.withUnderlined(true).withColor(ChatFormatting.YELLOW).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tasks sprint " + uuid.toString())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.tasks.sprint.tooltip"))));
  }

  private static MutableComponent createRemoveButton(UUID uuid) {
    return Component.translatable("enhanced_commands.commands.tasks.remove").withStyle(style -> style.withUnderlined(true).withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tasks remove " + uuid.toString())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("enhanced_commands.commands.tasks.remove.tooltip"))));
  }

  private static int executeSetTaskSuspension(MinecraftServer server, CommandContext<CommandSourceStack> context, UUID uuid, boolean suspension) throws CommandSyntaxException {
    final Map<UUID, IteratorTask<?>> uuidToTasks = ((ThreadExecutorExtension) server).getUUIDToIteratorTasks$ec();
    if (uuidToTasks.containsKey(uuid)) {
      final IteratorTask<?> iteratorTask = uuidToTasks.get(uuid);
      if (iteratorTask != null) {
        if (suspension) {
          if (iteratorTask.suspended) {
            throw new CommandSyntaxException(null, Component.translatable("enhanced_commands.commands.tasks.suspend.already_suspended", iteratorTask.name));
          } else {
            iteratorTask.suspended = true;
            context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.suspend.success", iteratorTask.name).append("  ").append(Component.translatable("enhanced_commands.commands.tasks.buttons", ComponentUtils.formatList(List.of(createContinueButton(uuid), createRemoveButton(uuid)), Component.literal("|"))).withStyle(ChatFormatting.GRAY)), true);
            return 2;
          }
        } else {
          if (iteratorTask.suspended) {
            iteratorTask.suspended = false;
            context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.continue.success", iteratorTask.name).append("  ").append(Component.translatable("enhanced_commands.commands.tasks.buttons", ComponentUtils.formatList(List.of(createSuspendButton(uuid), createRemoveButton(uuid)), Component.literal("|"))).withStyle(ChatFormatting.GRAY)), true);
            return 1;
          } else {
            throw new CommandSyntaxException(null, Component.translatable("enhanced_commands.commands.tasks.continue.not_suspended", iteratorTask.name));
          }
        }
      } else {
        uuidToTasks.remove(uuid);
      }
    }
    throw TASK_UUID_DOES_NOT_EXIST.create(uuid.toString());
  }

  private static int executeSprintTask(MinecraftServer server, CommandContext<CommandSourceStack> context, UUID uuid, int limit) throws CommandSyntaxException {
    final Map<UUID, IteratorTask<?>> uuidToTasks = ((ThreadExecutorExtension) server).getUUIDToIteratorTasks$ec();
    if (uuidToTasks.containsKey(uuid)) {
      final IteratorTask<?> iteratorTask = uuidToTasks.get(uuid);
      if (iteratorTask != null) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.sprint.start", iteratorTask.name), true);
        if (limit <= 0) {
          IterateUtils.exhaust(iteratorTask);
        } else {
          IterateUtils.exhaust(Iterators.limit(iteratorTask, limit));
        }
        if (!iteratorTask.hasNext()) {
          uuidToTasks.remove(iteratorTask.uuid);
          ((ThreadExecutorExtension) server).getIteratorTasks$ec().remove(iteratorTask);
        }
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.sprint.success", iteratorTask.name), true);
        return 1;
      } else {
        uuidToTasks.remove(uuid);
      }
    }

    throw TASK_UUID_DOES_NOT_EXIST.create(uuid.toString());
  }

  private static int executeListTasks(MinecraftServer server, CommandContext<CommandSourceStack> context, int limit) {
    final Queue<IteratorTask<?>> iteratorTasks = ((ThreadExecutorExtension) server).getIteratorTasks$ec();
    final int size = iteratorTasks.size();

    if (size == 0) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.tasks.list.none").withStyle(ChatFormatting.RED), false);
      return 0;
    }
    @NotNull CommandSourceStack source = context.getSource();
    source.sendFeedback$ecBridge(() -> {
      final MutableComponent message = Component.translatable("enhanced_commands.commands.tasks.list.summary", Integer.toString(size)).enhanced$$();
      for (IteratorTask<?> iteratorTask : Iterables.limit(iteratorTasks, limit)) {
        final List<Component> list = new ArrayList<>();
        if (iteratorTask.suspended) {
          list.add(Component.translatable("enhanced_commands.commands.tasks.buttons.suspended").withStyle(ChatFormatting.LIGHT_PURPLE));
          list.add(createContinueButton(iteratorTask.uuid));
        } else {
          list.add(createSuspendButton(iteratorTask.uuid));
        }
        list.add(createSprintButton(iteratorTask.uuid));
        list.add(createRemoveButton(iteratorTask.uuid));
        message.append(CommonComponents.NEW_LINE).append(Component.literal(" - ").withStyle(ChatFormatting.GRAY).append(Component.translatable("enhanced_commands.commands.tasks.buttons", ComponentUtils.formatList(list, Component.literal("|")))).append(CommonComponents.SPACE).append(iteratorTask.name));
      }
      if (size > limit) {
        message.append(CommonComponents.NEW_LINE).append(Component.translatable("enhanced_commands.commands.tasks.list.limit_note").withStyle(style -> style.withColor(0xffa960)));
      }
      return message;
    }, false);
    return size;
  }
}
