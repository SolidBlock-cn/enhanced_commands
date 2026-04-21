package pers.solid.ecmd.enchantment.function;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import pers.solid.ecmd.number.NumberProviderParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.extension.NumberProviderExtension;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @see EnchantmentHelper#selectEnchantment(RandomSource, ItemStack, int, Stream)
 */
public record NaturalEnchantmentModification(NumberProvider level, Optional<HolderSet<Enchantment>> possibleValues) implements EnchantmentModification {
  public static final MapCodec<NaturalEnchantmentModification> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NumberProviders.CODEC.fieldOf("level").forGetter(NaturalEnchantmentModification::level),
      RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("possible_values").forGetter(NaturalEnchantmentModification::possibleValues)
  ).apply(i, NaturalEnchantmentModification::new));

  @Override
  public void modify(ItemStack stack, ItemEnchantments.Mutable enchantments, ExecutionContext context) {
    final List<EnchantmentInstance> instances = EnchantmentHelper.selectEnchantment(context.random, stack, ((NumberProviderExtension) level).getInt(context), possibleValues.map(HolderSet::stream).orElseGet(() -> context.registries().lookupOrThrow(Registries.ENCHANTMENT).listElements().map(Function.identity())));
    for (EnchantmentInstance instance : instances) {
      enchantments.set(instance.enchantment, instance.level);
    }
  }

  @Override
  public EnchantmentModificationType<NaturalEnchantmentModification> getType() {
    return EnchantmentModificationTypes.NATURAL;
  }

  /**
   * 解析关键字 {@code natural} 及空格之后的内容。在运行此方法时，指针已经在关键字 {@code natural} 及空格之后。
   */
  public static <S> NaturalEnchantmentModification parseAfterKeyword(ParseContext<S> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final NumberProvider level = NumberProviderParser.parse(parseContext);
    final int cursorAfterLevel = reader.getCursor();
    reader.skipWhitespace();
    final HolderLookup.RegistryLookup<Enchantment> lookup = parseContext.registries().lookupOrThrow(Registries.ENCHANTMENT);
    parseContext.setSuggestion((context, builder) -> EnchantmentModificationTargetParser.suggestEnchantmentOrTag(builder, lookup));
    if (reader.canRead() && (ResourceLocation.isAllowedInResourceLocation(reader.peek()) || reader.peek() == '#')) {
      final HolderSet<Enchantment> holders = EnchantmentModificationTargetParser.parseEnchantmentList(parseContext);
      return new NaturalEnchantmentModification(level, Optional.of(holders));
    } else {
      if (reader.getCursor() <= cursorAfterLevel) {
        // 没有隔着空格的话，不提供关于附魔的建议。
        parseContext.clearSuggestion();
      }
      reader.setCursor(cursorAfterLevel);
      return new NaturalEnchantmentModification(level, Optional.empty());
    }
  }

  @Override
  public String expressAsString() {
    return possibleValues.map(
        enchantmentHolderSet -> "natural " + level + " " + enchantmentHolderSet.unwrap().map(tagKey -> "#" + tagKey.location(),
            holders -> holders.stream().map(Holder::getRegisteredName).collect(Collectors.joining("|")))).orElseGet(() -> "natural " + level);
  }
}
