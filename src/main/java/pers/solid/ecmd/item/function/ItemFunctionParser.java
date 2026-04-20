package pers.solid.ecmd.item.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.ResourceLocationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.mixins.ItemParserAccessor;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class ItemFunctionParser {
  public static final Component OVERLAY_TOOLTIP = Component.translatable("enhanced_commands.function.overlay.symbol_tooltip");
  public static final Component PICK_TOOLTIP = Component.translatable("enhanced_commands.function.pick.symbol_tooltip");
  public static final DynamicCommandExceptionType COMPONENT_NOT_SERIALIZABLE = new DynamicCommandExceptionType(componentId -> Component.translatable("enhanced_commands.argument.item.component_not_serializable", componentId));
  public static final Dynamic2CommandExceptionType COMPONENT_VALUE_INVALID = new Dynamic2CommandExceptionType((componentId, invalidReason) -> Component.translatable("enhanced_commands.argument.item.component_value_invalid", componentId, invalidReason));

  private ItemFunctionParser() {
  }

  public static ItemFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return parsePick(parseContext);
  }

  public static ItemFunction parsePick(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseOverlay(parseContext), functions -> {
      ImmutableList.Builder<ItemFunction> builder = new ImmutableList.Builder<>();
      for (ItemFunction function : functions) {
        builder.add(function);
      }
      return new PickItemFunction(new WeightedList.Uniform<>(builder.build()));
    }, "|", PICK_TOOLTIP, parseContext);
  }

  public static ItemFunction parseOverlay(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseUnit(parseContext), functions -> {
      ImmutableList.Builder<ItemFunction> builder = new ImmutableList.Builder<>();
      for (ItemFunction blockFunction : functions) {
        builder.add(blockFunction);
      }
      return new OverlayItemFunction(builder.build());
    }, "*", OVERLAY_TOOLTIP, parseContext);
  }

  public static <S> ItemFunction parseUnit(ParseContext<S> parseContext) throws CommandSyntaxException {
    final ItemFunction itemFunction = parseBase(parseContext);
    parseContext.addSuggestion((context, builder) -> suggestStartComponents(builder));
    if (parseContext.reader().canRead() && parseContext.reader().peek() == '[') {
      final ImmutableList<ItemFunction> affiliates = parseComponentList(parseContext.withAllowSparse(true));
      return new ItemComponentCombinationItemFunction(itemFunction, affiliates);
    }
    return itemFunction;
  }

  public static <S> ItemFunction parseBase(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();

    final HolderLookup.RegistryLookup<Item> itemLookup = parseContext.registries().lookupOrThrow(Registries.ITEM);

    final int cursorStart = reader.getCursor();
    final @Nullable ItemFunction parseParentheses = ParsingUtil.parseParentheses(() -> parse(parseContext), parseContext);
    if (parseParentheses != null) {
      return parseParentheses;
    }
    final @Nullable ItemFunction functionsGrammar = ItemFunctionParsing.FUNCTIONS_PARSER.parse(parseContext);
    if (functionsGrammar != null) {
      return functionsGrammar;
    } else {
      reader.setCursor(cursorStart);
    }

    parseContext.addSuggestion((context, builder) -> {
      if (builder.getRemaining().isEmpty()) {
        builder.suggest("*");
        builder.suggest("@");
      }
      return SharedSuggestionProvider.suggestResource(itemLookup.listElements(), builder, entry -> entry.key().location(), entry -> entry.value().getName());
    });

    if (reader.canRead()) {
      switch (reader.peek()) {
        case '*' -> {
          reader.skip();
          parseContext.clearSuggestion();
          return new RandomItemFunction();
        }
        case '@' -> {
          reader.skip();
          parseContext.clearSuggestion();
          return parseUnlimitedId(parseContext);
        }
      }
    }

    final int cursorBeforeId = reader.getCursor();
    final ResourceLocation identifier = ResourceLocation.read(reader);
    final int cursorAfterId = reader.getCursor();

    final Optional<Holder.Reference<Item>> itemReference = itemLookup.get(ResourceKey.create(Registries.ITEM, identifier));
    if (itemReference.isEmpty()) {
      reader.setCursor(cursorBeforeId);
      throw EnhancedCommandSyntaxException.withCursorEnd(ItemParserAccessor.getERROR_UNKNOWN_ITEM().createWithContext(reader, identifier), cursorAfterId);
    }

    parseContext.clearSuggestion();
    return new SimpleItemFunction(itemReference.get());
  }

  private static <S> ItemFunction parseUnlimitedId(ParseContext<S> parseContext) throws CommandSyntaxException {
    parseContext.setSuggestion((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.entrySet(), builder, entry -> entry.getKey().location(), entry -> entry.getValue().getName(entry.getValue().getDefaultInstance())));

    final StringReader reader = parseContext.reader();
    final int cursorBeforeId = reader.getCursor();
    final ResourceLocation identifier = ResourceLocation.read(reader);
    final int cursorAfterId = reader.getCursor();

    final Optional<Holder.Reference<Item>> itemReference = BuiltInRegistries.ITEM.get(identifier);
    if (itemReference.isEmpty()) {
      reader.setCursor(cursorBeforeId);
      throw EnhancedCommandSyntaxException.withCursorEnd(ItemParserAccessor.getERROR_UNKNOWN_ITEM().createWithContext(reader, identifier), cursorAfterId);
    }

    parseContext.clearSuggestion();
    return new SimpleItemFunction(itemReference.get());
  }

  private static <S> ImmutableList<ItemFunction> parseComponentList(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    reader.expect('[');

    parseContext.setSuggestion((context, builder) -> suggestComponentAssignmentOrRemoval(builder));
    final Set<DataComponentType<?>> usedComponents = new ReferenceArraySet<>();
    final ImmutableList.Builder<ItemFunction> affiliates = new ImmutableList.Builder<>();

    while (reader.canRead() && reader.peek() != ']') {
      reader.skipWhitespace();

      parseComponentListEntry(parseContext, reader, usedComponents, affiliates);

      parseContext.setSuggestion((context, builder) -> suggestNextOrEndComponents(builder));
      if (!reader.canRead() || reader.peek() != ',') {
        break;
      }

      reader.skip();
      reader.skipWhitespace();
      parseContext.setSuggestion((context, builder) -> suggestComponentAssignmentOrRemoval(builder));
      if (!reader.canRead()) {
        throw ItemParserAccessor.getERROR_EXPECTED_COMPONENT().createWithContext(reader);
      }
    }

    reader.expect(']');
    parseContext.clearSuggestion();

    return affiliates.build();
  }

  private static <S> void parseComponentListEntry(ParseContext<S> parseContext, StringReader reader, Set<DataComponentType<?>> usedComponents, ImmutableList.Builder<ItemFunction> affiliates) throws CommandSyntaxException {
    final @Nullable ItemFunction parenthesesParsed = ParsingUtil.parseParentheses(() -> parse(parseContext), parseContext);
    if (parenthesesParsed != null) {
      affiliates.add(parenthesesParsed);
      return;
    }

    final int storeCursor = parseContext.reader().getCursor();
    final @Nullable ItemFunction functionGrammarResult = ItemFunctionParsing.FUNCTIONS_PARSER.parse(parseContext);
    if (functionGrammarResult != null) {
      affiliates.add(functionGrammarResult);
      return;
    }
    parseContext.reader().setCursor(storeCursor);

    if (reader.canRead() && reader.peek() == '!') {
      reader.skip();
      DataComponentType<?> dataComponentType = parseComponentType(parseContext);
      if (!usedComponents.add(dataComponentType)) {
        throw ItemParserAccessor.getERROR_REPEATED_COMPONENT().create(dataComponentType);
      }

      affiliates.add(new RemoveComponentItemFunction<>(dataComponentType));
      parseContext.clearSuggestion();
      reader.skipWhitespace();
    } else {
      DataComponentType<?> dataComponentType = parseComponentType(parseContext);
      if (!usedComponents.add(dataComponentType)) {
        throw ItemParserAccessor.getERROR_REPEATED_COMPONENT().create(dataComponentType);
      }

      parseContext.setSuggestion((context, builder) -> suggestEqualOrColon(builder));
      reader.skipWhitespace();
      if (reader.canRead() && reader.peek() == '=') {
        reader.skip();
        parseContext.clearSuggestion();
        reader.skipWhitespace();
        affiliates.add(parseValueForEqualComponent(parseContext, dataComponentType));
      } else if (reader.canRead() && reader.peek() == ':') {
        reader.skip();
        parseContext.clearSuggestion();
        reader.skipWhitespace();
        affiliates.add(parseValueForColonComponent(parseContext, dataComponentType));
      } else {
        throw EnhancedCommandsCommandExceptionTypes.EXPECTED_2_SYMBOLS.createWithContext(reader, "=", ",");
      }
      reader.skipWhitespace();
    }
  }

  public static <S> DataComponentType<?> parseComponentType(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.addSuggestion((context, builder) -> suggestComponent(builder));
    if (!reader.canRead()) {
      throw ItemParserAccessor.getERROR_EXPECTED_COMPONENT().createWithContext(reader);
    }
    final int cursorBeforeComponentType = reader.getCursor();
    final ResourceLocation id = readIdentifierEnhanced(reader);
    final int cursorAfterComponentType = reader.getCursor();
    DataComponentType<?> dataComponentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id);
    if (dataComponentType == null) {
      reader.setCursor(cursorBeforeComponentType);
      throw EnhancedCommandSyntaxException.withCursorEnd(ItemParserAccessor.getERROR_UNKNOWN_COMPONENT().createWithContext(reader, id), cursorAfterComponentType);
    } else if (dataComponentType.isTransient()) {
      reader.setCursor(cursorBeforeComponentType);
      throw EnhancedCommandSyntaxException.withCursorEnd(COMPONENT_NOT_SERIALIZABLE.createWithContext(reader, id.toString()), cursorAfterComponentType);
    } else {
      return dataComponentType;
    }
  }

  private static <T, S> SetComponentItemFunction<T> parseValueForEqualComponent(ParseContext<S> context, DataComponentType<T> componentType) throws CommandSyntaxException {
    final StringReader reader = context.reader();
    int cursorBeforeValue = reader.getCursor();
    Tag tag = new TagParser(reader).readValue();
    final int cursorAfterValue = reader.getCursor();
    DataResult<T> dataResult = componentType.codecOrThrow().parse(context.registries().createSerializationContext(NbtOps.INSTANCE), tag);
    final T value = dataResult.getOrThrow((string) -> {
      reader.setCursor(cursorBeforeValue);
      return EnhancedCommandSyntaxException.withCursorEnd(COMPONENT_VALUE_INVALID.createWithContext(reader, componentType.toString(), string), cursorAfterValue);
    });
    return new SetComponentItemFunction<>(componentType, value);
  }

  private static <T, S> ModifyComponentItemFunction<T> parseValueForColonComponent(ParseContext<S> context, DataComponentType<T> componentType) throws CommandSyntaxException {
    final NbtFunction nbtFunction = NbtFunction.parse(context, false, false);
    return new ModifyComponentItemFunction<>(componentType, nbtFunction);
  }

  private static CompletableFuture<Suggestions> suggestComponentAssignmentOrRemoval(SuggestionsBuilder builder) {
    if (builder.getRemaining().isEmpty()) {
      builder.suggest(String.valueOf('!'));
    }
    return suggestComponent(builder, String.valueOf('='));
  }

  private static CompletableFuture<Suggestions> suggestComponent(SuggestionsBuilder builder) {
    return suggestComponent(builder, "");
  }

  private static CompletableFuture<Suggestions> suggestComponent(SuggestionsBuilder builder, String suffix) {
    String string = builder.getRemaining().toLowerCase(Locale.ROOT);
    final Consumer<Map.Entry<ResourceKey<DataComponentType<?>>, DataComponentType<?>>> resourceConsumer = (entry) -> {
      DataComponentType<?> dataComponentType = entry.getValue();
      if (dataComponentType.codec() != null) {
        ResourceLocation id = entry.getKey().location();
        builder.suggest(id + suffix);
      }

    };
    SharedSuggestionProvider.filterResources(BuiltInRegistries.DATA_COMPONENT_TYPE.entrySet(), string, (entry) -> (entry.getKey()).location(), MixinShared.getModifiedConsumer(
        entry -> entry.getKey().location(),
        resourceConsumer,
        builder.getRemaining(),
        (identifier, entry) -> builder.suggest(identifier.getPath() + suffix)

    ));
    return builder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestEqualOrColon(SuggestionsBuilder builder) {
    if (builder.getRemaining().isEmpty()) {
      builder.suggest("=", Component.translatable("enhanced_commands.argument.item_function.set_component_value"));
      builder.suggest(":", Component.translatable("enhanced_commands.argument.item_function.modify_component_value"));
    }
    return builder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestStartComponents(SuggestionsBuilder builder) {
    if (builder.getRemaining().isEmpty()) {
      builder.suggest("[", Component.translatable("enhanced_commands.argument.item_function.start_component_modification"));
    }
    return builder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestNextOrEndComponents(SuggestionsBuilder builder) {
    if (builder.getRemaining().isEmpty()) {
      builder.suggest(",", Component.translatable("enhanced_commands.argument.item_function.separate_component_modification"));
      builder.suggest("]", Component.translatable("enhanced_commands.argument.item_function.end_component_modification"));
    }
    return builder.buildFuture();
  }

  public static ResourceLocation readIdentifierEnhanced(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeIdentifier = reader.getCursor();
    final String string = readIdentifierGreedyEnhanced(reader);

    try {
      return ResourceLocation.parse(string);
    } catch (ResourceLocationException var4) {
      reader.setCursor(cursorBeforeIdentifier);
      throw ResourceLocation.ERROR_INVALID.createWithContext(reader);
    }
  }

  private static String readIdentifierGreedyEnhanced(StringReader reader) {
    final int cursorBeforeIdentifier = reader.getCursor();
    int colonCount = 0;
    while (reader.canRead() && ResourceLocation.isAllowedInResourceLocation(reader.peek())) {
      if (reader.peek() == ':') {
        colonCount++;
        if (colonCount > 1) {
          break;
        }
      }
      reader.skip();
    }

    int cursorAfterIdentifier = reader.getCursor();
    if (reader.peek(-1) == ':' && reader.canRead() && !ResourceLocation.isAllowedInResourceLocation(reader.peek())) {
      cursorAfterIdentifier -= 1;
      reader.setCursor(cursorAfterIdentifier);
    }

    return reader.getString().substring(cursorBeforeIdentifier, cursorAfterIdentifier);
  }
}
