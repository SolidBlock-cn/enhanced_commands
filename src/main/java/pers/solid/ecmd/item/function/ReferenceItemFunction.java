package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;
import pers.solid.ecmd.util.pack.RequiresValidation;

public record ReferenceItemFunction(Holder.Reference<ItemFunction> reference) implements ItemFunction, ReferenceEntry<ReferenceItemFunction, ItemFunction> {
  public static final MapCodec<ReferenceItemFunction> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), ItemFunction.REGISTRY_KEY, ReferenceItemFunction::new);

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    return reference().value().getModifiedStack(itemStack, originalStack, context);
  }

  @Override
  public ItemFunctionType<ReferenceItemFunction> getType() {
    return ItemFunctionTypes.REFERENCE;
  }

  @Override
  public Iterable<? extends RequiresValidation> membersToValidate() {
    return ReferenceEntry.super.membersToValidate();
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(reference.key().location());
  }

  @Override
  public ResourceKey<? extends Registry<ItemFunction>> registryKey() {
    return ItemFunction.REGISTRY_KEY;
  }

  public static class ReferencePrefixedParser extends PrefixedIdParser<ReferenceItemFunction, ItemFunction> {
    public static final ReferencePrefixedParser INSTANCE = new ReferencePrefixedParser();

    protected ReferencePrefixedParser() {
      super('$', Component.translatable("enhanced_commands.block_predicate.reference"), ItemFunction.REGISTRY_KEY);
    }

    @Override
    protected ReferenceItemFunction getResultByReference(Holder.Reference<ItemFunction> holderReference) {
      return new ReferenceItemFunction(holderReference);
    }
  }
}
