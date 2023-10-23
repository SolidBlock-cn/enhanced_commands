package pers.solid.ecmd.function.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.SetBlocksCommand;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.NbtConvertible;
import pers.solid.ecmd.util.mixin.MixinSharedVariables;

/**
 * 方块函数，用于定义如何在世界的某个地方设置方块。它类似于原版中的 {@link BlockStateArgument} 以及 WorldEdit 中的方块蒙版（block mask）。方块函数不止定义方块，有可能是对方块本身进行修改，也有可能对方块实体进行修改。由于它是在已有方块的基础上进行修改的，故称为方块函数。
 */
public interface BlockFunction extends ExpressionConvertible, NbtConvertible, BlockFunctionArgument {
  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_function.cannotParse"));

  static @NotNull BlockFunction parse(CommandRegistryAccess commandRegistryAccess, String s, ServerCommandSource source) throws CommandSyntaxException {
    return BlockFunctionArgument.parse(commandRegistryAccess, new SuggestedParser(s), false).apply(source);
  }

  default boolean setBlock(World world, BlockPos pos, int flags, int modFlags) {
    final BlockState origState = world.getBlockState(pos);
    MutableObject<NbtCompound> blockEntityData = new MutableObject<>(null);
    BlockState modifiedState = getModifiedState(origState, origState, world, pos, flags, blockEntityData);
    if ((modFlags & SetBlocksCommand.POST_PROCESS_FLAG) != 0) {
      modifiedState = Block.postProcessState(modifiedState, world, pos);
    }
    final boolean suppressInitialCheck = (modFlags & SetBlocksCommand.SUPPRESS_INITIAL_CHECK_FLAG) != 0;
    final boolean suppressReplacedCheck = (modFlags & SetBlocksCommand.SUPPRESS_REPLACED_CHECK_FLAG) != 0;
    if (suppressInitialCheck) MixinSharedVariables.suppressOnBlockAdded = true;
    if (suppressReplacedCheck) MixinSharedVariables.suppressOnStateReplaced = true;
    boolean result;
    try {
      result = world.setBlockState(pos, modifiedState, flags);
    } finally {
      if (suppressInitialCheck) MixinSharedVariables.suppressOnBlockAdded = false;
      if (suppressReplacedCheck) MixinSharedVariables.suppressOnStateReplaced = false;
    }
    final BlockEntity blockEntity = world.getBlockEntity(pos);
    if (blockEntity != null) {
      final NbtCompound modifiedData = blockEntityData.getValue();
      if (modifiedData != null) {
        blockEntity.readNbt(modifiedData);
        result = true;
      }
    }
    return result;
  }

  /**
   * 对已有的方块状态进行修改。如果此方块函数不修改方块状态，应该返回 blockState 参数。
   *
   * @param blockState      当前的一系列修改过程中所使用的方块状态。当不同的多个方块函数依次使用时，方块函数的返回值会用于这个参数。
   * @param origState       在整个修改过程之前，所使用的方块状态。当不同的多个方块函数依次使用时，此参数均不改变。
   * @param world           当前所在的世界。
   * @param pos             正在修改的方块所在的坐标。
   * @param flags           正在修改的方块修改时的 flags。
   * @param blockEntityData 此参数用于在修改方块的过程中一并修改方块实体。在完成对方块状态的修改后，才会将这个数据并入到方块实体中。
   * @return 修改后的方块状态。
   */
  @Contract(mutates = "param6")
  @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData);

  void writeNbt(@NotNull NbtCompound nbtCompound);

  @NotNull BlockFunctionType<?> getType();

  @Override
  default NbtCompound createNbt() {
    NbtCompound nbtCompound = new NbtCompound();
    final BlockFunctionType<?> type = getType();
    final Identifier id = BlockFunctionType.REGISTRY.getId(type);
    nbtCompound.putString("type", Preconditions.checkNotNull(id, "Unknown block function type: %s", type).toString());
    writeNbt(nbtCompound);
    return nbtCompound;
  }

  static @NotNull BlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
    final BlockFunctionType<?> type = BlockFunctionType.REGISTRY.get(new Identifier(nbtCompound.getString("type")));
    Preconditions.checkNotNull(type, "Unknown block function type: %s", type);
    return type.fromNbt(nbtCompound, world);
  }

  @Override
  default BlockFunction apply(ServerCommandSource source) {
    return this;
  }
}
