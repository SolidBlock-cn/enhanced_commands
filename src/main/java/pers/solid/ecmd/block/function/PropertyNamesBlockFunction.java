package pers.solid.ecmd.block.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.block.SimpleBlockParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.property.function.PropertyNameFunction;

import java.util.List;
import java.util.stream.Collectors;

public record PropertyNamesBlockFunction(@NotNull List<PropertyNameFunction> functions) implements BlockFunction {
  public static final MapCodec<PropertyNamesBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(PropertyNamesBlockFunction::new, PropertyNameFunction.CODEC.listOf().fieldOf("properties").forGetter(PropertyNamesBlockFunction::functions)));

  public PropertyNamesBlockFunction(@NotNull PropertyNameFunction... functions) {
    this(List.of(functions));
  }

  @Override
  public @NotNull String asString() {
    return "[" + functions.stream().map(PropertyNameFunction::asString).collect(Collectors.joining(",")) + "]";
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    final RandomSource random = context.getSplitter(this).at(pos);
    for (PropertyNameFunction propertyNameFunction : functions) {
      blockState = propertyNameFunction.getModifiedState(originalState, blockState, random);
    }
    return blockState;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.PROPERTY_NAMES;
  }

  public enum Type implements BlockFunctionType<PropertyNamesBlockFunction>, Parser<PropertyNamesBlockFunction> {
    PROPERTY_NAMES_TYPE;

    @Override
    public @NotNull MapCodec<PropertyNamesBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public PropertyNamesBlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("[", SimpleBlockParser.START_OF_PROPERTIES, suggestionsBuilder).buildFuture());
      final StringReader reader = parseContext.reader();
      if (reader.canRead() && reader.peek() == '[') {
        final SimpleBlockFunctionParser<?> suggestedParser = new SimpleBlockFunctionParser<>(parseContext);
        suggestedParser.parsePropertyNames();
        return new PropertyNamesBlockFunction(suggestedParser.propertyNameFunctions);
      } else {
        return null;
      }
    }
  }
}
