package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.NbtPredicate;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.argument.NbtFunctionArgumentType;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.mixin.FallingBlockEntityAccessor;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.MixinSharedVariables;

import java.util.function.Function;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum ConvertBlockCommand implements CommandRegistrationCallback {
  INSTANCE;

  final static KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("skip_light_update", BoolArgumentType.bool(), false)
      .addOptionalArg("notify_listeners", BoolArgumentType.bool(), true)
      .addOptionalArg("notify_neighbors", BoolArgumentType.bool(), false)
      .addOptionalArg("force_state", BoolArgumentType.bool(), true)
      .addOptionalArg("suppress_initial_check", BoolArgumentType.bool(), false)
      .addOptionalArg("suppress_replaced_check", BoolArgumentType.bool(), false)
      .addOptionalArg("force", BoolArgumentType.bool(), false)
      .addOptionalArg("nbt", NbtFunctionArgumentType.COMPOUND, null)
      .addOptionalArg("affect_fluid", BoolArgumentType.bool(), false)
      .build();

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {

    final Function<BlockPos, Text> fallingBlockFeedback = blockPos -> Text.translatable("enhanced_commands.commands.convertblock.falling_block.complete", TextUtil.wrapVector(blockPos));
    final Function<BlockPos, Text> blockDisplayFeedback = blockPos -> Text.translatable("enhanced_commands.commands.convertblock.block_display.complete", TextUtil.wrapVector(blockPos));
    dispatcher.register(literalR2("convertblock")
        .then(argument("pos", EnhancedPosArgumentType.blockPos())
            .then(literal("falling_block")
                .executes(context -> executeConvert(ConvertBlockCommand::convertToFallingBlock, fallingBlockFeedback, EnhancedPosArgumentType.getLoadedBlockPos(context, "pos"), KEYWORD_ARGS.defaultArgs(), context))
                .then(argument("keyword_args", KEYWORD_ARGS)
                    .executes(context -> executeConvert(ConvertBlockCommand::convertToFallingBlock, fallingBlockFeedback, EnhancedPosArgumentType.getLoadedBlockPos(context, "pos"), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context))))
            .then(literal("block_display")
                .executes(context -> executeConvert(ConvertBlockCommand::convertToBlockDisplay, blockDisplayFeedback, EnhancedPosArgumentType.getLoadedBlockPos(context, "pos"), KEYWORD_ARGS.defaultArgs(), context))
                .then(argument("keyword_args", KEYWORD_ARGS)
                    .executes(context -> executeConvert(ConvertBlockCommand::convertToBlockDisplay, blockDisplayFeedback, EnhancedPosArgumentType.getLoadedBlockPos(context, "pos"), KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"), context))))));
  }

  public static int executeConvert(Conversion conversion, Function<BlockPos, Text> feedback, BlockPos blockPos, KeywordArgs keywordArgs, CommandContext<ServerCommandSource> context) {
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    final CompoundNbtFunction nbtFunction = keywordArgs.getArg("nbt");

    final Entity entity = conversion.getConvertedEntity(world, blockPos, FillReplaceCommand.getFlags(keywordArgs), FillReplaceCommand.getModFlags(keywordArgs), keywordArgs.getBoolean("affect_fluid"));
    if (entity == null) {
      return 0;
    }
    if (nbtFunction != null) {
      final NbtCompound apply = nbtFunction.apply(NbtPredicate.entityToNbt(entity));
      entity.readNbt(apply);
    }
    CommandBridge.sendFeedback(source, () -> feedback.apply(blockPos), false);
    return 1;
  }

  public static FallingBlockEntity convertToFallingBlock(World world, BlockPos pos, int flags, int modFlags, boolean affectFluid) {
    BlockState state = world.getBlockState(pos);
    MixinSharedVariables.setBlockStateWithModFlags(world, pos, affectFluid ? Blocks.AIR.getDefaultState() : state.getFluidState().getBlockState(), flags, modFlags);
    if (!affectFluid) state = state.withIfExists(Properties.WATERLOGGED, false);
    FallingBlockEntity fallingBlockEntity = new FallingBlockEntity(EntityType.FALLING_BLOCK, world);
    fallingBlockEntity.setPosition(Vec3d.ofBottomCenter(pos));
    fallingBlockEntity.setFallingBlockPos(pos);
    ((FallingBlockEntityAccessor) fallingBlockEntity).setBlock(state);
    world.spawnEntity(fallingBlockEntity);
    return fallingBlockEntity;
  }

  public static DisplayEntity.BlockDisplayEntity convertToBlockDisplay(World world, BlockPos pos, int flags, int modFlags, boolean affectFluid) {
    BlockState state = world.getBlockState(pos);
    MixinSharedVariables.setBlockStateWithModFlags(world, pos, affectFluid ? Blocks.AIR.getDefaultState() : state.getFluidState().getBlockState(), flags, modFlags);
    if (!affectFluid) state = state.withIfExists(Properties.WATERLOGGED, false);
    final DisplayEntity.BlockDisplayEntity blockDisplayEntity = EntityType.BLOCK_DISPLAY.create(world);
    if (blockDisplayEntity == null) {
      return null;
    }
    blockDisplayEntity.setPosition(Vec3d.of(pos));
    blockDisplayEntity.setBlockState(state);
    world.spawnEntity(blockDisplayEntity);
    return blockDisplayEntity;
  }

  @FunctionalInterface
  public interface Conversion {
    Entity getConvertedEntity(World world, BlockPos pos, int flags, int modFlags, boolean affectFluid);
  }
}
