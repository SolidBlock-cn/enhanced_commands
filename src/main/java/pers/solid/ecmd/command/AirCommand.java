package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
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

public enum AirCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literalR2("air")
        .then(literal("get")
            .executes(context -> executeGetAir(context, Collections.singleton(context.getSource().getEntityOrThrow()), null))
            .then(argument("entities", EntityArgumentType.entities())
                .executes(context -> executeGetAir(context, EntityArgumentType.getEntities(context, "entities"), ConcentrationType.AVERAGE))
                .then(argument("concentration_type", ConcentrationTypeArgumentType.concentrationType())
                    .executes(context -> executeGetAir(context, EntityArgumentType.getEntities(context, "entities"), ConcentrationTypeArgumentType.getConcentrationType(context, "concentration_type"))))))
        .then(literal("set")
            .then(argument("entities", EntityArgumentType.entities())
                .then(argument("value", IntegerArgumentType.integer())
                    .executes(context -> executeSetAir(context, EntityArgumentType.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "value"))))))
        .then(literal("add")
            .executes(context -> executeAddAir(context, Collections.singleton(context.getSource().getEntityOrThrow())))
            .then(argument("entities", EntityArgumentType.entities())
                .executes(context -> executeAddAir(context, EntityArgumentType.getEntities(context, "entities")))
                .then(argument("value", IntegerArgumentType.integer())
                    .executes(context -> executeAddAir(context, EntityArgumentType.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "value"))))))
        .then(literal("remove")
            .executes(context -> executeRemoveAir(context, Collections.singleton(context.getSource().getEntityOrThrow())))
            .then(argument("entities", EntityArgumentType.entities())
                .executes(context -> executeRemoveAir(context, EntityArgumentType.getEntities(context, "entities")))
                .then(argument("value", IntegerArgumentType.integer())
                    .executes(context -> executeRemoveAir(context, EntityArgumentType.getEntities(context, "entities"), IntegerArgumentType.getInteger(context, "value")))))));
  }

  private static int executeGetAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, ConcentrationType concentrationType) {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      final int air = entity.getAir();
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.get.single", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(air).styled(TextUtil.STYLE_FOR_RESULT)), false);
      return air;
    } else {
      final IntList integers = new IntArrayList();
      for (Entity entity : entities) {
        integers.add(entity.getAir());
      }
      final double result = concentrationType.concentrateInt(integers);
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.get.multiple", integers.size(), concentrationType.getDisplayName(), Text.literal(concentrationType.longToString(result)).styled(TextUtil.STYLE_FOR_RESULT)), false);
      return (int) result;
    }
  }

  private static int executeSetAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, int value) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.set.single", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(entity.getAir()).styled(TextUtil.STYLE_FOR_RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(value);
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.set.multiple", TextUtil.literal(size).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_TARGET)), true);
      return size;
    }
  }

  private static int executeAddAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, int value) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(entity.getAir() + value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.add.single", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(entity.getAir()).styled(TextUtil.STYLE_FOR_RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(entity.getAir() + value);
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.add.multiple", TextUtil.literal(size).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_TARGET)), true);
      return size;
    }
  }

  private static int executeAddAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(entity.getMaxAir());
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.add_to_max.single", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(entity.getAir()).styled(TextUtil.STYLE_FOR_RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(entity.getMaxAir());
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.add_to_max.multiple", TextUtil.literal(size).styled(TextUtil.STYLE_FOR_TARGET)), true);
      return size;
    }
  }

  private static int executeRemoveAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, int value) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(entity.getAir() - value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.remove.single", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(entity.getAir()).styled(TextUtil.STYLE_FOR_RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(entity.getAir() - value);
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.remove.multiple", TextUtil.literal(size).styled(TextUtil.STYLE_FOR_TARGET), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_TARGET)), true);
      return size;
    }
  }

  private static int executeRemoveAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(0);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.remove_all.single", TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(0);
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.remove_all.multiple", TextUtil.literal(size).styled(TextUtil.STYLE_FOR_TARGET)), true);
      return size;
    }
  }
}
