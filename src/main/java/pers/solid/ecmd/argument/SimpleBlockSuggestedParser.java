package pers.solid.ecmd.argument;

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
import net.minecraft.registry.Registries;
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
import pers.solid.ecmd.predicate.property.Comparator;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.SuggestionUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.Arrays;
import java.util.stream.Stream;

public abstract class SimpleBlockSuggestedParser extends SuggestedParser {
  public static final DynamicCommandExceptionType UNKNOWN_COMPARATOR = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.block_predicate.unknown_comparator", o));
  public static final SimpleCommandExceptionType COMPARATOR_EXPECTED = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_predicate.comparator_expected"));
  public static final Text START_OF_PROPERTIES = Text.translatable("enhancedCommands.argument.block_predicate.start_of_properties");
  public static final Text NEXT_PROPERTY = Text.translatable("enhancedCommands.argument.block_predicate.next_property");
  public static final Text END_OF_PROPERTIES = Text.translatable("enhancedCommands.argument.block_predicate.end_of_properties");
  protected final CommandRegistryAccess commandRegistryAccess;
  public final RegistryWrapper<Block> registryWrapper;
  public Block block;
  public Identifier blockId;
  public RegistryEntryList.Named<Block> tagId;

  public SimpleBlockSuggestedParser(CommandRegistryAccess commandRegistryAccess, StringReader reader, RegistryWrapper<Block> registryWrapper) {
    super(reader);
    this.commandRegistryAccess = commandRegistryAccess;
    this.registryWrapper = registryWrapper;
  }

  protected static <T extends Comparable<T>> void suggestValuesForProperty(Property<T> property, SuggestionsBuilder suggestionsBuilder) {
    for (T value : property.getValues()) {
      final String valueName = property.name(value);
      if (valueName.startsWith(suggestionsBuilder.getRemainingLowerCase())) {
        suggestionsBuilder.suggest(valueName);
      }
    }
  }

  protected static <T extends Comparable<T>> Stream<String> getPropertyValueNameStream(Property<T> property) {
    return property.getValues().stream().map(property::name);
  }

  public void parseBlockId() throws CommandSyntaxException {
    if (reader.canRead() && reader.peek() == '@') {
      reader.skip();
      int cursorBeforeParsing = this.reader.getCursor();
      suggestions.clear();
      suggestions.add((context, suggestionsBuilder) -> CommandSource.forEachMatching(Registries.BLOCK.streamEntries()::iterator, suggestionsBuilder.getRemaining().toLowerCase(), reference -> reference.registryKey().getValue(), reference -> suggestionsBuilder.suggest(reference.registryKey().getValue().toString(), reference.value().getName())));
      blockId = Identifier.fromCommandInput(reader);
      block = Registries.BLOCK.getOrEmpty(blockId).orElseThrow(() -> {
        this.reader.setCursor(cursorBeforeParsing);
        return BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION.createWithContext(reader, blockId.toString());
      });
    } else {
      int cursorBeforeParsing = this.reader.getCursor();
      suggestions.add((context, suggestionsBuilder) -> {
        SuggestionUtil.suggestString("@", Text.translatable("enhancedCommands.argument.block.ignore_feature_flag"), suggestionsBuilder);
        CommandSource.forEachMatching(registryWrapper.streamEntries()::iterator, suggestionsBuilder.getRemaining().toLowerCase(), reference -> reference.registryKey().getValue(), reference -> suggestionsBuilder.suggest(reference.registryKey().getValue().toString(), reference.value().getName()));
      });
      this.blockId = Identifier.fromCommandInput(this.reader);
      this.block = this.registryWrapper.getOptional(RegistryKey.of(RegistryKeys.BLOCK, this.blockId)).orElseThrow(() -> {
        this.reader.setCursor(cursorBeforeParsing);
        if (Registries.BLOCK.containsId(blockId)) {
          final Block block1 = Registries.BLOCK.get(blockId);
          return ModCommandExceptionTypes.FEATURE_REQUIRED.createWithContext(reader, Text.literal(blockId.toString()).styled(TextUtil.STYLE_FOR_ACTUAL), block1.getName().styled(TextUtil.STYLE_FOR_TARGET));
        }
        return BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION.createWithContext(reader, blockId.toString());
      }).value();
    }
  }

  public void parseProperties() throws CommandSyntaxException {
    suggestions.clear();
    suggestions.add((context, suggestionsBuilder) -> {
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
    suggestions.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
    });
    if (reader.canRead() && reader.peek() == ']') {
      reader.skip();
      suggestions.clear();
      return;
    }
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
  protected abstract Comparator parseComparator() throws CommandSyntaxException;

  protected void addPropertiesFinishedSuggestions() {
    suggestions.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest(",", NEXT_PROPERTY);
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
    });
  }

  protected void addComparatorTypeSuggestions() {
    suggestions.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(Arrays.stream(Comparator.values()).map(Comparator::asString), suggestionsBuilder));
  }

  protected void addPropertyNameSuggestions() {
    suggestions.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(block.getStateManager().getProperties().stream().map(Property::getName), suggestionsBuilder));
  }

  public void parseBlockTagIdAndProperties() throws CommandSyntaxException {
    parseBlockTagId();
    if (tagId != null) {
      parsePropertyNames();
    }
  }

  public void parseBlockTagId() throws CommandSyntaxException {
    final int cursorBeforeHash = reader.getCursor();
    if (reader.canRead() && reader.peek() == '#') {
      reader.skip();

      // start parsing tag id, after the hash symbol
      suggestions.add((context, suggestionsBuilder) -> CommandSource.suggestIdentifiers(this.registryWrapper.streamTagKeys().map(TagKey::id), suggestionsBuilder, "#"));
      Identifier identifier = Identifier.fromCommandInput(this.reader);
      this.tagId = this.registryWrapper.getOptional(TagKey.of(RegistryKeys.BLOCK, identifier)).orElseThrow(() -> {
        this.reader.setCursor(cursorBeforeHash);
        return BlockArgumentParser.UNKNOWN_BLOCK_TAG_EXCEPTION.createWithContext(this.reader, identifier.toString());
      });
    }
  }

  public void parsePropertyNames() throws CommandSyntaxException {
    suggestions.clear();
    suggestions.add((context, suggestionsBuilder) -> {
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
    suggestions.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
    });
    if (reader.canRead() && reader.peek() == ']') {
      reader.skip();
      suggestions.clear();
      return;
    }

    while (this.reader.canRead()) {
      // parse a property name
      final int cursorBeforePropertyName = reader.getCursor();
      final String propertyName = this.reader.readString();
      if (propertyName.isEmpty()) {
        this.reader.setCursor(cursorBeforePropertyName);
        throw BlockArgumentParser.EMPTY_PROPERTY_EXCEPTION.createWithContext(this.reader, tagId == null ?
            "" : this.tagId.getTag().id().toString(), propertyName);
      }
      // parse comparator
      reader.skipWhitespace();
      final int cursorBeforeReadingComparator = reader.getCursor();
      reader.setCursor(cursorBeforePropertyName);
      final String remaining = reader.getRemaining();
      if (tagId == null || tagId.stream().flatMap(entry -> entry.value().getStateManager().getProperties().stream()).distinct().noneMatch(property -> property.getName().startsWith(remaining) && !property.getName().equals(remaining))) {
        suggestions.clear();
        reader.setCursor(cursorBeforeReadingComparator);
        addComparatorTypeSuggestions();
      }

      final Comparator comparator;
      if (reader.canRead()) {
        comparator = parseComparator();
      } else {
        throw BlockArgumentParser.EMPTY_PROPERTY_EXCEPTION.createWithContext(this.reader, tagId == null ?
            "" : this.tagId.getTag().id().toString(), propertyName);
      }

      // parse valueName
      final int cursorExpect = parsePropertyValue(propertyName, comparator);
      if (cursorExpect >= 0) {
        reader.setCursor(cursorExpect);
        break;
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

  private void addTagPropertiesNameSuggestions() {
    suggestions.add((context, suggestionsBuilder) -> {
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

  protected void addTagPropertiesValueSuggestions(String propertyName) {
    if (this.tagId != null) {
      suggestions.add((context, suggestionsBuilder) -> {
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

  protected abstract <T extends Comparable<T>> void parsePropertyValue(Property<T> property, Comparator comparator) throws CommandSyntaxException;

  protected abstract int parsePropertyValue(String propertyName, Comparator comparator) throws CommandSyntaxException;
}
