package pers.solid.ecmd.enchantment.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pers.solid.ecmd.util.ExecutionContext;

public record AddEnchantmentModification(EnchantmentModificationTarget enchantment, EnchantmentLevelProvider level, boolean checkValidity) implements EnchantmentModification {
  public static final MapCodec<AddEnchantmentModification> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnchantmentModificationTarget.CODEC.fieldOf("enchantment").forGetter(AddEnchantmentModification::enchantment),
      EnchantmentLevelProvider.CODEC.fieldOf("level").forGetter(AddEnchantmentModification::level),
      Codec.BOOL.optionalFieldOf("check_validity", false).forGetter(AddEnchantmentModification::checkValidity)
  ).apply(i, AddEnchantmentModification::new));

  @Override
  public void modify(ItemStack stack, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
    enchantment.streamEnchantments(stack, context).forEach(enchantment -> {
      if (checkValidity && !enchantment.value().isSupportedItem(stack)) {
        return;
      }
      final int levelCalculated = level.get(enchantment, context);
      enchantments.set(enchantment, levelCalculated);
    });
  }

  @Override
  public EnchantmentModificationType<AddEnchantmentModification> getType() {
    return EnchantmentModificationTypes.ADD;
  }
}
