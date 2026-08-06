package pers.solid.ecmd.item.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public record ReferenceItemPredicate(Holder.Reference<ItemPredicate> reference) implements ItemPredicate, ReferenceEntry<ItemPredicate> {
  public static final MapCodec<ReferenceItemPredicate> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), ItemPredicate.REGISTRY_KEY, ReferenceItemPredicate::new);
  public static final PrefixedIdParser<ReferenceItemPredicate, ItemPredicate> PREFIXED_ID_PARSER = new PrefixedIdParser<>('$', Component.translatable("enhanced_commands.block_predicate.reference"), ItemPredicate.REGISTRY_KEY, ReferenceItemPredicate::new);

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
}
