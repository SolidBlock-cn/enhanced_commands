package pers.solid.ecmd.enchantment.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pers.solid.ecmd.util.ExecutionContext;

public interface EnchantmentModification {
  MapCodec<EnchantmentModification> CODEC = Codec.lazyInitialized(() -> EnchantmentModificationType.CODEC).dispatchMap(EnchantmentModification::getType, EnchantmentModificationType::codec);

  void modify(ItemStack stack, ItemEnchantments.Mutable enchantments, ExecutionContext context);

  EnchantmentModificationType<?> getType();
}
