package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.Set;
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
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum VelocityCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    dispatcher.register(literalR2("velocity")
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
        .then(literal("set")
            .then(argument("target", entities())
                .then(argument("vector", vec3(false))
                    .executes(context -> executeSet(context.getSource(), getEntities(context, "target"), getVec3(context, "vector"))))))
    );
  }

  private static int executeSet(CommandSourceStack source, Collection<? extends Entity> entities, Vec3 vec3) {
    for (Entity entity : entities) {
      entity.setDeltaMovement(vec3);
      if (entity instanceof ServerPlayer player) {
        player.connection.send(new ClientboundPlayerPositionPacket(entity.getId(), new PositionMoveRotation(player.position(), vec3, player.getYRot(), player.getXRot()), Set.of()));
      }
    }

    source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.velocity.set.success", TextUtil.wrapEntities(entities), TextUtil.wrapVector(vec3)), true);
    return entities.size();
  }

  private static int executeModify(CommandSourceStack source, Collection<? extends Entity> entities, UnaryOperator<Vec3> modifier, Supplier<Component> notifier) {
    for (Entity entity : entities) {
      final Vec3 applied = modifier.apply(entity.getDeltaMovement());
      entity.setDeltaMovement(applied);
      if (entity instanceof ServerPlayer player) {
        player.connection.send(new ClientboundPlayerPositionPacket(entity.getId(), new PositionMoveRotation(player.position(), applied, player.getYRot(), player.getXRot()), Set.of()));
      }
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
