package pers.solid.ecmd.mixin;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.advancement.Advancement;
import net.minecraft.command.CommandSource;
import net.minecraft.command.EntitySelectorOptions;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.predicate.entity.*;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.mixin.MixinSharedVariables;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(EntitySelectorOptions.class)
public abstract class EntitySelectorOptionsMixin {

  @SuppressWarnings("rawtypes")
  @Shadow
  @Final
  private static Map OPTIONS;

  @ModifyExpressionValue(method = "getHandler", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0))
  private static @Nullable Object acceptOptionNameAlias(@Nullable Object originalValue, @Local(argsOnly = true) String option) {
    if (originalValue == null && EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.containsKey(option)) {
      return OPTIONS.get(EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.get(option));
    } else {
      return originalValue;
    }
  }

  /**
   * 对于 {@link EntitySelectorOptions#suggestOptions} 而言，除了提供选项名称的建议之外，还应该提供选项别称的建议。<br>
   * 说明：此方法遇到了不可访问的类，故直接忽略了泛型，但在运行时应该是能够正常运行的。
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @ModifyExpressionValue(method = "suggestOptions", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
  private static Iterator suggestOptionAliases(Iterator original) {
    if (!EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.isEmpty()) {
      final Iterator<Map.Entry<String, Object>> iterator = Maps.transformEntries(EntitySelectorOptionsExtension.OPTION_NAME_ALIASES, (key, value) -> OPTIONS.get(value)).entrySet().iterator();
      return Iterators.concat(original, iterator);
    } else {
      return original;
    }
  }

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
    var f = EntitySelectorOptionsExtension.INAPPLICABLE_REASONS.get(option);
    if (f == null && EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.containsKey(option)) {
      final String forwardName = EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.get(option);
      f = EntitySelectorOptionsExtension.INAPPLICABLE_REASONS.get(forwardName);
      option = forwardName;
    }
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
    final StringReader stringReader = reader.getReader();
    stringReader.setCursor(EntitySelectorReaderExtras.getOf(reader).cursorBeforeOptionName);
    throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptionsExtension.MIXED_OPTION_INVERSION.createWithContext(stringReader, "name"), EntitySelectorReaderExtras.getOf(reader).cursorAfterOptionName);
  }

  @Inject(method = "method_9982", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void addNamePredicateInformation(EntitySelectorReader reader, CallbackInfo ci, int i, boolean hasNegation, String expectedName) {
    EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new NameEntityPredicateEntry(expectedName, hasNegation));
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

  /**
   * 当使用 {@code @p} 时，limit 的值应该允许为负值，从而表示选择最远的实体。
   */
  @Inject(method = "method_9969", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/mojang/brigadier/StringReader;readInt()I", remap = false))
  private static void acceptsImplicitNegativeLimit(EntitySelectorReader reader, CallbackInfo ci, @Local(ordinal = 0) int cursor, @Local(ordinal = 1) LocalIntRef readInt) throws CommandSyntaxException {
    final EntitySelectorReaderExtras extras = EntitySelectorReaderExtras.getOf(reader);
    if ("p".equals(extras.atVariable) && readInt.get() < 0) {
      if (reader.hasSorter()) {
        final int cursorAfterInt = reader.getReader().getCursor();
        reader.getReader().setCursor(cursor);
        throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptionsExtension.INVALID_NEGATIVE_LIMIT_WITH_SORTER.createWithContext(reader.getReader()), cursorAfterInt);
      }
      readInt.set(-readInt.get());
      reader.setSorter(EntitySelectorReader.FURTHEST);
      reader.setHasSorter(true);
      extras.implicitNegativeLimit = true;
    }
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
  private static void suggestMoreGamemodes(EntitySelectorReader entitySelectorReader, SuggestionsBuilder advancements, Consumer<SuggestionsBuilder> consumer, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir, @Local String stringxx, @Local(ordinal = 0) boolean blxx, @Local(ordinal = 1) boolean bl2) {
    for (String name : MixinSharedVariables.EXTENDED_GAME_MODE_NAMES.keySet()) {
      if (name.toLowerCase(Locale.ROOT).startsWith(stringxx)) {
        if (bl2) {
          advancements.suggest("!" + name);
        }
        if (blxx) {
          advancements.suggest(name);
        }
      }
    }
  }

  @Inject(method = "method_9948", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;INAPPLICABLE_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", shift = At.Shift.BEFORE))
  private static void tweakInapplicableGameModeException(EntitySelectorReader reader, CallbackInfo ci) throws CommandSyntaxException {
    final StringReader stringReader = reader.getReader();
    stringReader.setCursor(EntitySelectorReaderExtras.getOf(reader).cursorBeforeOptionName);
    throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptionsExtension.MIXED_OPTION_INVERSION.createWithContext(stringReader, "gamemode"), EntitySelectorReaderExtras.getOf(reader).cursorAfterOptionName);
  }

  @ModifyExpressionValue(method = "method_9948", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", ordinal = 0, remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;INVALID_MODE_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")))
  private static CommandSyntaxException tweakInvalidModeException(CommandSyntaxException commandSyntaxException, @Local String string) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, commandSyntaxException.getCursor() + string.length());
  }

  @WrapWithCondition(method = "method_9948", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"))
  private static boolean readMultipleGameModes(EntitySelectorReader reader, Predicate<Entity> predicate, @Local boolean hasNegation, @Local @NotNull GameMode gameMode) throws CommandSyntaxException {
    // 尝试读取更多的游戏模式，即允许多个值。
    return EntitySelectorOptionsExtension.mixinReadMultipleGameModes(reader, hasNegation, gameMode);
  }

  @Inject(method = "method_9951", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void addSelectsTeamInformation(EntitySelectorReader reader, CallbackInfo ci, boolean hasNegation, String expectedTeamName) {
    EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new TeamEntityPredicateEntry(expectedTeamName, hasNegation));
  }

  @Inject(method = "method_9973", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorOptions;INAPPLICABLE_OPTION_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", shift = At.Shift.BEFORE))
  private static void tweakInapplicableTypeException(EntitySelectorReader reader, CallbackInfo ci) throws CommandSyntaxException {
    final StringReader stringReader = reader.getReader();
    stringReader.setCursor(EntitySelectorReaderExtras.getOf(reader).cursorBeforeOptionName);
    throw CommandSyntaxExceptionExtension.withCursorEnd(EntitySelectorOptionsExtension.MIXED_OPTION_INVERSION.createWithContext(stringReader, "type"), EntitySelectorReaderExtras.getOf(reader).cursorAfterOptionName);
  }

  @Inject(method = "method_9973", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"), locals = LocalCapture.CAPTURE_FAILSOFT, slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;readTagCharacter()Z"), to = @At(value = "FIELD", target = "Lnet/minecraft/registry/Registries;ENTITY_TYPE:Lnet/minecraft/registry/DefaultedRegistry;")))
  private static void addEntityTypeTagInformation(EntitySelectorReader reader, CallbackInfo ci, int cursorStart, boolean hasNegation, TagKey<EntityType<?>> tagKey) {
    EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new TypeTagEntityPredicateEntry(tagKey, hasNegation));
  }

  @Inject(method = "method_9973", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"), locals = LocalCapture.CAPTURE_FAILSOFT, slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/registry/Registries;ENTITY_TYPE:Lnet/minecraft/registry/DefaultedRegistry;")))
  private static void addEntityTypeInformation(EntitySelectorReader reader, CallbackInfo ci, int cursorStart, boolean hasNegation, Identifier identifier, EntityType<?> expectedType) {
    EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new TypeEntityPredicateEntry(expectedType, hasNegation));
  }

  @Inject(method = "method_17961", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false, shift = At.Shift.BEFORE))
  private static void tweakInvalidTypeException1(EntitySelectorReader entitySelectorReader, int i, Identifier identifier, CallbackInfoReturnable<CommandSyntaxException> cir, @Share("cursorAfterType") LocalIntRef cursorAfterType) {
    cursorAfterType.set(entitySelectorReader.getReader().getCursor());
  }

  @ModifyExpressionValue(method = "method_17961", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakInvalidTypeException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterType") LocalIntRef cursorAfterType) {
    return CommandSyntaxExceptionExtension.withCursorEnd(commandSyntaxException, cursorAfterType.get());
  }


  /**
   * 当读到 type 参数时，{@link EntitySelectorReaderExtras#implicitEntityType} 应该设为 {@code false}，从而拒绝 {@code "type"} 参数。
   */
  @Inject(method = "method_9973", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;readNegationCharacter()Z"))
  private static void setExplicitEntityType(EntitySelectorReader reader, CallbackInfo ci) {
    if (EntitySelectorReaderExtras.getOf(reader).implicitEntityType) {
      reader.setEntityType(null);
    }
    if (EntitySelectorReaderExtras.getOf(reader).implicitNonPlayers) {
      reader.setIncludesNonPlayers(true);
    }
    EntitySelectorReaderExtras.getOf(reader).implicitEntityType = false;
  }

  /**
   * 此处修改的是 {@code "type"} 选项的条件部分，当 {@link EntitySelectorReaderExtras#implicitEntityType} 为 {@code true} 时，应该接受此选项，从而允许对 {@code @r}、{@code p} 等选择器指定实体类型。
   */
  @ModifyReturnValue(method = "method_9939", at = @At("RETURN"))
  private static boolean acceptsImplicitEntityType(boolean original, @Local(argsOnly = true) EntitySelectorReader reader) {
    return original || EntitySelectorReaderExtras.getOf(reader).implicitEntityType;
  }

  @Inject(method = "method_9968", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void addTagInformation(EntitySelectorReader reader, CallbackInfo ci, boolean hasNegation, String tagName) {
    EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new TagEntityPredicateEntry(tagName, hasNegation));
  }

  @Inject(method = "method_9966", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void addNbtInformation(EntitySelectorReader reader, CallbackInfo ci, boolean hasNegation, NbtCompound nbtCompound) {
    EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new NbtMatchingEntityPredicateEntry(nbtCompound, hasNegation));
  }

  /**
   * 在还没有输入记分板项名称的地方，建议记分板项的名称。
   */
  @Inject(method = "method_9975", at = {
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", ordinal = 0, remap = false, shift = At.Shift.BEFORE),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", ordinal = 2, remap = false, shift = At.Shift.BEFORE)
  })
  private static void addScoreSuggestions(EntitySelectorReader reader, CallbackInfo ci, @Local StringReader stringReader) {
    final CommandContext<?> context = EntitySelectorReaderExtras.getOf(reader).context;
    if (context != null && (!StringReader.isAllowedInUnquotedString(stringReader.peek(-1)) || stringReader.canRead() && StringReader.isAllowedInUnquotedString(stringReader.peek()))) {
      EntitySelectorOptionsExtension.mixinGetScoreSuggestions(reader, stringReader, context);
    }
  }

  @Inject(method = "method_9975", at = {
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", remap = false, shift = At.Shift.AFTER),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", ordinal = 2, remap = false, shift = At.Shift.AFTER)
  })
  private static void removeScoreSuggestions(EntitySelectorReader reader, CallbackInfo ci, @Local StringReader stringReader) {
    final CommandContext<?> context = EntitySelectorReaderExtras.getOf(reader).context;
    if (context != null && stringReader.canRead()) {
      reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
    }
  }

  @Inject(method = "method_9975", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void addScoreInformation(EntitySelectorReader reader, CallbackInfo ci, StringReader stringReader, Map<String, NumberRange.IntRange> expectedScore) {
    EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new ScoreEntityPredicateEntry(expectedScore));
  }

  @Inject(method = "method_9974", at = {@At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", remap = false)}, slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;fromCommandInput(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/util/Identifier;")))
  private static void getAdvancementInformation_init(EntitySelectorReader reader, CallbackInfo ci, @Share("advancements") LocalRef<Map<Identifier, Either<Object2BooleanMap<String>, Boolean>>> advancements) {
    advancements.set(new LinkedHashMap<>());
  }

  @Inject(method = "method_9974", at = {
      @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;fromCommandInput(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/util/Identifier;", shift = At.Shift.BEFORE),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", ordinal = 5, shift = At.Shift.BEFORE, remap = false)
  })
  private static void addAdvancementSuggestions(EntitySelectorReader reader, CallbackInfo ci, @Local StringReader stringReader) {
    final CommandContext<?> context = EntitySelectorReaderExtras.getOf(reader).context;
    if (context != null && (!StringReader.isAllowedInUnquotedString(stringReader.peek(-1)) || stringReader.canRead() && StringReader.isAllowedInUnquotedString(stringReader.peek()))) {
      EntitySelectorOptionsExtension.mixinGetAdvancementIdSuggestions(reader, stringReader, context);
    }
  }

  @Inject(method = "method_9974", at = {
      @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;fromCommandInput(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/util/Identifier;", shift = At.Shift.AFTER),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", ordinal = 5, shift = At.Shift.AFTER, remap = false)
  })
  private static void removeAdvancementSuggestions(EntitySelectorReader reader, CallbackInfo ci, @Local StringReader stringReader) {
    final CommandContext<?> context = EntitySelectorReaderExtras.getOf(reader).context;
    if (context != null && stringReader.canRead()) {
      reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
    }
  }

  @Inject(method = "method_9974", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", remap = false), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;fromCommandInput(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/util/Identifier;")))
  private static void getAdvancementInformation_initCriteria(EntitySelectorReader reader, CallbackInfo ci, @Share("criterionMap") LocalRef<Object2BooleanMap<String>> criterionMap, @Local Identifier advancementId, @Share("advancements") LocalRef<Map<Identifier, Either<Object2BooleanMap<String>, Boolean>>> advancements) {
    final Object2BooleanLinkedOpenHashMap<String> value = new Object2BooleanLinkedOpenHashMap<>();
    criterionMap.set(value);
    advancements.get().put(advancementId, Either.left(value));
  }

  @Redirect(method = "method_9974", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", remap = false))
  private static String acceptQuotedCriterionName(StringReader instance) throws CommandSyntaxException {
    // 原版的代码中，读取进度条件名称时，只能读取不带引号的字符串，这会无法使用一些含有特殊字符的进度条件名称。
    // 为了解决这样的问题，这里将其调用为可以读取带引号的字符串。
    return instance.readString();
  }

  @Inject(method = "method_9974", at = {
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", shift = At.Shift.BEFORE, remap = false),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", shift = At.Shift.BEFORE, ordinal = 4, remap = false)
  })
  private static void addAdvancementCriterionSuggestion(EntitySelectorReader reader, CallbackInfo ci, @Local Identifier advancementId, @Local StringReader stringReader) {
    final CommandContext<?> context = EntitySelectorReaderExtras.getOf(reader).context;
    if (context == null) return;
    if (StringReader.isAllowedInUnquotedString(stringReader.peek(-1)) && !(stringReader.canRead() && StringReader.isAllowedInUnquotedString(stringReader.peek()))) return;
    if (context.getSource() instanceof final ServerCommandSource serverCommandSource) {
      final int cursor = stringReader.getCursor();
      final ServerAdvancementLoader advancementLoader = serverCommandSource.getServer().getAdvancementLoader();
      reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> {
        final Advancement advancement = advancementLoader.get(advancementId);
        if (advancement != null) {
          // 配合 acceptQuotedCriterionName 方法使用，当进度条件名称含有特殊的字符时，应该建议带有引号的字符串。
          return CommandSource.suggestMatching(advancement.getCriteria().keySet().stream().map(ParsingUtil::quoteStringIfNeeded), suggestionsBuilder.createOffset(cursor));
        } else {
          return Suggestions.empty();
        }
      });
    } else if (context.getSource() instanceof final CommandSource commandSource) {
      reader.setSuggestionProvider((suggestionsBuilder, suggestionsBuilderConsumer) -> commandSource.getCompletions(context));
    }
  }

  @Inject(method = "method_9974", at = {
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", shift = At.Shift.AFTER, remap = false),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", shift = At.Shift.AFTER, ordinal = 4, remap = false)
  })
  private static void removeAdvancementCriterionSuggestions(EntitySelectorReader reader, CallbackInfo ci, @Local StringReader stringReader) {
    final CommandContext<?> context = EntitySelectorReaderExtras.getOf(reader).context;
    if (context != null && stringReader.canRead()) {
      reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
    }
  }

  @Inject(method = "method_9974", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0))
  private static void getAdvancementInformation_addCriterion(EntitySelectorReader reader, CallbackInfo ci, @Local String criterionName, @Local boolean expectedValue, @Share("criterionMap") LocalRef<Object2BooleanMap<String>> localRef) {
    localRef.get().put(criterionName, expectedValue);
  }

  @Inject(method = "method_9974", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readBoolean()Z", ordinal = 1, remap = false)))
  private static void getAdvancementInformation_addConstant(EntitySelectorReader reader, CallbackInfo ci, @Local Identifier advancementId, @Local boolean expectedValue, @Share("advancements") LocalRef<Map<Identifier, Either<Object2BooleanMap<String>, Boolean>>> advancements) {
    advancements.get().put(advancementId, Either.right(expectedValue));
  }

  @Inject(method = "method_9974", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"))
  private static void addAdvancementInformation(EntitySelectorReader reader, CallbackInfo ci, @Share("advancements") LocalRef<Map<Identifier, Either<Object2BooleanMap<String>, Boolean>>> advancements) {
    final Map<Identifier, Either<Object2BooleanMap<String>, Boolean>> build = advancements.get();
    EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new AdvancementEntityPredicateEntry(build));
  }

  @WrapOperation(method = "method_22824", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Identifier;fromCommandInput(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/util/Identifier;"))
  private static Identifier addPredicateNameSuggestions(StringReader stringReader, Operation<Identifier> original, @Local(argsOnly = true) EntitySelectorReader reader) {
    final CommandContext<?> context = EntitySelectorReaderExtras.getOf(reader).context;
    if (context == null) {
      return original.call(stringReader);
    }
    EntitySelectorOptionsExtension.mixinGetLootConditionIdSuggestions(reader, stringReader, context);
    final Identifier fromCommandInput = original.call(stringReader);
    if (stringReader.canRead()) {
      reader.setSuggestionProvider(EntitySelectorReader.DEFAULT_SUGGESTION_PROVIDER);
    }
    return fromCommandInput;
  }

  @Inject(method = "method_22824", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setPredicate(Ljava/util/function/Predicate;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
  private static void addPredicateInformation(EntitySelectorReader reader, CallbackInfo ci, boolean bl, Identifier identifier) {
    EntitySelectorReaderExtras.getOf(reader).addDescription(source -> new LootTablePredicateEntityPredicateEntry(identifier, bl));
  }
}
