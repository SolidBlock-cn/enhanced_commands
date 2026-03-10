package pers.solid.ecmd.mixins.accessor;

import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.function.Predicate;

@Mixin(ItemPredicateArgument.class)
public interface ItemPredicateArgumentAccessor {
  @Accessor
  static ResourceLocation getCOUNT_ID() {
    throw new AssertionError("mixin accessor method");
  }

  @Accessor
  Grammar<List<Predicate<ItemStack>>> getGrammarWithContext();
}
