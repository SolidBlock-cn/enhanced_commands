package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableConsumer;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.util.NbtUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.Collection;
import java.util.Collections;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
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

public enum AirCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literalR2("air")
        .then(literal("get")
            .executes(context -> executeGetAir(context, Collections.singleton(context.getSource().getEntityOrThrow()), null))
            .then(argument("entities", entities())
                .executes(context -> executeGetAir(context, getEntities(context, "entities"), ConcentrationType.AVERAGE))
                .then(argument("concentration_type", concentrationType())
                    .executes(context -> executeGetAir(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type")))
                    .then(literal("store")
                        .then(argument("target", nbtTarget(registryAccess))
                            .then(argument("path", nbtPath())
                                .executes(context -> {
                                  final NbtTarget target = getNbtTarget(context, "target");
                                  final NbtPathArgumentType.NbtPath path = getNbtPath(context, "path");
                                  return executeGetAir(context, getEntities(context, "entities"), getConcentrationType(context, "concentration_type"), nbt -> target.modifyNbt(path, nbt));
                                })))))))
        .then(literal("set")
            .then(argument("entities", entities())
                .then(argument("value", integer())
                    .executes(context -> executeSetAir(context, getEntities(context, "entities"), getInteger(context, "value"))))
                .then(literal("from")
                    .then(literal("result").redirect(dispatcher.getRoot(), context -> {
                      final Collection<? extends Entity> entities = getEntities(context, "entities");
                      return context.getSource().mergeConsumers((context1, success, result) -> {
                        for (Entity entity : entities) {
                          entity.setAir(result);
                        }
                      }, SeparatedExecuteCommand.BINARY_RESULT_CONSUMER);
                    }))
                    .then(literal("success").redirect(dispatcher.getRoot(), context -> {
                      final Collection<? extends Entity> entities = getEntities(context, "entities");
                      return context.getSource().mergeConsumers((context1, success, result) -> {
                        for (Entity entity : entities) {
                          entity.setAir(success ? 1 : 0);
                        }
                      }, SeparatedExecuteCommand.BINARY_RESULT_CONSUMER);
                    }))
                    .then(literal("of").then(argument("source_entities", entities())
                        .executes(context -> executeSetAir(context, getEntities(context, "entities"), getSourceEntityAir(context, ConcentrationType.AVERAGE)))
                        .then(argument("source_concentration_type", concentrationType())
                            .executes(context -> executeSetAir(context, getEntities(context, "entities"), getSourceEntityAir(context, getConcentrationType(context, "source_concentration_type")))))))
                    .then(argument("source", nbtSource(registryAccess))
                        .then(argument("path", nbtPath())
                            .executes(context -> {
                              final NbtPathArgumentType.NbtPath path = getNbtPath(context, "path");
                              return executeSetAir(context, getEntities(context, "entities"), NbtUtil.toNumberOrThrow(getNbtSource(context, "source").getConcentratedNbts(path), path).intValue());
                            }))))))
        .then(literal("add")
            .executes(context -> executeAddAir(context, Collections.singleton(context.getSource().getEntityOrThrow())))
            .then(argument("entities", entities())
                .executes(context -> executeAddAir(context, getEntities(context, "entities")))
                .then(argument("value", integer())
                    .executes(context -> executeAddAir(context, getEntities(context, "entities"), getInteger(context, "value"))))))
        .then(literal("remove")
            .executes(context -> executeRemoveAir(context, Collections.singleton(context.getSource().getEntityOrThrow())))
            .then(argument("entities", entities())
                .executes(context -> executeRemoveAir(context, getEntities(context, "entities")))
                .then(argument("value", integer())
                    .executes(context -> executeRemoveAir(context, getEntities(context, "entities"), getInteger(context, "value")))))));
  }

  private static int executeGetAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, ConcentrationType concentrationType) {
    return executeGetAir(context, entities, concentrationType, null);
  }

  private static <T extends Throwable> int executeGetAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, ConcentrationType concentrationType, @Nullable FailableConsumer<NbtElement, T> nbtElementConsumer) throws T {
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      final int air = entity.getAir();
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.get.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(air).styled(Styles.RESULT)), false);
      if (nbtElementConsumer != null) {
        nbtElementConsumer.accept(NbtInt.of(air));
      }
      return air;
    } else {
      final IntList integers = new IntArrayList();
      for (Entity entity : entities) {
        integers.add(entity.getAir());
      }
      final double result = concentrationType.concentrateInt(integers);
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.get.multiple", integers.size(), concentrationType.getDisplayName(), Text.literal(concentrationType.longToString(result)).styled(Styles.RESULT)), false);
      if (nbtElementConsumer != null) {
        nbtElementConsumer.accept(concentrationType.longToNbt(result));
      }
      return (int) result;
    }
  }

  private static int executeSetAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, int value) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.set.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(entity.getAir()).styled(Styles.RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(value);
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.set.multiple", TextUtil.literal(size).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.TARGET)), true);
      return size;
    }
  }

  private static int executeAddAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, int value) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(entity.getAir() + value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.add.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(value).styled(Styles.TARGET), TextUtil.literal(entity.getAir()).styled(Styles.RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(entity.getAir() + value);
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.add.multiple", TextUtil.literal(size).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.TARGET)), true);
      return size;
    }
  }

  private static int executeAddAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(entity.getMaxAir());
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.add_to_max.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(entity.getAir()).styled(Styles.RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(entity.getMaxAir());
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.add_to_max.multiple", TextUtil.literal(size).styled(Styles.TARGET)), true);
      return size;
    }
  }

  private static int executeRemoveAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities, int value) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(entity.getAir() - value);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.remove.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.literal(value).styled(Styles.TARGET), TextUtil.literal(entity.getAir()).styled(Styles.RESULT)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(entity.getAir() - value);
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.remove.multiple", TextUtil.literal(size).styled(Styles.TARGET), TextUtil.literal(value).styled(Styles.TARGET)), true);
      return size;
    }
  }

  private static int executeRemoveAir(CommandContext<ServerCommandSource> context, Collection<? extends Entity> entities) {
    final int size = entities.size();
    if (size == 1) {
      final Entity entity = entities.iterator().next();
      entity.setAir(0);
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.air.remove_all.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET)), true);
      return 1;
    } else {
      for (Entity entity : entities) {
        entity.setAir(0);
      }
      CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.air.remove_all.multiple", TextUtil.literal(size).styled(Styles.TARGET)), true);
      return size;
    }
  }

  private static int getSourceEntityAir(CommandContext<ServerCommandSource> context, ConcentrationType concentrationType) throws CommandSyntaxException {
    final Collection<? extends Entity> sourceEntities = getEntities(context, "source_entities");
    if (sourceEntities.size() == 1) {
      final Entity entity = sourceEntities.iterator().next();
      return entity.getAir();
    } else {
      IntList ints = new IntArrayList();
      for (Entity sourceEntity : sourceEntities) {
        ints.add(sourceEntity.getAir());
      }
      return (int) concentrationType.concentrateInt(ints);
    }
  }
}
