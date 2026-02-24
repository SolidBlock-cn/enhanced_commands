package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Comparator;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.entities;
import static net.minecraft.commands.arguments.EntityArgument.getEntities;

public enum PileCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(literal("pile")
        .then(argument("targets", entities())
            .executes(PileCommand::executePile)));
  }

  public static final DynamicCommandExceptionType NOT_SUFFICIENT = new DynamicCommandExceptionType(s -> Component.translatable("enhanced_commands.commands.pile.not_sufficient", s));

  private static final DynamicCommandExceptionType PILING_FAILED = new DynamicCommandExceptionType(s -> Component.translatable("enhanced_commands.commands.pile.fail", s));

  private static int executePile(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final ServerLevel world = context.getSource().getLevel();
    final List<? extends Entity> entities = getEntities(context, "targets").stream().filter(entity -> entity.level() == world).sorted(Comparator.comparingInt((Entity o) -> BooleanUtils.toInteger(o.isAlwaysTicking()))).toList();
    final int size = entities.size();
    if (size < 2) {
      throw NOT_SUFFICIENT.create(size);
    }
    int x = 0;
    for (int i = 0; i < entities.size() - 1; i++) {
      final Entity previous = entities.get(i);
      final Entity next = entities.get(i + 1);
      final boolean successes = next.startRiding(previous, false);
      if (successes) {
        x++;
      }
    }
    final int result = x + 1;
    if (result == size) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.pile.success", size), false);
    } else if (x == 0) {
      throw PILING_FAILED.create(size);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.pile.success_partial", result), true);
    }
    return result;
  }
}
