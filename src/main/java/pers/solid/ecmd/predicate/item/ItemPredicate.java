package pers.solid.ecmd.predicate.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

public interface ItemPredicate extends ExpressionConvertible {
  MapCodec<ItemPredicate> MAP_CODEC = ItemPredicateType.REGISTRY.byNameCodec().dispatchMap(ItemPredicate::getType, ItemPredicateType::getCodec);
  Codec<ItemPredicate> CODEC = MAP_CODEC.codec();
  ResourceKey<Registry<ItemPredicate>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("item_predicate"));

  boolean test(ItemPredicate itemPredicate, ExecutionContext executionContext);

  @NotNull ItemPredicateType<?> getType();
}
