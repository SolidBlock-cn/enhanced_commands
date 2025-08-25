package pers.solid.ecmd.mixins.accessor;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.gen.Invoker;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.recipe.SingleStackRecipe.class)
public interface SingleStackRecipeAccessor {
  @Invoker
  ItemStack callResult();
}
