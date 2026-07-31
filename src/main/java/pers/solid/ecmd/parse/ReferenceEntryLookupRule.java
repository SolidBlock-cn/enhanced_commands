package pers.solid.ecmd.parse;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ComponentPredicateParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.commands.ResourceLocationParseRule;
import net.minecraft.util.parsing.packrat.commands.ResourceLookupRule;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.extension.ComponentPredicateParserContextExtension;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * 此类用于在 packrat 中解析可在数据包中数据驱动的内容的 ID，且进行了拓展，支持指定 {@link DefaultNamespace} 以处理不同的默认命名空间。由于数据包内容只有服务器知道，因此在客户端运行时会从服务器申请数据以获取命令建议。实现了 {@link EnhancedSuggestionSupplier} 以提供更高级的命令建议。
 *
 * @param <E> 可数据驱动内容的类型，同时也是 {@link #registryKey} 的类型参数
 */
public class ReferenceEntryLookupRule<E, T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, Holder.Reference<E>> implements EnhancedSuggestionSupplier {
  private final @Nullable HolderLookup.RegistryLookup<E> lookup;
  /**
   * 此类所代表的可数据驱动内容的注册表。
   */
  private final ResourceKey<Registry<E>> registryKey;
  /**
   * 在解析和提供建议时所使用的默认命名空间。注意：参数 {@code idParser} 也应当使用相同默认命名空间的 {@link IdWithDefaultNamespaceParseRule} 所对应的 atom，而非原版的 {@link ResourceLocationParseRule} 所对应的 atom。
   */
  private final DefaultNamespace defaultNamespace;

  /**
   * 创建以本模组命名空间（{@code enhanced_commands}）为默认命名空间的 lookupRule。
   *
   * @param idParser    解析 ID 所使用的 atom，注意应当是使用相同默认命名空间的 {@link IdWithDefaultNamespaceParseRule} 所对应的 atom，而非原版 {@link ResourceLocationParseRule} 所对应的 atom。
   * @param registryKey 此类所代表的可数据驱动内容的注册表。
   */
  public ReferenceEntryLookupRule(Atom<ResourceLocation> idParser, ComponentPredicateParser.Context<T, C, P> context, ResourceKey<Registry<E>> registryKey) {
    this(idParser, context, registryKey, DefaultNamespace.ENHANCED_COMMANDS);
  }

  /**
   * 创建以指定命名空间为默认命名空间的 lookupRule。
   *
   * @param idParser         解析 ID 所使用的 atom，注意应当是使用相同默认命名空间的 {@link IdWithDefaultNamespaceParseRule} 所对应的 atom，而非原版 {@link ResourceLocationParseRule} 所对应的 atom。
   * @param registryKey      此类所代表的可数据驱动内容的注册表。
   * @param defaultNamespace 此 lookupRule 所使用的默认命名空间，应当与 {@code idParser} 参数的 atom 所对应的规则所使用的默认命名空间相对应。
   */
  public ReferenceEntryLookupRule(Atom<ResourceLocation> idParser, ComponentPredicateParser.Context<T, C, P> context, ResourceKey<Registry<E>> registryKey, DefaultNamespace defaultNamespace) {
    super(idParser, context);
    this.registryKey = registryKey;
    this.defaultNamespace = defaultNamespace;
    @SuppressWarnings("unchecked") final ComponentPredicateParserContextExtension<T> contextExtension = (ComponentPredicateParserContextExtension<T>) context;
    final HolderLookup.Provider provider = contextExtension.registries$enhanced_commands();

    // 考虑此处可能是在客户端执行，lookup 可能是 null
    lookup = provider.lookup(registryKey).orElse(null);
  }

  protected Holder.Reference<E> validateElement(ImmutableStringReader reader, ResourceLocation elementType) throws Exception {
    final ResourceKey<E> resourceKey = ResourceKey.create(registryKey, elementType);

    return lookup == null ? Holder.Reference.createStandAlone(null, resourceKey) : lookup.get(resourceKey).orElseThrow(() -> EnhancedCommandsCommandExceptionTypes.UNKNOWN_ITEM_PREDICATE_ID.createWithContext(reader, resourceKey.location().toString()));
  }

  @Override
  public Optional<Holder.Reference<E>> parse(ParseState<StringReader> parseState) {
    final int markBeforeParse = parseState.mark();
    final Optional<Holder.Reference<E>> result = super.parse(parseState);
    // 即使正常返回，也需要向 errorCollector 添加建议，以确保在没有解析到错误时，也提供建议。
    if (result.isPresent()) {
      parseState.errorCollector().store(parseState.mark(), (EnhancedSuggestionSupplier) builder -> this.forceGetSuggestionsUnchecked(builder.createOffset(markBeforeParse)), Unit.INSTANCE);
    }
    return result;
  }

  public Stream<ResourceLocation> possibleResources() {
    return Stream.empty();
  }

  @Override
  public Stream<String> possibleValues(ParseState<StringReader> parseState) {
    return Stream.empty();
  }

  @Override
  public CompletableFuture<Suggestions> forceGetSuggestionsUnchecked(SuggestionsBuilder builder) {
    final CommandContext<?> commandContext = MixinShared.commandContextForPackrat;
    final Object source = commandContext == null ? null : commandContext.getSource();

    if (lookup != null) {
      return defaultNamespace.suggestIdentifiers(lookup.listElementIds().map(ResourceKey::location), builder);
    } else if (source instanceof SharedSuggestionProvider sharedSuggestionProvider) {
      // 如果 lookup 为 null，可能是因为是客户端，也可能是因为服务器本就没有这个注册表。
      // 服务器调用 customSuggestion 通常不进行任何操作。
      return sharedSuggestionProvider.customSuggestion(commandContext);
    } else {
      return Suggestions.empty();
    }
  }
}
