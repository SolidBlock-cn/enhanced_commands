package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtPredicateParser;
import pers.solid.ecmd.argument.SimpleBlockParser;
import pers.solid.ecmd.argument.SimpleBlockPredicateParser;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collections;
import java.util.List;

public interface BlockPredicateArgument extends FailableFunction<ServerCommandSource, BlockPredicate, CommandSyntaxException> {
  Text INTERSECT_TOOLTIP = Text.translatable("enhanced_commands.block_predicate.all.symbol_tooltip");
  Text UNION_TOOLTIP = Text.translatable("enhanced_commands.block_predicate.any.symbol_tooltip");

  static @NotNull BlockPredicate parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return parseUnion(parseContext);
  }

  static @NotNull BlockPredicate parseUnion(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseIntersect(parseContext), AnyBlockPredicate::new, "|", UNION_TOOLTIP, parseContext);
  }

  static @NotNull BlockPredicate parseIntersect(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseCombination(parseContext), AllBlockPredicate::new, "&", INTERSECT_TOOLTIP, parseContext);
  }

  static @NotNull <S> BlockPredicate parseCombination(ParseContext<S> parseContext) throws CommandSyntaxException {
    final BlockPredicate parseUnit = parseUnit(parseContext);
    if (parseUnit instanceof NbtPredicate) {
      return parseUnit;
    }
    final StringReader reader = parseContext.reader();
    List<PropertyNamePredicate> propertyNamePredicates;
    if (!(parseUnit instanceof PropertiesNamesBlockPredicate) && reader.canRead(0) && reader.peek(-1) != ']') {
      // 当前面以“]”结尾时，说明已经在其他解析器中读取了属性，此时在这里不再读取任何属性
      // 尝试读取属性
      parseContext.addSuggestion((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest("[", SimpleBlockParser.START_OF_PROPERTIES);
        }
        return suggestionsBuilder.buildFuture();
      });
      if (reader.canRead() && reader.peek() == '[') {
        final SimpleBlockPredicateParser<S> suggestedParser = new SimpleBlockPredicateParser<>(parseContext);
        suggestedParser.parsePropertyNames();
        propertyNamePredicates = suggestedParser.propertyNamePredicates;
      } else propertyNamePredicates = null;
    } else propertyNamePredicates = null;
    NbtPredicate nbtPredicate;
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("{", NbtPredicateParser.START_OF_COMPOUND);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == '{') {
      // 尝试读取 NBT
      nbtPredicate = new NbtPredicateParser<>(parseContext).parseCompound(false, false);
    } else nbtPredicate = null;
    if (propertyNamePredicates != null || nbtPredicate != null) {
      return new PropertiesNbtCombinationBlockPredicate(parseUnit, propertyNamePredicates == null ? null : new PropertiesNamesBlockPredicate(propertyNamePredicates), nbtPredicate == null ? null : new NbtBlockPredicate(nbtPredicate));
    }
    return parseUnit;
  }

  @NotNull
  static BlockPredicate parseUnit(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorOnStart = reader.getCursor();
    // 刻意将 simple 调整到最后面
    for (Parser<BlockPredicate> argumentParser : Iterables.concat(BlockPredicateTypes.PARSERS, Collections.singleton(SimpleBlockPredicate.Type.SIMPLE_TYPE))) {
      reader.setCursor(cursorOnStart);
      final BlockPredicate parse = argumentParser.parse(parseContext);
      if (parse != null) {
        return parse;
      }
    }
    reader.setCursor(cursorOnStart);
    throw BlockPredicate.CANNOT_PARSE.createWithContext(reader);
  }

  @Override
  BlockPredicate apply(ServerCommandSource source) throws CommandSyntaxException;
}
