package pers.solid.ecmd.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.EntitySelectorOptions;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.predicate.entity.EntitySelectionOptionExtension;
import pers.solid.ecmd.predicate.entity.EntitySelectorReaderExtras;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.mixin.MixinSharedVariables;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mixin(EntitySelectorOptions.class)
public abstract class EntitySelectorOptionsMixin {

  /**
   * 在抛出 {@link EntitySelectorOptions#INAPPLICABLE_OPTION_EXCEPTION} 前，重置 cursor 为整个 name 的部分。
   */
  @ModifyArg(method = "getHandler", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;INAPPLICABLE_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;"), to = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;UNKNOWN_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")), index = 0)
  private static ImmutableStringReader tweakInapplicableException(ImmutableStringReader reader, @Local(argsOnly = true) int restoreCursor, @Local LocalIntRef cursorEnd) {
    cursorEnd.set(reader.getCursor());
    ((StringReader) reader).setCursor(restoreCursor);
    return reader;
  }

  /**
   * 在抛出 {@link EntitySelectorOptions#INAPPLICABLE_OPTION_EXCEPTION} 前，重置 cursorEnd 为整个 name 的后面。。
   */
  @ModifyExpressionValue(method = "getHandler", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;INAPPLICABLE_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;"), to = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;UNKNOWN_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")))
  private static CommandSyntaxException tweakInapplicableException2(CommandSyntaxException commandSyntaxException, @Local LocalIntRef cursorEnd) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursorEnd.get());
  }

  /**
   * 在产生 {@link EntitySelectorOptions#INAPPLICABLE_OPTION_EXCEPTION} 前，先检查有无此模组中定义的特殊的错误消息，如果有且非 {@code null}，则抛出这个。
   */
  @Inject(method = "getHandler", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;INAPPLICABLE_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", shift = At.Shift.BEFORE))
  private static void throwBetterInapplicableException(EntitySelectorReader reader, String option, int restoreCursor, CallbackInfoReturnable<EntitySelectorOptions.SelectorHandler> cir) throws CommandSyntaxException {
    final var f = EntitySelectionOptionExtension.INAPPLICABLE_REASONS.get(option);
    if (f != null) {
      final StringReader stringReader = reader.getReader();
      final int cursorAfterName = stringReader.getCursor();
      final @Nullable var c = f.getReason(reader, option, restoreCursor);
      if (c != null) {
        throw CommandSyntaxExceptionExtension.withCursorEnd(c, cursorAfterName);
      } else {
        // 为了保持稳定性，需要还原此更改。
        stringReader.setCursor(cursorAfterName);
      }
    }
  }

  @ModifyExpressionValue(method = "getHandler", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;UNKNOWN_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")))
  private static CommandSyntaxException tweakUnknownOptionException(CommandSyntaxException commandSyntaxException, @Local String option) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, commandSyntaxException.getCursor() + option.length());
  }

  @Inject(method = "method_9982", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static void tweakExcludingNameException(EntitySelectorReader reader, CallbackInfo ci) throws CommandSyntaxException {
    final StringReader reader1 = reader.getReader();
    reader1.setCursor(EntitySelectorReaderExtras.getOf(reader).cursorBeforeOptionName);
    throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectionOptionExtension.MIXED_NAME.createWithContext(reader1), EntitySelectorReaderExtras.getOf(reader).cursorAfterOptionName);
  }

  @Inject(method = "method_9981", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false, shift = At.Shift.BEFORE))
  private static void tweakNegativeDistanceException1(EntitySelectorReader reader, CallbackInfo ci, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    cursorAfterParse.set(reader.getReader().getCursor());
  }

  /**
   * 对负 "distance" 值时抛出的异常进行修改，使之指针持续至值末尾的位置。
   */
  @ModifyExpressionValue(method = "method_9981", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakNegativeDistanceException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursorAfterParse.get());
  }

  @Inject(method = "method_9980", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false, shift = At.Shift.BEFORE))
  private static void tweakNegativeLevelException1(EntitySelectorReader reader, CallbackInfo ci, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    cursorAfterParse.set(reader.getReader().getCursor());
  }

  @ModifyExpressionValue(method = "method_9980", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakNegativeLevelException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursorAfterParse.get());
  }

  @Inject(method = "method_9969", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false, shift = At.Shift.BEFORE))
  private static void tweakSmallLimitException1(EntitySelectorReader reader, CallbackInfo ci, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    cursorAfterParse.set(reader.getReader().getCursor());
  }

  @ModifyExpressionValue(method = "method_9969", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakSmallLimitException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursorAfterParse.get());
  }

  @Inject(method = "method_9953", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false, shift = At.Shift.BEFORE))
  private static void tweakIrreversibleSortException1(EntitySelectorReader reader, CallbackInfo ci, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    cursorAfterParse.set(reader.getReader().getCursor());
  }

  @ModifyExpressionValue(method = "method_9953", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakIrreversibleSortException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursorAfterParse.get());
  }

  /**
   * 修改 "gamemode" 的值的建议，使之接受本模组中的扩展的游戏模式名称。
   */
  @Inject(method = "method_9946", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;buildFuture()Ljava/util/concurrent/CompletableFuture;", shift = At.Shift.BEFORE, remap = false))
  private static void suggestMoreGamemodes(EntitySelectorReader entitySelectorReader, SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir, @Local String stringxx, @Local(ordinal = 0) boolean blxx, @Local(ordinal = 1) boolean bl2) {
    for (String name : MixinSharedVariables.EXTENDED_GAME_MODE_NAMES.keySet()) {
      if (name.toLowerCase(Locale.ROOT).startsWith(stringxx)) {
        if (bl2) {
          builder.suggest("!" + name);
        }
        if (blxx) {
          builder.suggest(name);
        }
      }
    }
  }

  @Inject(method = "method_9948", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;INAPPLICABLE_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", shift = At.Shift.BEFORE))
  private static void tweakInapplicableGameModeException(EntitySelectorReader reader, CallbackInfo ci) throws CommandSyntaxException {
    final StringReader stringReader = reader.getReader();
    stringReader.setCursor(EntitySelectorReaderExtras.getOf(reader).cursorBeforeOptionName);
    throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectionOptionExtension.MIXED_GAME_MODE.createWithContext(stringReader), EntitySelectorReaderExtras.getOf(reader).cursorAfterOptionName);
  }

  @ModifyExpressionValue(method = "method_9948", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", ordinal = 0, remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;INVALID_MODE_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")))
  private static CommandSyntaxException tweakInvalidModeException(CommandSyntaxException commandSyntaxException, @Local String string) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, commandSyntaxException.getCursor() + string.length());
  }

  @Inject(method = "method_9973", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;INAPPLICABLE_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", shift = At.Shift.BEFORE))
  private static void tweakInapplicableTypeException(EntitySelectorReader reader, CallbackInfo ci) throws CommandSyntaxException {
    final StringReader stringReader = reader.getReader();
    stringReader.setCursor(EntitySelectorReaderExtras.getOf(reader).cursorBeforeOptionName);
    throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectionOptionExtension.MIXED_TYPE.createWithContext(stringReader), EntitySelectorReaderExtras.getOf(reader).cursorAfterOptionName);
  }

  @Inject(method = "method_17961", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false, shift = At.Shift.BEFORE))
  private static void tweakInvalidTypeException1(EntitySelectorReader entitySelectorReader, int i, Identifier identifier, CallbackInfoReturnable<CommandSyntaxException> cir, @Share("cursorAfterType") LocalIntRef cursorAfterType) {
    cursorAfterType.set(entitySelectorReader.getReader().getCursor());
  }

  @ModifyExpressionValue(method = "method_17961", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakInvalidTypeException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterType") LocalIntRef cursorAfterType) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursorAfterType.get());
  }
}
