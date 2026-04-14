package pers.solid.ecmd.block.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.block.SimpleBlockParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.property.function.*;
import pers.solid.ecmd.property.predicate.Comparator;

import java.util.*;

import static pers.solid.ecmd.util.EnhancedCommandSyntaxException.withCursorEnd;

public class SimpleBlockFunctionParser<S> extends SimpleBlockParser<S> {
  public final List<PropertyFunction<?>> propertyFunctions = new ArrayList<>();
  public final Set<Property<?>> mentionedProperties = new HashSet<>();
  public final List<PropertyNameFunction> propertyNameFunctions = new ArrayList<>();
  public final Set<String> mentionedPropertyNames = new HashSet<>();
  public static final SimpleCommandExceptionType DUPLICATE_GENERAL_PROPERTY_FUNCTION = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.block_function.property.duplicate_general"));
  public static final SimpleCommandExceptionType EXHAUSTED_GENERAL_PROPERTIES = new SimpleCommandExceptionType(Component.translatable("enhanced_commands.block_function.property.exhausted_general_properties"));
  /**
   * 使用通用属性（<code>*</code> 或 <code>~</code> 不带具体的属性名称）时，此字段表示需要排除的属性（就是已经被其他属性函数使用了的）。没有使用通用属性时，则为 {@code null}。
   */
  public @Nullable Set<Property<?>> exceptionForGeneralProperty = null;
  /**
   * 使用通用属性（<code>*</code> 或 <code>~</code> 不带具体的属性名称）时，此字段表示需要排除的属性名称（就是已经被其他属性名称函数使用了的）。没有使用通用属性时，则为 {@code null}。
   */
  public @Nullable Set<String> exceptionForGeneralPropertyName = null;
  private boolean must = false;
  private int cursorBeforeGeneralFunction = -1;

  public SimpleBlockFunctionParser(ParseContext<S> parseContext) {
    super(parseContext);
  }


  @Override
  protected Comparator parseComparator() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    if (reader.canRead() && reader.peek() == '=') {
      reader.skip();
      if (reader.canRead() && reader.peek() == '=') {
        reader.skip();
        must = true;
      } else {
        must = false;
      }
    } else {
      throw SimpleBlockParser.COMPARATOR_EXPECTED.createWithContext(reader);
    }
    return Comparator.EQ;
  }

  @Override
  protected <T extends Comparable<T>> void parsePropertyNameValue(Property<T> property, Comparator comparator) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.clearSuggestion();
    addSpecialPropertyValueSuggestions();
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestValuesForProperty(property, suggestionsBuilder));
    if (reader.canRead()) {
      if (reader.peek() == '*') {
        propertyFunctions.add(new RandomPropertyFunction<>(property, must));
        reader.skip();
        parseContext.clearSuggestion();
        return;
      } else if (reader.peek() == '~') {
        propertyFunctions.add(new BypassingPropertyFunction<>(property, must));
        reader.skip();
        parseContext.clearSuggestion();
        return;
      }
    }
    final int cursorBeforeParseValue = reader.getCursor();
    final String valueName = reader.readString();
    final Optional<T> parse = property.getValue(valueName);
    if (parse.isPresent()) {
      propertyFunctions.add(new SimplePropertyFunction<>(property, parse.get(), must));
      parseContext.clearSuggestion();
    } else {
      final int cursorAfterParseValue = reader.getCursor();
      reader.setCursor(cursorBeforeParseValue);
      throw withCursorEnd(BlockStateParser.ERROR_INVALID_VALUE.createWithContext(reader, blockId.toString(), property.getName(), valueName), cursorAfterParseValue);
    }
  }

  private void addSpecialPropertyValueSuggestions() {
    parseContext.addSuggestion((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("*", Component.translatable("enhanced_commands.block_function.random_value"));
        suggestionsBuilder.suggest("~", Component.translatable("enhanced_commands.block_function.originalValue"));
      }
      return suggestionsBuilder.buildFuture();
    });
  }

  @Override
  protected Property<?> parseProperty() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final Property<?> property = super.parseProperty();
    // 增加了一个属性之后，需要检查是否已经有通用属性函数，且该函数是否是多余的（即已被所有其他属性指定）。
    mentionedProperties.add(property);
    if (block != null && exceptionForGeneralProperty != null && exceptionForGeneralProperty.containsAll(block.getStateDefinition().getProperties())) {
      parseContext.clearSuggestion();
      reader.setCursor(cursorBeforeGeneralFunction);
      throw EXHAUSTED_GENERAL_PROPERTIES.createWithContext(reader);
    }
    return property;
  }

  @Override
  protected void parsePropertyNameValue(String propertyName, Comparator comparator) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    mentionedPropertyNames.add(propertyName);
    parseContext.clearSuggestion();
    addSpecialPropertyValueSuggestions();
    if (reader.canRead()) {
      if (reader.peek() == '*') {
        propertyNameFunctions.add(new RandomPropertyNameFunction(propertyName, must));
        reader.skip();
        parseContext.clearSuggestion();
      } else if (reader.peek() == '~') {
        propertyNameFunctions.add(new BypassingPropertyNameFunction(propertyName, must));
        reader.skip();
        parseContext.clearSuggestion();
      }
    }
    final int cursorBeforeValue = reader.getCursor();
    final String valueName = reader.readString();
    final int cursorAfterValue = reader.getCursor();
    if (tagId != null) {
      final SuggestionProvider<S> sp = getTagPropertiesValueSuggestions(tagId, propertyName);
      parseContext.setSuggestion((context, suggestionsBuilder) -> {
        final SuggestionsBuilder offset = suggestionsBuilder.createOffset(cursorBeforeValue);
        final SuggestionsBuilder offset2 = suggestionsBuilder.createOffset(cursorAfterValue);
        @SuppressWarnings("unchecked") final CommandContext<Object> cast = (CommandContext<Object>) context;
        PROPERTY_FINISHED.getSuggestions(cast, offset2);
        return sp.getSuggestions(context, offset).thenCombine(offset2.buildFuture(), (suggestions, suggestions2) -> suggestions.isEmpty() ? suggestions2 : suggestions);
      });
    } else {
      parseContext.clearSuggestion();
      addPropertiesFinishedSuggestions();
    }
    propertyNameFunctions.add(new SimplePropertyNameFunction(propertyName, valueName, must));
    reader.skipWhitespace();
  }

  @Override
  protected void addComparatorTypeSuggestions() {
    parseContext.addSuggestion((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(List.of("=", "=="), suggestionsBuilder));
  }

  @Override
  protected void parsePropertyEntry() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final boolean propertiesExhausted = block != null && mentionedProperties.containsAll(block.getStateDefinition().getProperties());
    if (exceptionForGeneralProperty == null) {
      if (!propertiesExhausted) {
        parseContext.addSuggestion((context, suggestionsBuilder) -> {
          ParsingUtil.suggestString("*", Component.translatable("enhanced_commands.block_function.property.all_random"), suggestionsBuilder);
          ParsingUtil.suggestString("~", Component.translatable("enhanced_commands.block_function.property.all_original"), suggestionsBuilder);
          return suggestionsBuilder.buildFuture();
        });
      }
    }
    if (reader.canRead()) {
      final char peek = reader.peek();
      if (peek == '*' || peek == '~') {
        cursorBeforeGeneralFunction = reader.getCursor();
        if (exceptionForGeneralProperty != null) {
          throw withCursorEnd(DUPLICATE_GENERAL_PROPERTY_FUNCTION.createWithContext(reader), reader.getCursor() + 1);
        } else if (propertiesExhausted) {
          throw withCursorEnd(EXHAUSTED_GENERAL_PROPERTIES.createWithContext(reader), reader.getCursor() + 1);
        }
        reader.skip();
        parseContext.clearSuggestion();
        exceptionForGeneralProperty = mentionedProperties;
        propertyFunctions.add(peek == '*' ? new AllRandomPropertyFunction(exceptionForGeneralProperty) : new AllOriginalPropertyFunction(exceptionForGeneralProperty));
        return;
      }
    }
    super.parsePropertyEntry();
  }

  @Override
  protected void parsePropertyNameEntry() throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    if (exceptionForGeneralPropertyName == null) {
      parseContext.addSuggestion((context, suggestionsBuilder) -> {
        ParsingUtil.suggestString("*", Component.translatable("enhanced_commands.block_function.property.all_random"), suggestionsBuilder);
        ParsingUtil.suggestString("~", Component.translatable("enhanced_commands.block_function.property.all_original"), suggestionsBuilder);
        return suggestionsBuilder.buildFuture();
      });
    }
    if (reader.canRead()) {
      final char peek = reader.peek();
      if (peek == '*' || peek == '~') {
        if (exceptionForGeneralPropertyName != null) {
          throw withCursorEnd(DUPLICATE_GENERAL_PROPERTY_FUNCTION.createWithContext(reader), reader.getCursor() + 1);
        }
        reader.skip();
        parseContext.clearSuggestion();
        exceptionForGeneralPropertyName = mentionedPropertyNames;
        propertyNameFunctions.add(peek == '*' ? new AllRandomPropertyNameFunction(exceptionForGeneralPropertyName) : new AllOriginalPropertyNameFunctions(exceptionForGeneralPropertyName));
        return;
      }
    }
    super.parsePropertyNameEntry();
  }
}
