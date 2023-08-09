package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.function.property.*;
import pers.solid.ecmd.predicate.property.Comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SimpleBlockFunctionSuggestedParser extends SimpleBlockSuggestedParser {
  public final List<PropertyFunction<?>> propertyFunctions = new ArrayList<>();
  public final List<PropertyNameFunction> propertyNameFunctions = new ArrayList<>();
  private boolean must = false;

  public SimpleBlockFunctionSuggestedParser(CommandRegistryAccess commandRegistryAccess, StringReader reader) {
    super(commandRegistryAccess, reader, commandRegistryAccess.createWrapper(RegistryKeys.BLOCK));
  }

  public SimpleBlockFunctionSuggestedParser(SuggestedParser parser) {
    this(parser.commandRegistryAccess, parser.reader);
    this.suggestions = parser.suggestions;
  }

  @NotNull
  protected Comparator parseComparator() throws CommandSyntaxException {
    if (reader.canRead() && reader.peek() == '=') {
      reader.skip();
      if (reader.canRead() && reader.peek() == '=') {
        reader.skip();
        must = true;
      } else {
        must = false;
      }
    } else {
      throw COMPARATOR_EXPECTED.createWithContext(reader);
    }
    return Comparator.EQ;
  }

  @Override
  protected <T extends Comparable<T>> void parsePropertyValue(Property<T> property, Comparator comparator) throws CommandSyntaxException {

    suggestions.clear();
    addSpecialPropertyValueSuggestions();
    suggestions.add((context, suggestionsBuilder) -> suggestValuesForProperty(property, suggestionsBuilder));
    if (reader.canRead()) {
      if (reader.peek() == '*') {
        propertyFunctions.add(new RandomPropertyFunction<>(property, must));
        reader.skip();
        suggestions.clear();
        return;
      } else if (reader.peek() == '~') {
        propertyFunctions.add(new BypassingPropertyFunction<>(property, must));
        reader.skip();
        suggestions.clear();
        return;
      }
      final int cursorBeforeParseValue = reader.getCursor();
      final String valueName = reader.readString();
      final Optional<T> parse = property.parse(valueName);
      if (parse.isPresent()) {
        propertyFunctions.add(new SimplePropertyFunction<>(property, parse.get(), must));
        suggestions.clear();
      } else {
        this.reader.setCursor(cursorBeforeParseValue);
        throw BlockArgumentParser.INVALID_PROPERTY_EXCEPTION.createWithContext(this.reader, blockId.toString(), property.getName(), valueName);
      }
    }
  }

  private void addSpecialPropertyValueSuggestions() {
    suggestions.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("*", Text.translatable("enhancedCommands.argument.block_function.randomValue"));
        suggestionsBuilder.suggest("~", Text.translatable("enhancedCommands.argument.block_function.originalValue"));
      }
    });
  }

  @Override
  protected int parsePropertyValue(String propertyName, Comparator comparator) throws CommandSyntaxException {
    suggestions.clear();
    addSpecialPropertyValueSuggestions();
    if (reader.canRead()) {
      if (reader.peek() == '*') {
        propertyNameFunctions.add(new RandomPropertyNameFunction(propertyName, must));
        reader.skip();
        suggestions.clear();
        return -1;
      } else if (reader.peek() == '~') {
        propertyNameFunctions.add(new BypassingPropertyNameFunction(propertyName, must));
        reader.skip();
        suggestions.clear();
        return -1;
      }
    }
    final int cursorBeforeValue = reader.getCursor();
    final String valueName = this.reader.readString();
    addTagPropertiesValueSuggestions(propertyName);
    final boolean expectEndOfValue = tagId == null || tagId.stream().flatMap(entry -> entry.value().getStateManager().getProperties().stream().filter(property -> property.getName().equals(propertyName))).flatMap(SimpleBlockPredicateSuggestedParser::getPropertyValueNameStream).noneMatch(value -> value.startsWith(valueName) && !value.equals(valueName));
    if (expectEndOfValue) {
      suggestions.clear();
    }
    addPropertiesFinishedSuggestions();
    propertyNameFunctions.add(new SimplePropertyNameFunction(propertyName, valueName, must));
    reader.skipWhitespace();
    return expectEndOfValue ? -1 : cursorBeforeValue;
  }
}
