package pers.solid.ecmd.enchantment.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @see EnchantmentHelper#selectEnchantment(RandomSource, ItemStack, int, Stream)
 */
public record NaturalEnchantmentModification(int level, Optional<HolderSet<Enchantment>> possibleValues) implements EnchantmentModification {
  public static final MapCodec<NaturalEnchantmentModification> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.INT.fieldOf("level").forGetter(NaturalEnchantmentModification::level),
      RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("possible_values").forGetter(NaturalEnchantmentModification::possibleValues)
  ).apply(i, NaturalEnchantmentModification::new));

  @Override
  public void modify(ItemStack stack, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
    final List<EnchantmentInstance> instances = EnchantmentHelper.selectEnchantment(context.random, stack, level, possibleValues.map(HolderSet::stream).orElseGet(() -> context.registries().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(Function.identity())));
    for (EnchantmentInstance instance : instances) {
      enchantments.set(instance.enchantment, instance.level);
    }
  }

  @Override
  public EnchantmentModificationType<NaturalEnchantmentModification> getType() {
    return EnchantmentModificationTypes.NATURAL;
  }
}
