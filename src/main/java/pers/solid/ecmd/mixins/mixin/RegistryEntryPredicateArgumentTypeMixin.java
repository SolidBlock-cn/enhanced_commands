package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.Util;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.argument.EnhancedEntryPredicate;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(ResourceOrTagArgument.class)
public abstract class RegistryEntryPredicateArgumentTypeMixin<T> {
  @Shadow
  @Final
  ResourceKey<? extends Registry<T>> registryKey;

  @Shadow
  @Final
  private HolderLookup<T> registryLookup;

  @Inject(
      method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/ResourceOrTagArgument$Result;",
      at = @At("HEAD")
  )
  public void injectedParseHead(StringReader stringReader, CallbackInfoReturnable<Holder.Reference<T>> cir, @Share("cursorBeforeId") LocalIntRef localIntRef) {
    localIntRef.set(stringReader.getCursor());
  }

  @Inject(method = "listSuggestions", at = @At(value = "RETURN"), cancellable = true)
  public <S> void suggestWithTooltip(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    MixinShared.mixinSuggestWithTooltip(registryKey, registryLookup, builder, cir);
  }

  @Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
  public <S> void suggestForMoreValues(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    final StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    try {
      int newStart = builder.getStart();
      Set<ResourceLocation> tagIds = new HashSet<>();
      Set<ResourceLocation> entryIds = new HashSet<>();
      while (stringReader.canRead()) {
        if (stringReader.canRead() && stringReader.peek() == '#') {
          stringReader.skip();
          tagIds.add(ResourceLocation.read(stringReader));
        } else {
          entryIds.add(ResourceLocation.read(stringReader));
        }
        if (stringReader.canRead() && stringReader.peek() == '|') {
          stringReader.skip();
          newStart = stringReader.getCursor();
        } else {
          break;
        }
      }

      if (newStart == builder.getStart()) {
        return;
      }

      final SuggestionsBuilder offset = builder.createOffset(newStart);
      final Function<? super T, ? extends Message> nameSuggestionProvider = ParsingUtil.getNameSuggestionProvider(registryKey);

      // 参见 MixinShared#mixinSuggestWithTooltip
      SharedSuggestionProvider.suggestResource(this.registryLookup.listTagIds().map(TagKey::location).filter(identifier -> !tagIds.contains(identifier)), offset, "#");
      if (nameSuggestionProvider != null) {
        cir.setReturnValue(SharedSuggestionProvider.suggestResource(registryLookup.listElements().filter(r -> !entryIds.contains(r.key().location())), offset, ref -> ref.key().location(), ref -> nameSuggestionProvider.apply(ref.value())));
      } else if (Registries.BIOME.equals(registryKey)) {
        cir.setReturnValue(SharedSuggestionProvider.suggestResource(registryLookup.listElementIds().filter(key -> !entryIds.contains(key.location())), offset, ResourceKey::location, key -> Component.translatable(Util.makeDescriptionId("biome", key.location()))));
      } else {
        cir.setReturnValue(SharedSuggestionProvider.suggestResource(this.registryLookup.listElementIds().map(ResourceKey::location).filter(identifier -> !entryIds.contains(identifier)), offset));
      }
    } catch (CommandSyntaxException ignored) {
    }
  }

  @ModifyArg(
      method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/ResourceOrTagArgument$Result;",
      at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"),
      slice = @Slice(
          from = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagKey;create(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/tags/TagKey;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/ResourceOrTagArgument$TagResult;<init>(Lnet/minecraft/core/HolderSet$Named;)V")
      )
  )
  public Supplier<CommandSyntaxException> modifiedParseTagException(Supplier<CommandSyntaxException> exceptionSupplier, @Share("cursorBeforeId") LocalIntRef localIntRef, @Local(argsOnly = true) StringReader stringReader) {
    return () -> {
      final int cursorBeforeId = localIntRef.get();
      final int cursorAfterId = stringReader.getCursor();
      stringReader.setCursor(cursorBeforeId);
      final CommandSyntaxException commandSyntaxException = exceptionSupplier.get();
      return CommandSyntaxExceptionExtension.withCursorEnd(new CommandSyntaxException(commandSyntaxException.getType(), commandSyntaxException.getRawMessage(), stringReader.getString(), stringReader.getCursor()), cursorAfterId);
    };
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/ResourceOrTagArgument$Result;", at = @At(value = "NEW", target = "(Lnet/minecraft/core/HolderSet$Named;)Lnet/minecraft/commands/arguments/ResourceOrTagArgument$TagResult;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  public void acceptMultipleValuesOnTag(StringReader stringReader, CallbackInfoReturnable<ResourceOrTagArgument.Result<T>> cir, int cursorBeforeValue, ResourceLocation tagId, TagKey<T> tagKey, HolderSet.Named<T> named) throws CommandSyntaxException {
    if (stringReader.canRead() && stringReader.peek() == '|') {
      final EnhancedEntryPredicate.TagBased<T> firstValue = new EnhancedEntryPredicate.TagBased<>(named);
      cir.setReturnValue(EnhancedEntryPredicate.mixinGetCompoundPredicate(registryLookup, registryKey, stringReader, firstValue));
    }
  }

  @ModifyArg(
      method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/ResourceOrTagArgument$Result;",
      at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"),
      slice = @Slice(
          from = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceKey;create(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceKey;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/ResourceOrTagArgument$ResourceResult;<init>(Lnet/minecraft/core/Holder$Reference;)V")
      )
  )
  public Supplier<CommandSyntaxException> modifiedParseEntryException(Supplier<CommandSyntaxException> original, @Share("cursorBeforeId") LocalIntRef localIntRef, @Local(argsOnly = true) StringReader stringReader, @Local ResourceLocation identifier) {
    return MixinShared.mixinModifiedParseThrow(registryKey, original, localIntRef, stringReader, identifier);
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/ResourceOrTagArgument$Result;", at = @At(value = "NEW", target = "(Lnet/minecraft/core/Holder$Reference;)Lnet/minecraft/commands/arguments/ResourceOrTagArgument$ResourceResult;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  public void acceptMultipleValuesOnEntry(StringReader stringReader, CallbackInfoReturnable<ResourceOrTagArgument.Result<T>> cir, ResourceLocation identifier2, ResourceKey<T> registryKey, Holder.Reference<T> reference) throws CommandSyntaxException {
    if (stringReader.canRead() && stringReader.peek() == '|') {
      final EnhancedEntryPredicate.EntryBased<T> firstValue = new EnhancedEntryPredicate.EntryBased<>(reference);
      cir.setReturnValue(EnhancedEntryPredicate.mixinGetCompoundPredicate(registryLookup, this.registryKey, stringReader, firstValue));
    }
  }
}
