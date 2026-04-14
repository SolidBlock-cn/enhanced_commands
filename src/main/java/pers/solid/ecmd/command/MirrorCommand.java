package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.api.FlipStateCallback;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.BlockTransformationCommand;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.GeoUtil;

import static net.minecraft.commands.Commands.argument;
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum MirrorCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final KeywordArgsArgument keywordArgs = BlockTransformationCommand.createKeywordArgs(commandBuildContext)
        .addOptionalArg("pivot", EnhancedPosArgument.blockPos(), EnhancedPosArgument.CURRENT_POS)
        .build();
    EnhancedCommandsCommands.registerWithRegionArgumentModification(
        dispatcher,
        literalR2("mirror"),
        literalR2("/mirror"),
        argument("region", RegionArgument.region(commandBuildContext))
            .then(argument("axis", SimpleEnumArgument.axis(false))
                .executes(context -> executeMirror(SimpleEnumArgument.getAxis(context, "axis"), keywordArgs.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgs)
                    .executes(context -> executeMirror(SimpleEnumArgument.getAxis(context, "axis"), KeywordArgsArgument.getKeywordArgs(context, "keyword_args"), context))))
            .executes(context -> executeMirror(AxisProvider.FRONT_BACK.apply(context.getSource()), keywordArgs.defaultArgs(), context))
    );
  }

  public static int executeMirror(Direction.Axis axis, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeMirror(RegionArgument.getRegion(context, "region"), axis, keywordArgs, context);
  }

  public static int executeMirror(Region region, Direction.Axis axis, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final BlockPos pivot = keywordArgs.<Coordinates>getArg("pivot").getBlockPos(context.getSource());
    return new BlockTransformationCommand() {
      @Override
      public Vec3i transformBlockPos(Vec3i original) {
        return GeoUtil.mirror(original, axis, pivot);
      }

      @Override
      public Vec3 transformPos(Vec3 original) {
        return GeoUtil.mirror(original, axis, pivot.getCenter());
      }

      @Override
      public Vec3 transformPosBack(Vec3 transformed) {
        return GeoUtil.mirror(transformed, axis, pivot.getCenter());
      }

      @Override
      public void transformEntity(Entity entity) {
        final float newYaw = entity.mirror(switch (axis) {
          case X -> Mirror.FRONT_BACK;
          case Z -> Mirror.LEFT_RIGHT;
          default -> Mirror.NONE;
        });
        final float newPitch = axis == Direction.Axis.Y ? -entity.getXRot() : entity.getXRot();
        if (entity instanceof ServerPlayer serverPlayerEntity) {
          serverPlayerEntity.connection.teleport(entity.getX(), entity.getY(), entity.getZ(), newYaw, newPitch, RelativeMovement.ALL);
        } else {
          entity.setYRot(newYaw);
          if (axis == Direction.Axis.Y) {
            entity.setPosRaw(entity.getX(), entity.getY(), entity.getZ());
            entity.setXRot(newPitch);
            entity.setOldPosAndRot();
          }
        }
      }

      @Override
      public void transformEntityBack(Entity entity) {
        transformEntity(entity);
      }

      @Override
      public BlockState transformBlockState(BlockState original) {
        return FlipStateCallback.getMirroredState(original, axis);
      }

      @Override
      public Region transformRegion(Region region) {
        return region.mirrored(axis, pivot.getCenter());
      }

      @Override
      public void notifyCompletion(CommandSourceStack source, int affectedBlocks, int affectedEntities) {
        if (affectedEntities == -1) {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.mirror.complete", Integer.toString(affectedBlocks)).enhanced$$(), true);
        } else {
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.mirror.complete_with_entities", Integer.toString(affectedBlocks), Integer.toString(affectedEntities)).enhanced$$(), true);
        }
      }

      @Override
      public MutableComponent getIteratorTaskName(Region region) {
        return Component.translatable("enhanced_commands.commands.mirror.task", region.asString());
      }
    }.execute(region, keywordArgs, context);
  }
}
