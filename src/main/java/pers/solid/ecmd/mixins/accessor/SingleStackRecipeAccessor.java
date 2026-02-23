package pers.solid.ecmd.mixins.accessor;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.item.crafting.SingleItemRecipe.class)
public interface SingleStackRecipeAccessor {
  @Invoker
  ItemStack callResult();
}
