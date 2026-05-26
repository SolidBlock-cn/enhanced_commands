package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.math.ConcentrationType;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.EntityArgument.entities;
import static net.minecraft.commands.arguments.EntityArgument.getEntities;
import static net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3;
import static net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3;
import static pers.solid.ecmd.argument.SimpleEnumArgument.concentrationType;
import static pers.solid.ecmd.argument.SimpleEnumArgument.getConcentrationType;
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum VelocityCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    dispatcher.register(literalR2("velocity")
        .then(literal("get")
            .executes(context -> executeGet(context, Collections.singleton(context.getSource().getEntityOrException()), ConcentrationType.AVERAGE))
            .then(argument("target", entities())
                .executes(context -> executeGet(context, getEntities(context, "target"), ConcentrationType.AVERAGE))
                .then(argument("concentration_type", concentrationType())
                    .executes(context -> executeGet(context, getEntities(context, "target"), getConcentrationType(context, "concentration_type"))))))
        .then(literal("set")
            .then(argument("target", entities())
                .then(argument("vector", vec3(false))
                    .executes(context -> executeSet(context.getSource(), getEntities(context, "target"), getVec3(context, "vector"))))))
        .then(literal("add")
            .then(argument("target", entities())
                .then(argument("vector", vec3(false))
                    .executes(context -> executeAdd(context.getSource(), getEntities(context, "target"), getVec3(context, "vector"))))))
        .then(literal("subtract")
            .then(argument("target", entities())
                .then(argument("vector", vec3(false))
                    .executes(context -> executeSubtract(context.getSource(), getEntities(context, "target"), getVec3(context, "vector"))))))
        .then(literal("scale")
            .then(argument("target", entities())
                .then(argument("scale", doubleArg())
                    .executes(context -> executeScale(context.getSource(), getEntities(context, "target"), getDouble(context, "scale"))))))
        .then(literal("multiply")
            .then(argument("target", entities())
                .then(argument("vector", vec3(false))
                    .executes(context -> executeMultiply(context.getSource(), getEntities(context, "target"), getVec3(context, "vector"))))))
        .then(literal("cross")
            .then(argument("target", entities())
                .then(argument("vector", vec3(false))
                    .executes(context -> executeCross(context.getSource(), getEntities(context, "target"), getVec3(context, "vector"))))))
        .then(literal("setsize")
            .then(argument("target", entities())
                .then(argument("size", doubleArg())
                    .executes(context -> executeSetSize(context.getSource(), getEntities(context, "target"), getDouble(context, "size"))))))
    );
  }

  private static int executeGet(CommandContext<CommandSourceStack> context, Collection<? extends Entity> entities, ConcentrationType concentrationType) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    if (entities.size() == 1) {
      final Entity entity = entities.iterator().next();
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.velocity.get.single", TextUtil.styled(entity.getDisplayName(), Styles.TARGET), TextUtil.wrapVector(entity.getDeltaMovement()).withStyle(Styles.RESULT)), false);
      return 1;
    } else {
      final List<Vec3> list = new ArrayList<>();
      for (Entity entity : entities) {
        list.add(entity.getDeltaMovement());
      }
      final Vec3 concentrated = concentrationType.concentrateVec3(list);
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.velocity.get.multiple", TextUtil.styled(TextUtil.wrapEntities(entities), Styles.TARGET), concentrationType.getDisplayName(), TextUtil.wrapVector(concentrated).withStyle(Styles.RESULT)), false);
      return entities.size();
    }
  }

  private static int executeSet(CommandSourceStack source, Collection<? extends Entity> entities, Vec3 vec3) {
    boolean hasPlayer = false;
    for (Entity entity : entities) {
      entity.setDeltaMovement(vec3);
      if (entity instanceof ServerPlayer player) {
        player.connection.send(new ClientboundPlayerPositionPacket(entity.getId(), new PositionMoveRotation(player.position(), vec3, player.getYRot(), player.getXRot()), Set.of()));
      }
    }

    if (hasPlayer) {
      // 由于玩家是实验性功能，当这些实体中有玩家时，会发出提示：
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.velocity.experimental_warning").withStyle(ChatFormatting.RED), false);
    }
    source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.velocity.set.success", TextUtil.wrapEntities(entities), TextUtil.wrapVector(vec3)), true);
    return entities.size();
  }

  private static int executeModify(CommandSourceStack source, Collection<? extends Entity> entities, UnaryOperator<Vec3> modifier, Supplier<Component> notifier) {
    boolean hasPlayer = false;
    for (Entity entity : entities) {
      final Vec3 applied = modifier.apply(entity.getDeltaMovement());
      entity.setDeltaMovement(applied);
      if (entity instanceof ServerPlayer player) {
//        player.connection.send(new ClientboundPlayerPositionPacket(entity.getId(), player.position(), applied, player.getYRot(), player.getXRot(), Set.of()));
        // todo 在 1.21.1 中同步
      }
    }

    if (hasPlayer) {
      // 由于玩家是实验性功能，当这些实体中有玩家时，会发出提示：
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.velocity.experimental_warning").withStyle(ChatFormatting.RED), false);
    }
    source.sendFeedback$ecBridge(notifier, true);
    return entities.size();
  }

  private static int executeAdd(CommandSourceStack source, Collection<? extends Entity> entities, Vec3 vec3) {
    return executeModify(source, entities, v -> v.add(vec3), () -> Component.translatable("enhanced_commands.commands.velocity.add.success", TextUtil.wrapEntities(entities), TextUtil.wrapVector(vec3)));
  }

  private static int executeSubtract(CommandSourceStack source, Collection<? extends Entity> entities, Vec3 vec3) {
    return executeModify(source, entities, v -> v.subtract(vec3), () -> Component.translatable("enhanced_commands.commands.velocity.subtract.success", TextUtil.wrapEntities(entities), TextUtil.wrapVector(vec3)));
  }

  private static int executeScale(CommandSourceStack source, Collection<? extends Entity> entities, double factor) {
    return executeModify(source, entities, v -> v.scale(factor), () -> Component.translatable("enhanced_commands.commands.velocity.scale.success", TextUtil.wrapEntities(entities), factor));
  }

  private static int executeMultiply(CommandSourceStack source, Collection<? extends Entity> entities, Vec3 vec3) {
    return executeModify(source, entities, v -> v.multiply(vec3), () -> Component.translatable("enhanced_commands.commands.velocity.multiply.success", TextUtil.wrapEntities(entities), TextUtil.wrapVector(vec3)));
  }

  private static int executeCross(CommandSourceStack source, Collection<? extends Entity> entities, Vec3 vec3) {
    return executeModify(source, entities, v -> v.cross(vec3), () -> Component.translatable("enhanced_commands.commands.velocity.cross.success", TextUtil.wrapEntities(entities), TextUtil.wrapVector(vec3)));
  }

  private static int executeSetSize(CommandSourceStack source, Collection<? extends Entity> entities, double size) {
    return executeModify(source, entities, v -> {
      final double originalSize = v.length();
      if (originalSize == 0) {
        return v;
      } else {
        return v.scale(size / originalSize);
      }
    }, () -> Component.translatable("enhanced_commands.commands.velocity.setsize.success", TextUtil.wrapEntities(entities), size));
  }
}
