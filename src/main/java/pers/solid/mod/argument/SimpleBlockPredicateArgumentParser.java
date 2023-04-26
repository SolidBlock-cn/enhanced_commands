package pers.solid.mod.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import pers.solid.mod.predicate.property.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @see net.minecraft.command.argument.BlockArgumentParser
 */
public class SimpleBlockPredicateArgumentParser extends ArgumentParser {
  public final RegistryWrapper<Block> registryWrapper;
  public Block block;
  public Identifier blockId;
  public List<PropertyEntry<?>> propertyEntries = new ArrayList<>();

  public static final Text START_OF_PROPERTIES = Text.translatable("argument.ecBlockStatePredicate.start_of_properties");
  public static final Text END_OF_PROPERTIES = Text.translatable("argument.ecBlockStatePredicate.end_of_properties");
  public static final Text NEXT_PROPERTY = Text.translatable("argument.ecBlockStatePredicate.next_property");
  public static final Text MATCH_ANY_VALUE = Text.translatable("argument.ecBlockStatePredicate.anyValue");
  public static final Text MATCH_NONE_VALUE = Text.translatable("argument.ecBlockStatePredicate.noneValue");
  public static final DynamicCommandExceptionType UNKNOWN_COMPARATOR = new DynamicCommandExceptionType(o -> Text.translatable("argument.ecBlockStatePredicate.unknown_comparator", o));
  public static final SimpleCommandExceptionType COMPARATOR_EXPECTED = new SimpleCommandExceptionType(Text.translatable("argument.ecBlockStatePredicate.comparator_expected"));
  public RegistryEntryList.Named<Block> tagId;
  public List<PropertyNameEntry> propertyNameEntries = new ArrayList<>();

  public SimpleBlockPredicateArgumentParser(CommandRegistryAccess commandRegistryAccess, StringReader reader) {
    super(commandRegistryAccess, reader);
    this.registryWrapper = commandRegistryAccess.createWrapper(RegistryKeys.BLOCK);
  }

  public SimpleBlockPredicateArgumentParser(ArgumentParser parser) {
    this(parser.commandRegistryAccess, parser.reader);
    this.suggestions = parser.suggestions;
  }

  public void parseBlockId() throws CommandSyntaxException {
    int cursorBeforeParsing = this.reader.getCursor();
    this.blockId = Identifier.fromCommandInput(this.reader);
    this.block = this.registryWrapper.getOptional(RegistryKey.of(RegistryKeys.BLOCK, this.blockId)).orElseThrow(() -> {
      this.reader.setCursor(cursorBeforeParsing);
      return BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION.createWithContext(this.reader, this.blockId.toString());
    }).value();
    suggestions.add(suggestionsBuilder -> suggestionsBuilder.suggest("[", START_OF_PROPERTIES));
  }

  public void parseProperties() throws CommandSyntaxException {
    suggestions.clear();
    suggestions.add(suggestionsBuilder -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("[", START_OF_PROPERTIES);
      }
    });
    if (reader.canRead() && reader.peek() == '[') {
      reader.skip();
      reader.skipWhitespace();
      suggestions.clear();
    } else {
      return;
    }
    addPropertyNameSuggestions();
    suggestions.add(suggestionsBuilder -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
    });
    while (reader.canRead()) {
      final int cursorBeforeReadString = reader.getCursor();
      // parse block property name
      String propertyName = reader.readString();
      if (propertyName.isEmpty()) {
        this.reader.setCursor(cursorBeforeReadString);
        throw BlockArgumentParser.EMPTY_PROPERTY_EXCEPTION.createWithContext(this.reader, this.blockId.toString(), propertyName);
      }
      final StateManager<Block, BlockState> stateManager = block.getStateManager();
      Property<?> property = stateManager.getProperty(propertyName);
      if (property == null) {
        reader.setCursor(cursorBeforeReadString);
        throw BlockArgumentParser.UNKNOWN_PROPERTY_EXCEPTION.createWithContext(reader, blockId, propertyName);
      }
      suggestions.clear();
      reader.skipWhitespace();

      // parse comparator

      addComparatorTypeSuggestions();
      final Comparator comparator;
      if (reader.canRead()) {
        comparator = parseComparator();
      } else {
        throw BlockArgumentParser.EMPTY_PROPERTY_EXCEPTION.createWithContext(this.reader, this.blockId.toString(), propertyName);
      }

      // parse valueName
      parsePropertyValue(property, comparator);
      if (suggestions.isEmpty()) {
        addPropertiesFinishedSuggestions();
      }
      reader.skipWhitespace();

      if (reader.canRead()) {
        if (reader.peek() == ']') {
          reader.skip();
          suggestions.clear();
          return;
        } else if (reader.peek() == ',') {
          reader.skip();
          suggestions.clear();
          addPropertyNameSuggestions();
          reader.skipWhitespace();
        }
      }
    }
    throw BlockArgumentParser.UNCLOSED_PROPERTIES_EXCEPTION.createWithContext(this.reader);
  }

  @NotNull
  private Comparator parseComparator() throws CommandSyntaxException {
    final Comparator comparator;
    StringBuilder stringBuilder = new StringBuilder();
    final int cursorBeforeComparator = reader.getCursor();
    while (reader.canRead()) {
      final char peek = reader.peek();
      if (peek == '=' || peek == '<' || peek == '!' || peek == '>') {
        if (stringBuilder.length() > 0 && stringBuilder.charAt(stringBuilder.length() - 1) == '=' && peek == '!') {
          break;
        }
        stringBuilder.append(peek);
        reader.skip();
      } else break;
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

  private void addPropertiesFinishedSuggestions() {
    suggestions.add(suggestionsBuilder -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest(",", NEXT_PROPERTY);
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
    });
  }

  private void addComparatorTypeSuggestions() {
    suggestions.add(suggestionsBuilder -> {
      for (Comparator value : Comparator.values()) {
        if (value.asString().startsWith(suggestionsBuilder.getRemaining())) {
          suggestionsBuilder.suggest(value.asString());
        }
      }
    });
  }

  private void addPropertyNameSuggestions() {
    suggestions.add(suggestionsBuilder -> {
      for (Property<?> property : block.getStateManager().getProperties()) {
        if (property.getName().startsWith(suggestionsBuilder.getRemainingLowerCase())) {
          suggestionsBuilder.suggest(property.getName());
        }
      }
    });
  }

  private <T extends Comparable<T>> void parsePropertyValue(Property<T> property, Comparator comparator) throws CommandSyntaxException {
    suggestions.clear();
    if (comparator == Comparator.EQ || comparator == Comparator.NE) {
      addSpecialPropertyValueSuggestions();
    }
    suggestions.add(suggestionsBuilder -> suggestValuesForProperty(property, suggestionsBuilder));
    if (reader.canRead()) {
      if (comparator == Comparator.EQ || comparator == Comparator.NE) {
        if (reader.peek() == '*') {
          propertyEntries.add(new ExistencePropertyEntry<>(property, comparator == Comparator.EQ));
          reader.skip();
          suggestions.clear();
          return;
        }
      }
      final int cursorBeforeParseValue = reader.getCursor();
      final String valueName = reader.readString();
      final Optional<T> parse = property.parse(valueName);
      if (parse.isPresent()) {
        propertyEntries.add(new ValuePropertyEntry<>(property, comparator, parse.get()));
        suggestions.clear();
      } else {
        this.reader.setCursor(cursorBeforeParseValue);
        throw BlockArgumentParser.INVALID_PROPERTY_EXCEPTION.createWithContext(this.reader, blockId.toString(), property.getName(), valueName);
      }
    }
  }

  private void addSpecialPropertyValueSuggestions() {
    suggestions.add(suggestionsBuilder -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        if (suggestionsBuilder.getInput().endsWith("!=")) {
          suggestionsBuilder.suggest("*", MATCH_NONE_VALUE);
        } else {
          suggestionsBuilder.suggest("*", MATCH_ANY_VALUE);
        }
      }
    });
  }

  private static <T extends Comparable<T>> void suggestValuesForProperty(Property<T> property, SuggestionsBuilder suggestionsBuilder) {
    for (T value : property.getValues()) {
      final String valueName = property.name(value);
      if (valueName.startsWith(suggestionsBuilder.getRemainingLowerCase())) {
        suggestionsBuilder.suggest(valueName);
      }
    }
  }

  public void parseBlockTagIdAndProperties() throws CommandSyntaxException {
    parseBlockTagId();
    if (tagId != null) {
      parsePropertyName();
    }
  }

  public void parseBlockTagId() throws CommandSyntaxException {
    final int cursorBeforeHash = reader.getCursor();
    if (reader.canRead() && reader.peek() == '#') {
      reader.skip();

      // start parsing tag id, after the hash symbol
      suggestions.add(suggestionsBuilder -> CommandSource.suggestIdentifiers(this.registryWrapper.streamTagKeys().map(TagKey::id), suggestionsBuilder, "#"));
      Identifier identifier = Identifier.fromCommandInput(this.reader);
      this.tagId = this.registryWrapper.getOptional(TagKey.of(RegistryKeys.BLOCK, identifier)).orElseThrow(() -> {
        this.reader.setCursor(cursorBeforeHash);
        return BlockArgumentParser.UNKNOWN_BLOCK_TAG_EXCEPTION.createWithContext(this.reader, identifier.toString());
      });
    }
  }

  public void parsePropertyName() throws CommandSyntaxException {
    suggestions.clear();
    suggestions.add(suggestionsBuilder -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("[", START_OF_PROPERTIES);
      }
    });
    if (reader.canRead() && reader.peek() == '[') {
      reader.skip();
      reader.skipWhitespace();
      suggestions.clear();
    } else {
      return;
    }
    addTagPropertiesNameSuggestions();
    suggestions.add(suggestionsBuilder -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
    });

    while (this.reader.canRead()) {
      // parse a property name
      final int cursorBeforePropertyName = reader.getCursor();
      final String propertyName = this.reader.readString();
      if (propertyName.isEmpty()) {
        this.reader.setCursor(cursorBeforePropertyName);
        throw BlockArgumentParser.EMPTY_PROPERTY_EXCEPTION.createWithContext(this.reader, this.tagId.getTag().id().toString(), propertyName);
      }
      // parse comparator
      reader.skipWhitespace();
      final int cursorBeforeReadingComparator = reader.getCursor();
      reader.setCursor(cursorBeforePropertyName);
      final String remaining = reader.getRemaining();
      if (tagId.stream().flatMap(entry -> entry.value().getStateManager().getProperties().stream()).distinct().noneMatch(property -> property.getName().startsWith(remaining) && !property.getName().equals(remaining))) {
        suggestions.clear();
        reader.setCursor(cursorBeforeReadingComparator);
        addComparatorTypeSuggestions();
      }

      final Comparator comparator;
      if (reader.canRead()) {
        comparator = parseComparator();
      } else {
        throw BlockArgumentParser.EMPTY_PROPERTY_EXCEPTION.createWithContext(this.reader, this.tagId.getTag().id().toString(), propertyName);
      }

      // parse valueName
      final int cursorExpect = parsePropertyValue(propertyName, comparator);
      if (cursorExpect >= 0) {
        reader.setCursor(cursorExpect);
        return;
      }
      reader.skipWhitespace();

      if (reader.canRead()) {
        if (reader.peek() == ']') {
          reader.skip();
          suggestions.clear();
          return;
        } else if (reader.peek() == ',') {
          reader.skip();
          suggestions.clear();
          addTagPropertiesNameSuggestions();
          reader.skipWhitespace();
        }
      }
    }
    throw BlockArgumentParser.UNCLOSED_PROPERTIES_EXCEPTION.createWithContext(this.reader);
  }

  /**
   * @return the cursor before the value, or -1 if no value is expected
   */
  private int parsePropertyValue(String propertyName, Comparator comparator) throws CommandSyntaxException {
    suggestions.clear();
    if (comparator == Comparator.EQ || comparator == Comparator.NE) {
      addSpecialPropertyValueSuggestions();
    }
    if (reader.canRead()) {
      if (comparator == Comparator.EQ || comparator == Comparator.NE) {
        if (reader.peek() == '*') {
          propertyNameEntries.add(new NameExistencePropertyEntry(propertyName, comparator == Comparator.EQ));
          reader.skip();
          suggestions.clear();
          return -1;
        }
      }
    }
    final int cursorBeforeValue = reader.getCursor();
    final String valueName = this.reader.readString();
    addTagPropertiesValueSuggestions(propertyName);
    final boolean expectEndOfValue = tagId == null || tagId.stream().flatMap(entry -> entry.value().getStateManager().getProperties().stream().filter(property -> property.getName().equals(propertyName))).flatMap(SimpleBlockPredicateArgumentParser::getPropertyValueNameStream).noneMatch(value -> value.startsWith(valueName) && !value.equals(valueName));
    if (expectEndOfValue) {
      suggestions.clear();
    }
    addPropertiesFinishedSuggestions();
    propertyNameEntries.add(new ValueNamePropertyEntry(propertyName, comparator, valueName));
    reader.skipWhitespace();
    return expectEndOfValue ? -1 : cursorBeforeValue;
  }

  private static <T extends Comparable<T>> Stream<String> getPropertyValueNameStream(Property<T> property) {
    return property.getValues().stream().map(property::name);
  }

  private void addTagPropertiesNameSuggestions() {
    suggestions.add(suggestionsBuilder -> {
      String string = suggestionsBuilder.getRemainingLowerCase();
      if (this.tagId != null) {
        for (RegistryEntry<Block> registryEntry : this.tagId) {
          for (Property<?> property : registryEntry.value().getStateManager().getProperties()) {
            if (property.getName().startsWith(string)) {
              suggestionsBuilder.suggest(property.getName());
            }
          }
        }
      }
    });
  }

  private void addTagPropertiesValueSuggestions(String propertyName) {
    if (this.tagId != null) {
      suggestions.add(suggestionsBuilder -> {
        for (RegistryEntry<Block> registryEntry : this.tagId) {
          Block block = registryEntry.value();
          Property<?> property = block.getStateManager().getProperty(propertyName);
          if (property != null) {
            suggestValuesForProperty(property, suggestionsBuilder);
          }
        }
      });
    }
  }
}
