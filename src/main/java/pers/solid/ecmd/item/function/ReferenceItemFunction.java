package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public record ReferenceItemFunction(Holder.Reference<ItemFunction> reference) implements ItemFunction, ReferenceEntry<ItemFunction> {
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
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return ReferenceEntry.super.membersToValidate();
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(reference.key().location());
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
