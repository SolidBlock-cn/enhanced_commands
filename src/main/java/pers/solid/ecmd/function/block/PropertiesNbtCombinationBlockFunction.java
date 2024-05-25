package pers.solid.ecmd.function.block;

import com.google.common.base.Functions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record PropertiesNbtCombinationBlockFunction(@NotNull BlockFunction base, @Nullable PropertyNamesBlockFunction properties, @Nullable NbtBlockFunction nbt) implements BlockFunction {
  public static final Codec<PropertiesNbtCombinationBlockFunction> CODEC = RecordCodecBuilder.create(i -> i.apply3((b, p, n) -> new PropertiesNbtCombinationBlockFunction(b, p.orElse(null), n.orElse(null)),
      BlockFunction.CODEC.fieldOf("base").forGetter(PropertiesNbtCombinationBlockFunction::base),
      PropertyNameFunction.CODEC.listOf().optionalFieldOf("properties").xmap(o -> o.map(PropertyNamesBlockFunction::new), o -> o.map(PropertyNamesBlockFunction::propertyNameFunctions)).forGetter(Functions.compose(Optional::ofNullable, PropertiesNbtCombinationBlockFunction::properties)),
      CompoundNbtFunction.CODEC.optionalFieldOf("nbt").xmap(o -> o.map(NbtBlockFunction::new), o -> o.map(NbtBlockFunction::nbtFunction)).forGetter(Functions.compose(Optional::ofNullable, PropertiesNbtCombinationBlockFunction::nbt))));

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
    public @NotNull Codec<PropertiesNbtCombinationBlockFunction> getCodec() {
      return CODEC;
    }
  }
}
