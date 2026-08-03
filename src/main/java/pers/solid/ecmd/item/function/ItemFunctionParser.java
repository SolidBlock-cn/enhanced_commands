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
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.mixins.accessor.ItemParserAccessor;
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

/**
 * <p>用于解析物品函数语法的类。本模组用此类的解析完全替代原版的 {@link ItemParser}。
 *
 * @see ItemParser
 */
public final class ItemFunctionParser {
  public static final Component OVERLAY_TOOLTIP = Component.translatable("enhanced_commands.function.overlay.symbol_tooltip");
  public static final Component PICK_TOOLTIP = Component.translatable("enhanced_commands.function.pick.symbol_tooltip");
  /**
   * 组件类型的 codec 为 null 时抛出的错误。
   */
  public static final DynamicCommandExceptionType COMPONENT_NOT_SERIALIZABLE = new DynamicCommandExceptionType(componentId -> Component.translatable("enhanced_commands.argument.item.component_not_serializable", componentId));

  /**
   * 组件类型有效，且其值为有效的 NBT，但该 NBT 内容无效（与 codec 不匹配）时抛出的错误。
   */
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

  /**
   * 解析由 “{@code |}” 和 “{@code *}” 所隔开的单元，例如 <code><u>diamond_sword[...]</u>|<u>diamond_axe[...]</u>*<u>pick(...)</u></code> 中画下划线的内容。<br>
   * 语法：{@code base + "[" + componentList + "]"}
   */
  public static <S> ItemFunction parseUnit(ParseContext<S> parseContext) throws CommandSyntaxException {
    final ItemFunction itemFunction = parseBase(parseContext);
    parseContext.addSuggestion((context, builder) -> suggestStartComponents(builder));
    if (parseContext.reader().canRead() && parseContext.reader().peek() == '[') {
      final ImmutableList<ItemFunction> affiliates = parseComponentList(parseContext.withAllowSparse(true));
      return new ItemComponentCombinationItemFunction(itemFunction, affiliates);
    }
    return itemFunction;
  }

  /**
   * 解析物品函数语法中不带中括号部分的内容，即 <code><u>diamond_sword</u>[invulnerable={}]</code> 中画下划线的内容。<br>
   * 语法：{@code "(" 物品函数 ")" | 函数式语法 | "*" | "@" | 物品 ID | "@" + 物品 ID}
   */
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
        builder.suggest("#");
        builder.suggest("$", Component.translatable("enhanced_commands.block_predicate.reference"));
      }
      return SharedSuggestionProvider.suggestResource(itemLookup.listElements(), builder, entry -> entry.key().location(), entry -> entry.value().getDescription());
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
        case '#' -> {
          parseContext.clearSuggestion();
          return parseTag(parseContext);
        }
        case '$' -> {
          reader.skip();
          parseContext.clearSuggestion();
          final ReferenceItemFunction.ReferencePrefixedParser parser = ReferenceItemFunction.ReferencePrefixedParser.INSTANCE;
          final Holder.Reference<ItemFunction> holderReference = parser.parseAndGetReference(parseContext);
          return parser.getResultByReference(holderReference);
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

  /**
   * 解析 “{@code @}” 符号后的 ID，此时实验性内容不受限制。
   */
  private static <S> ItemFunction parseUnlimitedId(ParseContext<S> parseContext) throws CommandSyntaxException {
    parseContext.setSuggestion((context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.entrySet(), builder, entry -> entry.getKey().location(), entry -> entry.getValue().getName(entry.getValue().getDefaultInstance())));

    final StringReader reader = parseContext.reader();
    final int cursorBeforeId = reader.getCursor();
    final ResourceLocation identifier = ResourceLocation.read(reader);
    final int cursorAfterId = reader.getCursor();

    final Optional<Holder.Reference<Item>> itemReference = BuiltInRegistries.ITEM.getHolder(identifier);
    if (itemReference.isEmpty()) {
      reader.setCursor(cursorBeforeId);
      throw EnhancedCommandSyntaxException.withCursorEnd(ItemParserAccessor.getERROR_UNKNOWN_ITEM().createWithContext(reader, identifier), cursorAfterId);
    }

    parseContext.clearSuggestion();
    return new SimpleItemFunction(itemReference.get());
  }

  static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType(tagId -> Component.translatableEscape("arguments.item.tag.unknown", tagId.toString())
  );

  private static <S> ItemFunction parseTag(ParseContext<S> parseContext) throws CommandSyntaxException {
    final HolderLookup.RegistryLookup<Item> lookup = parseContext.registries().lookupOrThrow(Registries.ITEM);
    parseContext.setSuggestion((context, builder) -> suggestTagIds(builder, lookup));
    final StringReader reader = parseContext.reader();
    final int cursorBeforeHash = reader.getCursor();

    reader.expect('#');
    final ResourceLocation tagId = ResourceLocation.read(reader);
    final int cursorAfterId = reader.getCursor();

    final HolderSet<Item> holders = lookup.get(TagKey.create(Registries.ITEM, tagId)).orElseThrow(() -> {
      reader.setCursor(cursorBeforeHash);
      return EnhancedCommandSyntaxException.withCursorEnd(ERROR_UNKNOWN_TAG.createWithContext(reader, tagId), cursorAfterId);
    });
    parseContext.clearSuggestion();
    return new TagItemFunction(holders);
  }

  private static CompletableFuture<Suggestions> suggestTagIds(SuggestionsBuilder builder, HolderLookup.RegistryLookup<Item> lookup) {
    return SharedSuggestionProvider.suggestResource(lookup.listTagIds().map(TagKey::location), builder, "#");
  }

  /**
   * 解析组件列表。组件列表中的每一项可以是修改组件，也可以是其他特殊的内容，参见 {@link #parseComponentListEntry(ParseContext, StringReader, Set, ImmutableList.Builder)}。<br>
   * 语法：{@code [ entry, entry, entry ...] }
   */
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

  /**
   * 解析组件列表中的一个项。项支持“{@code 组件=值}”和“{@code !组件名称}”的原版语法，但也支持一些特殊语法，如{@code 组件:NBT 函数}、函数式语法或被括号括起来的完整物品函数。注意：在组件列表的项中表示一个单独的物品函数时，使用函数式语法的可以直接写，不是函数式语法的必须用括号括起来。<br>
   * 语法：{@code "(" 物品函数 ")" | 函数式语法 | 组件名称 "=" 用 NBT 表示的值 | 组件名称 ":" NBT 函数}
   */
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
      DataComponentType<?> dataComponentType = parseComponentType(parseContext, "");
      if (!usedComponents.add(dataComponentType)) {
        throw ItemParserAccessor.getERROR_REPEATED_COMPONENT().create(dataComponentType);
      }

      affiliates.add(new RemoveComponentItemFunction<>(dataComponentType));
      parseContext.clearSuggestion();
      reader.skipWhitespace();
    } else {
      DataComponentType<?> dataComponentType = parseComponentType(parseContext, "=");
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

  /**
   * 解析物品组件名称，即 <code>iron_sword[<u>enchantments</u>={sharpness: 1}]</code> 中画横线的部分。
   */
  public static <S> DataComponentType<?> parseComponentType(ParseContext<S> parseContext, String componentSuffix) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    parseContext.addSuggestion((context, builder) -> suggestComponentIds(builder, componentSuffix));
    if (!reader.canRead()) {
      throw ItemParserAccessor.getERROR_EXPECTED_COMPONENT().createWithContext(reader);
    }
    final int cursorBeforeComponentType = reader.getCursor();
    final ResourceLocation id = readIdentifierEnhanced(reader);
    final int cursorAfterComponentType = reader.getCursor();
    DataComponentType<?> dataComponentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(id);
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

  /**
   * 解析组件名称和等号后的内容。只有当组件名称后是等号时，才调用此方法解析后面的内容。解析方式与原版一致，会解析 NBT（不是 NBT 函数或 NBT 谓词），并立即使用物品组件的 codec 解析，解析出错时直接抛出。
   */
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

  /**
   * 解析组件名称和冒号后的内容。只有当组件名称后是冒号时，才调用此方法解析后面的内容。会解析 NBT 函数，只有在实际应用物品函数时，此 NBT 函数才会被调用并用物品组件的 codec 解析。
   */
  private static <T, S> ModifyComponentItemFunction<T> parseValueForColonComponent(ParseContext<S> context, DataComponentType<T> componentType) throws CommandSyntaxException {
    final NbtFunction nbtFunction = NbtFunction.parse(context, false, false);
    return new ModifyComponentItemFunction<>(componentType, nbtFunction);
  }

  private static CompletableFuture<Suggestions> suggestComponentAssignmentOrRemoval(SuggestionsBuilder builder) {
    if (builder.getRemaining().isEmpty()) {
      builder.suggest("!", Component.translatable("enhanced_commands.argument.item_function.remove_component"));
    }
    return suggestComponentIds(builder, "=");
  }

  private static CompletableFuture<Suggestions> suggestComponentIds(SuggestionsBuilder builder, String suffix) {
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

  /**
   * <p>相比原版的 {@link ResourceLocation#read(StringReader)} 作了一些改进，包括：第二个冒号不再解析下去；非 ID 可识别字符（如空格、中括号、花括号）前面就是冒号时，不解析这个冒号。这是为了避免出现在组件 ID 后直接接冒号但这个冒号被识别为物品 ID 的情况。
   * <p>使用原版 {@link ResourceLocation#read(StringReader)} 以及使用此方法解析 ID 的对比：
   * <ul>
   *   <li>原版：<code>item_id[<u>component_id:</u>{...}, ...]</code></li>
   *   <li>此方法：<code>item_id[<u>component_id</u>:{...}, ...]</code></li>
   *   <li>原版：<code>item_id[<u>namespace:component_id:nbt_function</u>, ...]</code></li>
   *   <li>此方法：<code>item_id[<u>namespace:component_id</u>:nbt_function, ...]</code></li>
   * </ul>
   * <p>
   * 注意：如果 NBT 函数是直接用字母等可用作 ID 的字符表示的，且物品组件 ID 中也没有指定命令空间，那么冒号和后面的内容仍会被识别为组件 ID，这种情况可通过添加空格来避免，例如：
   * <ul>
   *   <li>此方法：<code>item_id[<u>component_id:nbt_function</u>, ...]</code></li>
   *   <li>此方法：<code>item_id[<u>component_id</u>: nbt_function, ...]</code></li>
   * </ul>
   *
   * @see ResourceLocation#read(StringReader)
   */
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

  /**
   * @see ResourceLocation#readGreedy(StringReader)
   */
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
