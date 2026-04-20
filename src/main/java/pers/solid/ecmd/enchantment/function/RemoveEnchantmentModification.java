package pers.solid.ecmd.enchantment.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pers.solid.ecmd.util.ExecutionContext;

public record RemoveEnchantmentModification(EnchantmentModificationTarget enchantment) implements EnchantmentModification {
  public static final MapCodec<RemoveEnchantmentModification> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnchantmentModificationTarget.CODEC.fieldOf("enchantment").forGetter(RemoveEnchantmentModification::enchantment)
  ).apply(i, RemoveEnchantmentModification::new));

  @Override
  public void modify(ItemStack stack, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
    enchantments.removeIf(enchantment.asPredicate(stack, context));
  }

  @Override
  public EnchantmentModificationType<RemoveEnchantmentModification> getType() {
    return EnchantmentModificationTypes.REMOVE;
  }
}
