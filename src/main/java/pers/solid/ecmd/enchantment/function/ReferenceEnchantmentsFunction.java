package pers.solid.ecmd.enchantment.function;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pers.solid.ecmd.util.DefaultNamespace;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.pack.ReferenceEntry;

public record ReferenceEnchantmentsFunction(Holder.Reference<EnchantmentsFunction> reference) implements EnchantmentsFunction, ReferenceEntry<EnchantmentsFunction> {
  public static final MapCodec<ReferenceEnchantmentsFunction> CODEC = ReferenceEntry.createCodec(DefaultNamespace.ENHANCED_COMMANDS.idCodec(true), EnchantmentsFunction.REGISTRY_KEY, ReferenceEnchantmentsFunction::new);

  @Override
  public void modify(ItemStack stack, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
    reference.value().modify(stack, enchantments, context);
  }

  @Override
  public EnchantmentModificationType<ReferenceEnchantmentsFunction> getType() {
    return EnchantmentModificationTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(reference.key().location());
  }
}
