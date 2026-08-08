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
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.block.SimpleBlockParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.property.function.PropertyNameFunction;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.List;
import java.util.stream.Collectors;

public record PropertyNamesBlockFunction(List<PropertyNameFunction> functions) implements BlockFunction, DoesNotRequireValidation {
  public static final MapCodec<PropertyNamesBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.ap(PropertyNamesBlockFunction::new, PropertyNameFunction.CODEC.listOf().fieldOf("properties").forGetter(PropertyNamesBlockFunction::functions)));

  public PropertyNamesBlockFunction(PropertyNameFunction functions) {
    this(List.of(functions));
  }

  public PropertyNamesBlockFunction(PropertyNameFunction... functions) {
    this(List.of(functions));
  }

  @Override
  public String expressAsString() {
    return "[" + functions.stream().map(PropertyNameFunction::expressAsString).collect(Collectors.joining(",")) + "]";
  }

  @Override
  public BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<@Nullable CompoundTag> blockEntityData, ExecutionContext context) throws CommandSyntaxException {
    final RandomSource random = context.getSplitter(this).at(pos);
    for (PropertyNameFunction propertyNameFunction : functions) {
      blockState = propertyNameFunction.getModifiedState(originalState, blockState, random);
    }
    return blockState;
  }

  @Override
  public BlockFunctionType<PropertyNamesBlockFunction> getType() {
    return BlockFunctionTypes.PROPERTY_NAMES;
  }

  public enum PropertyNamesParser implements Parser<PropertyNamesBlockFunction> {
    INSTANCE;

    @Override
    public @Nullable PropertyNamesBlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
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
