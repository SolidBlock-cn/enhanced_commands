package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
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
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.predicate.property.Comparator;
import pers.solid.ecmd.util.ModCommandExceptionTypes;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public abstract class SimpleBlockSuggestedParser<S> extends SuggestedParser<S> {
  public static final DynamicCommandExceptionType UNKNOWN_COMPARATOR = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.block_predicate.unknown_comparator", o));
  public static final SimpleCommandExceptionType COMPARATOR_EXPECTED = new SimpleCommandExceptionType(Text.translatable("enhanced_commands.block_predicate.comparator_expected"));
  public static final Text START_OF_PROPERTIES = Text.translatable("enhanced_commands.block_predicate.start_of_properties");
  public static final Text NEXT_PROPERTY = Text.translatable("enhanced_commands.block_predicate.next_property");
  public static final Text END_OF_PROPERTIES = Text.translatable("enhanced_commands.block_predicate.end_of_properties");
  public static final SuggestionProvider<Object> PROPERTY_FINISHED = (context, builder) -> {
    if (builder.getRemaining().isEmpty()) {
      builder.suggest(",", NEXT_PROPERTY);
      builder.suggest("]", END_OF_PROPERTIES);
    }
    return builder.buildFuture();
  };
  public final RegistryWrapper<Block> registryWrapper;
  protected final CommandRegistryAccess registryAccess;
  public Block block;
  public Identifier blockId;
  public RegistryEntryList.Named<Block> tagId;

  public SimpleBlockSuggestedParser(CommandRegistryAccess registryAccess, StringReader reader) {
    super(reader);
    this.registryAccess = registryAccess;
    this.registryWrapper = registryAccess.getWrapperOrThrow(RegistryKeys.BLOCK);
  }

  public SimpleBlockSuggestedParser(CommandRegistryAccess registryAccess, SuggestedParser<S> parent) {
    super(parent.reader, parent.suggestions);
    this.registryAccess = registryAccess;
    this.registryWrapper = registryAccess.getWrapperOrThrow(RegistryKeys.BLOCK);
  }

  protected static <T extends Comparable<T>> CompletableFuture<Suggestions> suggestValuesForProperty(Property<T> property, SuggestionsBuilder suggestionsBuilder) {
    return CommandSource.suggestMatching(property.getValues().stream().map(property::name), suggestionsBuilder);
  }

  protected static <T extends Comparable<T>> Stream<String> getPropertyValueNameStream(Property<T> property) {
    return property.getValues().stream().map(property::name);
  }

  public void parseBlockId() throws CommandSyntaxException {
    if (reader.canRead() && reader.peek() == '@') {
      reader.skip();
      int cursorBeforeParsing = this.reader.getCursor();
      setSuggestion((context, suggestionsBuilder) -> CommandSource.suggestFromIdentifier(Registries.BLOCK.streamEntries(), suggestionsBuilder, reference -> reference.registryKey().getValue(), reference -> reference.value().getName()));
      blockId = Identifier.fromCommandInput(reader);
      block = Registries.BLOCK.getOrEmpty(blockId).orElseThrow(() -> {
        final int cursorAfterParsing = reader.getCursor();
        this.reader.setCursor(cursorBeforeParsing);
        return CommandSyntaxExceptionExtension.withCursorEnd(BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION.createWithContext(reader, blockId.toString()), cursorAfterParsing);
      });
    } else {
      int cursorBeforeParsing = this.reader.getCursor();
      addSuggestion((context, suggestionsBuilder) -> {
        ParsingUtil.suggestString("@", Text.translatable("enhanced_commands.argument.block.ignore_feature_flag"), suggestionsBuilder);
        return CommandSource.suggestFromIdentifier(registryWrapper.streamEntries(), suggestionsBuilder, r -> r.registryKey().getValue(), r -> r.value().getName());
      });
      this.blockId = Identifier.fromCommandInput(this.reader);
      this.block = this.registryWrapper.getOptional(RegistryKey.of(RegistryKeys.BLOCK, this.blockId)).orElseThrow(() -> {
        final int cursorAfterParsing = reader.getCursor();
        this.reader.setCursor(cursorBeforeParsing);
        if (Registries.BLOCK.containsId(blockId)) {
          final Block block1 = Registries.BLOCK.get(blockId);
          return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.BLOCK_ID_FEATURE_FLAG_REQUIRED.createWithContext(reader, blockId, block1.getName()), cursorAfterParsing);
        } else {
          return CommandSyntaxExceptionExtension.withCursorEnd(BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION.createWithContext(reader, blockId.toString()), cursorAfterParsing);
        }
      }).value();
    }
  }

  public void parseProperties() throws CommandSyntaxException {
    setSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("[", START_OF_PROPERTIES);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == '[') {
      reader.skip();
      reader.skipWhitespace();
      clearSuggestion();
    } else {
      return;
    }
    setSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == ']') {
      reader.skip();
      clearSuggestion();
      return;
    }
    while (reader.canRead(-1)) {
      parsePropertyEntry();
      reader.skipWhitespace();

      if (true) {// todo check
        addPropertiesFinishedSuggestions();
      }
      if (parsePropertyEntryEnd()) return;
    }
    throw BlockArgumentParser.UNCLOSED_PROPERTIES_EXCEPTION.createWithContext(this.reader);
  }

  /**
   * 解析属性列表中的逗号和结束方括号。
   *
   * @return 是否表示着整个属性列表（含方括号）已经结束。
   */
  private boolean parsePropertyEntryEnd() throws CommandSyntaxException {
    boolean commaFound = false;
    if (reader.canRead() && reader.peek() == ',') {
      commaFound = true;
      reader.skip();
      clearSuggestion();
      reader.skipWhitespace();
    }
    if (reader.canRead() && reader.peek() == ']') {
      reader.skip();
      clearSuggestion();
      return true;
    }
    if (!commaFound) {
      reader.expect(',');
    }
    return false;
  }

  protected void parsePropertyEntry() throws CommandSyntaxException {
    final Property<?> property = parseProperty();
    reader.skipWhitespace();

    // parse comparator

    addComparatorTypeSuggestions();
    final Comparator comparator;
    if (reader.canRead()) {
      comparator = parseComparator();
    } else {
      throw BlockArgumentParser.EMPTY_PROPERTY_EXCEPTION.createWithContext(this.reader, this.blockId.toString(), property.getName());
    }
    reader.skipWhitespace();

    // parse valueName
    parsePropertyNameValue(property, comparator);
  }

  @NotNull
  protected Property<?> parseProperty() throws CommandSyntaxException {
    addPropertyNameSuggestions();
    final int cursorBeforeReadString = reader.getCursor();
    // parse block property propertyName
    String propertyName = reader.readString();
    if (propertyName.isEmpty()) {
      final int cursorAfterReadString = reader.getCursor();
      this.reader.setCursor(cursorBeforeReadString);
      throw CommandSyntaxExceptionExtension.withCursorEnd(BlockArgumentParser.EMPTY_PROPERTY_EXCEPTION.createWithContext(this.reader, this.blockId.toString(), propertyName), cursorAfterReadString);
    }
    final StateManager<Block, BlockState> stateManager = block.getStateManager();
    Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) {
      final int cursorAfterReadString = reader.getCursor();
      reader.setCursor(cursorBeforeReadString);
      throw CommandSyntaxExceptionExtension.withCursorEnd(BlockArgumentParser.UNKNOWN_PROPERTY_EXCEPTION.createWithContext(reader, blockId, propertyName), cursorAfterReadString);
    }
    clearSuggestion();
    return property;
  }

  @NotNull
  protected abstract Comparator parseComparator() throws CommandSyntaxException;

  @SuppressWarnings("unchecked")
  protected void addPropertiesFinishedSuggestions() {
    addSuggestion((SuggestionProvider<S>) PROPERTY_FINISHED);
  }

  protected abstract void addComparatorTypeSuggestions();

  protected void addPropertyNameSuggestions() {
    addSuggestion((context, suggestionsBuilder) -> CommandSource.suggestMatching(block.getStateManager().getProperties().stream().map(Property::getName), suggestionsBuilder));
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
      addSuggestion((context, suggestionsBuilder) -> CommandSource.suggestIdentifiers(this.registryWrapper.streamTagKeys().map(TagKey::id), suggestionsBuilder, "#"));
      Identifier identifier = Identifier.fromCommandInput(this.reader);
      this.tagId = this.registryWrapper.getOptional(TagKey.of(RegistryKeys.BLOCK, identifier)).orElseThrow(() -> {
        this.reader.setCursor(cursorBeforeHash);
        return BlockArgumentParser.UNKNOWN_BLOCK_TAG_EXCEPTION.createWithContext(this.reader, identifier.toString());
      });
    }
  }

  public void parsePropertyNames() throws CommandSyntaxException {
    setSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("[", START_OF_PROPERTIES);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == '[') {
      reader.skip();
      reader.skipWhitespace();
      clearSuggestion();
    } else {
      return;
    }
    addSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == ']') {
      reader.skip();
      clearSuggestion();
      return;
    }

    while (this.reader.canRead(-1)) {
      parsePropertyNameEntry();
      reader.skipWhitespace();

      if (parsePropertyEntryEnd()) return;
    }
    throw BlockArgumentParser.UNCLOSED_PROPERTIES_EXCEPTION.createWithContext(this.reader);
  }

  protected void parsePropertyNameEntry() throws CommandSyntaxException {
    // parse a property propertyName
    addTagPropertiesNameSuggestions();
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
      clearSuggestion();
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
    reader.skipWhitespace();

    // parse valueName
    parsePropertyNameValue(propertyName, comparator);
  }

  private void addTagPropertiesNameSuggestions() {
    addSuggestion((context, suggestionsBuilder) -> {
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
      return suggestionsBuilder.buildFuture();
    });
  }

  protected void addTagPropertiesValueSuggestions(String propertyName) {
    if (this.tagId != null) {
      addSuggestion((context, suggestionsBuilder) -> {
        for (RegistryEntry<Block> registryEntry : this.tagId) {
          Block block = registryEntry.value();
          Property<?> property = block.getStateManager().getProperty(propertyName);
          if (property != null) {
            suggestValuesForProperty(property, suggestionsBuilder);
          }
        }
        return suggestionsBuilder.buildFuture();
      });
    }
  }

  protected abstract <T extends Comparable<T>> void parsePropertyNameValue(Property<T> property, Comparator comparator) throws CommandSyntaxException;

  protected abstract void parsePropertyNameValue(String propertyName, Comparator comparator) throws CommandSyntaxException;
}
