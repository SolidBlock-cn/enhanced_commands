package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.util.BlockMirror;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.api.FlipStateCallback;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.BlockTransformationCommand;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.regionbuilder.RegionBuilder;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import static net.minecraft.server.command.CommandManager.argument;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum MirrorCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final KeywordArgsArgumentType keywordArgs = BlockTransformationCommand.createKeywordArgs(registryAccess)
        .addOptionalArg("pivot", EnhancedPosArgumentType.blockPos(), EnhancedPosArgumentType.CURRENT_POS)
        .build();
    ModCommands.registerWithArgumentModification(
        dispatcher,
        literalR2("mirror"),
        literalR2("/mirror")
            .executes(context -> {
              final ServerCommandSource source = context.getSource();
              return executeMirror(((ServerPlayerEntityExtension) source.getPlayerOrThrow()).ec$getOrEvaluateActiveRegionOrThrow(source), AxisArgument.FRONT_BACK.apply(source), keywordArgs.defaultArgs(), context);
            }),
        argument("region", RegionArgumentType.region(registryAccess))
            .executes(context -> executeMirror(AxisArgument.FRONT_BACK.apply(context.getSource()), keywordArgs.defaultArgs(), context))
            .then(argument("axis", AxisArgumentType.axis(false))
                .executes(context -> executeMirror(AxisArgumentType.getAxis(context, "axis"), keywordArgs.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgs)
                    .executes(context -> executeMirror(AxisArgumentType.getAxis(context, "axis"), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context)))),
        ModCommands.REGION_ARGUMENTS_MODIFIER
    );
  }

  public static int executeMirror(Direction.Axis axis, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeMirror(RegionArgumentType.getRegion(context, "region"), axis, keywordArgs, context);
  }

  public static int executeMirror(Region region, Direction.Axis axis, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final BlockPos pivot = keywordArgs.<PosArgument>getArg("pivot").toAbsoluteBlockPos(context.getSource());
    return new BlockTransformationCommand() {
      @Override
      public Vec3i transformBlockPos(Vec3i original) {
        return GeoUtil.mirror(original, axis, pivot);
      }

      @Override
      public Vec3d transformPos(Vec3d original) {
        return GeoUtil.mirror(original, axis, pivot.toCenterPos());
      }

      @Override
      public Vec3d transformPosBack(Vec3d transformed) {
        return GeoUtil.mirror(transformed, axis, pivot.toCenterPos());
      }

      @Override
      public void transformEntity(Entity entity) {
        final float newYaw = entity.applyMirror(switch (axis) {
          case X -> BlockMirror.FRONT_BACK;
          case Z -> BlockMirror.LEFT_RIGHT;
          default -> BlockMirror.NONE;
        });
        final float newPitch = axis == Direction.Axis.Y ? -entity.getPitch() : entity.getPitch();
        if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
          serverPlayerEntity.networkHandler.requestTeleport(entity.getX(), entity.getY(), entity.getZ(), newYaw, newPitch, PositionFlag.VALUES);
        } else {
          entity.setYaw(newYaw);
          if (axis == Direction.Axis.Y) {
            entity.setPos(entity.getX(), entity.getY(), entity.getZ());
            entity.setPitch(newPitch);
            entity.resetPosition();
          }
        }
      }

      @Override
      public BlockState transformBlockState(BlockState original) {
        return FlipStateCallback.getMirroredState(original, axis);
      }

      @Override
      public Region transformRegion(Region region) {
        return region.mirrored(axis, pivot.toCenterPos());
      }

      @Override
      public void transformRegionBuilder(RegionBuilder regionBuilder) {
        regionBuilder.mirror(axis, pivot.toCenterPos());
      }

      @Override
      public void notifyCompletion(ServerCommandSource source, int affectedNum) {
        CommandBridge.sendFeedback(source, () -> TextUtil.enhancedTranslatable("enhancedCommands.commands.mirror.complete", Integer.toString(affectedNum)), true);
      }

      @Override
      public @NotNull MutableText getIteratorTaskName(Region region) {
        return Text.translatable("enhancedCommands.commands.mirror.task", region.asString());
      }
    }.execute(region, keywordArgs, context);
  }
}