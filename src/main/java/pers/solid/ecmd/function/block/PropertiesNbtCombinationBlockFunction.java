package pers.solid.ecmd.function.block;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.nbt.NbtFunction;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record PropertiesNbtCombinationBlockFunction(@NotNull BlockFunction firstBlockFunction, @Nullable PropertyNamesBlockFunction propertyNamesFunction, @Nullable NbtBlockFunction nbtBlockFunction) implements BlockFunction {
  @Contract(value = "_, null, null -> fail", pure = true)
  public PropertiesNbtCombinationBlockFunction {
    if (propertyNamesFunction == null && nbtBlockFunction == null) {
      throw new IllegalArgumentException("The property names and nbt predicate cannot be both null. In that case, directly use the first block predicate.");
    }
    if (firstBlockFunction instanceof NbtFunction) {
      throw new IllegalArgumentException("The firstBlockFunction cannot be NbtFunction or PropertyNamesFunction");
    }
    if (firstBlockFunction instanceof PropertyNamesBlockFunction && propertyNamesFunction != null) {
      throw new IllegalArgumentException("The propertyNamesFunction must be null when the firstBlockFunction is instance of PropertyNamesFunction");
    }
  }

  @Override
  public @NotNull String asString() {
    return Stream.of(firstBlockFunction, propertyNamesFunction, nbtBlockFunction).filter(Objects::nonNull).map(BlockFunction::asString).collect(Collectors.joining());
  }


  @Override
  public @NotNull BlockFunctionType<PropertiesNbtCombinationBlockFunction> getType() {
    return BlockFunctionTypes.PROPERTIES_NBT_COMBINATION;
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    blockState = firstBlockFunction.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    if (propertyNamesFunction != null) {
      blockState = propertyNamesFunction.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }
    if (nbtBlockFunction != null) {
      blockState = nbtBlockFunction.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }
    return blockState;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.put("first", firstBlockFunction.createNbt());
    if (propertyNamesFunction != null) {
      nbtCompound.put("properties", propertyNamesFunction.createNbt());
    }
    if (nbtBlockFunction != null) {
      nbtCompound.put("nbt", nbtBlockFunction.createNbt());
    }
  }

  public enum Type implements BlockFunctionType<PropertiesNbtCombinationBlockFunction> {
    PROPERTIES_NBT_COMBINATION_TYPE;

    @Override
    public @NotNull PropertiesNbtCombinationBlockFunction fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new PropertiesNbtCombinationBlockFunction(
          BlockFunction.fromNbt(nbtCompound.getCompound("first"), world),
          nbtCompound.contains("properties", NbtElement.COMPOUND_TYPE) ? PropertyNamesBlockFunction.Type.PROPERTY_NAMES_TYPE.fromNbt(nbtCompound.getCompound("properties"), world) : null,
          nbtCompound.contains("nbt", NbtElement.COMPOUND_TYPE) ? NbtBlockFunction.Type.NBT_TYPE.fromNbt(nbtCompound.getCompound("nbt"), world) : null
      );
    }
  }
}
