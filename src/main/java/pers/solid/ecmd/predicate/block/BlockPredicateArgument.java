package pers.solid.ecmd.predicate.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtPredicateSuggestedParser;
import pers.solid.ecmd.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.ecmd.argument.SimpleBlockSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public interface BlockPredicateArgument extends Function<ServerCommandSource, BlockPredicate> {
  Text INTERSECT_TOOLTIP = Text.translatable("enhancedCommands.argument.block_predicate.intersect.symbol_tooltip");
  Text UNION_TOOLTIP = Text.translatable("enhancedCommands.argument.block_predicate.union.symbol_tooltip");

  static @NotNull BlockPredicateArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    return parse(commandRegistryAccess, parser, suggestionsOnly, true);
  }

  static @NotNull BlockPredicateArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return parseUnion(commandRegistryAccess, parser, suggestionsOnly, allowsSparse);
  }

  static @NotNull BlockPredicateArgument parseUnion(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return SuggestionUtil.parseUnifiable(() -> parseIntersect(commandRegistryAccess, parser, suggestionsOnly, allowsSparse), predicates -> source -> new UnionBlockPredicate(ImmutableList.copyOf(Lists.transform(predicates, input -> input.apply(source)))), "|", UNION_TOOLTIP, parser, allowsSparse);
  }

  static @NotNull BlockPredicateArgument parseIntersect(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return SuggestionUtil.parseUnifiable(() -> parseCombination(commandRegistryAccess, parser, suggestionsOnly), predicates -> source -> new IntersectBlockPredicate(ImmutableList.copyOf(Lists.transform(predicates, input -> input.apply(source)))), "&", INTERSECT_TOOLTIP, parser, allowsSparse);
  }

  static @NotNull BlockPredicateArgument parseCombination(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final BlockPredicateArgument parseUnit = parseUnit(commandRegistryAccess, parser, suggestionsOnly);
    if (parseUnit instanceof NbtPredicate) {
      return parseUnit;
    }
    List<PropertyNamePredicate> propertyNamePredicates;
    if (!(parseUnit instanceof PropertyNamesBlockPredicate) && parser.reader.canRead(0) && parser.reader.peek(-1) != ']') {
      // 当前面以“]”结尾时，说明已经在其他解析器中读取了属性，此时在这里不再读取任何属性
      // 尝试读取属性
      parser.suggestionProviders.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest("[", SimpleBlockSuggestedParser.START_OF_PROPERTIES);
        }
      });
      if (parser.reader.canRead() && parser.reader.peek() == '[') {
        final SimpleBlockPredicateSuggestedParser suggestedParser = new SimpleBlockPredicateSuggestedParser(commandRegistryAccess, parser);
        suggestedParser.parsePropertyNames();
        propertyNamePredicates = suggestedParser.propertyNamePredicates;
      } else propertyNamePredicates = null;
    } else propertyNamePredicates = null;
    NbtPredicate nbtPredicate;
    parser.suggestionProviders.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("{", NbtPredicateSuggestedParser.START_OF_COMPOUND);
      }
    });
    if (parser.reader.canRead() && parser.reader.peek() == '{') {
      // 尝试读取 NBT
      nbtPredicate = new NbtPredicateSuggestedParser(parser.reader, parser.suggestionProviders).parseCompound(false, false);
    } else nbtPredicate = null;
    if (propertyNamePredicates != null || nbtPredicate != null) {
      return source -> new PropertiesNbtCombinationBlockPredicate(parseUnit.apply(source), propertyNamePredicates == null ? null : new PropertyNamesBlockPredicate(propertyNamePredicates), nbtPredicate == null ? null : new NbtBlockPredicate(nbtPredicate));
    }
    return parseUnit;
  }

  @NotNull
  static BlockPredicateArgument parseUnit(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final int cursorOnStart = parser.reader.getCursor();
    final Stream<BlockPredicateType<?>> stream = commandRegistryAccess.createWrapper(BlockPredicateType.REGISTRY_KEY).streamEntries().map(RegistryEntry.Reference::value);
    Iterable<BlockPredicateType<?>> iterable = Iterables.concat(stream.filter(type -> type != BlockPredicateTypes.SIMPLE)::iterator, Collections.singleton(BlockPredicateTypes.SIMPLE));
    for (BlockPredicateType<?> type : iterable) {
      parser.reader.setCursor(cursorOnStart);
      final BlockPredicateArgument parse = type.parse(commandRegistryAccess, parser, suggestionsOnly);
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }
    parser.reader.setCursor(cursorOnStart);
    throw BlockPredicate.CANNOT_PARSE.createWithContext(parser.reader);
  }
}
