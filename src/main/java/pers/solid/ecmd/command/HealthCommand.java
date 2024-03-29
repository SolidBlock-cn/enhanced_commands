package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.util.NbtUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.Collection;
import java.util.Collections;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static net.minecraft.command.argument.EntityArgumentType.entities;
import static net.minecraft.command.argument.EntityArgumentType.getEntities;
import static net.minecraft.command.argument.NbtPathArgumentType.getNbtPath;
import static net.minecraft.command.argument.NbtPathArgumentType.nbtPath;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.ConcentrationTypeArgumentType.concentrationType;
import static pers.solid.ecmd.argument.ConcentrationTypeArgumentType.getConcentrationType;
import static pers.solid.ecmd.argument.NbtSourceArgumentType.getNbtSource;
import static pers.solid.ecmd.argument.NbtSourceArgumentType.nbtSource;
import static pers.solid.ecmd.argument.NbtTargetArgumentType.getNbtTarget;
import static pers.solid.ecmd.argument.NbtTargetArgumentType.nbtTarget;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum HealthCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literalR2("health")
        .executes(context -> executeGetHealth(context, Collections.singleton(context.getSource().getEntityOrThrow()), null, 1))
        .then(literal("get")
            .executes(context -> executeGetHealth(context, Collections.singleton(context.getSource().getEntityOrThrow()), null, 1))
            .then(argument("entities", entities())
                .executes(context -> executeGetHealth(context, getEntities(context, "entities"), ConcentrationType.AVERAGE, 1))
                .then(argument("concentration_type", concentrationType())
                    .executes(context -> executeGetHealth(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type"), 1))
                    .then(argument("scale", floatArg())
                        .executes(context -> executeGetHealth(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type"), getFloat(context, "scale"))))
                    .then(literal("store")
                        .then(argument("target", nbtTarget(registryAccess))
                            .then(argument("path", nbtPath())
                                .executes(context -> executeGetHealth(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type"), 1, getNbtTarget(context, "target"), getNbtPath(context, "path")))))))))
        .then(literal("set")
            .then(argument("entities", entities())
                .then(argument("value", floatArg())
                    .executes(context -> executeSetHealth(context, getEntities(context, "entities"), getFloat(context, "value"))))
                .then(literal("from")
                    .then(literal("result").redirect(dispatcher.getRoot(), context -> {
                      final Collection<? extends Entity> entities = getEntities(context, "entities");
                      return context.getSource().mergeConsumers((context1, success, result) -> {
                        for (Entity entity : entities) {
                          if (entity instanceof LivingEntity livingEntity) {
                            livingEntity.setHealth(result);
                          }
                        }
                      }, SeparatedExecuteCommand.BINARY_RESULT_CONSUMER);
                    }))
                    .then(literal("success").redirect(dispatcher.getRoot(), context -> {
                      final Collection<? extends Entity> entities = getEntities(context, "entities");
                      return context.getSource().mergeConsumers((context1, success, result) -> {
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
                    .then(argument("source", nbtSource(registryAccess))
                        .then(argument("path", nbtPath())
                            .executes(context -> {
                              final NbtPathArgumentType.NbtPath path = getNbtPath(context, "path");
                              return executeSetHealth(context, getEntities(context, "entities"), NbtUtil.toNumberOrThrow(getNbtSource(context, "source").getConcentratedNbts(path), path).floatValue());
                            }))))))
        .then(literal("add")
            .executes(context -> executeAddHealth(context, Collections.singleton(context.getSource().getEntityOrThrow())))
            .then(argument("entities", entities())
                .executes(context -> executeAddHealth(context, getEntities(context, "entities")))
                .then(argument("value", floatArg())
                    .executes(context -> executeAddHealth(context, getEntities(context, "entities"), getFloat(context, "value"))))))
        .then(literal("remove")
            .executes(context -> executeRemoveHealth(context, Collections.singleton(context.getSource().getEntityOrThrow())))
            .then(argument("entities", entities())
                .executes(context -> executeRemoveHealth(context, getEntities(context, "entities")))
                .then(argument("value", floatArg())
                    .executes(context -> executeRemoveHealth(context, getEntities(context, "entities"), getFloat(context, "value")))))));
  }

  public static final DynamicCommandExceptionType NOT_LIVING = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.health.get.single.not_living", o));
  public static final DynamicCommandExceptionType NOT_LIVING_MULTIPLE = new DynamicCommandExceptionType(o -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.get.multiple.not_living", o));

  private static int executeGetHealth(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, ConcentrationType concentrationType, double scale) throws CommandSyntaxException {
    return executeGetHealth(context, entities, concentrationType, scale, null, null);
  }

  private static int executeGetHealth(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, ConcentrationType concentrationType, double scale, @Nullable NbtTarget nbtTarget, NbtPathArgumentType.@Nullable NbtPath nbtPath) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      final float health = livingEntity.getHealth();
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.get.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(health).styled(Styles.RESULT)), false);
      if (nbtTarget != null && nbtPath != null) {
        nbtTarget.modifyNbt(nbtPath, NbtFloat.of(health));
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.get.multiple", floats.size(), concentrationType.getDisplayName(), Text.literal(concentrationType.floatToString(result)).styled(Styles.RESULT)), false);
      if (nbtTarget != null && nbtPath != null) {
        nbtTarget.modifyNbt(nbtPath, concentrationType.floatToNbt(result));
      }
      return (int) (result * scale);
    }
  }

  private static int executeSetHealth(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, float value) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.set.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET), TextUtil.literal(livingEntity.getHealth()).styled(Styles.RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.set.multiple", TextUtil.literal(finalSuccesses).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT)), true);
      return successes;
    }
  }

  private static int executeAddHealth(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, float value) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(livingEntity.getHealth() + value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.add.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT), TextUtil.literal(livingEntity.getHealth()).styled(Styles.RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.add.multiple", TextUtil.literal(finalSuccesses).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT)), true);
      return successes;
    }
  }

  private static int executeAddHealth(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(livingEntity.getMaxHealth());
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.add_to_max.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET), TextUtil.literal(livingEntity.getHealth()).styled(Styles.RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.add_to_max.multiple", TextUtil.literal(finalSuccesses).styled(Styles.TARGET)), true);
      return successes;
    }
  }

  private static int executeRemoveHealth(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, float value) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(livingEntity.getHealth() - value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.remove.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT), TextUtil.literal(livingEntity.getHealth()).styled(Styles.RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.remove.multiple", TextUtil.literal(finalSuccesses).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.RESULT)), true);
      return successes;
    }
  }

  private static int executeRemoveHealth(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(0);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.remove_all.single", TextUtil.styled(livingEntity.getDisplayName(), Styles.TARGET)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.remove_all.multiple", TextUtil.literal(finalSuccesses).styled(Styles.TARGET)), true);
      return successes;
    }
  }

  private static float getSourceEntityHealth(CommandContext<ServerCommandSource> context, ConcentrationType concentrationType) throws CommandSyntaxException {
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
