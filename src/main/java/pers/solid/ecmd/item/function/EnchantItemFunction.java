package pers.solid.ecmd.item.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import pers.solid.ecmd.enchantment.function.EnchantmentsFunction;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.List;
import java.util.stream.Collectors;

public record EnchantItemFunction(List<EnchantmentsFunction> modifiers) implements ItemFunction {
  public static final MapCodec<EnchantItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      EnchantmentsFunction.CODEC.listOf().fieldOf("modifiers").forGetter(EnchantItemFunction::modifiers)
  ).apply(i, EnchantItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException {
    final ItemEnchantments enchantments = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    final ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
    for (EnchantmentsFunction modifier : modifiers) {
      modifier.modify(itemStack, mutable, context);
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
    return modifiers.stream().map(ExpressionConvertible::expressAsString).collect(Collectors.joining(", ", "enchant(", ")"));
  }

  public static class Parser implements FunctionContentParser.SequentialParams<EnchantItemFunction> {
    private final ImmutableList.Builder<EnchantmentsFunction> modifications = new ImmutableList.Builder<>();

    @Override
    public EnchantItemFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new EnchantItemFunction(modifications.build());
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final EnchantmentsFunction parse = EnchantmentsFunction.parse(parseContext);
      modifications.add(parse);
    }
  }
}
