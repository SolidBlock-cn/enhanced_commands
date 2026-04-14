package pers.solid.ecmd.block.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;

public enum EmptyBlockFunction implements BlockFunction, BlockFunctionType<EmptyBlockFunction> {
  INSTANCE;
  public static final MapCodec<EmptyBlockFunction> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    return blockState;
  }

  @Override
  public EmptyBlockFunction getType() {
    return BlockFunctionTypes.EMPTY;
  }

  @Override
  public MapCodec<EmptyBlockFunction> getCodec() {
    return CODEC;
  }

  @Override
  public String asString() {
    return "~";
  }
}
