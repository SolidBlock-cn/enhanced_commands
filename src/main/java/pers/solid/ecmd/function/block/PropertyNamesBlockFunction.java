package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SimpleBlockFunctionParser;
import pers.solid.ecmd.argument.SimpleBlockParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

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
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final Random random = context.getSplitter(this).split(pos);
    for (PropertyNameFunction propertyNameFunction : functions) {
      blockState = propertyNameFunction.getModifiedState(origState, blockState, random);
    }
    return blockState;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.PROPERTY_NAMES;
  }

  public enum Type implements BlockFunctionType<PropertyNamesBlockFunction>, Parser<BlockFunctionArgument> {
    PROPERTY_NAMES_TYPE;

    @Override
    public @NotNull MapCodec<PropertyNamesBlockFunction> getCodec() {
      return CODEC;
    }

    @Override
    public @Nullable BlockFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      final SuggestedParser<?> parser = parseContext.parser();
      parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("[", SimpleBlockParser.START_OF_PROPERTIES, suggestionsBuilder).buildFuture());
      if (parser.reader.canRead() && parser.reader.peek() == '[') {
        final SimpleBlockFunctionParser<?> suggestedParser = new SimpleBlockFunctionParser<>(parseContext);
        suggestedParser.parsePropertyNames();
        return new PropertyNamesBlockFunction(suggestedParser.propertyNameFunctions);
      } else {
        return null;
      }
    }
  }
}
