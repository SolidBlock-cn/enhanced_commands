package pers.solid.ecmd.item.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;
import pers.solid.ecmd.enchantment.function.EnchantmentModification;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.List;
import java.util.stream.Collectors;

public record EnchantItemFunction(List<EnchantmentModification> modifications) implements ItemFunction {
  public static final MapCodec<EnchantItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnchantmentModification.CODEC.codec().listOf().fieldOf("modifications").forGetter(EnchantItemFunction::modifications)
  ).apply(i, EnchantItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    final ItemEnchantments enchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    final ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
    for (EnchantmentModification modification : modifications) {
      modification.modify(itemStack, mutable, context);
    }
    itemStack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    return itemStack;
  }

  @Override
  public ItemFunctionType<EnchantItemFunction> getType() {
    return ItemFunctionTypes.ENCHANT;
  }

  @Override
  public String expressAsString() {
    return modifications.stream().map(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ", "enchant(", ")"));
  }

  public static class Parser implements FunctionContentParser.SequentialParams<EnchantItemFunction> {
    private final ImmutableList.Builder<EnchantmentModification> modifications = new ImmutableList.Builder<>();

    @Override
    public @Nullable EnchantItemFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new EnchantItemFunction(modifications.build());
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final EnchantmentModification parse = EnchantmentModification.parse(parseContext);
      modifications.add(parse);
    }
  }
}
