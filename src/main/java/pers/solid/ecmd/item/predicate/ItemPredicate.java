package pers.solid.ecmd.item.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.ItemPredicateArgument;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @see pers.solid.ecmd.argument.ItemPredicateArgument
 * @see net.minecraft.commands.arguments.item.ItemPredicateArgument
 */
public interface ItemPredicate extends ExpressionConvertible {
  MapCodec<ItemPredicate> MAP_CODEC = ItemPredicateType.REGISTRY.byNameCodec().dispatchMap(ItemPredicate::getType, ItemPredicateType::getCodec);
  Codec<ItemPredicate> CODEC = CodecUtil.combined(CodecUtil.combinedIdAndTag(SimpleItemPredicate.STRING_BASED_CODEC, TagItemPredicate.STRING_BASED_CODEC),
      MAP_CODEC.codec(),
      predicate -> predicate instanceof TagItemPredicate simpleTag && simpleTag.items().unwrapKey().isPresent() ? Either.right(simpleTag) : predicate instanceof SimpleItemPredicate simpleItem ? Either.left(simpleItem) : null,
      either -> either.map(Function.identity(), Function.identity()));

  ResourceKey<Registry<ItemPredicate>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("item_predicate"));

  static Predicate<ItemStack> asVanillaPredicate(ItemPredicate modded, Predicate<ItemStack> vanilla) {
    if (modded instanceof ItemPredicateWithoutContext withoutContext) {
      return withoutContext;
    }
    return new VanillaWrapper(modded, vanilla);
  }

  static ItemPredicate convertOrUnknown(Predicate<ItemStack> forward) {
    if (forward instanceof VanillaWrapper itemPredicateEntry) {
      return itemPredicateEntry.modded;
    } else if (forward instanceof ItemPredicateWithoutContext withoutContext) {
      return withoutContext;
    } else {
      return new UnknownItemPredicate(forward);
    }
  }

  boolean test(ItemStack stack, ExecutionContext executionContext);

  ItemPredicateType<?> getType();

  static ItemPredicate parse(ParseContext<?> context) throws CommandSyntaxException {
    return context.parseAndSuggestArgument(ItemPredicateArgument.itemPredicate((CommandBuildContext) context.registries()));
  }

  class VanillaWrapper implements Predicate<ItemStack> {
    private final ItemPredicate modded;
    private final Predicate<ItemStack> vanilla;

    public VanillaWrapper(ItemPredicate modded, Predicate<ItemStack> vanilla) {
      this.modded = modded;
      this.vanilla = vanilla;
    }

    @Override
    public boolean test(ItemStack itemStack) {
      return vanilla.test(itemStack);
    }
  }
}
