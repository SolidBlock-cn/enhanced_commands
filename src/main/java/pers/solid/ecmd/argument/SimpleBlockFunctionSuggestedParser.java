package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.function.property.*;
import pers.solid.ecmd.predicate.property.Comparator;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.*;

import static pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension.withCursorEnd;

public class SimpleBlockFunctionSuggestedParser extends SimpleBlockSuggestedParser {
  public final List<PropertyFunction<?>> propertyFunctions = new ArrayList<>();
  public final Set<Property<?>> mentionedProperties = new HashSet<>();
  public final List<PropertyNameFunction> propertyNameFunctions = new ArrayList<>();
  public final Set<String> mentionedPropertyNames = new HashSet<>();
  public static final SimpleCommandExceptionType DUPLICATE_GENERAL_PROPERTY_FUNCTION = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_function.property.duplicate_general"));
  public static final SimpleCommandExceptionType EXHAUSTED_GENERAL_PROPERTIES = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_function.property.exhausted_general_properties"));
  /**
   * 使用通用属性（<code>*</code> 或 <code>~</code> 不带具体的属性名称）时，此字段表示需要排除的属性（就是已经被其他属性函数使用了的）。没有使用通用属性时，则为 {@code null}。
   */
  public @Nullable Collection<Property<?>> exceptionForGeneralProperty = null;
  /**
   * 使用通用属性（<code>*</code> 或 <code>~</code> 不带具体的属性名称）时，此字段表示需要排除的属性名称（就是已经被其他属性名称函数使用了的）。没有使用通用属性时，则为 {@code null}。
   */
  public @Nullable Collection<String> exceptionForGeneralPropertyName = null;
  private boolean must = false;
  private int cursorBeforeGeneralFunction = -1;

  public SimpleBlockFunctionSuggestedParser(CommandRegistryAccess commandRegistryAccess, StringReader reader) {
    super(commandRegistryAccess, reader, commandRegistryAccess.createWrapper(RegistryKeys.BLOCK));
  }

  public SimpleBlockFunctionSuggestedParser(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
    this(commandRegistryAccess, parser.reader);
    this.suggestionProviders = parser.suggestionProviders;
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
  protected <T extends Comparable<T>> void parsePropertyNameValue(Property<T> property, Comparator comparator) throws CommandSyntaxException {
    suggestionProviders.clear();
    addSpecialPropertyValueSuggestions();
    suggestionProviders.add((context, suggestionsBuilder) -> suggestValuesForProperty(property, suggestionsBuilder));
    if (reader.canRead()) {
      if (reader.peek() == '*') {
        propertyFunctions.add(new RandomPropertyFunction<>(property, must));
        reader.skip();
        suggestionProviders.clear();
        return;
      } else if (reader.peek() == '~') {
        propertyFunctions.add(new BypassingPropertyFunction<>(property, must));
        reader.skip();
        suggestionProviders.clear();
        return;
      }
      final int cursorBeforeParseValue = reader.getCursor();
      final String valueName = reader.readString();
      final Optional<T> parse = property.parse(valueName);
      if (parse.isPresent()) {
        propertyFunctions.add(new SimplePropertyFunction<>(property, parse.get(), must));
        suggestionProviders.clear();
      } else {
        final int cursorAfterParseValue = reader.getCursor();
        this.reader.setCursor(cursorBeforeParseValue);
        throw withCursorEnd(BlockArgumentParser.INVALID_PROPERTY_EXCEPTION.createWithContext(this.reader, blockId.toString(), property.getName(), valueName), cursorAfterParseValue);
      }
    }
  }

  private void addSpecialPropertyValueSuggestions() {
    suggestionProviders.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("*", Text.translatable("enhancedCommands.argument.block_function.randomValue"));
        suggestionsBuilder.suggest("~", Text.translatable("enhancedCommands.argument.block_function.originalValue"));
      }
    });
  }

  @Override
  protected @NotNull Property<?> parseProperty() throws CommandSyntaxException {
    final Property<?> property = super.parseProperty();
    // 增加了一个属性之后，需要检查是否已经有通用属性函数，且该函数是否是多余的（即已被所有其他属性指定）。
    mentionedProperties.add(property);
    if (block != null && exceptionForGeneralProperty != null && exceptionForGeneralProperty.containsAll(block.getStateManager().getProperties())) {
      suggestionProviders.clear();
      reader.setCursor(cursorBeforeGeneralFunction);
      throw EXHAUSTED_GENERAL_PROPERTIES.createWithContext(reader);
    }
    return property;
  }

  @Override
  protected int parsePropertyNameValue(String propertyName, Comparator comparator) throws CommandSyntaxException {
    mentionedPropertyNames.add(propertyName);
    suggestionProviders.clear();
    addSpecialPropertyValueSuggestions();
    if (reader.canRead()) {
      if (reader.peek() == '*') {
        propertyNameFunctions.add(new RandomPropertyNameFunction(propertyName, must));
        reader.skip();
        suggestionProviders.clear();
        return -1;
      } else if (reader.peek() == '~') {
        propertyNameFunctions.add(new BypassingPropertyNameFunction(propertyName, must));
        reader.skip();
        suggestionProviders.clear();
        return -1;
      }
    }
    final int cursorBeforeValue = reader.getCursor();
    final String valueName = this.reader.readString();
    addTagPropertiesValueSuggestions(propertyName);
    final boolean expectEndOfValue = tagId == null || reader.canRead() || tagId.stream().flatMap(entry -> entry.value().getStateManager().getProperties().stream().filter(property -> property.getName().equals(propertyName))).flatMap(SimpleBlockPredicateSuggestedParser::getPropertyValueNameStream).noneMatch(value -> value.startsWith(valueName) && !value.equals(valueName));
    if (expectEndOfValue && !valueName.isEmpty()) {
      suggestionProviders.clear();
    }
    addPropertiesFinishedSuggestions();
    propertyNameFunctions.add(new SimplePropertyNameFunction(propertyName, valueName, must));
    reader.skipWhitespace();
    return expectEndOfValue ? -1 : cursorBeforeValue;
  }

  @Override
  protected void addComparatorTypeSuggestions() {
    suggestionProviders.add((context, suggestionsBuilder) -> CommandSource.suggestMatching(List.of("=", "=="), suggestionsBuilder));
  }

  @Override
  protected void parsePropertyEntry() throws CommandSyntaxException {
    final boolean propertiesExhausted = block != null && mentionedProperties.containsAll(block.getStateManager().getProperties());
    if (exceptionForGeneralProperty == null) {
      if (!propertiesExhausted) {
        suggestionProviders.add((context, suggestionsBuilder) -> {
          ParsingUtil.suggestString("*", Text.translatable("enhancedCommands.argument.block_function.property.all_random"), suggestionsBuilder);
          ParsingUtil.suggestString("~", Text.translatable("enhancedCommands.argument.block_function.property.all_original"), suggestionsBuilder);
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
        suggestionProviders.clear();
        exceptionForGeneralProperty = mentionedProperties;
        propertyFunctions.add(peek == '*' ? new AllRandomPropertyFunction(exceptionForGeneralProperty) : new AllOriginalPropertyFunction(exceptionForGeneralProperty));
        return;
      }
    }
    super.parsePropertyEntry();
  }

  @Override
  protected boolean parsePropertyNameEntry() throws CommandSyntaxException {
    if (exceptionForGeneralPropertyName == null) {
      suggestionProviders.add((context, suggestionsBuilder) -> {
        ParsingUtil.suggestString("*", Text.translatable("enhancedCommands.argument.block_function.property.all_random"), suggestionsBuilder);
        ParsingUtil.suggestString("~", Text.translatable("enhancedCommands.argument.block_function.property.all_original"), suggestionsBuilder);
      });
    }
    if (reader.canRead()) {
      final char peek = reader.peek();
      if (peek == '*' || peek == '~') {
        if (exceptionForGeneralPropertyName != null) {
          throw withCursorEnd(DUPLICATE_GENERAL_PROPERTY_FUNCTION.createWithContext(reader), reader.getCursor() + 1);
        }
        reader.skip();
        suggestionProviders.clear();
        exceptionForGeneralPropertyName = mentionedPropertyNames;
        propertyNameFunctions.add(peek == '*' ? new AllRandomPropertyNameFunction(exceptionForGeneralPropertyName) : new AllOriginalPropertyNameFunctions(exceptionForGeneralPropertyName));
        return false;
      }
    }
    return super.parsePropertyNameEntry();
  }
}
