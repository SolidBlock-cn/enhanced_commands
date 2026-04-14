package pers.solid.ecmd.block.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record ConditionsBlockFunction(@NotNull List<ConditionalBlockFunction> conditions) implements BlockFunction {
  public static final MapCodec<ConditionsBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ExtraCodecs.nonEmptyList(ConditionalBlockFunction.CODEC.codec().listOf()).fieldOf("conditions").forGetter(ConditionsBlockFunction::conditions)).apply(i, ConditionsBlockFunction::new));

  public ConditionsBlockFunction(@NotNull ConditionalBlockFunction... conditions) {
    this(List.of(conditions));
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState originalState, Level level, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    final BlockInWorld blockInWorld = new BlockInWorld(level, pos, false);
    for (ConditionalBlockFunction function : conditions) {
      if (function.condition().test(blockInWorld, context)) {
        return function.functionIfTrue().getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
      }
    }
    if (!conditions.isEmpty()) {
      return conditions.getLast().getModifiedState(blockState, originalState, level, pos, blockEntityData, context);
    } else {
      return originalState;
    }
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.CONDITIONS;
  }

  @Override
  public @NotNull String asString() {
    return conditions.stream().map(f -> f.condition().asString() + ", " + f.functionIfTrue().asString() + (f.functionIfFalse() == EmptyBlockFunction.INSTANCE ? "" : ", " + f.functionIfFalse().asString())).collect(Collectors.joining("; ", "ifs(", ")"));
  }

  public enum Type implements BlockFunctionType<ConditionsBlockFunction> {
    CONDITIONS_TYPE;

    @Override
    public @NotNull MapCodec<ConditionsBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionContentParser<ConditionsBlockFunction> {
    private final List<ConditionalBlockFunction> functions = new ArrayList<>();

    @Override
    public ConditionsBlockFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new ConditionsBlockFunction(functions);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      reader.skipWhitespace();

      parseContext.addSuggestion((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest(")");
        }
        return suggestionsBuilder.buildFuture();
      });
      if (reader.canRead() && reader.peek() == ')') {
        return;
      }
      while (true) {
        parseContext.clearSuggestion();
        BlockPredicate predicate = BlockPredicate.parse(parseContext);
        reader.skipWhitespace();
        parseContext.addSuggestion((context, builder) -> {
          if (builder.getRemaining().isEmpty()) {
            builder.suggest(",");
          }
          return builder.buildFuture();
        });
        reader.expect(',');
        reader.skipWhitespace();
        parseContext.clearSuggestion();
        reader.skipWhitespace();

        BlockFunction functionIfTrue = BlockFunction.parse(parseContext);
        reader.skipWhitespace();
        parseContext.addSuggestion((context, builder) -> {
          if (builder.getRemaining().isEmpty()) {
            builder.suggest(",").suggest(";");
          }
          return builder.buildFuture();
        });
        if (reader.canRead() && reader.peek() == ',') {
          reader.skip();
          reader.skipWhitespace();
          parseContext.clearSuggestion();
          BlockFunction functionIfFalse = BlockFunction.parse(parseContext);

          functions.add(new ConditionalBlockFunction(predicate, functionIfTrue, functionIfFalse));
        } else {
          functions.add(new ConditionalBlockFunction(predicate, functionIfTrue));
        }
        parseContext.addSuggestion((context, builder) -> {
          if (builder.getRemaining().isEmpty()) {
            builder.suggest(";").buildFuture();
          }
          return builder.buildFuture();
        });

        if (reader.canRead()) {
          final char peek = reader.peek();
          if (peek == ';') {
            reader.skip();
            reader.skipWhitespace();
            parseContext.clearSuggestion();
            functions.add(new ConditionalBlockFunction(predicate, functionIfTrue));
          } else {
            break;
          }
        } else {
          throw EnhancedCommandsCommandExceptionTypes.EXPECTED_2_SYMBOLS.createWithContext(reader, ",", ";");
        }
      }
    }
  }
}
