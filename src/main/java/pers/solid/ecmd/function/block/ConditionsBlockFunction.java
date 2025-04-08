package pers.solid.ecmd.function.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.block.BlockPredicateArgument;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.parse.FunctionLikeParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record ConditionsBlockFunction(@NotNull List<ConditionalBlockFunction> conditions) implements BlockFunction {
  public static final MapCodec<ConditionsBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Codecs.nonEmptyList(ConditionalBlockFunction.CODEC.codec().listOf()).fieldOf("conditions").forGetter(ConditionsBlockFunction::conditions)).apply(i, ConditionsBlockFunction::new));

  public ConditionsBlockFunction(@NotNull ConditionalBlockFunction... conditions) {
    this(List.of(conditions));
  }

  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, World world, BlockPos pos, MutableObject<NbtCompound> blockEntityData, BlockFunctionContext context) {
    final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(world, pos, false);
    for (ConditionalBlockFunction function : conditions) {
      if (function.condition().test(cachedBlockPosition, context)) {
        return function.functionIfTrue().getModifiedState(blockState, origState, world, pos, blockEntityData, context);
      }
    }
    if (!conditions.isEmpty()) {
      return conditions.getLast().getModifiedState(blockState, origState, world, pos, blockEntityData, context);
    } else {
      return origState;
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

  public static class Parser implements FunctionLikeParser<BlockFunctionArgument> {
    private final List<FailableFunction<ServerCommandSource, BlockFunction, CommandSyntaxException>> functions = new ArrayList<>();

    @Override
    public BlockFunctionArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) throws CommandSyntaxException {
      return source -> new ConditionsBlockFunction(IterateUtils.transformFailableImmutableList(functions, function -> (ConditionalBlockFunction) function.apply(source)));
    }

    @Override
    public void parseWithinParenthesis(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
      parser.reader.skipWhitespace();

      parser.addSuggestion((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest(rightParString());
        }
        return suggestionsBuilder.buildFuture();
      });
      if (parser.reader.canRead() && parser.reader.peek() == rightPar()) {
        return;
      }
      while (true) {
        parser.clearSuggestion();
        BlockPredicateArgument predicate = BlockPredicateArgument.parse(registryAccess, parser, suggestionsOnly);
        parser.reader.skipWhitespace();
        parser.addSuggestion((context, builder) -> {
          if (builder.getRemaining().isEmpty()) {
            builder.suggest(",");
          }
          return builder.buildFuture();
        });
        parser.reader.expect(',');
        parser.reader.skipWhitespace();
        parser.clearSuggestion();
        parser.reader.skipWhitespace();

        BlockFunctionArgument functionIfTrue = BlockFunctionArgument.parse(registryAccess, parser, suggestionsOnly);
        parser.reader.skipWhitespace();
        parser.addSuggestion((context, builder) -> {
          if (builder.getRemaining().isEmpty()) {
            builder.suggest(",").suggest(";");
          }
          return builder.buildFuture();
        });
        if (parser.reader.canRead() && parser.reader.peek() == ',') {
          parser.reader.skip();
          parser.reader.skipWhitespace();
          parser.clearSuggestion();
          BlockFunctionArgument functionIfFalse = BlockFunctionArgument.parse(registryAccess, parser, suggestionsOnly);

          functions.add(source -> new ConditionalBlockFunction(predicate.apply(source), functionIfTrue.apply(source), functionIfFalse.apply(source)));
        } else {
          functions.add(source -> new ConditionalBlockFunction(predicate.apply(source), functionIfTrue.apply(source)));
        }
        parser.addSuggestion((context, builder) -> {
          if (builder.getRemaining().isEmpty()) {
            builder.suggest(";").buildFuture();
          }
          return builder.buildFuture();
        });

        if (parser.reader.canRead()) {
          final char peek = parser.reader.peek();
          if (peek == ';') {
            parser.reader.skip();
            parser.reader.skipWhitespace();
            parser.clearSuggestion();
            functions.add(source -> new ConditionalBlockFunction(predicate.apply(source), functionIfTrue.apply(source)));
          } else {
            break;
          }
        } else {
          throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.createWithContext(parser.reader, ",", ";");
        }
      }
    }
  }
}
