package pers.solid.ecmd.function.block;

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
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SimpleBlockFunctionParser;
import pers.solid.ecmd.function.property.PropertyFunction;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record SimpleBlockFunction(@NotNull Block block, @NotNull List<PropertyFunction<?>> properties) implements BlockFunction {
  public static final Codec<SimpleBlockFunction> STRING_BASED_CODEC = BuiltInRegistries.BLOCK.byNameCodec().flatComapMap(block -> new SimpleBlockFunction(block, ImmutableList.of()), simpleBlockFunction -> simpleBlockFunction.properties.isEmpty() ? DataResult.success(simpleBlockFunction.block) : DataResult.error(() -> "cannot serialize function with properties to strings"));

  public static final MapCodec<SimpleBlockFunction> CODEC = BuiltInRegistries.BLOCK.byNameCodec().dispatchMap("block", SimpleBlockFunction::block, block -> RecordCodecBuilder.mapCodec(i -> i.ap(properties -> new SimpleBlockFunction(block, properties), CodecUtil.optionalField("properties", PropertyFunction.getCodec(block).listOf(), Collections.emptyList()).forGetter(SimpleBlockFunction::properties))));

  public SimpleBlockFunction(@NotNull Block block) {
    this(block, Collections.emptyList());
  }

  @Override
  public @NotNull String asString() {
    final StringBuilder stringBuilder = new StringBuilder(BuiltInRegistries.BLOCK.getKey(block).toString());
    if (!properties.isEmpty()) {
      stringBuilder.append('[');
      stringBuilder.append(properties.stream().map(PropertyFunction::asString).collect(Collectors.joining(", ")));
      stringBuilder.append(']');
    }
    return stringBuilder.toString();
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    BlockState stateToPlace = block.defaultBlockState();
    final RandomSource random = context.getSplitter(this).at(pos);
    for (PropertyFunction<?> propertyFunction : properties) {
      stateToPlace = propertyFunction.getModifiedState(stateToPlace, originalState, random);
    }
    return stateToPlace;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.SIMPLE;
  }

  public enum Type implements BlockFunctionType<SimpleBlockFunction>, Parser<BlockFunction> {
    SIMPLE_TYPE;

    @Override
    public @NotNull MapCodec<SimpleBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull SimpleBlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      SimpleBlockFunctionParser<?> parser = new SimpleBlockFunctionParser<>(parseContext);
      parser.parseBlockId();
      parser.parseProperties();
      return new SimpleBlockFunction(parser.block, parser.propertyFunctions);
    }
  }
}
