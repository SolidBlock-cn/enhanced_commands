package pers.solid.ecmd.item.function;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;
import java.util.stream.Collectors;

public record ItemComponentCombinationItemFunction(ItemFunction base, List<ItemFunction> affiliate) implements ItemFunction {
  public static final MapCodec<ItemComponentCombinationItemFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      ItemFunction.CODEC.fieldOf("base").forGetter(ItemComponentCombinationItemFunction::base),
      ItemFunction.CODEC.listOf().optionalFieldOf("affiliate", ImmutableList.of()).forGetter(ItemComponentCombinationItemFunction::affiliate)
  ).apply(i, ItemComponentCombinationItemFunction::new));

  @Override
  public ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) {
    itemStack = base.getModifiedStack(itemStack, originalStack, context);
    for (ItemFunction itemFunction : affiliate) {
      itemStack = itemFunction.getModifiedStack(itemStack, originalStack, context);
    }
    return itemStack;
  }

  @Override
  public ItemFunctionType<ItemComponentCombinationItemFunction> getType() {
    return ItemFunctionTypes.ITEM_COMPONENT_COMBINATION;
  }

  @Override
  public String asString() {
    final String baseString = base.asString();
    if (affiliate.isEmpty()) {
      return baseString;
    } else {
      return baseString + affiliate.stream().map(ItemFunction::asString).collect(Collectors.joining(", ", "[", "]"));
    }
  }

  public enum Type implements ItemFunctionType<ItemComponentCombinationItemFunction> {
    ITEM_COMPONENT_COMBINATION_TYPE;

    @Override
    public MapCodec<ItemComponentCombinationItemFunction> getCodec() {
      return CODEC;
    }
  }
}
