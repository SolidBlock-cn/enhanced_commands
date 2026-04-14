package pers.solid.ecmd.block.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static pers.solid.ecmd.util.EnhancedCommandSyntaxException.withCursorEnd;

public abstract class WeightedListParser<T> implements Parser<WeightedList<T>> {
  public static final SimpleCommandExceptionType SUM_ZERO = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.function.pick.zero_sum"));
  public boolean weighted = false;
  public @Nullable List<ObjectDoublePair<T>> pairs;
  protected double weightSum = 0;
  protected int cursorBeforeEntries;

  @Override
  public WeightedList<T> parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    parseEntryList(parseContext);
    Objects.requireNonNull(pairs, "pairs");
    final StringReader reader = parseContext.reader();
    if (weightSum == 0) {
      final int cursorEnd = reader.getCursor();
      reader.setCursor(cursorBeforeEntries);
      throw withCursorEnd(SUM_ZERO.createWithContext(reader), cursorEnd);
    }
    if (weighted) {
      return new WeightedList.Weighted<>(pairs);
    } else {
      return new WeightedList.Uniform<>(IterateUtils.transformFailableImmutableList(pairs, Pair::left));
    }
  }

  protected abstract @Nullable T parseElement(ParseContext<?> parseContext) throws CommandSyntaxException;

  public void parseEntryList(ParseContext<?> parseContext) throws CommandSyntaxException {
    this.pairs = new ArrayList<>();
    final StringReader reader = parseContext.reader();
    cursorBeforeEntries = reader.getCursor();

    // 解析元素的部分
    while (true) {
      parseContext.clearSuggestion();
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
      parseContext.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder
          .suggest(separatorString()).buildFuture());
      if (!reader.canRead()) {
        break;
      }
      reader.skipWhitespace();
      final char peek = reader.peek();
      if (peek == separator()) {
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

  private char separator() {
    return ',';
  }

  public static <T> WeightedListParser<T> of(Parser<T> elementParser) {
    return new WeightedListParser<>() {
      @Override
      protected @Nullable T parseElement(ParseContext<?> parseContext) throws CommandSyntaxException {
        return elementParser.parse(parseContext);
      }
    };
  }
}
