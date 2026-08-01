package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;
import pers.solid.ecmd.util.pack.SafeReference;

public record ReferenceItemFunction(SafeReference<ItemFunction> reference) implements ItemFunction, ReferenceEntry<ReferenceItemFunction, ItemFunction> {
  public static final MapCodec<ReferenceItemFunction> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), ItemFunction.REGISTRY_KEY, ReferenceItemFunction::new);

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    return reference().valueOrThrow(context).getModifiedStack(itemStack, originalStack, context);
  }

  @Override
  public ItemFunctionType<ReferenceItemFunction> getType() {
    return ItemFunctionTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(reference.identifier());
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
    protected ReferenceItemFunction getResultByReference(SafeReference<ItemFunction> holderReference) {
      return new ReferenceItemFunction(holderReference);
    }
  }
}
