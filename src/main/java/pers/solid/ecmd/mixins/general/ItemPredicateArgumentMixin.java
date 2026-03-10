package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.ImmutableStringReader;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import pers.solid.ecmd.predicate.item.*;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(ItemPredicateArgument.class)
public abstract class ItemPredicateArgumentMixin {
  /**
   * 修改 {@link ItemPredicateArgument#PSEUDO_COMPONENTS} 中的参数使之支持序列化。
   */
  @ModifyReturnValue(method = "method_58523", at = @At("RETURN"))
  private static Predicate<ItemStack> modifyPseudoComponentsArg(Predicate<ItemStack> original, @Local(argsOnly = true) MinMaxBounds.Ints count) {
    return ItemPredicate.vanillaWrapper(original, new CountItemPredicate(count));
  }

  /**
   * 修改 {@link ItemPredicateArgument#PSEUDO_PREDICATES} 中的参数使之支持序列化。
   */
  @ModifyReturnValue(method = "method_58529", at = @At("RETURN"))
  private static Predicate<ItemStack> modifyPseudoPredicatesArg(Predicate<ItemStack> original, @Local(argsOnly = true) MinMaxBounds.Ints count) {
    return ItemPredicate.vanillaWrapper(original, new CountItemPredicate(count));
  }

  @Mixin(targets = "net.minecraft.commands.arguments.item.ItemPredicateArgument$ComponentWrapper")
  public static abstract class ComponentWrapperMixin {
    /**
     * 修改 {@link ItemPredicateArgument.ComponentWrapper#create(ImmutableStringReader, ResourceLocation, DataComponentType)} 中的 {@code presenceChecker} 参数，使之支持序列化。
     */
    @ModifyArg(method = "create", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/item/ItemPredicateArgument$ComponentWrapper;<init>(Lnet/minecraft/resources/ResourceLocation;Ljava/util/function/Predicate;Lcom/mojang/serialization/Decoder;)V"), index = 1)
    private static <T> Predicate<ItemStack> modifyComponentWrapperPresenceChecker(Predicate<ItemStack> original, @Local(argsOnly = true) DataComponentType<T> componentType) {
      return ItemPredicate.vanillaWrapper(original, new ComponentPresenceItemPredicate<>(componentType));
    }

    /**
     * 修改 {@link ItemPredicateArgument.ComponentWrapper#create(ImmutableStringReader, ResourceLocation, DataComponentType)} 中的 {@code valueChecker} 参数所返回的 predicate，使之支持序列化。
     */
    @ModifyArg(method = "create", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;map(Ljava/util/function/Function;)Lcom/mojang/serialization/Decoder;", remap = false))
    private static <T> Function<T, Predicate<ItemStack>> modifyComponentWrapperValueChecker(Function<T, Predicate<ItemStack>> original, @Local(argsOnly = true) DataComponentType<Object> componentType) {
      return object -> ItemPredicate.vanillaWrapper(original.apply(object), new ComponentValueCheckItemPredicate<>(componentType, object));
    }
  }

  @Mixin(targets = "net.minecraft.commands.arguments.item.ItemPredicateArgument$Context")
  public static abstract class ContextMixin {
    @ModifyReturnValue(method = "forElementType(Lcom/mojang/brigadier/ImmutableStringReader;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> modifyPredicateForElementType(Predicate<ItemStack> original, @Local Holder.Reference<Item> reference) {
      return ItemPredicate.vanillaWrapper(original, new SimpleItemPredicate(reference.value()));
    }

    @ModifyReturnValue(method = "forTagType(Lcom/mojang/brigadier/ImmutableStringReader;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> modifyPredicateForTagType(Predicate<ItemStack> original, @Local HolderSet<Item> holderSet) {
      return ItemPredicate.vanillaWrapper(original, new TagItemPredicate(holderSet));
    }

    @ModifyReturnValue(method = "negate(Ljava/util/function/Predicate;)Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> modifyNegatedPredicate(Predicate<ItemStack> original, @Local(argsOnly = true) Predicate<ItemStack> value) {
      return ItemPredicate.vanillaWrapper(original, new NegatingItemPredicate(ItemPredicate.convertOrUnknown(value)));
    }

    @ModifyReturnValue(method = "anyOf(Ljava/util/List;)Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> modifyAnyOfPredicate(Predicate<ItemStack> original, @Local(argsOnly = true) List<Predicate<ItemStack>> values) {
      if (values.size() == 1) {
        return original;
      }
      return ItemPredicate.vanillaWrapper(original, new AnyItemPredicate(values.stream().map(ItemPredicate::convertOrUnknown).toList()));
    }
  }
}
