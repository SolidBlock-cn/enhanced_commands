package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4d;
import org.joml.Vector3d;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.BlockTransformationCommand;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.GeoUtil;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.argument.EnhancedPosArgumentType.CURRENT_POS;
import static pers.solid.ecmd.argument.EnhancedPosArgumentType.blockPos;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum RotateCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final KeywordArgsArgumentType keywordArgs = BlockTransformationCommand.createKeywordArgs(commandBuildContext)
        .addOptionalArg("interpolate", BoolArgumentType.bool(), false)
        .addOptionalArg("pivot", blockPos(), CURRENT_POS)
        .build();

    ModCommands.registerWithRegionArgumentModification(
        dispatcher,
        literalR2("rotate"),
        literalR2("/rotate"),
        argument("region", RegionArgumentType.region(commandBuildContext))
            .then(argument("rotation", AngleArgumentType.angle(false))
                .executes(context -> executeRotate(null, keywordArgs.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgs)
                    .executes(context -> executeRotate(null, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context)))
                .then(literal("around")
                    .then(argument("around_direction", DirectionArgumentType.direction())
                        .executes(context -> executeRotate(new AxisAngle4d(Math.toRadians(AngleArgumentType.getAngle(context, "rotation")), DirectionArgumentType.getDirection(context, "around_direction").step()), keywordArgs.defaultArgs(), context))
                        .then(argument("keyword_args", keywordArgs)
                            .executes(context -> executeRotate(new AxisAngle4d(Math.toRadians(AngleArgumentType.getAngle(context, "rotation")), DirectionArgumentType.getDirection(context, "around_direction").step()), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context))))
                    .then(literal("vector")
                        .then(argument("x", DoubleArgumentType.doubleArg())
                            .then(argument("y", DoubleArgumentType.doubleArg())
                                .then(argument("z", DoubleArgumentType.doubleArg())
                                    .executes(context -> executeRotate(new AxisAngle4d(Math.toRadians(AngleArgumentType.getAngle(context, "rotation")), DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z")).normalize(), keywordArgs.defaultArgs(), context))
                                    .then(argument("keyword_args", keywordArgs)
                                        .executes(context -> executeRotate(new AxisAngle4d(Math.toRadians(AngleArgumentType.getAngle(context, "rotation")), DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z")).normalize(), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context))))))))));
  }

  public static int executeRotate(@Nullable AxisAngle4d axisAngle4d, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final double rotation;
    final @Nullable Rotation blockRotation;
    if (axisAngle4d == null) {
      rotation = Mth.wrapDegrees(AngleArgumentType.getAngle(context, "rotation"));
      if (rotation == 0) {
        blockRotation = Rotation.NONE;
      } else if (rotation == 90) {
        blockRotation = Rotation.COUNTERCLOCKWISE_90;
      } else if (rotation == -180) {
        blockRotation = Rotation.CLOCKWISE_180;
      } else if (rotation == -90) {
        blockRotation = Rotation.CLOCKWISE_90;
      } else {
        blockRotation = null;
      }
    } else {
      rotation = axisAngle4d.angle;
      blockRotation = null;
    }
    axisAngle4d = axisAngle4d == null ? new AxisAngle4d(Math.toRadians(rotation), 0, 1, 0) : axisAngle4d;
    final @NotNull Rotation nearestBlockRotation;
    final BlockPos pivot = keywordArgs.<Coordinates>getArg("pivot").getBlockPos(context.getSource());

    if (blockRotation != null) {
      nearestBlockRotation = blockRotation;
    } else {
      if (rotation < 45 || rotation >= 315) {
        nearestBlockRotation = Rotation.NONE;
      } else if (rotation < 135) {
        nearestBlockRotation = Rotation.CLOCKWISE_90;
      } else if (rotation < 225) {
        nearestBlockRotation = Rotation.CLOCKWISE_180;
      } else {
        nearestBlockRotation = Rotation.COUNTERCLOCKWISE_90;
      }
    }

    // 由于是可变变量，需要复制为 final 变量后再在 lambda 中使用
    final @NotNull AxisAngle4d finalAxisAngle4d = axisAngle4d;
    final BlockTransformationCommand blockTransformationCommand = new BlockTransformationCommand() {
      @Override
      public Vec3i transformBlockPos(Vec3i original) {
        if (blockRotation != null) {
          return GeoUtil.rotate(original, blockRotation, pivot);
        } else {
          original = original.subtract(pivot);
          final Vector3d transform = finalAxisAngle4d.transform(new Vector3d(original.getX(), original.getY(), original.getZ()));
          return new BlockPos(Mth.floor(transform.x + 0.5), Mth.floor(transform.y + 0.5), Mth.floor(transform.z + 0.5)).offset(pivot);
        }
      }

      @Override
      public Vec3 transformPos(Vec3 original) {
        if (blockRotation != null) {
          return GeoUtil.rotate(original, blockRotation, pivot.getCenter());
        } else {
          original = original.subtract(pivot.getCenter());
          final Vector3d transform = finalAxisAngle4d.transform(new Vector3d(original.x, original.y, original.z));
          return new Vec3(transform.x, transform.y, transform.z).add(pivot.getCenter());
        }
      }

      @Override
      public Vec3 transformPosBack(Vec3 transformed) {
        if (blockRotation != null) {
          return GeoUtil.rotate(transformed, switch (blockRotation) {
            case CLOCKWISE_180 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> blockRotation;
          }, pivot.getCenter());
        } else {
          transformed = transformed.subtract(pivot.getCenter());
          final Vector3d transform = new AxisAngle4d(finalAxisAngle4d.angle, -finalAxisAngle4d.x, -finalAxisAngle4d.y, -finalAxisAngle4d.z).transform(new Vector3d(transformed.x, transformed.y, transformed.z));
          return new Vec3(transform.x, transform.y, transform.z).add(pivot.getCenter());
        }
      }

      @Override
      public void transformEntity(@NotNull Entity entity) {
        final float newYaw;
        if (blockRotation != null) {
          newYaw = entity.rotate(blockRotation);
        } else {
          newYaw = entity.getYRot() - (float) rotation;
        }
        if (entity instanceof ServerPlayer serverPlayerEntity) {
          serverPlayerEntity.connection.teleport(entity.getX(), entity.getY(), entity.getZ(), newYaw, entity.getXRot(), RelativeMovement.ALL);
        } else {
          entity.setYRot(newYaw);
        }
      }

      @Override
      public void transformEntityBack(@NotNull Entity entity) {
        final float newYaw;
        if (blockRotation != null) {
          newYaw = entity.rotate(switch (blockRotation) {
            case NONE -> Rotation.NONE;
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case CLOCKWISE_180 -> Rotation.CLOCKWISE_180;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
          });
        } else {
          newYaw = entity.getYRot() + (float) rotation;
        }
        if (entity instanceof ServerPlayer serverPlayerEntity) {
          serverPlayerEntity.connection.teleport(entity.getX(), entity.getY(), entity.getZ(), newYaw, entity.getXRot(), RelativeMovement.ALL);
        } else {
          entity.setYRot(newYaw);
        }
      }

      @Override
      public @NotNull BlockState transformBlockState(@NotNull BlockState original) {
        return original.rotate(nearestBlockRotation);
      }

      @Override
      public @NotNull Region transformRegion(@NotNull Region region) {
        return region.rotated(nearestBlockRotation, pivot.getCenter());
      }

      @Override
      public void notifyCompletion(CommandSourceStack source, int affectedBlocks, int affectedEntities) {
        if (affectedEntities == -1) {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.rotate.complete", Integer.toString(affectedBlocks)).enhanced$$(), true);
        } else {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.rotate.complete_with_entities", Integer.toString(affectedBlocks), Integer.toString(affectedEntities)).enhanced$$(), true);
        }
      }

      @Override
      public @NotNull MutableComponent getIteratorTaskName(Region region) {
        return Component.translatable("enhanced_commands.commands.rotate.task", region.asString());
      }
    };

    return blockTransformationCommand.execute(RegionArgumentType.getRegion(context, "region"), keywordArgs, context);
  }
}
