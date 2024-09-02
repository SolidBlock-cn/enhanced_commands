package pers.solid.ecmd.function.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

public enum EmptyBlockFunction implements BlockFunction, BlockFunctionType<EmptyBlockFunction> {
  INSTANCE;
  public static final MapCodec<EmptyBlockFunction> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
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
