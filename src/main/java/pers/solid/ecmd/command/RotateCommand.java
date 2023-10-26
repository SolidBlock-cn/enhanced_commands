package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
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
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.argument.RegionArgumentType;
import pers.solid.ecmd.build.BlockTransformationCommand;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionbuilder.RegionBuilder;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import static net.minecraft.server.command.CommandManager.argument;
import static pers.solid.ecmd.argument.EnhancedPosArgumentType.CURRENT_POS;
import static pers.solid.ecmd.argument.EnhancedPosArgumentType.blockPos;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum RotateCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType keywordArgs = BlockTransformationCommand.createKeywordArgs(registryAccess)
        .addOptionalArg("pivot", blockPos(), CURRENT_POS)
        .addOptionalArg("interpolate", BoolArgumentType.bool(), false)
        .build();

    ModCommands.registerWithRegionArgumentModification(dispatcher, registryAccess, literalR2("rotate"), literalR2("/rotate"), argument("rotation", FloatArgumentType.floatArg())
        .executes(context -> executesRotate(keywordArgs.defaultArgs(), context))
        .then(argument("keyword_args", keywordArgs)
            .executes(context -> executesRotate(KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context))));
  }

  public static int executesRotate(KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final float rotation = MathHelper.wrapDegrees(FloatArgumentType.getFloat(context, "rotation"));
    final @Nullable BlockRotation blockRotation;
    if (rotation == 0) {
      blockRotation = BlockRotation.NONE;
    } else if (rotation == 90) {
      blockRotation = BlockRotation.CLOCKWISE_90;
    } else if (rotation == 180) {
      blockRotation = BlockRotation.CLOCKWISE_180;
    } else if (rotation == 270) {
      blockRotation = BlockRotation.COUNTERCLOCKWISE_90;
    } else {
      blockRotation = null;
    }
    final @Nullable AxisAngle4d axisAngle4d = blockRotation == null ? new AxisAngle4d(-Math.toRadians(rotation), 0, 1, 0) : null;
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
    final BlockTransformationCommand blockTransformationCommand = new BlockTransformationCommand() {
      @Override
      public Vec3i transformBlockPos(Vec3i original) {
        if (blockRotation != null) {
          return GeoUtil.rotate(original, blockRotation, pivot);
        } else {
          original = original.subtract(pivot);
          final Vector3d transform = axisAngle4d.transform(new Vector3d(original.getX() + 0.5, original.getY() + 0.5, original.getZ() + 0.5));
          return new BlockPos(MathHelper.floor(transform.x), MathHelper.floor(transform.y), MathHelper.floor(transform.z)).add(pivot);
        }
      }

      @Override
      public Vec3d transformPos(Vec3d original) {
        if (blockRotation != null) {
          return GeoUtil.rotate(original, blockRotation, pivot.toCenterPos());
        } else {
          original = original.subtract(pivot.toCenterPos());
          final Vector3d transform = axisAngle4d.transform(new Vector3d(original.x, original.y, original.z));
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
          final Vector3d transform = new AxisAngle4d(axisAngle4d.angle, -axisAngle4d.x, -axisAngle4d.y, -axisAngle4d.z).transform(new Vector3d(transformed.x, transformed.y, transformed.z));
          return new Vec3d(transform.x, transform.y, transform.z).add(pivot.toCenterPos());
        }
      }

      @Override
      public void transformEntity(Entity entity) {
        if (blockRotation != null) {
          entity.setYaw(entity.applyRotation(blockRotation));
        } else {
          entity.setYaw(entity.getYaw() + rotation);
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
