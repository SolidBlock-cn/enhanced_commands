package pers.solid.ecmd.function.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

public enum EmptyBlockFunction implements BlockFunction, BlockFunctionType<EmptyBlockFunction> {
  INSTANCE;
  public static final MapCodec<EmptyBlockFunction> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    return blockState;
  }

  @Override
  public @NotNull EmptyBlockFunction getType() {
    return BlockFunctionTypes.EMPTY;
  }

  @Override
  public @NotNull MapCodec<EmptyBlockFunction> getCodec() {
    return CODEC;
  }

  @Override
  public @NotNull String asString() {
    return "~";
  }
}
