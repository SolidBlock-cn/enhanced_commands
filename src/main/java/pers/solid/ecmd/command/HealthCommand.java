package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.argument.ConcentrationTypeArgumentType;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.Collection;
import java.util.Collections;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum HealthCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literalR2("health")
        .then(literal("get")
            .executes(context -> executeGetHealth(context, Collections.singleton(context.getSource().getEntityOrThrow()), null, 1))
            .then(argument("entities", EntityArgumentType.entities())
                .executes(context -> executeGetHealth(context, EntityArgumentType.getEntities(context, "entities"), ConcentrationType.AVERAGE, 1))
                .then(argument("concentration_type", ConcentrationTypeArgumentType.concentrationType())
                    .executes(context -> executeGetHealth(context, EntityArgumentType.getEntities(context, "entities"), ConcentrationTypeArgumentType.getConcentrationType(context, "concentration_type"), 1))
                    .then(argument("multiplier", DoubleArgumentType.doubleArg())
                        .executes(context -> executeGetHealth(context, EntityArgumentType.getEntities(context, "entities"), ConcentrationTypeArgumentType.getConcentrationType(context, "concentration_type"), DoubleArgumentType.getDouble(context, "multiplier")))))))
        .then(literal("set")
            .then(argument("entities", EntityArgumentType.entities())
                .then(argument("value", FloatArgumentType.floatArg())
                    .executes(context -> executeSetHealth(context, EntityArgumentType.getEntities(context, "entities"), FloatArgumentType.getFloat(context, "value"))))))
        .then(literal("add")
            .executes(context -> executeAddHealth(context, Collections.singleton(context.getSource().getEntityOrThrow())))
            .then(argument("entities", EntityArgumentType.entities())
                .executes(context -> executeAddHealth(context, EntityArgumentType.getEntities(context, "entities")))
                .then(argument("value", FloatArgumentType.floatArg())
                    .executes(context -> executeAddHealth(context, EntityArgumentType.getEntities(context, "entities"), FloatArgumentType.getFloat(context, "value"))))))
        .then(literal("remove")
            .executes(context -> executeRemoveHealth(context, Collections.singleton(context.getSource().getEntityOrThrow())))
            .then(argument("entities", EntityArgumentType.entities())
                .executes(context -> executeRemoveHealth(context, EntityArgumentType.getEntities(context, "entities")))
                .then(argument("value", FloatArgumentType.floatArg())
                    .executes(context -> executeRemoveHealth(context, EntityArgumentType.getEntities(context, "entities"), FloatArgumentType.getFloat(context, "value")))))));
  }

  public static final DynamicCommandExceptionType NOT_LIVING = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.health.get.single.not_living", o));
  public static final DynamicCommandExceptionType NOT_LIVING_MULTIPLE = new DynamicCommandExceptionType(o -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.get.multiple.not_living", o));

  private static int executeGetHealth(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, ConcentrationType concentrationType, double multiplier) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      final float health = livingEntity.getHealth();
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.get.single", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(health).styled(TextUtil.STYLE_FOR_RESULT)), false);
      return (int) (health * multiplier);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.get.multiple", floats.size(), concentrationType.getDisplayName(), Text.literal(concentrationType.floatToString(result)).styled(TextUtil.STYLE_FOR_RESULT)), false);
      return (int) (result * multiplier);
    }
  }

  private static int executeSetHealth(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, float value) throws CommandSyntaxException {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      if (!(entity instanceof LivingEntity livingEntity)) {
        throw NOT_LIVING.create(entity.getDisplayName());
      }
      livingEntity.setHealth(value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.set.single", TextUtil.styled(livingEntity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(livingEntity.getHealth()).styled(TextUtil.STYLE_FOR_RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.set.multiple", TextUtil.literal(finalSuccesses).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.add.single", TextUtil.styled(livingEntity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_RESULT), TextUtil.literal(livingEntity.getHealth()).styled(TextUtil.STYLE_FOR_RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.add.multiple", TextUtil.literal(finalSuccesses).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.add_to_max.single", TextUtil.styled(livingEntity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(livingEntity.getHealth()).styled(TextUtil.STYLE_FOR_RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.add_to_max.multiple", TextUtil.literal(finalSuccesses).styled(TextUtil.STYLE_FOR_TARGET)), true);
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
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.remove.single", TextUtil.styled(livingEntity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_RESULT), TextUtil.literal(livingEntity.getHealth()).styled(TextUtil.STYLE_FOR_RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.remove.multiple", TextUtil.literal(finalSuccesses).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_RESULT)), true);
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
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.health.remove_all.single", TextUtil.styled(livingEntity.getDisplayName(), TextUtil.STYLE_FOR_TARGET)), true);
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
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.health.remove_all.multiple", TextUtil.literal(finalSuccesses).styled(TextUtil.STYLE_FOR_TARGET)), true);
      return successes;
    }
  }
}
