package pers.solid.ecmd.block.function;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.property.function.PropertyNameFunction;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record PropertiesNbtCombinationBlockFunction(BlockFunction base, @Nullable PropertyNamesBlockFunction properties, @Nullable NbtBlockFunction nbt) implements BlockFunction {
  public static final MapCodec<PropertiesNbtCombinationBlockFunction> CODEC = RecordCodecBuilder.<Triple<BlockFunction, Optional<PropertyNamesBlockFunction>, Optional<NbtBlockFunction>>>mapCodec(i -> i.apply3(Triple::of,
      BlockFunction.CODEC.fieldOf("base").forGetter(Triple::getLeft),
      CodecUtil.optionalField("properties", PropertyNameFunction.CODEC.listOf()).xmap(o -> o.map(PropertyNamesBlockFunction::new), o -> o.map(PropertyNamesBlockFunction::functions)).forGetter(Triple::getMiddle),
      CodecUtil.optionalField("nbt", NbtFunction.CODEC).xmap(o -> o.map(NbtBlockFunction::new), o -> o.map(NbtBlockFunction::nbtFunction)).forGetter(Triple::getRight))).flatXmap(triple -> {
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
    if (base instanceof NbtBlockFunction) {
      throw new IllegalArgumentException("The base cannot be NbtFunction or PropertyNamesFunction");
    }
    if (base instanceof PropertyNamesBlockFunction && properties != null) {
      throw new IllegalArgumentException("The properties must be null when the base is instance of PropertyNamesFunction");
    }
  }

  @Override
  public String expressAsString() {
    return Stream.of(base, properties, nbt).filter(Objects::nonNull).map(BlockFunction::expressAsString).collect(Collectors.joining());
  }

  @Override
  public BlockFunctionType<PropertiesNbtCombinationBlockFunction> getType() {
    return BlockFunctionTypes.PROPERTIES_NBT_COMBINATION;
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) {
    blockState = base.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    if (properties != null) {
      blockState = properties.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    }
    if (nbt != null) {
      blockState = nbt.getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    }
    return blockState;
  }
}
