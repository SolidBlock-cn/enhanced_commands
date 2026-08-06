package pers.solid.ecmd.block.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

public enum EmptyBlockFunction implements BlockFunction, DoesNotRequireValidation {
  INSTANCE;
  public static final MapCodec<EmptyBlockFunction> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    return blockState;
  }

  @Override
  public BlockFunctionType<EmptyBlockFunction> getType() {
    return BlockFunctionTypes.EMPTY;
  }

  @Override
  public String expressAsString() {
    return "~";
  }
}
