package pers.solid.ecmd.mixins.general;

import com.google.common.base.Predicates;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.predicate.item.*;
import pers.solid.ecmd.util.extension.ComponentPredicateParserContextExtension;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(ItemPredicateArgument.class)
public abstract class ItemPredicateArgumentMixin {
  /**
   * 修改 {@link ItemPredicateArgument#PSEUDO_COMPONENTS} 中的参数使之支持序列化。
   */
  @ModifyReturnValue(method = "method_58523", at = @At("RETURN"))
  private static Predicate<ItemStack> modifyPseudoComponentsArg(Predicate<ItemStack> original, @Local(argsOnly = true) MinMaxBounds.Ints count) {
    return ItemPredicate.asVanillaPredicate(new CountItemPredicate(count), original);
  }

  /**
   * 修改 {@link ItemPredicateArgument#PSEUDO_PREDICATES} 中的参数使之支持序列化。
   */
  @ModifyReturnValue(method = "method_58529", at = @At("RETURN"))
  private static Predicate<ItemStack> modifyPseudoPredicatesArg(Predicate<ItemStack> original, @Local(argsOnly = true) MinMaxBounds.Ints count) {
    return ItemPredicate.asVanillaPredicate(new CountItemPredicate(count), original);
  }

  /**
   * 记录参数 {@code commandContext} 的值，因为在调用 {@link Grammar#parseForSuggestions(SuggestionsBuilder)} 的参数没有，但是在 mixin 中会用到。
   */
  @Inject(method = "listSuggestions", at = @At("HEAD"))
  private <S> void recordCommandContextInSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    MixinShared.commandContextForPackrat = commandContext;
  }

  /**
   * 在完成对 {@link Grammar#parseForSuggestions(SuggestionsBuilder)} 的调用后，及时将其设置为 null，释放内存。
   */
  @Inject(method = "listSuggestions", at = @At("RETURN"))
  private <S> void removeCommandContextInSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    MixinShared.commandContextForPackrat = null;
  }

  @Mixin(targets = "net.minecraft.commands.arguments.item.ItemPredicateArgument$ComponentWrapper")
  public static abstract class ComponentWrapperMixin {
    /**
     * 修改 {@link ItemPredicateArgument.ComponentWrapper#create(ImmutableStringReader, ResourceLocation, DataComponentType)} 中的 {@code presenceChecker} 参数，使之支持序列化。
     */
    @ModifyArg(method = "create", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/item/ItemPredicateArgument$ComponentWrapper;<init>(Lnet/minecraft/resources/ResourceLocation;Ljava/util/function/Predicate;Lcom/mojang/serialization/Decoder;)V"), index = 1)
    private static <T> Predicate<ItemStack> modifyComponentWrapperPresenceChecker(Predicate<ItemStack> original, @Local(argsOnly = true) DataComponentType<T> componentType) {
      return ItemPredicate.asVanillaPredicate(new ComponentPresenceItemPredicate<>(componentType), original);
    }

    /**
     * 修改 {@link ItemPredicateArgument.ComponentWrapper#create(ImmutableStringReader, ResourceLocation, DataComponentType)} 中的 {@code valueChecker} 参数所返回的 predicate，使之支持序列化。
     */
    @ModifyArg(method = "create", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Codec;map(Ljava/util/function/Function;)Lcom/mojang/serialization/Decoder;", remap = false))
    private static <T> Function<T, Predicate<ItemStack>> modifyComponentWrapperValueChecker(Function<T, Predicate<ItemStack>> original, @Local(argsOnly = true) DataComponentType<Object> componentType) {
      return object -> ItemPredicate.asVanillaPredicate(new ComponentValueCheckItemPredicate<>(componentType, object), original.apply(object));
    }
  }

  @Mixin(targets = "net.minecraft.commands.arguments.item.ItemPredicateArgument$Context")
  public static abstract class ContextMixin implements ComponentPredicateParserContextExtension<Predicate<ItemStack>> {
    private HolderLookup.Provider registries;

    /**
     * 在初始化时，将参数 {@code registries} 记录在字段中，以用于解析。
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void recordRegistries(HolderLookup.Provider registries, CallbackInfo ci) {
      this.registries = registries;
    }

    @ModifyReturnValue(method = "forElementType(Lcom/mojang/brigadier/ImmutableStringReader;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> modifyPredicateForElementType(Predicate<ItemStack> original, @Local Holder.Reference<Item> reference) {
      return ItemPredicate.asVanillaPredicate(new SimpleItemPredicate(reference.value()), original);
    }

    @ModifyReturnValue(method = "forTagType(Lcom/mojang/brigadier/ImmutableStringReader;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> modifyPredicateForTagType(Predicate<ItemStack> original, @Local HolderSet<Item> holderSet) {
      return ItemPredicate.asVanillaPredicate(new TagItemPredicate(holderSet), original);
    }

    @ModifyReturnValue(method = "negate(Ljava/util/function/Predicate;)Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> modifyNegatedPredicate(Predicate<ItemStack> original, @Local(argsOnly = true) Predicate<ItemStack> value) {
      return ItemPredicate.asVanillaPredicate(new NegatingItemPredicate(ItemPredicate.convertOrUnknown(value)), original);
    }

    @ModifyReturnValue(method = "anyOf(Ljava/util/List;)Ljava/util/function/Predicate;", at = @At("RETURN"))
    private Predicate<ItemStack> modifyAnyOfPredicate(Predicate<ItemStack> original, @Local(argsOnly = true) List<Predicate<ItemStack>> values) {
      if (values.size() == 1) {
        return original;
      }
      return ItemPredicate.asVanillaPredicate(new AnyItemPredicate(values.stream().map(ItemPredicate::convertOrUnknown).toList()), original);
    }

    @Override
    public Predicate<ItemStack> allOf$enhanced_commands(List<Predicate<ItemStack>> values) {
      if (values.size() == 1) {
        return values.get(0);
      }
      return ItemPredicate.asVanillaPredicate(new AllItemPredicate(values.stream().map(ItemPredicate::convertOrUnknown).toList()), Util.allOf(values));
    }

    @Override
    public Predicate<ItemStack> combine$enhanced_commands(List<Predicate<ItemStack>> values) {
      if (values.size() == 1) {
        return values.get(0);
      }
      return ItemPredicate.asVanillaPredicate(SimpleCombinationItemPredicate.of(values.stream().map(ItemPredicate::convertOrUnknown).toList()), Util.allOf(values));
    }

    @Override
    public boolean supportsItemPredicate$enhanced_commands() {
      return true;
    }

    @Override
    public Predicate<ItemStack> convertFromItemPredicate$enhanced_commands(ItemPredicate itemPredicate) {
      return ItemPredicate.asVanillaPredicate(itemPredicate, Predicates.alwaysTrue());
    }

    @Override
    public HolderLookup.Provider registries$enhanced_commands() {
      return registries;
    }
  }
}
