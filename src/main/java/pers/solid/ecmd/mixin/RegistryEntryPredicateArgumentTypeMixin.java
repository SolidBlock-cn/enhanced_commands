package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.RegistryEntryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
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
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

@Mixin(RegistryEntryPredicateArgumentType.class)
public abstract class RegistryEntryPredicateArgumentTypeMixin<T> {
  @Shadow
  @Final
  RegistryKey<? extends Registry<T>> registryRef;

  @Shadow
  @Final
  private RegistryWrapper<T> registryWrapper;

  @Inject(
      method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;",
      at = @At("HEAD")
  )
  public void injectedParseHead(StringReader stringReader, CallbackInfoReturnable<RegistryEntry.Reference<T>> cir, @Share("cursorBeforeId") LocalIntRef localIntRef) {
    localIntRef.set(stringReader.getCursor());
  }

  @Inject(method = "listSuggestions", at = @At(value = "RETURN", shift = At.Shift.BEFORE), cancellable = true)
  public <S> void suggestWithTooltip(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    MixinShared.mixinSuggestWithTooltip(registryRef, registryWrapper, builder, cir);
  }

  @Inject(method = "listSuggestions", at = @At("HEAD"), cancellable = true)
  public <S> void suggestForMoreValues(CommandContext<S> context, SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
    final StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    try {
      int newStart = builder.getStart();
      Set<Identifier> tagIds = new HashSet<>();
      Set<Identifier> entryIds = new HashSet<>();
      while (stringReader.canRead()) {
        if (stringReader.canRead() && stringReader.peek() == '#') {
          stringReader.skip();
          tagIds.add(Identifier.fromCommandInput(stringReader));
        } else {
          entryIds.add(Identifier.fromCommandInput(stringReader));
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
      final Function<? super T, ? extends Message> nameSuggestionProvider = ParsingUtil.getNameSuggestionProvider(registryRef);

      // 参见 MixinShared#mixinSuggestWithTooltip
      CommandSource.suggestIdentifiers(this.registryWrapper.streamTagKeys().map(TagKey::id).filter(identifier -> !tagIds.contains(identifier)), offset, "#");
      if (nameSuggestionProvider != null) {
        cir.setReturnValue(CommandSource.suggestFromIdentifier(registryWrapper.streamEntries().filter(r -> !entryIds.contains(r.registryKey().getValue())), offset, ref -> ref.registryKey().getValue(), ref -> nameSuggestionProvider.apply(ref.value())));
      } else if (RegistryKeys.BIOME.equals(registryRef)) {
        cir.setReturnValue(CommandSource.suggestFromIdentifier(registryWrapper.streamKeys().filter(key -> !entryIds.contains(key.getValue())), offset, RegistryKey::getValue, key -> Text.translatable(Util.createTranslationKey("biome", key.getValue()))));
      } else {
        cir.setReturnValue(CommandSource.suggestIdentifiers(this.registryWrapper.streamKeys().map(RegistryKey::getValue).filter(identifier -> !entryIds.contains(identifier)), offset));
      }
    } catch (CommandSyntaxException ignored) {
    }
  }

  @ModifyArg(
      method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;",
      at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"),
      slice = @Slice(
          from = @At(value = "INVOKE", target = "Lnet/minecraft/registry/tag/TagKey;of(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/util/Identifier;)Lnet/minecraft/registry/tag/TagKey;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$TagBased;<init>(Lnet/minecraft/registry/entry/RegistryEntryList$Named;)V")
      )
  )
  public Supplier<CommandSyntaxException> modifiedParseTagException(Supplier<CommandSyntaxException> exceptionSupplier, @Share("cursorBeforeId") LocalIntRef localIntRef, @Local StringReader stringReader) {
    return () -> {
      final int cursorBeforeId = localIntRef.get();
      final int cursorAfterId = stringReader.getCursor();
      stringReader.setCursor(cursorBeforeId);
      final CommandSyntaxException commandSyntaxException = exceptionSupplier.get();
      return CommandSyntaxExceptionExtension.withCursorEnd(new CommandSyntaxException(commandSyntaxException.getType(), commandSyntaxException.getRawMessage(), stringReader.getString(), stringReader.getCursor()), cursorAfterId);
    };
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;", at = @At(value = "NEW", target = "(Lnet/minecraft/registry/entry/RegistryEntryList$Named;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$TagBased;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  public void acceptMultipleValuesOnTag(StringReader stringReader, CallbackInfoReturnable<RegistryEntryPredicateArgumentType.EntryPredicate<T>> cir, int cursorBeforeValue, Identifier tagId, TagKey<T> tagKey, RegistryEntryList.Named<T> named) throws CommandSyntaxException {
    if (stringReader.canRead() && stringReader.peek() == '|') {
      final EnhancedEntryPredicate.TagBased<T> firstValue = new EnhancedEntryPredicate.TagBased<>(named);
      cir.setReturnValue(EnhancedEntryPredicate.mixinGetCompoundPredicate(registryWrapper, registryRef, stringReader, firstValue));
    }
  }

  @ModifyArg(
      method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;",
      at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"),
      slice = @Slice(
          from = @At(value = "INVOKE", target = "Lnet/minecraft/registry/RegistryKey;of(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/util/Identifier;)Lnet/minecraft/registry/RegistryKey;"),
          to = @At(value = "INVOKE", target = "Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryBased;<init>(Lnet/minecraft/registry/entry/RegistryEntry$Reference;)V")
      )
  )
  public Supplier<CommandSyntaxException> modifiedParseEntryException(Supplier<CommandSyntaxException> original, @Share("cursorBeforeId") LocalIntRef localIntRef, @Local StringReader stringReader, @Local Identifier identifier) {
    return MixinShared.mixinModifiedParseThrow(registryRef, original, localIntRef, stringReader, identifier);
  }

  @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryPredicate;", at = @At(value = "NEW", target = "(Lnet/minecraft/registry/entry/RegistryEntry$Reference;)Lnet/minecraft/command/argument/RegistryEntryPredicateArgumentType$EntryBased;", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  public void acceptMultipleValuesOnEntry(StringReader stringReader, CallbackInfoReturnable<RegistryEntryPredicateArgumentType.EntryPredicate<T>> cir, Identifier identifier2, RegistryKey<T> registryKey, RegistryEntry.Reference<T> reference) throws CommandSyntaxException {
    if (stringReader.canRead() && stringReader.peek() == '|') {
      final EnhancedEntryPredicate.EntryBased<T> firstValue = new EnhancedEntryPredicate.EntryBased<>(reference);
      cir.setReturnValue(EnhancedEntryPredicate.mixinGetCompoundPredicate(registryWrapper, registryRef, stringReader, firstValue));
    }
  }
}
