package pers.solid.ecmd.function.block;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record PropertiesNbtCombinationBlockFunction(@NotNull BlockFunction base, @Nullable PropertyNamesBlockFunction properties, @Nullable NbtBlockFunction nbt) implements BlockFunction {
  public static final MapCodec<PropertiesNbtCombinationBlockFunction> CODEC = RecordCodecBuilder.<Triple<BlockFunction, Optional<PropertyNamesBlockFunction>, Optional<NbtBlockFunction>>>mapCodec(i -> i.apply3(Triple::of,
      BlockFunction.CODEC.fieldOf("base").forGetter(Triple::getLeft),
      CodecUtil.optionalField("properties", PropertyNameFunction.CODEC.listOf()).xmap(o -> o.map(PropertyNamesBlockFunction::new), o -> o.map(PropertyNamesBlockFunction::functions)).forGetter(Triple::getMiddle),
      CodecUtil.optionalField("nbt", CompoundNbtFunction.CODEC).xmap(o -> o.map(NbtBlockFunction::new), o -> o.map(NbtBlockFunction::nbtFunction)).forGetter(Triple::getRight))).flatXmap(triple -> {
    try {
      return DataResult.success(new PropertiesNbtCombinationBlockFunction(triple.getLeft(), triple.getMiddle().orElse(null), triple.getRight().orElse(null)));
    } catch (IllegalArgumentException e) {
      return DataResult.error(e::getMessage);
    }
  }, f -> DataResult.success(Triple.of(f.base, Optional.ofNullable(f.properties), Optional.ofNullable(f.nbt))));

  @Contract(value = "_, null, null -> fail", pure = true)
  public PropertiesNbtCombinationBlockFunction {
    if (properties == null && nbt == null) {
      throw new IllegalArgumentException("The property names and nbt predicate cannot be both null. In that case, directly use the first block predicate.");
    }
    if (base instanceof NbtFunction) {
      throw new IllegalArgumentException("The base cannot be NbtFunction or PropertyNamesFunction");
    }
    if (base instanceof PropertyNamesBlockFunction && properties != null) {
      throw new IllegalArgumentException("The properties must be null when the base is instance of PropertyNamesFunction");
    }
  }

  @Override
  public @NotNull String asString() {
    return Stream.of(base, properties, nbt).filter(Objects::nonNull).map(BlockFunction::asString).collect(Collectors.joining());
  }

  @Override
  public @NotNull BlockFunctionType<PropertiesNbtCombinationBlockFunction> getType() {
    return BlockFunctionTypes.PROPERTIES_NBT_COMBINATION;
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, int flags, MutableObject<NbtCompound> blockEntityData) {
    blockState = base.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    if (properties != null) {
      blockState = properties.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }
    if (nbt != null) {
      blockState = nbt.getModifiedState(blockState, origState, world, pos, flags, blockEntityData);
    }
    return blockState;
  }

  public enum Type implements BlockFunctionType<PropertiesNbtCombinationBlockFunction> {
    PROPERTIES_NBT_COMBINATION_TYPE;

    @Override
    public @NotNull MapCodec<PropertiesNbtCombinationBlockFunction> getCodec() {
      return CODEC;
    }
  }
}
