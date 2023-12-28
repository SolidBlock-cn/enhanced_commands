package pers.solid.ecmd.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.predicate.property.Comparator;
import pers.solid.ecmd.predicate.property.*;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.SuggestionProvider;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.*;
import java.util.function.Predicate;

/**
 * @see net.minecraft.command.argument.BlockArgumentParser
 */
public class SimpleBlockPredicateSuggestedParser extends SimpleBlockSuggestedParser {
  public static final Text MATCH_ANY_VALUE = Text.translatable("enhanced_commands.block_predicate.any_value");
  public static final Text MATCH_NONE_VALUE = Text.translatable("enhanced_commands.block_predicate.none_value");
  public final List<PropertyPredicate<?>> propertyPredicates = new ArrayList<>();
  public final List<PropertyNamePredicate> propertyNamePredicates = new ArrayList<>();

  public SimpleBlockPredicateSuggestedParser(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
    super(commandRegistryAccess, parser.reader, parser.suggestionProviders);
  }

  @NotNull
  protected Comparator parseComparator() throws CommandSyntaxException {
    final Comparator comparator;
    StringBuilder stringBuilder = new StringBuilder();
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
    suggestionProviders.clear();
    final boolean usingEqual = comparator == Comparator.EQ || comparator == Comparator.NE;
    if (usingEqual) {
      addSpecialPropertyValueSuggestions();
    }
    suggestionProviders.add((context, suggestionsBuilder) -> suggestValuesForProperty(property, suggestionsBuilder));
    if (usingEqual) {
      if (reader.canRead() && reader.peek() == '*') {
        propertyPredicates.add(new ExistencePropertyPredicate<>(property, comparator == Comparator.EQ));
        reader.skip();
        suggestionProviders.clear();
        return;
      }
    }
    final LinkedHashSet<T> values = new LinkedHashSet<>(1);
    suggestionProviders.clear();
    while (true) {
      suggestionProviders.add((context, suggestionsBuilder) -> suggestValuesForProperty(property, suggestionsBuilder, t -> !values.contains(t)));
      final int cursorBeforeParseValue = reader.getCursor();
      final String valueName = reader.readString();
      final Optional<T> parse = property.parse(valueName);
      final int cursorAfterParseValue = reader.getCursor();
      if (parse.isEmpty()) {
        reader.setCursor(cursorBeforeParseValue);
        throw CommandSyntaxExceptionExtension.withCursorEnd(BlockArgumentParser.INVALID_PROPERTY_EXCEPTION.createWithContext(this.reader, blockId.toString(), property.getName(), valueName), cursorAfterParseValue);
      } else if (values.contains(parse.get())) {
        reader.setCursor(cursorBeforeParseValue);
        throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(this.reader, valueName), cursorAfterParseValue);
      } else {
        values.add(parse.get());
        suggestionProviders.clear();
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
      propertyPredicates.add(new MultiValuePropertyPredicate<>(property, values, comparator == Comparator.NE));
    }
  }


  protected static <T extends Comparable<T>> void suggestValuesForProperty(Property<T> property, SuggestionsBuilder suggestionsBuilder, Predicate<T> predicate) {
    CommandSource.suggestMatching(property.getValues().stream().filter(predicate).map(property::name), suggestionsBuilder);
  }

  private void addSpecialPropertyValueSuggestions() {
    suggestionProviders.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        final String input = suggestionsBuilder.getInput().stripTrailing();
        if (input.endsWith("!=") || input.endsWith("=!")) {
          suggestionsBuilder.suggest("*", MATCH_NONE_VALUE);
        } else {
          suggestionsBuilder.suggest("*", MATCH_ANY_VALUE);
        }
      }
    });
  }

  /**
   *
   */
  @Override
  protected void parsePropertyNameValue(String propertyName, Comparator comparator) throws CommandSyntaxException {
    suggestionProviders.clear();
    final boolean usingEqual = comparator == Comparator.EQ || comparator == Comparator.NE;
    if (usingEqual) {
      addSpecialPropertyValueSuggestions();
    }
    addTagPropertiesValueSuggestions(propertyName);
    if (reader.canRead()) {
      if (usingEqual) {
        if (reader.peek() == '*') {
          propertyNamePredicates.add(new ExistencePropertyNamePredicate(propertyName, comparator == Comparator.EQ));
          reader.skip();
          suggestionProviders.clear();
          return;
        }
      }
    }
    final LinkedHashSet<String> values = new LinkedHashSet<>(1);
    final SuggestionProvider propertyValueSuggestion = (context, suggestionsBuilder) -> {
      if (tagId != null) {
        for (RegistryEntry<Block> registryEntry : this.tagId) {
          Block block = registryEntry.value();
          Property<?> property = block.getStateManager().getProperty(propertyName);
          if (property != null) {
            suggestValueNamesForProperty(property, suggestionsBuilder, s -> !values.contains(s));
          }
        }
      }
    };
    while (true) {
      suggestionProviders.clear();
      final int cursorBeforeValue = reader.getCursor();
      final String valueName = this.reader.readString();
      final int cursorAfterValue = reader.getCursor();
      if (values.contains(valueName)) {
        reader.setCursor(cursorBeforeValue);
        throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.DUPLICATE_VALUE.createWithContext(reader, valueName), cursorAfterValue);
      }
      values.add(valueName);

      suggestionProviders.add(SuggestionProvider.offset((context, suggestionsBuilder) -> {
        final SuggestionsBuilder offset = suggestionsBuilder.createOffset(cursorBeforeValue);
        propertyValueSuggestion.accept(context, offset);
        final SuggestionsBuilder offset2 = suggestionsBuilder.createOffset(cursorAfterValue);
        PROPERTY_FINISHED.accept(context, offset2);
        return offset.buildFuture().thenCombine(offset2.buildFuture(), (suggestions, suggestions2) -> suggestions.isEmpty() ? suggestions2 : suggestions);
      }));
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
      propertyNamePredicates.add(new MultiValuePropertyNamePredicate(propertyName, values, comparator == Comparator.NE));
    }
    reader.skipWhitespace();
  }

  protected static <T extends Comparable<T>> void suggestValueNamesForProperty(Property<T> property, SuggestionsBuilder suggestionsBuilder, Predicate<String> predicate) {
    CommandSource.suggestMatching(property.getValues().stream().map(property::name).filter(predicate), suggestionsBuilder);
  }

  @Override
  protected void addComparatorTypeSuggestions() {
    suggestionProviders.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(Arrays.stream(Comparator.values()).map(Comparator::asString), suggestionsBuilder));
  }
}
