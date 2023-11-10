package pers.solid.ecmd.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.predicate.property.*;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @see net.minecraft.command.argument.BlockArgumentParser
 */
public class SimpleBlockPredicateSuggestedParser extends SimpleBlockSuggestedParser {
  public static final Text MATCH_ANY_VALUE = Text.translatable("enhanced_commands.argument.block_predicate.anyValue");
  public static final Text MATCH_NONE_VALUE = Text.translatable("enhanced_commands.argument.block_predicate.noneValue");
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
        if (!stringBuilder.isEmpty() && stringBuilder.charAt(stringBuilder.length() - 1) == '=' && peek == '!') {
          break;
        }
        stringBuilder.append(peek);
        reader.skip();
      } else
        break;
    }
    final String comparatorName = stringBuilder.toString();
    comparator = Comparator.fromName(comparatorName);
    if (comparator == null) {
      reader.setCursor(cursorBeforeComparator);
      if (comparatorName.isEmpty()) {
        throw COMPARATOR_EXPECTED.createWithContext(reader);
      } else {
        throw UNKNOWN_COMPARATOR.createWithContext(reader, comparatorName);
      }
    }
    reader.skipWhitespace();
    return comparator;
  }

  @Override
  protected <T extends Comparable<T>> void parsePropertyNameValue(Property<T> property, Comparator comparator) throws CommandSyntaxException {
    suggestionProviders.clear();
    if (comparator == Comparator.EQ || comparator == Comparator.NE) {
      addSpecialPropertyValueSuggestions();
    }
    suggestionProviders.add((context, suggestionsBuilder) -> suggestValuesForProperty(property, suggestionsBuilder));
    if (reader.canRead()) {
      if (comparator == Comparator.EQ || comparator == Comparator.NE) {
        if (reader.peek() == '*') {
          propertyPredicates.add(new ExistencePropertyPredicate<>(property, comparator == Comparator.EQ));
          reader.skip();
          suggestionProviders.clear();
          return;
        }
      }
      final int cursorBeforeParseValue = reader.getCursor();
      final String valueName = reader.readString();
      final Optional<T> parse = property.parse(valueName);
      if (parse.isPresent()) {
        propertyPredicates.add(new ValuePropertyPredicate<>(property, comparator, parse.get()));
        suggestionProviders.clear();
      } else {
        final int cursorAfterParseValue = reader.getCursor();
        this.reader.setCursor(cursorBeforeParseValue);
        throw CommandSyntaxExceptionExtension.withCursorEnd(BlockArgumentParser.INVALID_PROPERTY_EXCEPTION.createWithContext(this.reader, blockId.toString(), property.getName(), valueName), cursorAfterParseValue);
      }
    }
  }

  private void addSpecialPropertyValueSuggestions() {
    suggestionProviders.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        if (suggestionsBuilder.getInput().endsWith("!=")) {
          suggestionsBuilder.suggest("*", MATCH_NONE_VALUE);
        } else {
          suggestionsBuilder.suggest("*", MATCH_ANY_VALUE);
        }
      }
    });
  }

  /**
   * @return the cursor before the value, or -1 if no value is expected
   */
  @Override
  protected int parsePropertyNameValue(String propertyName, Comparator comparator) throws CommandSyntaxException {
    suggestionProviders.clear();
    if (comparator == Comparator.EQ || comparator == Comparator.NE) {
      addSpecialPropertyValueSuggestions();
    }
    if (reader.canRead()) {
      if (comparator == Comparator.EQ || comparator == Comparator.NE) {
        if (reader.peek() == '*') {
          propertyNamePredicates.add(new NameExistencePropertyPredicate(propertyName, comparator == Comparator.EQ));
          reader.skip();
          suggestionProviders.clear();
          return -1;
        }
      }
    }
    final int cursorBeforeValue = reader.getCursor();
    final String valueName = this.reader.readString();
    addTagPropertiesValueSuggestions(propertyName);
    final boolean expectEndOfValue = tagId == null || tagId.stream().flatMap(entry -> entry.value().getStateManager().getProperties().stream().filter(property -> property.getName().equals(propertyName))).flatMap(SimpleBlockPredicateSuggestedParser::getPropertyValueNameStream).noneMatch(value -> value.startsWith(valueName) && !value.equals(valueName));
    if (expectEndOfValue && !valueName.isEmpty()) {
      suggestionProviders.clear();
    }
    addPropertiesFinishedSuggestions();
    propertyNamePredicates.add(new ValueNamePropertyPredicate(propertyName, comparator, valueName));
    reader.skipWhitespace();
    return expectEndOfValue ? -1 : cursorBeforeValue;
  }

  @Override
  protected void addComparatorTypeSuggestions() {
    suggestionProviders.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(Arrays.stream(Comparator.values()).map(Comparator::asString), suggestionsBuilder));
  }
}
