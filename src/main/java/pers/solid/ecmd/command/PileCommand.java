package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Comparator;
import java.util.List;

import static net.minecraft.command.argument.EntityArgumentType.entities;
import static net.minecraft.command.argument.EntityArgumentType.getEntities;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum PileCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literal("pile")
        .then(argument("targets", entities())
            .executes(PileCommand::executePile)));
  }

  public static final DynamicCommandExceptionType NOT_SUFFICIENT = new DynamicCommandExceptionType(s -> Text.translatable("enhanced_commands.commands.pile.not_sufficient", s));

  private static final DynamicCommandExceptionType PILING_FAILED = new DynamicCommandExceptionType(s -> Text.translatable("enhanced_commands.commands.pile.fail", s));

  private static int executePile(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerWorld world = context.getSource().getWorld();
    final List<? extends Entity> entities = getEntities(context, "targets").stream().filter(entity -> entity.getWorld() == world).sorted(Comparator.comparingInt((Entity o) -> BooleanUtils.toInteger(o.isPlayer()))).toList();
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
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.pile.success", size), false);
    } else if (x == 0) {
      throw PILING_FAILED.create(size);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.pile.success_partial", result), true);
    }
    return result;
  }
}
