package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.iterator.IterateUtils;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;

import java.util.ArrayList;
import java.util.List;

import static pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension.withCursorEnd;

public abstract class WeightedListParser<T> implements Parser<WeightedList<T>> {
  public boolean weighted = false;
  public List<ObjectDoublePair<T>> pairs;
  protected double weightSum = 0;
  protected int cursorBeforeEntries;

  @Override
  public WeightedList<T> parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    parseEntryList(parseContext);
    if (weightSum == 0) {
      final SuggestedParser<?> parser = parseContext.parser();
      final int cursorEnd = parser.reader.getCursor();
      parser.reader.setCursor(cursorBeforeEntries);
      throw withCursorEnd(PickBlockFunction.SUM_ZERO.createWithContext(parser.reader), cursorEnd);
    }
    if (weighted) {
      return new WeightedList.Weighted<>(pairs);
    } else {
      return new WeightedList.Uniform<>(IterateUtils.transformFailableImmutableList(pairs, Pair::left));
    }
  }

  protected abstract T parseElement(ParseContext<?> parseContext) throws CommandSyntaxException;

  public void parseEntryList(ParseContext<?> parseContext) throws CommandSyntaxException {
    this.pairs = new ArrayList<>();
    final SuggestedParser<?> parser = parseContext.parser();
    final StringReader reader = parser.reader;
    cursorBeforeEntries = reader.getCursor();

    // 解析方块函数的部分
    while (true) {
      parser.clearSuggestion();
      final T parse = parseElement(parseContext);
      reader.skipWhitespace();
      if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) {
        final int cursorBeforeDouble = reader.getCursor();
        final double weight = reader.readDouble();
        final int cursorAfterDouble = reader.getCursor();
        if (weight < 0) {
          reader.setCursor(cursorBeforeDouble);
          throw withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, 0, weight), cursorAfterDouble);
        }
        weightSum += weight;
        weighted |= weight != 1;
        pairs.add(ObjectDoublePair.of(parse, weight));
      } else {
        pairs.add(ObjectDoublePair.of(parse, 1));
        weightSum += 1;
      }

      reader.skipWhitespace();
      parser.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder
          .suggest(separatorString()).buildFuture());
      if (!reader.canRead()) {
        break;
      }
      reader.skipWhitespace();
      final char peek = reader.peek();
      if (peek == ',') {
        reader.skip();
        reader.skipWhitespace();
      } else {
        break;
      }
    }
  }

  private String separatorString() {
    return ",";
  }

  public static <T> WeightedListParser<T> of(Parser<T> elementParser) {
    return new WeightedListParser<>() {
      @Override
      protected T parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
        return elementParser.parse(parseContext);
      }
    };
  }
}
