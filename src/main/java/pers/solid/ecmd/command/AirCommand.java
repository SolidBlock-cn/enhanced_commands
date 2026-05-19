package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.function.FailableConsumer;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.nbt.data.NbtTarget;
import pers.solid.ecmd.util.NbtUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.Collections;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.entities;
import static net.minecraft.commands.arguments.EntityArgument.getEntities;
import static net.minecraft.commands.arguments.NbtPathArgument.getPath;
import static net.minecraft.commands.arguments.NbtPathArgument.nbtPath;
import static pers.solid.ecmd.argument.NbtSourceArgument.getNbtSource;
import static pers.solid.ecmd.argument.NbtSourceArgument.nbtSource;
import static pers.solid.ecmd.argument.NbtTargetArgument.getNbtTarget;
import static pers.solid.ecmd.argument.NbtTargetArgument.nbtTarget;
import static pers.solid.ecmd.argument.SimpleEnumArgument.*;
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum AirCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(literalR2("air")
        .then(literal("get")
            .executes(context -> executeGetAir(context, Collections.singleton(context.getSource().getEntityOrException()), ConcentrationType.AVERAGE))
            .then(argument("entities", entities())
                .executes(context -> executeGetAir(context, getEntities(context, "entities"), ConcentrationType.AVERAGE))
                .then(argument("concentration_type", concentrationType())
                    .executes(context -> executeGetAir(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type")))
                    .then(literal("store")
                        .then(argument("target", nbtTarget(commandBuildContext))
                            .then(argument("path", nbtPath())
                                .executes(context -> {
                                  final NbtTarget<?> target = getNbtTarget(context, "target");
                                  final NbtPathArgument.NbtPath path = getPath(context, "path");
                                  final CommandSourceStack source = context.getSource();
                                  return executeGetAir(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type"), nbt -> target.setNbtInPath(source, path, nbt));
                                })))))))
        .then(literal("set")
            .then(argument("entities", entities())
                .then(argument("value", integer())
                    .executes(context -> executeSetAir(context, getEntities(context, "entities"), getInteger(context, "value"))))
                .then(literal("from")
                    .then(literal("result").redirect(dispatcher.getRoot(), context -> {
                      final Collection<? extends Entity> entities = getEntities(context, "entities");
                      return context.getSource().withCallback((success, result) -> {
                        for (Entity entity : entities) {
                          entity.setAirSupply(result);
                        }
                      }, SeparatedExecuteCommand.BINARY_RESULT_CONSUMER);
                    }))
                    .then(literal("success").redirect(dispatcher.getRoot(), context -> {
                      final Collection<? extends Entity> entities = getEntities(context, "entities");
                      return context.getSource().withCallback((success, result) -> {
                        for (Entity entity : entities) {
                          entity.setAirSupply(success ? 1 : 0);
                        }
                      }, SeparatedExecuteCommand.BINARY_RESULT_CONSUMER);
                    }))
                    .then(literal("of").then(argument("source_entities", entities())
                        .executes(context -> executeSetAir(context, getEntities(context, "entities"), getSourceEntityAir(context, ConcentrationType.AVERAGE)))
                        .then(argument("source_concentration_type", concentrationType())
                            .executes(context -> executeSetAir(context, getEntities(context, "entities"), getSourceEntityAir(context, getConcentrationType(context, "source_concentration_type")))))))
                    .then(argument("source", nbtSource(commandBuildContext))
                        .then(argument("path", nbtPath())
                            .executes(context -> executeSetAirFromSource(context, getPath(context, "path"), NbtConcentrationType.FIRST))
                            .then(argument("concentration_type", nbtConcentrationType())
                                .executes(context -> executeSetAirFromSource(context, getPath(context, "path"), getNbtConcentrationType(context, "concentration_type")))))))))
        .then(literal("add")
            .executes(context -> executeAddAir(context, Collections.singleton(context.getSource().getEntityOrException())))
            .then(argument("entities", entities())
                .executes(context -> executeAddAir(context, getEntities(context, "entities")))
                .then(argument("value", integer())
                    .executes(context -> executeAddAir(context, getEntities(context, "entities"), getInteger(context, "value"))))))
        .then(literal("remove")
            .executes(context -> executeRemoveAir(context, Collections.singleton(context.getSource().getEntityOrException())))
            .then(argument("entities", entities())
                .executes(context -> executeRemoveAir(context, getEntities(context, "entities")))
                .then(argument("value", integer())
                    .executes(context -> executeRemoveAir(context, getEntities(context, "entities"), getInteger(context, "value")))))));
  }

  private int executeSetAirFromSource(CommandContext<CommandSourceStack> context, NbtPathArgument.NbtPath path, NbtConcentrationType nbtConcentrationType) throws CommandSyntaxException {
    return executeSetAir(context, getEntities(context, "entities"), NbtUtil.toNumberOrThrow(getNbtSource(context, "source").getConcentratedNbts(context.getSource(), path, nbtConcentrationType, context.getSource().getLevel().getRandom()), path).getAsInt());
  }

  private static int executeGetAir(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, ConcentrationType concentrationType) throws CommandSyntaxException {
    return executeGetAir(context, entities, concentrationType, null);
  }

  private static <T extends Throwable> int executeGetAir(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, ConcentrationType concentrationType, @Nullable FailableConsumer<Tag, T> nbtElementConsumer) throws T, CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      final int air = entity.getAirSupply();
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.air.get.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(air).withStyle(Styles.RESULT)), false);
      if (nbtElementConsumer != null) {
        nbtElementConsumer.accept(IntTag.valueOf(air));
      }
      return air;
    } else {
      final IntList integers = new IntArrayList();
      for (Entity entity : entities) {
        integers.add(entity.getAirSupply());
      }
      final double result = concentrationType.concentrateInt(integers);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.air.get.multiple", integers.size(), concentrationType.getDisplayName(), Component.literal(concentrationType.longToString(result)).withStyle(Styles.RESULT)).enhanced$$(), false);
      if (nbtElementConsumer != null) {
        nbtElementConsumer.accept(concentrationType.longToNbt(result));
      }
      return (int) result;
    }
  }

  private static int executeSetAir(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, int value) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAirSupply(value);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.air.set.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(entity.getAirSupply()).withStyle(Styles.RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAirSupply(value);
      }
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.air.set.multiple", TextUtil.literal(size).withStyle(Styles.TARGET), TextUtil.literal(value).withStyle(Styles.TARGET)).enhanced$$(), true);
      return size;
    }
  }

  private static int executeAddAir(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, int value) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAirSupply(entity.getAirSupply() + value);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.air.add.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(value).withStyle(Styles.TARGET), TextUtil.literal(entity.getAirSupply()).withStyle(Styles.RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAirSupply(entity.getAirSupply() + value);
      }
      context.getSource().sendFeedback$ecBridge(() -> {
        final MutableComponent target = TextUtil.literal(size).withStyle(Styles.TARGET);
        final MutableComponent by = TextUtil.literal(value).withStyle(Styles.TARGET);
        return Component.translatable("enhanced_commands.commands.air.add.multiple", target, by).enhanced$$();
      }, true);
      return size;
    }
  }

  private static int executeAddAir(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAirSupply(entity.getMaxAirSupply());
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.air.add_to_max.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(entity.getAirSupply()).withStyle(Styles.RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAirSupply(entity.getMaxAirSupply());
      }
      context.getSource().sendFeedback$ecBridge(() -> {
        Object[] args = new Object[]{TextUtil.literal(size).withStyle(Styles.TARGET)};
        return Component.translatable("enhanced_commands.commands.air.add_to_max.multiple", args).enhanced$$();
      }, true);
      return size;
    }
  }

  private static int executeRemoveAir(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, int value) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAirSupply(entity.getAirSupply() - value);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.air.remove.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(value).withStyle(Styles.TARGET), TextUtil.literal(entity.getAirSupply()).withStyle(Styles.RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAirSupply(entity.getAirSupply() - value);
      }
      context.getSource().sendFeedback$ecBridge(() -> {
        final MutableComponent target = TextUtil.literal(size).withStyle(Styles.TARGET);
        final MutableComponent by = TextUtil.literal(value).withStyle(Styles.TARGET);
        return Component.translatable("enhanced_commands.commands.air.remove.multiple", target, by).enhanced$$();
      }, true);
      return size;
    }
  }

  private static int executeRemoveAir(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAirSupply(0);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.air.remove_all.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAirSupply(0);
      }
      context.getSource().sendFeedback$ecBridge(() -> {
        Object[] args = new Object[]{TextUtil.literal(size).withStyle(Styles.TARGET)};
        return Component.translatable("enhanced_commands.commands.air.remove_all.multiple", args).enhanced$$();
      }, true);
      return size;
    }
  }

  private static int getSourceEntityAir(CommandContext<CommandSourceStack> context, ConcentrationType concentrationType) throws CommandSyntaxException {
    final Collection<? extends Entity> sourceEntities = getEntities(context, "source_entities");
    if (sourceEntities.size() == 1) {
      final Entity entity = sourceEntities.iterator().next();
      return entity.getAirSupply();
    } else {
      IntList ints = new IntArrayList();
      for (Entity sourceEntity : sourceEntities) {
        ints.add(sourceEntity.getAirSupply());
      }
      return (int) concentrationType.concentrateInt(ints);
    }
  }
}
