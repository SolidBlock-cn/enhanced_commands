package pers.solid.ecmd.function.block;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;

public enum EmptyBlockFunction implements BlockFunction, BlockFunctionType<EmptyBlockFunction> {
  INSTANCE;
  public static final Codec<EmptyBlockFunction> CODEC = Codec.unit(INSTANCE);

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    return blockState;
  }

  @Override
  public @NotNull BlockFunctionType<?> getType() {
    return this;
  }

  @Override
  public @NotNull Codec<EmptyBlockFunction> getCodec() {
    return CODEC;
  }

  @Override
  public @NotNull String asString() {
    return "~";
  }
}
