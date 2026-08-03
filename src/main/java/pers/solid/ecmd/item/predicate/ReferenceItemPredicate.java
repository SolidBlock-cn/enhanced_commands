package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public record ReferenceItemPredicate(Holder.Reference<ItemPredicate> reference) implements ItemPredicate, ReferenceEntry<ReferenceItemPredicate, ItemPredicate> {
  public static final MapCodec<ReferenceItemPredicate> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), ItemPredicate.REGISTRY_KEY, ReferenceItemPredicate::new);

  @Override
  public boolean test(ItemStack stack, ExecutionContext executionContext) {
    return reference().value().test(stack, executionContext);
  }

  @Override
  public ItemPredicateType<ReferenceItemPredicate> getType() {
    return ItemPredicateTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(reference.key().location());
  }

  @Override
  public ResourceKey<? extends Registry<ItemPredicate>> registryKey() {
    return ItemPredicate.REGISTRY_KEY;
  }

  public static class ReferencePrefixedParser extends PrefixedIdParser<ReferenceItemPredicate, ItemPredicate> {
    public static final ReferenceItemPredicate.ReferencePrefixedParser INSTANCE = new ReferenceItemPredicate.ReferencePrefixedParser();

    protected ReferencePrefixedParser() {
      super('$', Component.translatable("enhanced_commands.block_predicate.reference"), ItemPredicate.REGISTRY_KEY);
    }

    @Override
    protected ReferenceItemPredicate getResultByReference(Holder.Reference<ItemPredicate> holderReference) {
      return new ReferenceItemPredicate(holderReference);
    }
  }
}
