package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.predicate.property.Comparator;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public abstract class SimpleBlockParser<S> {
  public static final DynamicCommandExceptionType UNKNOWN_COMPARATOR = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.argument.block_predicate.unknown_comparator", o));
  public static final SimpleCommandExceptionType COMPARATOR_EXPECTED = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.block_predicate.comparator_expected"));
  public static final Component START_OF_PROPERTIES = Component.translatable("enhanced_commands.block_predicate.start_of_properties");
  public static final Component NEXT_PROPERTY = Component.translatable("enhanced_commands.block_predicate.next_property");
  public static final Component END_OF_PROPERTIES = Component.translatable("enhanced_commands.block_predicate.end_of_properties");
  public static final SuggestionProvider<Object> PROPERTY_FINISHED = (context, builder) -> {
    if (builder.getRemaining().isEmpty()) {
      builder.suggest(",", NEXT_PROPERTY);
      builder.suggest("]", END_OF_PROPERTIES);
    }
    return builder.buildFuture();
  };
  public Block block;
  public ResourceLocation blockId;
  public @Nullable HolderSet.Named<Block> tagId;
  public final ParseContext<S> parseContext;

  protected SimpleBlockParser(ParseContext<S> parseContext) {
    this.parseContext = parseContext;
  }

  protected static <T extends Comparable<T>> CompletableFuture<Suggestions> suggestValuesForProperty(Property<T> property, SuggestionsBuilder suggestionsBuilder) {
    return SharedSuggestionProvider.suggest(property.getPossibleValues().stream().map(property::getName), suggestionsBuilder);
  }

  protected static <T extends Comparable<T>> Stream<String> getPropertyValueNameStream(Property<T> property) {
    return property.getPossibleValues().stream().map(property::getName);
  }

  public void parseBlockId() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final HolderLookup.RegistryLookup<Block> registryWrapper = parseContext.commandBuildContext().lookupOrThrow(Registries.BLOCK);
    if (reader.canRead() && reader.peek() == '@') {
      reader.skip();
      int cursorBeforeParsing = reader.getCursor();
      parseContext.setSuggestion((context, suggestionsBuilder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.BLOCK.listElements(), suggestionsBuilder, reference -> reference.key().location(), reference -> reference.value().getName()));
      blockId = ResourceLocation.read(reader);
      block = BuiltInRegistries.BLOCK.getOptional(blockId).orElseThrow(() -> {
        final int cursorAfterParsing = reader.getCursor();
        reader.setCursor(cursorBeforeParsing);
        return CommandSyntaxExceptionExtension.withCursorEnd(BlockStateParser.ERROR_UNKNOWN_BLOCK.createWithContext(reader, blockId.toString()), cursorAfterParsing);
      });
    } else {
      int cursorBeforeParsing = reader.getCursor();
      parseContext.addSuggestion((context, suggestionsBuilder) -> {
        ParsingUtil.suggestString("@", Component.translatable("enhanced_commands.argument.block.ignore_feature_flag"), suggestionsBuilder);
        return SharedSuggestionProvider.suggestResource(registryWrapper.listElements(), suggestionsBuilder, r -> r.key().location(), r -> r.value().getName());
      });
      this.blockId = ResourceLocation.read(reader);
      this.block = registryWrapper.get(ResourceKey.create(Registries.BLOCK, this.blockId)).orElseThrow(() -> {
        final int cursorAfterParsing = reader.getCursor();
        reader.setCursor(cursorBeforeParsing);
        if (BuiltInRegistries.BLOCK.containsKey(blockId)) {
          final Block block1 = BuiltInRegistries.BLOCK.getValue(blockId);
          return CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.BLOCK_ID_FEATURE_FLAG_REQUIRED.createWithContext(reader, blockId, block1.getName()), cursorAfterParsing);
        } else {
          return CommandSyntaxExceptionExtension.withCursorEnd(BlockStateParser.ERROR_UNKNOWN_BLOCK.createWithContext(reader, blockId.toString()), cursorAfterParsing);
        }
      }).value();
    }
  }

  public void parseProperties() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.setSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("[", START_OF_PROPERTIES);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == '[') {
      reader.skip();
      reader.skipWhitespace();
      parseContext.clearSuggestion();
    } else {
      return;
    }
    parseContext.setSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == ']') {
      reader.skip();
      parseContext.clearSuggestion();
      return;
    }
    while (reader.canRead(-1)) {
      parsePropertyEntry();
      reader.skipWhitespace();

      addPropertiesFinishedSuggestions();
      if (parsePropertyEntryEnd()) return;
    }
    throw BlockStateParser.ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(reader);
  }

  /**
   * 解析属性列表中的逗号和结束方括号。
   *
   * @return 是否表示着整个属性列表（含方括号）已经结束。
   */
  private boolean parsePropertyEntryEnd() throws CommandSyntaxException {
    boolean commaFound = false;
    final StringReader reader = parseContext.reader();
    if (reader.canRead() && reader.peek() == ',') {
      commaFound = true;
      reader.skip();
      parseContext.clearSuggestion();
      reader.skipWhitespace();
    }
    if (reader.canRead() && reader.peek() == ']') {
      reader.skip();
      parseContext.clearSuggestion();
      return true;
    }
    if (!commaFound) {
      reader.expect(',');
    }
    return false;
  }

  protected void parsePropertyEntry() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final Property<?> property = parseProperty();
    reader.skipWhitespace();

    // parse comparator

    addComparatorTypeSuggestions();
    final Comparator comparator;
    if (reader.canRead()) {
      comparator = parseComparator();
    } else {
      throw BlockStateParser.ERROR_EXPECTED_VALUE.createWithContext(reader, this.blockId.toString(), property.getName());
    }
    reader.skipWhitespace();

    // parse valueName
    parsePropertyNameValue(property, comparator);
  }

  @NotNull
  protected Property<?> parseProperty() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    addPropertyNameSuggestions();
    final int cursorBeforeReadString = reader.getCursor();
    // parse block property propertyName
    String propertyName = reader.readString();
    if (propertyName.isEmpty()) {
      final int cursorAfterReadString = reader.getCursor();
      reader.setCursor(cursorBeforeReadString);
      throw CommandSyntaxExceptionExtension.withCursorEnd(BlockStateParser.ERROR_EXPECTED_VALUE.createWithContext(reader, this.blockId.toString(), propertyName), cursorAfterReadString);
    }
    final StateDefinition<Block, BlockState> stateManager = block.getStateDefinition();
    Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) {
      final int cursorAfterReadString = reader.getCursor();
      reader.setCursor(cursorBeforeReadString);
      throw CommandSyntaxExceptionExtension.withCursorEnd(BlockStateParser.ERROR_UNKNOWN_PROPERTY.createWithContext(reader, blockId, propertyName), cursorAfterReadString);
    }
    parseContext.clearSuggestion();
    return property;
  }

  @NotNull
  protected abstract Comparator parseComparator() throws CommandSyntaxException;

  @SuppressWarnings("unchecked")
  protected void addPropertiesFinishedSuggestions() {
    parseContext.addSuggestion((SuggestionProvider<S>) PROPERTY_FINISHED);
  }

  protected abstract void addComparatorTypeSuggestions();

  protected void addPropertyNameSuggestions() {
    parseContext.addSuggestion((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(block.getStateDefinition().getProperties().stream().map(Property::getName), suggestionsBuilder));
  }

  public void parseBlockTagIdAndProperties() throws CommandSyntaxException {
    parseBlockTagId();
    if (tagId != null) {
      parsePropertyNames();
    }
  }

  public void parseBlockTagId() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeHash = reader.getCursor();
    final HolderLookup.RegistryLookup<Block> registryWrapper = parseContext.commandBuildContext().lookupOrThrow(Registries.BLOCK);
    if (reader.canRead() && reader.peek() == '#') {
      reader.skip();

      // start parsing tag id, after the hash symbol
      parseContext.addSuggestion((context, suggestionsBuilder) -> SharedSuggestionProvider.suggestResource(registryWrapper.listTagIds().map(TagKey::location), suggestionsBuilder, "#"));
      ResourceLocation identifier = ResourceLocation.read(reader);
      this.tagId = registryWrapper.get(TagKey.create(Registries.BLOCK, identifier)).orElseThrow(() -> {
        reader.setCursor(cursorBeforeHash);
        return BlockStateParser.ERROR_UNKNOWN_TAG.createWithContext(reader, identifier.toString());
      });
    }
  }

  public void parsePropertyNames() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.setSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("[", START_OF_PROPERTIES);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == '[') {
      reader.skip();
      reader.skipWhitespace();
      parseContext.clearSuggestion();
    } else {
      return;
    }
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("]", END_OF_PROPERTIES);
      }
      return suggestionsBuilder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == ']') {
      reader.skip();
      parseContext.clearSuggestion();
      return;
    }

    while (reader.canRead(-1)) {
      parsePropertyNameEntry();
      reader.skipWhitespace();

      if (parsePropertyEntryEnd()) return;
    }
    throw BlockStateParser.ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(reader);
  }

  protected void parsePropertyNameEntry() throws CommandSyntaxException {
    // parse a property propertyName
    addTagPropertiesNameSuggestions();
    final StringReader reader = parseContext.reader();
    final int cursorBeforePropertyName = reader.getCursor();
    final String propertyName = reader.readString();
    if (propertyName.isEmpty()) {
      reader.setCursor(cursorBeforePropertyName);
      throw BlockStateParser.ERROR_EXPECTED_VALUE.createWithContext(reader, tagId == null ?
          "" : this.tagId.key().location().toString(), propertyName);
    }
    // parse comparator
    reader.skipWhitespace();
    final int cursorBeforeReadingComparator = reader.getCursor();
    reader.setCursor(cursorBeforePropertyName);
    final String remaining = reader.getRemaining();
    if (tagId == null || tagId.stream().flatMap(entry -> entry.value().getStateDefinition().getProperties().stream()).distinct().noneMatch(property -> property.getName().startsWith(remaining) && !property.getName().equals(remaining))) {
      parseContext.clearSuggestion();
      reader.setCursor(cursorBeforeReadingComparator);
      addComparatorTypeSuggestions();
    }

    final Comparator comparator;
    if (reader.canRead()) {
      comparator = parseComparator();
    } else {
      throw BlockStateParser.ERROR_EXPECTED_VALUE.createWithContext(reader, tagId == null ?
          "" : this.tagId.key().location().toString(), propertyName);
    }
    reader.skipWhitespace();

    // parse valueName
    parsePropertyNameValue(propertyName, comparator);
  }

  private void addTagPropertiesNameSuggestions() {
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
      String string = suggestionsBuilder.getRemainingLowerCase();
      if (this.tagId != null) {
        for (Holder<Block> registryEntry : this.tagId) {
          for (Property<?> property : registryEntry.value().getStateDefinition().getProperties()) {
            if (property.getName().startsWith(string)) {
              suggestionsBuilder.suggest(property.getName());
            }
          }
        }
      }
      return suggestionsBuilder.buildFuture();
    });
  }

  protected static <T> SuggestionProvider<T> getTagPropertiesValueSuggestions(@NotNull HolderSet.Named<Block> tagId, String propertyName) {
    return (context, suggestionsBuilder) -> {
      for (Holder<Block> registryEntry : tagId) {
        Block block = registryEntry.value();
        Property<?> property = block.getStateDefinition().getProperty(propertyName);
        if (property != null) {
          suggestValuesForProperty(property, suggestionsBuilder);
        }
      }
      return suggestionsBuilder.buildFuture();
    };
  }

  protected abstract <T extends Comparable<T>> void parsePropertyNameValue(Property<T> property, Comparator comparator) throws CommandSyntaxException;

  protected abstract void parsePropertyNameValue(String propertyName, Comparator comparator) throws CommandSyntaxException;
}
