package pers.solid.ecmd.item.predicate;

import com.mojang.brigadier.ImmutableStringReader;
import net.minecraft.commands.arguments.item.ComponentPredicateParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.commands.ResourceLookupRule;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;
import pers.solid.ecmd.util.extension.ComponentPredicateParserContextExtension;

import java.util.stream.Stream;

public class ReferenceItemPredicateLookupRule<T, C, P> extends ResourceLookupRule<ComponentPredicateParser.Context<T, C, P>, ReferenceItemPredicate> {

  private final @Nullable HolderLookup.RegistryLookup<ItemPredicate> lookup;

  public ReferenceItemPredicateLookupRule(Atom<ResourceLocation> idParser, ComponentPredicateParser.Context<T, C, P> context) {
    super(idParser, context);
    @SuppressWarnings("unchecked") final ComponentPredicateParserContextExtension<T> contextExtension = (ComponentPredicateParserContextExtension<T>) context;
    final HolderLookup.Provider provider = contextExtension.registries$enhanced_commands();

    // 考虑此处可能是在客户端执行
    lookup = provider.lookup(ItemPredicate.REGISTRY_KEY).orElse(null);
  }

  protected ReferenceItemPredicate validateElement(ImmutableStringReader reader, ResourceLocation elementType) throws Exception {
    final ResourceKey<ItemPredicate> resourceKey = ResourceKey.create(ItemPredicate.REGISTRY_KEY, elementType);
    return new ReferenceItemPredicate(lookup == null ? Holder.Reference.createStandAlone(null, resourceKey) : lookup.get(resourceKey).orElseThrow(() -> EnhancedCommandsCommandExceptionTypes.UNKNOWN_ITEM_PREDICATE_ID.createWithContext(reader, resourceKey.location().toString())));
  }

  public Stream<ResourceLocation> possibleResources() {
    return lookup == null ? Stream.empty() : lookup.listElementIds().map(ResourceKey::location);
  }
}
