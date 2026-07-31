package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ReferenceEntry;

public record ReferenceItemFunction(Holder.Reference<ItemFunction> value) implements ItemFunction, ReferenceEntry<ReferenceItemFunction, ItemFunction> {
  public static final MapCodec<ReferenceItemFunction> CODEC = ReferenceEntry.createCodec(ItemFunction.REGISTRY_KEY, ItemFunction.CODEC, ReferenceItemFunction::new);

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    return value().value().getModifiedStack(itemStack, originalStack, context);
  }

  @Override
  public ItemFunctionType<ReferenceItemFunction> getType() {
    return ItemFunctionTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + value.getRegisteredName();
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
    protected ReferenceItemFunction getResultByHolderReference(Holder.Reference<ItemFunction> holderReference) {
      return new ReferenceItemFunction(holderReference);
    }
  }
}
