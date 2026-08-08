package pers.solid.ecmd.item.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.pack.RequiresValidation;

public interface ItemFunction extends ExpressionConvertible, RequiresValidation {
  ResourceKey<Registry<ItemFunction>> REGISTRY_KEY = ResourceKey.createRegistryKey(EnhancedCommands.id("item_function"));
  MapCodec<ItemFunction> MAP_CODEC = ItemFunctionType.CODEC.dispatchMap(ItemFunction::getType, ItemFunctionType::codec);
  Codec<ItemFunction> CODEC = MAP_CODEC.codec();

  ItemStack getModifiedStack(ItemStack itemStack, ItemStack originalStack, ExecutionContext context) throws CommandSyntaxException;

  ItemFunctionType<?> getType();

  static ItemFunction parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ItemFunctionParser.parseItemFunction(parseContext);
  }
}
