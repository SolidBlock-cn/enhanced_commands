package pers.solid.ecmd.mixins.accessor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SingleItemRecipe.class)
public interface SingleItemRecipeAccessor {
  @Invoker
  ItemStack callResult();
}
