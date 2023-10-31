package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4d;
import org.joml.Vector3d;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.BlockTransformationCommand;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionbuilder.RegionBuilder;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.EnhancedPosArgumentType.CURRENT_POS;
import static pers.solid.ecmd.argument.EnhancedPosArgumentType.blockPos;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum RotateCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType keywordArgs = BlockTransformationCommand.createKeywordArgs(registryAccess)
        .addOptionalArg("interpolate", BoolArgumentType.bool(), false)
        .addOptionalArg("pivot", blockPos(), CURRENT_POS)
        .build();

    ModCommands.registerWithRegionArgumentModification(dispatcher, registryAccess, literalR2("rotate"), literalR2("/rotate"), argument("rotation", AngleArgumentType.angle(false))
        .executes(context -> executeRotate(null, keywordArgs.defaultArgs(), context))
        .then(argument("keyword_args", keywordArgs)
            .executes(context -> executeRotate(null, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context)))
        .then(literal("around")
            .then(argument("around_direction", DirectionArgumentType.direction())
                .executes(context -> executeRotate(new AxisAngle4d(Math.toRadians(AngleArgumentType.getAngle(context, "rotation")), DirectionArgumentType.getDirection(context, "around_direction").getUnitVector()), keywordArgs.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgs)
                    .executes(context -> executeRotate(new AxisAngle4d(Math.toRadians(AngleArgumentType.getAngle(context, "rotation")), DirectionArgumentType.getDirection(context, "around_direction").getUnitVector()), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context))))
            .then(literal("vector")
                .then(argument("x", DoubleArgumentType.doubleArg())
                    .then(argument("y", DoubleArgumentType.doubleArg())
                        .then(argument("z", DoubleArgumentType.doubleArg())
                            .executes(context -> executeRotate(new AxisAngle4d(Math.toRadians(AngleArgumentType.getAngle(context, "rotation")), DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z")).normalize(), keywordArgs.defaultArgs(), context))
                            .then(argument("keyword_args", keywordArgs)
                                .executes(context -> executeRotate(new AxisAngle4d(Math.toRadians(AngleArgumentType.getAngle(context, "rotation")), DoubleArgumentType.getDouble(context, "x"), DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z")).normalize(), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context)))))))));
  }

  public static int executeRotate(@Nullable AxisAngle4d axisAngle4d, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final double rotation;
    final @Nullable BlockRotation blockRotation;
    if (axisAngle4d == null) {
      rotation = MathHelper.wrapDegrees(AngleArgumentType.getAngle(context, "rotation"));
      if (rotation == 0) {
        blockRotation = BlockRotation.NONE;
      } else if (rotation == 90) {
        blockRotation = BlockRotation.COUNTERCLOCKWISE_90;
      } else if (rotation == -180) {
        blockRotation = BlockRotation.CLOCKWISE_180;
      } else if (rotation == -90) {
        blockRotation = BlockRotation.CLOCKWISE_90;
      } else {
        blockRotation = null;
      }
    } else {
      rotation = axisAngle4d.angle;
      blockRotation = null;
    }
    axisAngle4d = axisAngle4d == null ? new AxisAngle4d(Math.toRadians(rotation), 0, 1, 0) : axisAngle4d;
    final @NotNull BlockRotation nearestBlockRotation;
    final BlockPos pivot = keywordArgs.<PosArgument>getArg("pivot").toAbsoluteBlockPos(context.getSource());

    if (blockRotation != null) {
      nearestBlockRotation = blockRotation;
    } else {
      if (rotation < 45 || rotation >= 315) {
        nearestBlockRotation = BlockRotation.NONE;
      } else if (rotation < 135) {
        nearestBlockRotation = BlockRotation.CLOCKWISE_90;
      } else if (rotation < 225) {
        nearestBlockRotation = BlockRotation.CLOCKWISE_180;
      } else {
        nearestBlockRotation = BlockRotation.COUNTERCLOCKWISE_90;
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
          return new BlockPos(MathHelper.floor(transform.x + 0.5), MathHelper.floor(transform.y + 0.5), MathHelper.floor(transform.z + 0.5)).add(pivot);
        }
      }

      @Override
      public Vec3d transformPos(Vec3d original) {
        if (blockRotation != null) {
          return GeoUtil.rotate(original, blockRotation, pivot.toCenterPos());
        } else {
          original = original.subtract(pivot.toCenterPos());
          final Vector3d transform = finalAxisAngle4d.transform(new Vector3d(original.x, original.y, original.z));
          return new Vec3d(transform.x, transform.y, transform.z).add(pivot.toCenterPos());
        }
      }

      @Override
      public Vec3d transformPosBack(Vec3d transformed) {
        if (blockRotation != null) {
          return GeoUtil.rotate(transformed, switch (blockRotation) {
            case CLOCKWISE_180 -> BlockRotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> BlockRotation.CLOCKWISE_90;
            default -> blockRotation;
          }, pivot.toCenterPos());
        } else {
          transformed = transformed.subtract(pivot.toCenterPos());
          final Vector3d transform = new AxisAngle4d(finalAxisAngle4d.angle, -finalAxisAngle4d.x, -finalAxisAngle4d.y, -finalAxisAngle4d.z).transform(new Vector3d(transformed.x, transformed.y, transformed.z));
          return new Vec3d(transform.x, transform.y, transform.z).add(pivot.toCenterPos());
        }
      }

      @Override
      public void transformEntity(Entity entity) {
        final float newYaw;
        if (blockRotation != null) {
          newYaw = entity.applyRotation(blockRotation);
        } else {
          newYaw = entity.getYaw() + (float) rotation;
        }
        if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
          serverPlayerEntity.networkHandler.requestTeleport(entity.getX(), entity.getY(), entity.getZ(), newYaw, entity.getPitch(), PositionFlag.VALUES);
        } else {
          entity.setYaw(newYaw);
        }
      }

      @Override
      public BlockState transformBlockState(BlockState original) {
        return original.rotate(nearestBlockRotation);
      }

      @Override
      public Region transformRegion(Region region) {
        return region.rotated(nearestBlockRotation, pivot.toCenterPos());
      }

      @Override
      public void transformRegionBuilder(RegionBuilder regionBuilder) {
        regionBuilder.rotate(nearestBlockRotation, pivot.toCenterPos());
      }

      @Override
      public void notifyCompletion(ServerCommandSource source, int affectedNum) {
        CommandBridge.sendFeedback(source, () -> TextUtil.enhancedTranslatable("enhancedCommands.commands.rotate.complete", Integer.toString(affectedNum)), true);
      }

      @Override
      public @NotNull MutableText getIteratorTaskName(Region region) {
        return Text.translatable("enhancedCommands.commands.rotate.task", region.asString());
      }
    };

    return blockTransformationCommand.execute(RegionArgumentType.getRegion(context, "region"), keywordArgs, context);
  }
}
