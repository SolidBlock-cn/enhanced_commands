package pers.solid.ecmd.item.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

public interface ItemFunction extends ExpressionConvertible {
  ResourceKey<Registry<ItemFunction>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("item_function"));
  MapCodec<ItemFunction> MAP_CODEC = ItemFunctionType.REGISTRY.byNameCodec().dispatchMap(ItemFunction::getType, ItemFunctionType::getCodec);
  Codec<ItemFunction> CODEC = MAP_CODEC.codec();

  @NotNull ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context);

  @NotNull ItemFunctionType<?> getType();
}
