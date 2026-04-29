package pers.solid.ecmd.block.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.property.function.PropertyFunction;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record SimpleBlockFunction(Block block, List<PropertyFunction<?>> properties) implements BlockFunction {
  public static final Codec<SimpleBlockFunction> STRING_BASED_CODEC = BuiltInRegistries.BLOCK.byNameCodec().flatComapMap(block -> new SimpleBlockFunction(block, ImmutableList.of()), simpleBlockFunction -> simpleBlockFunction.properties.isEmpty() ? DataResult.success(simpleBlockFunction.block) : DataResult.error(() -> "cannot serialize function with properties to strings"));

  public static final MapCodec<SimpleBlockFunction> CODEC = BuiltInRegistries.BLOCK.byNameCodec().dispatchMap("block", SimpleBlockFunction::block, block -> RecordCodecBuilder.mapCodec(i -> i.ap(properties -> new SimpleBlockFunction(block, properties), CodecUtil.optionalField("properties", PropertyFunction.getCodec(block).listOf(), Collections.emptyList()).forGetter(SimpleBlockFunction::properties))));

  public SimpleBlockFunction(Block block) {
    this(block, Collections.emptyList());
  }

  @Override
  public String expressAsString() {
    final StringBuilder stringBuilder = new StringBuilder(BuiltInRegistries.BLOCK.getKey(block).toString());
    if (!properties.isEmpty()) {
      stringBuilder.append('[');
      stringBuilder.append(properties.stream().map(PropertyFunction::expressAsString).collect(Collectors.joining(", ")));
      stringBuilder.append(']');
    }
    return stringBuilder.toString();
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, @UnknownNullability MutableObject<@Nullable CompoundTag> blockEntityData, BlockFunctionContext context) throws CommandSyntaxException {
    BlockState stateToPlace = block.defaultBlockState();
    final RandomSource random = context.getSplitter(this).at(pos);
    for (PropertyFunction<?> propertyFunction : properties) {
      stateToPlace = propertyFunction.getModifiedState(stateToPlace, originalState, random);
    }
    return stateToPlace;
  }

  @Override
  public BlockFunctionType<SimpleBlockFunction> getType() {
    return BlockFunctionTypes.SIMPLE;
  }

  public enum SimpleParser implements Parser<BlockFunction> {
    INSTANCE;

    @Override
    public BlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      SimpleBlockFunctionParser<?> parser = new SimpleBlockFunctionParser<>(parseContext);
      parser.parseBlockId();
      parser.parseProperties();
      Objects.requireNonNull(parser.block, "block");
      return new SimpleBlockFunction(parser.block, parser.propertyFunctions);
    }
  }
}
