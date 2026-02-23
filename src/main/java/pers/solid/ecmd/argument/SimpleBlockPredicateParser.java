package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.predicate.property.*;
import pers.solid.ecmd.predicate.property.Comparator;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * @see net.minecraft.commands.arguments.blocks.BlockStateParser
 */
public class SimpleBlockPredicateParser<S> extends SimpleBlockParser<S> {
  public static final Component MATCH_ANY_VALUE = Component.translatable("enhanced_commands.block_predicate.any_value");
  public static final Component MATCH_NONE_VALUE = Component.translatable("enhanced_commands.block_predicate.none_value");
  public final List<PropertyPredicate<?>> propertyPredicates = new ArrayList<>();
  public final List<PropertyNamePredicate> propertyNamePredicates = new ArrayList<>();

  public SimpleBlockPredicateParser(ParseContext<S> parseContext) {
    super(parseContext);
  }

  @NotNull
  protected Comparator parseComparator() throws CommandSyntaxException {
    final Comparator comparator;
    StringBuilder stringBuilder = new StringBuilder();
    final StringReader reader = parseContext.reader();
    final int cursorBeforeComparator = reader.getCursor();
    while (reader.canRead()) {
      final char peek = reader.peek();
      if (peek == '=' || peek == '<' || peek == '!' || peek == '>') {
        stringBuilder.append(peek);
        reader.skip();
      } else {
        break;
      }
    }
    final String comparatorName = stringBuilder.toString();
    comparator = Comparator.fromName(comparatorName);
    if (comparator == null) {
      final int cursorAfterComparator = reader.getCursor();
      reader.setCursor(cursorBeforeComparator);
      if (comparatorName.isEmpty()) {
        throw COMPARATOR_EXPECTED.createWithContext(reader);
      } else {
        throw CommandSyntaxExceptionExtension.withCursorEnd(UNKNOWN_COMPARATOR.createWithContext(reader, comparatorName), cursorAfterComparator);
      }
    }
    reader.skipWhitespace();
    return comparator;
  }

  @Override
  protected <T extends Comparable<T>> void parsePropertyNameValue(Property<T> property, Comparator comparator) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.clearSuggestion();
    final boolean usingEqual = comparator == Comparator.EQ || comparator == Comparator.NE;
    if (usingEqual) {
      addSpecialPropertyValueSuggestions();
    }
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestValuesForProperty(property, suggestionsBuilder));
    if (usingEqual) {
      if (reader.canRead() && reader.peek() == '*') {
        propertyPredicates.add(new ExistencePropertyPredicate<>(property, comparator == Comparator.EQ));
        reader.skip();
        parseContext.clearSuggestion();
        return;
      }
    }
    final LinkedHashSet<T> values = new LinkedHashSet<>(1);
    parseContext.clearSuggestion();
    while (true) {
      parseContext.addSuggestion((context, suggestionsBuilder) -> suggestValuesForProperty(property, suggestionsBuilder, t -> !values.contains(t)));
      final int cursorBeforeParseValue = reader.getCursor();
      final String valueName = reader.readString();
      final Optional<T> parse = property.getValue(valueName);
      final int cursorAfterParseValue = reader.getCursor();
      if (parse.isEmpty()) {
        reader.setCursor(cursorBeforeParseValue);
        throw CommandSyntaxExceptionExtension.withCursorEnd(BlockStateParser.ERROR_INVALID_VALUE.createWithContext(reader, blockId.toString(), property.getName(), valueName), cursorAfterParseValue);
      } else if (values.contains(parse.get())) {
        reader.setCursor(cursorBeforeParseValue);
        throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, valueName), cursorAfterParseValue);
      } else {
        values.add(parse.get());
        parseContext.clearSuggestion();
      }
      if (!usingEqual) break;

      reader.skipWhitespace();
      if (reader.canRead() && reader.peek() == '|') {
        reader.skip();
        reader.skipWhitespace();
      } else {
        reader.setCursor(cursorAfterParseValue);
        break;
      }
    }
    if (values.size() == 1) {
      propertyPredicates.add(new ComparisonPropertyPredicate<>(property, comparator, values.iterator().next()));
    } else {
      propertyPredicates.add(new MultiValuePropertyPredicate<>(property, ImmutableList.copyOf(values), comparator == Comparator.NE));
    }
  }


  protected static <T extends Comparable<T>> CompletableFuture<Suggestions> suggestValuesForProperty(Property<T> property, SuggestionsBuilder suggestionsBuilder, Predicate<T> predicate) {
    return SharedSuggestionProvider.suggest(property.getPossibleValues().stream().filter(predicate).map(property::getName), suggestionsBuilder);
  }

  private void addSpecialPropertyValueSuggestions() {
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        final String input = suggestionsBuilder.getInput().stripTrailing();
        if (input.endsWith("!=") || input.endsWith("=!")) {
          suggestionsBuilder.suggest("*", MATCH_NONE_VALUE);
        } else {
          suggestionsBuilder.suggest("*", MATCH_ANY_VALUE);
        }
      }
      return suggestionsBuilder.buildFuture();
    });
  }

  /**
   *
   */
  @Override
  protected void parsePropertyNameValue(String propertyName, Comparator comparator) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.clearSuggestion();
    final boolean usingEqual = comparator == Comparator.EQ || comparator == Comparator.NE;
    if (usingEqual) {
      addSpecialPropertyValueSuggestions();
    }
    if (this.tagId != null) {
      parseContext.addSuggestion(getTagPropertiesValueSuggestions(this.tagId, propertyName));
    }
    if (reader.canRead()) {
      if (usingEqual) {
        if (reader.peek() == '*') {
          propertyNamePredicates.add(new ExistencePropertyNamePredicate(propertyName, comparator == Comparator.EQ));
          reader.skip();
          parseContext.clearSuggestion();
          return;
        }
      }
    }
    final LinkedHashSet<String> values = new LinkedHashSet<>(1);
    final SuggestionProvider<S> propertyValueSuggestion = (context, builder) -> {
      if (tagId != null) {
        for (Holder<Block> registryEntry : this.tagId) {
          Block block = registryEntry.value();
          Property<?> property = block.getStateDefinition().getProperty(propertyName);
          if (property != null) {
            suggestValueNamesForProperty(property, builder, s -> !values.contains(s));
          }
        }
      }
      return builder.buildFuture();
    };
    while (true) {
      parseContext.clearSuggestion();
      final int cursorBeforeValue = reader.getCursor();
      final String valueName = reader.readString();
      final int cursorAfterValue = reader.getCursor();
      if (values.contains(valueName)) {
        reader.setCursor(cursorBeforeValue);
        throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, valueName), cursorAfterValue);
      }
      values.add(valueName);

      parseContext.addSuggestion((context, suggestionsBuilder) -> {
        final SuggestionsBuilder offset = suggestionsBuilder.createOffset(cursorBeforeValue);
        final SuggestionsBuilder offset2 = suggestionsBuilder.createOffset(cursorAfterValue);
        //noinspection unchecked
        PROPERTY_FINISHED.getSuggestions((CommandContext<Object>) context, offset2);
        return propertyValueSuggestion.getSuggestions(context, offset).thenCombine(offset2.buildFuture(), (suggestions, suggestions2) -> suggestions.isEmpty() ? suggestions2 : suggestions);
      });
      if (!usingEqual) break;

      reader.skipWhitespace();
      if (reader.canRead() && reader.peek() == '|') {
        reader.skip();
        reader.skipWhitespace();
      } else {
        reader.setCursor(cursorAfterValue);
        break;
      }
    }

    if (values.size() == 1) {
      propertyNamePredicates.add(new ComparisonPropertyNamePredicate(propertyName, comparator, values.iterator().next()));
    } else {
      propertyNamePredicates.add(new MultiValuePropertyNamePredicate(propertyName, ImmutableList.copyOf(values), comparator == Comparator.NE));
    }
    reader.skipWhitespace();
  }

  protected static <T extends Comparable<T>> void suggestValueNamesForProperty(Property<T> property, SuggestionsBuilder suggestionsBuilder, Predicate<String> predicate) {
    SharedSuggestionProvider.suggest(property.getPossibleValues().stream().map(property::getName).filter(predicate), suggestionsBuilder);
  }

  @Override
  protected void addComparatorTypeSuggestions() {
    parseContext.addSuggestion((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(Arrays.stream(Comparator.values()).map(Comparator::getSerializedName), suggestionsBuilder));
  }
}
