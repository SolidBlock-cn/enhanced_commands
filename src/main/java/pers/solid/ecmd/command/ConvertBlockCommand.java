package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgument;
import pers.solid.ecmd.argument.KeywordArgsCommon;
import pers.solid.ecmd.mixins.accessor.BlockDisplayEntityAccessor;
import pers.solid.ecmd.mixins.accessor.FallingBlockEntityAccessor;
import pers.solid.ecmd.nbt.function.CompoundNbtFunction;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.function.Function;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum ConvertBlockCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;


  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final KeywordArgsArgument keywordArgs = KeywordArgsArgument.builderFromShared(KeywordArgsCommon.CONVERT_BLOCKS, commandBuildContext).build();

    final Function<BlockPos, Component> fallingBlockFeedback = blockPos -> Component.translatable("enhanced_commands.commands.convertblock.falling_block.complete", TextUtil.wrapVector(blockPos));
    final Function<BlockPos, Component> blockDisplayFeedback = blockPos -> Component.translatable("enhanced_commands.commands.convertblock.block_display.complete", TextUtil.wrapVector(blockPos));
    dispatcher.register(literalR2("convertblock")
        .then(argument("pos", EnhancedPosArgument.blockPos())
            .then(literal("falling_block")
                .executes(context -> executeConvert(ConvertBlockCommand::convertToFallingBlock, fallingBlockFeedback, EnhancedPosArgument.getLoadedBlockPos(context, "pos"), keywordArgs.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgs)
                    .executes(context -> executeConvert(ConvertBlockCommand::convertToFallingBlock, fallingBlockFeedback, EnhancedPosArgument.getLoadedBlockPos(context, "pos"), KeywordArgsArgument.getKeywordArgs(context, "keyword_args"), context))))
            .then(literal("block_display")
                .executes(context -> executeConvert(ConvertBlockCommand::convertToBlockDisplay, blockDisplayFeedback, EnhancedPosArgument.getLoadedBlockPos(context, "pos"), keywordArgs.defaultArgs(), context))
                .then(argument("keyword_args", keywordArgs)
                    .executes(context -> executeConvert(ConvertBlockCommand::convertToBlockDisplay, blockDisplayFeedback, EnhancedPosArgument.getLoadedBlockPos(context, "pos"), KeywordArgsArgument.getKeywordArgs(context, "keyword_args"), context))))));
  }

  public static int executeConvert(Conversion conversion, Function<BlockPos, Component> feedback, BlockPos blockPos, KeywordArgs keywordArgs, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final ServerLevel world = source.getLevel();
    final CompoundNbtFunction nbtFunction = keywordArgs.getArg("nbt");

    final Entity entity = conversion.getConvertedEntity(world, blockPos, FillReplaceCommand.getFlags(keywordArgs), FillReplaceCommand.getModFlags(keywordArgs), keywordArgs.getBoolean("affect_fluid"));
    if (entity == null) {
      return 0;
    }
    if (nbtFunction != null) {
      final CompoundTag apply = nbtFunction.apply(NbtPredicate.getEntityTagToCompare(entity), new ExecutionContext(world.getRandom(), source, null));
      entity.load(apply);
    }
    source.sendFeedback$ecBridge(() -> feedback.apply(blockPos), false);
    return 1;
  }

  public static FallingBlockEntity convertToFallingBlock(Level world, BlockPos pos, int flags, int modFlags, boolean affectFluid) {
    BlockState state = world.getBlockState(pos);
    MixinShared.setBlockStateWithModFlags(world, pos, affectFluid ? Blocks.AIR.defaultBlockState() : state.getFluidState().createLegacyBlock(), flags, modFlags);
    if (!affectFluid) state = state.trySetValue(BlockStateProperties.WATERLOGGED, false);
    FallingBlockEntity fallingBlockEntity = new FallingBlockEntity(EntityType.FALLING_BLOCK, world);
    fallingBlockEntity.setPos(Vec3.atBottomCenterOf(pos));
    fallingBlockEntity.setStartPos(pos);
    ((FallingBlockEntityAccessor) fallingBlockEntity).setBlockState(state);
    world.addFreshEntity(fallingBlockEntity);
    return fallingBlockEntity;
  }

  public static Display.BlockDisplay convertToBlockDisplay(Level world, BlockPos pos, int flags, int modFlags, boolean affectFluid) {
    BlockState state = world.getBlockState(pos);
    MixinShared.setBlockStateWithModFlags(world, pos, affectFluid ? Blocks.AIR.defaultBlockState() : state.getFluidState().createLegacyBlock(), flags, modFlags);
    if (!affectFluid) state = state.trySetValue(BlockStateProperties.WATERLOGGED, false);
    final Display.BlockDisplay blockDisplayEntity = EntityType.BLOCK_DISPLAY.create(world);
    if (blockDisplayEntity == null) {
      return null;
    }
    blockDisplayEntity.setPos(Vec3.atLowerCornerOf(pos));
    ((BlockDisplayEntityAccessor) blockDisplayEntity).callSetBlockState(state);
    world.addFreshEntity(blockDisplayEntity);
    return blockDisplayEntity;
  }

  @FunctionalInterface
  public interface Conversion {
    Entity getConvertedEntity(Level world, BlockPos pos, int flags, int modFlags, boolean affectFluid);
  }
}
