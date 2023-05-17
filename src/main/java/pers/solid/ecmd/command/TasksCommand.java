package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;

import java.util.Queue;

public enum TasksCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(CommandManager.literal("tasks")
        .executes(context -> executeCountTasks(context.getSource().getServer(), context))
        .then(CommandManager.literal("count")
            .executes(context -> executeCountTasks(context.getSource().getServer(), context)))
        .then(CommandManager.literal("remove")
            .executes(context -> executeRemoveTasks(context.getSource().getServer(), context))));
  }

  private static int executeCountTasks(MinecraftServer server, CommandContext<ServerCommandSource> context) {
    final Queue<ThreadExecutorExtension.IteratorTask<?>> iteratorTasks = ((ThreadExecutorExtension) server).ec_getIteratorTasks();
    final int size = iteratorTasks.size();
    if (size == 0) {
      context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.tasks.count.none", size), true);
    } else if (size == 1) {
      context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.tasks.count.single", size), true);
    } else {
      context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.tasks.count.plural", size), true);
    }
    return size;
  }

  private static int executeRemoveTasks(MinecraftServer server, CommandContext<ServerCommandSource> context) {
    final Queue<ThreadExecutorExtension.IteratorTask<?>> iteratorTasks = ((ThreadExecutorExtension) server).ec_getIteratorTasks();
    final int size = iteratorTasks.size();
    iteratorTasks.clear();
    if (size == 0) {
      context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.tasks.remove.none", size), true);
    } else if (size == 1) {
      context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.tasks.remove.single", size), true);
    } else {
      context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.tasks.remove.plural", size), true);
    }
    return size;
  }
}
