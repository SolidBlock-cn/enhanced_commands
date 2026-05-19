package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.FloatTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
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

public enum HealthCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(literalR2("health")
        .executes(context -> executeGetHealth(context, Collections.singleton(context.getSource().getEntityOrException()), null, 1))
        .then(literal("get")
            .executes(context -> executeGetHealth(context, Collections.singleton(context.getSource().getEntityOrException()), null, 1))
            .then(argument("entities", entities())
                .executes(context -> executeGetHealth(context, getEntities(context, "entities"), ConcentrationType.AVERAGE, 1))
                .then(argument("concentration_type", concentrationType())
                    .executes(context -> executeGetHealth(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type"), 1))
                    .then(argument("scale", floatArg())
                        .executes(context -> executeGetHealth(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type"), getFloat(context, "scale"))))
                    .then(literal("store")
                        .then(argument("target", nbtTarget(commandBuildContext))
                            .then(argument("path", nbtPath())
                                .executes(context -> executeGetHealth(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type"), 1, getNbtTarget(context, "target"), getPath(context, "path")))))))))
        .then(literal("set")
            .then(argument("entities", entities())
                .then(argument("value", floatArg())
                    .executes(context -> executeSetHealth(context, getEntities(context, "entities"), getFloat(context, "value"))))
                .then(literal("from")
                    .then(literal("result").redirect(dispatcher.getRoot(), context -> {
                      final Collection<? extends Entity> entities = getEntities(context, "entities");
                      return context.getSource().withCallback((success, result) -> {
                        for (Entity entity : entities) {
                          if (entity instanceof LivingEntity livingEntity) {
                            livingEntity.setHealth(result);
                          }
                        }
                      }, SeparatedExecuteCommand.BINARY_RESULT_CONSUMER);
                    }))
                    .then(literal("success").redirect(dispatcher.getRoot(), context -> {
                      final Collection<? extends Entity> entities = getEntities(context, "entities");
                      return context.getSource().withCallback((success, result) -> {
                        for (Entity entity : entities) {
                          if (entity instanceof LivingEntity livingEntity) {
                            livingEntity.setHealth(success ? 1 : 0);
                          }
                        }
                      }, SeparatedExecuteCommand.BINARY_RESULT_CONSUMER);
                    }))
                    .then(literal("of").then(argument("source_entities", entities())
                        .executes(context -> executeSetHealth(context, getEntities(context, "entities"), getSourceEntityHealth(context, ConcentrationType.AVERAGE)))
                        .then(argument("source_concentration_type", concentrationType())
                            .executes(context -> executeSetHealth(context, getEntities(context, "entities"), getSourceEntityHealth(context, getConcentrationType(context, "source_concentration_type")))))))
                    .then(argument("source", nbtSource(commandBuildContext))
                        .then(argument("path", nbtPath())
                            .executes(context -> executeSetHealthFromSource(context, getPath(context, "path"), NbtConcentrationType.FIRST))
                            .then(argument("concentration_type", nbtConcentrationType())
                                .executes(context -> executeSetHealthFromSource(context, getPath(context, "path"), getNbtConcentrationType(context, "concentration_type")))))))))
        .then(literal("add")
            .executes(context -> executeAddHealth(context, Collections.singleton(context.getSource().getEntityOrException())))
            .then(argument("entities", entities())
                .executes(context -> executeAddHealth(context, getEntities(context, "entities")))
                .then(argument("value", floatArg())
                    .executes(context -> executeAddHealth(context, getEntities(context, "entities"), getFloat(context, "value"))))))
        .then(literal("remove")
            .executes(context -> executeRemoveHealth(context, Collections.singleton(context.getSource().getEntityOrException())))
            .then(argument("entities", entities())
                .executes(context -> executeRemoveHealth(context, getEntities(context, "entities")))
                .then(argument("value", floatArg())
                    .executes(context -> executeRemoveHealth(context, getEntities(context, "entities"), getFloat(context, "value")))))));
  }

  private int executeSetHealthFromSource(CommandContext<CommandSourceStack> context, NbtPathArgument.NbtPath path, NbtConcentrationType nbtConcentrationType) throws CommandSyntaxException {
    return executeSetHealth(context, getEntities(context, "entities"), NbtUtil.toNumberOrThrow(getNbtSource(context, "source").getConcentratedNbts(context.getSource(), path, nbtConcentrationType, context.getSource().getLevel().getRandom()), path).getAsFloat());
  }

  public static final DynamicCommandExceptionType NOT_LIVING = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.commands.health.get.single.not_living", o));
  public static final DynamicCommandExceptionType NOT_LIVING_MULTIPLE = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.commands.health.get.multiple.not_living", o).enhanced$$());

  private static int executeGetHealth(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, ConcentrationType concentrationType, double scale) throws CommandSyntaxException {
    return executeGetHealth(context, entities, concentrationType, scale, null, null);
  }

  private static int executeGetHealth(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, ConcentrationType concentrationType, double scale, @Nullable NbtTarget<?> nbtTarget, NbtPathArgument.@Nullable NbtPath nbtPath) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      final float health = livingEntity.getHealth();
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.get.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(health).withStyle(Styles.RESULT)), false);
      if (nbtTarget != null && nbtPath != null) {
        nbtTarget.setNbtInPath(context.getSource(), nbtPath, FloatTag.valueOf(health));
      }
      return (int) (health * scale);
    } else {
      final FloatList floats = new FloatArrayList();
      for (Entity entity : entities) {
        if (entity instanceof LivingEntity livingEntity) {
          floats.add(livingEntity.getHealth());
        }
      }
      if (floats.isEmpty()) {
        throw NOT_LIVING_MULTIPLE.create(entities.size());
      }
      final double result = concentrationType.concentrateFloat(floats);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.get.multiple", floats.size(), concentrationType.getDisplayName(), Component.literal(concentrationType.floatToString(result)).withStyle(Styles.RESULT)).enhanced$$(), false);
      if (nbtTarget != null && nbtPath != null) {
        nbtTarget.setNbtInPath(context.getSource(), nbtPath, concentrationType.floatToNbt(result));
      }
      return (int) (result * scale);
    }
  }

  private static int executeSetHealth(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, float value) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(value);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.set.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET), TextUtil.literal(livingEntity.getHealth()).withStyle(Styles.RESULT)), true);
      return 1;
    } else {
      int successes = 0;
      for (Entity entity : entities) {
        if (entity instanceof LivingEntity livingEntity) {
          livingEntity.setHealth(value);
          successes++;
        }
      }
      if (successes == 0) {
        throw NOT_LIVING_MULTIPLE.create(entities.size());
      }
      int finalSuccesses = successes;
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.set.multiple", TextUtil.literal(finalSuccesses).withStyle(Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT)).enhanced$$(), true);
      return successes;
    }
  }

  private static int executeAddHealth(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, float value) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(livingEntity.getHealth() + value);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.add.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT), TextUtil.literal(livingEntity.getHealth()).withStyle(Styles.RESULT)), true);
      return 1;
    } else {
      int successes = 0;
      for (Entity entity : entities) {
        if (entity instanceof LivingEntity livingEntity) {
          livingEntity.setHealth(livingEntity.getHealth() + value);
          successes++;
        }
      }
      if (successes == 0) {
        throw NOT_LIVING_MULTIPLE.create(entities.size());
      }
      int finalSuccesses = successes;
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.add.multiple", TextUtil.literal(finalSuccesses).withStyle(Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT)).enhanced$$(), true);
      return successes;
    }
  }

  private static int executeAddHealth(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(livingEntity.getMaxHealth());
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.add_to_max.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET), TextUtil.literal(livingEntity.getHealth()).withStyle(Styles.RESULT)), true);
      return 1;
    } else {
      int successes = 0;
      for (Entity entity : entities) {
        if (entity instanceof LivingEntity livingEntity) {
          livingEntity.setHealth(livingEntity.getMaxHealth());
          successes++;
        }
      }
      if (successes == 0) {
        throw NOT_LIVING_MULTIPLE.create(entities.size());
      }
      int finalSuccesses = successes;
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.add_to_max.multiple", TextUtil.literal(finalSuccesses).withStyle(Styles.TARGET)).enhanced$$(), true);
      return successes;
    }
  }

  private static int executeRemoveHealth(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, float value) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(livingEntity.getHealth() - value);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.remove.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT), TextUtil.literal(livingEntity.getHealth()).withStyle(Styles.RESULT)), true);
      return 1;
    } else {
      int successes = 0;
      for (Entity entity : entities) {
        if (entity instanceof LivingEntity livingEntity) {
          livingEntity.setHealth(livingEntity.getHealth() - value);
          successes++;
        }
      }
      if (successes == 0) {
        throw NOT_LIVING_MULTIPLE.create(entities.size());
      }
      int finalSuccesses = successes;
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.remove.multiple", TextUtil.literal(finalSuccesses).withStyle(Styles.TARGET), TextUtil.literal(value).withStyle(Styles.RESULT)).enhanced$$(), true);
      return successes;
    }
  }

  private static int executeRemoveHealth(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(0);
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.remove_all.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET)), true);
      return 1;
    } else {
      int successes = 0;
      for (Entity entity : entities) {
        if (entity instanceof LivingEntity livingEntity) {
          livingEntity.setHealth(0);
          successes++;
        }
      }
      if (successes == 0) {
        throw NOT_LIVING_MULTIPLE.create(entities.size());
      }
      int finalSuccesses = successes;
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.health.remove_all.multiple", TextUtil.literal(finalSuccesses).withStyle(Styles.TARGET)).enhanced$$(), true);
      return successes;
    }
  }

  private static float getSourceEntityHealth(CommandContext<CommandSourceStack> context, ConcentrationType concentrationType) throws CommandSyntaxException {
    final Collection<? extends Entity> sourceEntities = getEntities(context, "source_entities");
    if (sourceEntities.size() == 1) {
      final Entity entity = sourceEntities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      return livingEntity.getHealth();
    } else {
      FloatList floats = new FloatArrayList();
      for (Entity sourceEntity : sourceEntities) {
        if (sourceEntity instanceof LivingEntity livingEntity) {
          floats.add(livingEntity.getHealth());
        }
      }
      if (floats.isEmpty()) {
        throw NOT_LIVING_MULTIPLE.create(sourceEntities.size());
      }
      return (float) concentrationType.concentrateFloat(floats);
    }
  }
}
