package pers.solid.ecmd.mixins.general;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
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
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.config.EntitySelectorParsingConfig;
import pers.solid.ecmd.config.GeneralParsingConfig;
import pers.solid.ecmd.entity.predicate.*;
import pers.solid.ecmd.mixins.accessor.EntitySelectorParserAccessor;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.bridge.BridgeIntRange;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(EntitySelectorOptions.class)
public abstract class EntitySelectorOptionsMixin {

  @SuppressWarnings("rawtypes")
  @Shadow
  @Final
  private static Map OPTIONS;

  @ModifyExpressionValue(method = "get", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0))
  private static @Nullable Object acceptOptionNameAlias(@Nullable Object originalValue, @Local(argsOnly = true) String option) {
    if (EntitySelectorParsingConfig.current.acceptOptionNameAlias && originalValue == null && EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.containsKey(option)) {
      return OPTIONS.get(EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.get(option));
    } else {
      return originalValue;
    }
  }

  /**
   * 对于 {@link EntitySelectorOptions#suggestNames} 而言，除了提供选项名称的建议之外，还应该提供选项别称的建议。<br>
   * 说明：此方法遇到了不可访问的类，故直接忽略了泛型，但在运行时应该是能够正常运行的。
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @ModifyExpressionValue(method = "suggestNames", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
  private static Iterator suggestOptionAliases(Iterator original) {
    if (EntitySelectorParsingConfig.current.acceptOptionNameAlias && !EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.isEmpty()) {
      final Iterator<Map.Entry<String, Object>> iterator = Maps.transformEntries(EntitySelectorOptionsExtension.OPTION_NAME_ALIASES, (key, value) -> OPTIONS.get(value)).entrySet().iterator();
      return Iterators.concat(original, iterator);
    } else {
      return original;
    }
  }

  /**
   * 在抛出 {@link EntitySelectorOptions#ERROR_INAPPLICABLE_OPTION} 前，重置 cursor 为整个 propertyName 的部分。
   */
  @ModifyArg(method = "get", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;ERROR_INAPPLICABLE_OPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", opcode = Opcodes.GETSTATIC), to = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;ERROR_UNKNOWN_OPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", opcode = Opcodes.GETSTATIC)), index = 0)
  private static ImmutableStringReader tweakInapplicableException(ImmutableStringReader reader, @Local(argsOnly = true) int restoreCursor, @Local(argsOnly = true) LocalIntRef cursorEnd) {
    cursorEnd.set(reader.getCursor());
    ((StringReader) reader).setCursor(restoreCursor);
    return reader;
  }

  /**
   * 在抛出 {@link EntitySelectorOptions#ERROR_INAPPLICABLE_OPTION} 前，重置 cursorEnd 为整个 propertyName 的后面。
   */
  @ModifyExpressionValue(method = "get", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;ERROR_INAPPLICABLE_OPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", opcode = Opcodes.GETSTATIC), to = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;ERROR_UNKNOWN_OPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", opcode = Opcodes.GETSTATIC)))
  private static CommandSyntaxException tweakInapplicableException2(CommandSyntaxException commandSyntaxException, @Local(argsOnly = true) int cursorEnd) {
    return EnhancedCommandSyntaxException.withCursorEnd(commandSyntaxException, cursorEnd);
  }

  /**
   * 在产生 {@link EntitySelectorOptions#ERROR_INAPPLICABLE_OPTION} 前，先检查有无此模组中定义的特殊的错误消息，如果有且非 {@code null}，则抛出这个。
   */
  @Inject(method = "get", at = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;ERROR_INAPPLICABLE_OPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", opcode = Opcodes.GETSTATIC))
  private static void throwBetterInapplicableException(EntitySelectorParser reader, String option, int restoreCursor, CallbackInfoReturnable<EntitySelectorOptions.Modifier> cir) throws CommandSyntaxException {
    if (!EntitySelectorParsingConfig.current.detailedInapplicableEntitySelectorOption) return;
    var f = EntitySelectorOptionsExtension.INAPPLICABLE_REASONS.get(option);
    if (f == null && EntitySelectorParsingConfig.current.acceptOptionNameAlias && EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.containsKey(option)) {
      final String forwardName = EntitySelectorOptionsExtension.OPTION_NAME_ALIASES.get(option);
      f = EntitySelectorOptionsExtension.INAPPLICABLE_REASONS.get(forwardName);
      option = forwardName;
    }
    if (f != null) {
      final StringReader stringReader = reader.getReader();
      final int cursorAfterName = stringReader.getCursor();
      final @Nullable var c = f.getReason(reader, option, restoreCursor);
      if (c != null) {
        throw EnhancedCommandSyntaxException.withCursorEnd(c, cursorAfterName);
      } else {
        // 为了保持稳定性，需要还原此更改。
        stringReader.setCursor(cursorAfterName);
      }
    }
  }

  @ModifyExpressionValue(method = "get", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;ERROR_UNKNOWN_OPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", opcode = Opcodes.GETSTATIC)))
  private static CommandSyntaxException tweakUnknownOptionException(CommandSyntaxException commandSyntaxException, @Local(argsOnly = true) String option) {
    return EnhancedCommandSyntaxException.addCursorEnd(commandSyntaxException, option);
  }

  @Inject(method = "method_9982", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static void tweakExcludingNameException(EntitySelectorParser reader, CallbackInfo ci) throws CommandSyntaxException {
    if (!EntitySelectorParsingConfig.current.detailedInapplicableEntitySelectorOption) return;
    final StringReader stringReader = reader.getReader();
    stringReader.setCursor(reader.extension$ec().cursorBeforeOptionName);
    throw EnhancedCommandSyntaxException.withCursorEnd(EntitySelectorOptionsExtension.MIXED_OPTION_INVERSION.createWithContext(stringReader, "propertyName"), reader.extension$ec().cursorAfterOptionName);
  }

  @ModifyArg(method = "method_9982", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static Predicate<Entity> addNamePredicateInformation(Predicate<Entity> predicate, @Local boolean inverted, @Local String expectedName) {
    return new StaticEntityPredicateWrapper(predicate, new NameEntityPredicateEntry(expectedName, inverted));
  }

  @Inject(method = "method_9981", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false))
  private static void tweakNegativeDistanceException1(EntitySelectorParser reader, CallbackInfo ci, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    cursorAfterParse.set(reader.getReader().getCursor());
  }

  /**
   * 对负 "distance" 值时抛出的异常进行修改，使之指针持续至值末尾的位置。
   */
  @ModifyExpressionValue(method = "method_9981", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakNegativeDistanceException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    return EnhancedCommandSyntaxException.withCursorEnd(commandSyntaxException, cursorAfterParse.get());
  }

  @Inject(method = "method_9980", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false))
  private static void tweakNegativeLevelException1(EntitySelectorParser reader, CallbackInfo ci, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    cursorAfterParse.set(reader.getReader().getCursor());
  }

  @ModifyExpressionValue(method = "method_9980", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakNegativeLevelException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    return EnhancedCommandSyntaxException.withCursorEnd(commandSyntaxException, cursorAfterParse.get());
  }

  @Inject(method = "method_9980", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/critereon/MinMaxBounds$Ints;fromReader(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/advancements/critereon/MinMaxBounds$Ints;"))
  private static void acceptNegativeLevel(EntitySelectorParser reader, CallbackInfo ci, @Share("inverted") LocalBooleanRef ref) throws CommandSyntaxException {
    final boolean inverted = EntitySelectorParsingConfig.current.allowLevelInversion && reader.shouldInvertValue();
    ref.set(inverted);
    final EntitySelectorReaderExtras extras = reader.extension$ec();
    final StringReader stringReader = reader.getReader();
    if (inverted) {
      stringReader.skipWhitespace();
      extras.usedParams.put("level", true);
    } else if (extras.usedParams.getOrDefault("level", false)) {
      // 此前使用过 leve=!xxx，但此处没有使用否定
      // 此时会报错
      stringReader.setCursor(extras.cursorBeforeOptionName);
      throw EnhancedCommandSyntaxException.withCursorEnd(EntitySelectorOptionsExtension.MIXED_OPTION_INVERSION.createWithContext(stringReader, "level"), extras.cursorAfterOptionName);
    }
  }

  @WrapWithCondition(method = "method_9980", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;setLevel(Lnet/minecraft/advancements/critereon/MinMaxBounds$Ints;)V"))
  private static boolean applyNegativeLevel(EntitySelectorParser instance, MinMaxBounds.Ints levelRange, @Share("inverted") LocalBooleanRef ref) {
    if (ref.get()) {
      instance.addPredicate$ec(new LevelEntityPredicateEntry(BridgeIntRange.fromVanilla(levelRange), true));
      return false;
    } else {
      return true;
    }
  }

  @Inject(method = "method_9969", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false))
  private static void tweakSmallLimitException1(EntitySelectorParser reader, CallbackInfo ci, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    cursorAfterParse.set(reader.getReader().getCursor());
  }

  @ModifyExpressionValue(method = "method_9969", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/SimpleCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakSmallLimitException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    return EnhancedCommandSyntaxException.withCursorEnd(commandSyntaxException, cursorAfterParse.get());
  }

  /**
   * 当使用 {@code @p} 时，limit 的值应该允许为负值，从而表示选择最远的实体。
   */
  @Inject(method = "method_9969", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false))
  private static void acceptsImplicitNegativeLimit(EntitySelectorParser reader, CallbackInfo ci, @Local(ordinal = 0) int cursor, @Local(ordinal = 1) LocalIntRef readInt) throws CommandSyntaxException {
    if (!EntitySelectorParsingConfig.current.allowNegativeDistanceForNearest) {
      return;
    }
    final EntitySelectorReaderExtras extras = reader.extension$ec();
    if ("p".equals(extras.atVariable) && readInt.get() < 0) {
      if (reader.isSorted()) {
        final int cursorAfterInt = reader.getReader().getCursor();
        reader.getReader().setCursor(cursor);
        throw EnhancedCommandSyntaxException.withCursorEnd(EntitySelectorOptionsExtension.INVALID_NEGATIVE_LIMIT_WITH_SORTER.createWithContext(reader.getReader()), cursorAfterInt);
      }
      readInt.set(-readInt.get());
      reader.setOrder(EntitySelectorParser.ORDER_FURTHEST);
      reader.setSorted(true);
      extras.implicitNegativeLimit = true;
    }
  }

  @Inject(method = "method_9953", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false))
  private static void tweakIrreversibleSortException1(EntitySelectorParser reader, CallbackInfo ci, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    cursorAfterParse.set(reader.getReader().getCursor());
  }

  @ModifyExpressionValue(method = "method_9953", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakIrreversibleSortException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterParse") LocalIntRef cursorAfterParse) {
    return EnhancedCommandSyntaxException.withCursorEnd(commandSyntaxException, cursorAfterParse.get());
  }

  /**
   * 修改 "gamemode" 的值的建议，使之接受本模组中的扩展的游戏模式名称。
   */
  @Inject(method = "method_9946", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameType;values()[Lnet/minecraft/world/level/GameType;"))
  private static void suggestMoreGamemodes(EntitySelectorParser entitySelectorReader, SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir, @Local(ordinal = 0) String stringxx, @Local(ordinal = 0) boolean blx, @Local(ordinal = 1) boolean bl2) {
    if (!GeneralParsingConfig.current.acceptGameModeAlias) {
      return;
    }
    for (String name : MixinShared.EXTENDED_GAME_MODE_NAMES.keySet()) {
      if (name.toLowerCase(Locale.ROOT).startsWith(stringxx)) {
        if (bl2) {
          builder.suggest("!" + name);
        }
        if (blx) {
          builder.suggest(name);
        }
      }
    }
  }

  @Inject(method = "method_9948", at = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;ERROR_INAPPLICABLE_OPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", opcode = Opcodes.GETSTATIC))
  private static void tweakInapplicableGameModeException(EntitySelectorParser reader, CallbackInfo ci) throws CommandSyntaxException {
    if (!EntitySelectorParsingConfig.current.detailedInapplicableEntitySelectorOption) return;
    final StringReader stringReader = reader.getReader();
    stringReader.setCursor(reader.extension$ec().cursorBeforeOptionName);
    throw EnhancedCommandSyntaxException.withCursorEnd(EntitySelectorOptionsExtension.MIXED_OPTION_INVERSION.createWithContext(stringReader, "gamemode"), reader.extension$ec().cursorAfterOptionName);
  }

  @ModifyExpressionValue(method = "method_9948", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", ordinal = 0, remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;ERROR_GAME_MODE_INVALID:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", opcode = Opcodes.GETSTATIC)))
  private static CommandSyntaxException tweakInvalidModeException(CommandSyntaxException commandSyntaxException, @Local String string) {
    return EnhancedCommandSyntaxException.addCursorEnd(commandSyntaxException, string);
  }

  @WrapWithCondition(method = "method_9948", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static boolean readMultipleGameModes(EntitySelectorParser reader, Predicate<Entity> predicate, @Local boolean inverted, @Local @NotNull GameType gameMode) throws CommandSyntaxException {
    // 尝试读取更多的游戏模式，即允许多个值。
    if (!EntitySelectorParsingConfig.current.allowMultipleGameModes) {
      return true;
    }
    return EntitySelectorOptionsExtension.mixinReadMultipleGameModes(reader, inverted, gameMode);
  }


  @ModifyArg(method = "method_9948", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static Predicate<Entity> addGameModeInformation(Predicate<Entity> predicate, @Local boolean inverted, @Local @NotNull GameType gameMode) {
    return new StaticEntityPredicateWrapper(predicate, new GameModeEntityPredicateEntry.Single(gameMode, inverted));
  }

  @ModifyArg(method = "method_9951", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static Predicate<Entity> addSelectsTeamInformation(Predicate<Entity> predicate, @Local boolean inverted, @Local String expectedTeamName) {
    return new StaticEntityPredicateWrapper(predicate, new TeamEntityPredicateEntry(expectedTeamName, inverted));
  }

  @Inject(method = "method_9973", at = @At(value = "FIELD", target = "Lnet/minecraft/commands/arguments/selector/options/EntitySelectorOptions;ERROR_INAPPLICABLE_OPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;", opcode = Opcodes.GETSTATIC))
  private static void tweakInapplicableTypeException(EntitySelectorParser reader, CallbackInfo ci) throws CommandSyntaxException {
    final StringReader stringReader = reader.getReader();
    stringReader.setCursor(reader.extension$ec().cursorBeforeOptionName);
    throw EnhancedCommandSyntaxException.withCursorEnd(EntitySelectorOptionsExtension.MIXED_OPTION_INVERSION.createWithContext(stringReader, "type"), reader.extension$ec().cursorAfterOptionName);
  }

  /**
   * 在读取了实体标签后，如果识别出来标签是不完整的，则抛出错误以避免进入下一环节。
   */
  @Inject(method = "method_9973", at = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagKey;create(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/tags/TagKey;", shift = At.Shift.AFTER))
  private static void avoidClearTagSuggestions(EntitySelectorParser reader, CallbackInfo ci, @Local int cursorBeforeType) throws CommandSyntaxException {
    if (!EntitySelectorParsingConfig.current.fixEntityTypeTagSuggestions) {
      return;
    }
    final CommandContext<?> context = reader.extension$ec().context;
    if (context != null) {
      final var suggestionProvider = ((EntitySelectorParserAccessor) reader).getSuggestions();
      reader.setSuggestions((builder, suggestionsBuilderConsumer) -> suggestionProvider.apply(builder.createOffset(cursorBeforeType), suggestionsBuilderConsumer).thenCombine(builder.suggest(",").suggest("]").buildFuture(), (suggestions, suggestions2) -> suggestions.isEmpty() ? suggestions2 : suggestions));
      final StringReader stringReader = reader.getReader();
      if (!stringReader.canRead()) {
        throw EntitySelectorParser.ERROR_EXPECTED_END_OF_OPTIONS.create();
      }
    }
  }

  /**
   * 在提供实体类型 id 的建议时，同时显示其名称。
   */
  @WrapOperation(method = "method_9921", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/SharedSuggestionProvider;suggestResource(Ljava/lang/Iterable;Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)Ljava/util/concurrent/CompletableFuture;"))
  private static CompletableFuture<Suggestions> improveEntityTypeSuggestion(Iterable<ResourceLocation> candidates, SuggestionsBuilder builder, Operation<CompletableFuture<Suggestions>> original) {
    if (EntitySelectorParsingConfig.current.improveEntityTypeSuggestion) {
      return SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.holders(), builder, r -> r.key().location(), r -> r.value().getDescription());
    } else {
      return original.call(candidates, builder);
    }
  }

  @Inject(method = "method_9973", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V", ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/tags/TagKey;create(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/tags/TagKey;")), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  private static void acceptMultipleTypesOnEntry(EntitySelectorParser reader, CallbackInfo ci, int cursorBeforeNegation, boolean inverted, TagKey<EntityType<?>> tagKey) throws CommandSyntaxException {
    if (!EntitySelectorParsingConfig.current.allowMultipleTypes) {
      return;
    }
    if (EntitySelectorOptionsExtension.mixinReadMultipleTypes(reader, inverted, Either.right(tagKey))) {
      ci.cancel();
    }
  }

  @Inject(method = "method_9973", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/core/registries/BuiltInRegistries;ENTITY_TYPE:Lnet/minecraft/core/DefaultedRegistry;", opcode = Opcodes.GETSTATIC)), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  private static void acceptMultipleTypesOnTag(EntitySelectorParser reader, CallbackInfo ci, int cursorBeforeNegation, boolean inverted, ResourceLocation identifier, EntityType<?> entityType) throws CommandSyntaxException {
    if (!EntitySelectorParsingConfig.current.allowMultipleTypes) {
      return;
    }
    if (EntitySelectorOptionsExtension.mixinReadMultipleTypes(reader, inverted, Either.left(entityType))) {
      reader.setIncludesEntities(true);
      ci.cancel();
    }
  }

  @ModifyArg(method = "method_9973", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;isTag()Z"), to = @At(value = "FIELD", target = "Lnet/minecraft/core/registries/BuiltInRegistries;ENTITY_TYPE:Lnet/minecraft/core/DefaultedRegistry;", opcode = Opcodes.GETSTATIC)))
  private static Predicate<Entity> addEntityTypeTagInformation(Predicate<Entity> predicate, @Local boolean inverted, @Local TagKey<EntityType<?>> tagKey) {
    return new StaticEntityPredicateWrapper(predicate, new TypeTagEntityPredicateEntry(tagKey, inverted));
  }

  @ModifyArg(method = "method_9973", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/core/registries/BuiltInRegistries;ENTITY_TYPE:Lnet/minecraft/core/DefaultedRegistry;", opcode = Opcodes.GETSTATIC)))
  private static Predicate<Entity> addEntityTypeInformation(Predicate<Entity> predicate, @Local boolean inverted, @Local EntityType<?> expectedType) {
    return new StaticEntityPredicateWrapper(predicate, new TypeEntityPredicateEntry(expectedType, inverted));
  }

  @Inject(method = "method_17961", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;setCursor(I)V", remap = false))
  private static void tweakInvalidTypeException1(EntitySelectorParser entitySelectorReader, int i, ResourceLocation identifier, CallbackInfoReturnable<CommandSyntaxException> cir, @Share("cursorAfterType") LocalIntRef cursorAfterType) {
    cursorAfterType.set(entitySelectorReader.getReader().getCursor());
  }

  @ModifyExpressionValue(method = "method_17961", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false))
  private static CommandSyntaxException tweakInvalidTypeException2(CommandSyntaxException commandSyntaxException, @Share("cursorAfterType") LocalIntRef cursorAfterType) {
    return EnhancedCommandSyntaxException.withCursorEnd(commandSyntaxException, cursorAfterType.get());
  }


  /**
   * 当读到 type 参数时，{@link EntitySelectorReaderExtras#implicitEntityType} 应该设为 {@code false}，从而拒绝 {@code "type"} 参数。
   */
  @Inject(method = "method_9973", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;shouldInvertValue()Z"))
  private static void setExplicitEntityType(EntitySelectorParser reader, CallbackInfo ci) {
    if (reader.extension$ec().implicitEntityType) {
      reader.limitToType(null);
    }
    if (reader.extension$ec().implicitNonPlayers) {
      reader.setIncludesEntities(true);
    }
    reader.extension$ec().implicitEntityType = false;
  }

  /**
   * 此处修改的是 {@code "type"} 选项的条件部分，当 {@link EntitySelectorReaderExtras#implicitEntityType} 为 {@code true} 时，应该接受此选项，从而允许对 {@code @r}、{@code p} 等选择器指定实体类型。
   */
  @ModifyReturnValue(method = "method_9939", at = @At("RETURN"))
  private static boolean acceptsImplicitEntityType(boolean original, @Local(argsOnly = true) EntitySelectorParser reader) {
    return original || reader.extension$ec().implicitEntityType;
  }

  @ModifyArg(method = "method_9968", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static Predicate<Entity> addTagInformation(Predicate<Entity> predicate, @Local boolean inverted, @Local String tagName) {
    return new StaticEntityPredicateWrapper(predicate, new TagEntityPredicateEntry(tagName, inverted));
  }

  @ModifyArg(method = "method_9966", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static Predicate<Entity> addNbtInformation(Predicate<Entity> predicate, @Local boolean inverted, @Local CompoundTag nbtCompound) {
    return new StaticEntityPredicateWrapper(predicate, new NbtMatchingEntityPredicateEntry(nbtCompound, inverted));
  }

  /**
   * 在还没有输入记分板项名称的地方，建议记分板项的名称。
   */
  @Inject(method = "method_9975", at = {
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", ordinal = 0, remap = false),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", ordinal = 2, remap = false)
  })
  private static void addScoreSuggestions(EntitySelectorParser reader, CallbackInfo ci, @Local StringReader stringReader) {
    if (!EntitySelectorParsingConfig.current.showScoreObjectiveSuggestions) {
      return;
    }
    final CommandContext<?> context = reader.extension$ec().context;
    if (context != null && (!StringReader.isAllowedInUnquotedString(stringReader.peek(-1)) || stringReader.canRead() && StringReader.isAllowedInUnquotedString(stringReader.peek()))) {
      EntitySelectorOptionsExtension.mixinGetScoreSuggestions(reader, stringReader, context);
    }
  }

  @Inject(method = "method_9975", at = {
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", remap = false, shift = At.Shift.AFTER),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", ordinal = 2, remap = false, shift = At.Shift.AFTER)
  })
  private static void removeScoreSuggestions(EntitySelectorParser reader, CallbackInfo ci, @Local StringReader stringReader) {
    if (!EntitySelectorParsingConfig.current.showScoreObjectiveSuggestions) {
      return;
    }
    final CommandContext<?> context = reader.extension$ec().context;
    if (context != null && stringReader.canRead()) {
      reader.setSuggestions(EntitySelectorParser.SUGGEST_NOTHING);
    }
  }

  @Inject(method = "method_9975", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", remap = false))
  private static void initInvertedScoreSet(EntitySelectorParser reader, CallbackInfo ci, @Share("invertedScores") LocalRef<List<ScoresEntityPredicateEntry.Entry>> invertedScores) {
    invertedScores.set(new ArrayList<>());
  }

  @Inject(method = "method_9975", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/critereon/MinMaxBounds$Ints;fromReader(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/advancements/critereon/MinMaxBounds$Ints;"))
  private static void acceptScoreNegation(EntitySelectorParser reader, CallbackInfo ci, @Local String unquotedString, @Share("inverted") LocalBooleanRef localBooleanRef) {
    if (EntitySelectorParsingConfig.current.allowScoreInversion && reader.shouldInvertValue()) {
      reader.getReader().skipWhitespace();
      localBooleanRef.set(true);
    } else {
      localBooleanRef.set(false);
    }
  }

  @WrapOperation(method = "method_9975", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/critereon/MinMaxBounds$Ints;fromReader(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/advancements/critereon/MinMaxBounds$Ints;")))
  private static Object prepareScoreNegations(Map<String, MinMaxBounds.Ints> map, Object key, Object value, Operation<Object> original, @Share("inverted") LocalBooleanRef localBooleanRef, @Share("invertedScores") LocalRef<List<ScoresEntityPredicateEntry.Entry>> invertedScores) {
    if (localBooleanRef.get()) {
      invertedScores.get().add(new ScoresEntityPredicateEntry.Entry((String) key, (MinMaxBounds.Ints) value, true));
      return null;
    } else {
      return original.call(map, key, value);
    }
  }

  @ModifyExpressionValue(method = "method_9975", at = @At(value = "INVOKE", target = "Ljava/util/Map;isEmpty()Z"))
  private static boolean applyScoreNegationsToPredicate1(boolean original, @Share("invertedScores") LocalRef<List<ScoresEntityPredicateEntry.Entry>> invertedScores) {
    // 由于在判断添加谓词时会检测 predicates.isEmpty()，如果仅使用了反向的分数谓词，那么 predicates 也会是 empty
    return original && invertedScores.get().isEmpty();
  }

  @ModifyArg(method = "method_9975", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static Predicate<Entity> applyScoreNegationsToPredicate2(Predicate<Entity> predicate, @Share("invertedScores") LocalRef<List<ScoresEntityPredicateEntry.Entry>> ref) {
    final List<ScoresEntityPredicateEntry.Entry> invertedScores = ref.get();
    if (!invertedScores.isEmpty()) {
      return predicate.and(EntitySelectorOptionsExtension.mixinInvertedScoredPredicate(invertedScores));
    } else {
      return predicate;
    }
  }

  @ModifyArg(method = "method_9975", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static Predicate<Entity> addScoreInformation(Predicate<Entity> predicate, @Local Map<String, MinMaxBounds.Ints> expectedScore, @Share("invertedScores") LocalRef<List<ScoresEntityPredicateEntry.Entry>> invertedScores) {
    return new StaticEntityPredicateWrapper(predicate, new ScoresEntityPredicateEntry(Stream.concat(expectedScore.entrySet().stream().map(entry -> new ScoresEntityPredicateEntry.Entry(entry.getKey(), entry.getValue(), false)), invertedScores.get().stream()).toList()));
  }

  @Inject(method = "method_9974", at = {@At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", remap = false)}, slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;read(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/resources/ResourceLocation;")))
  private static void getAdvancementInformation_init(EntitySelectorParser reader, CallbackInfo ci, @Share("advancements") LocalRef<Map<ResourceLocation, Either<Map<String, Boolean>, Boolean>>> advancements) {
    advancements.set(new LinkedHashMap<>());
  }

  @Inject(method = "method_9974", at = {
      @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;read(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/resources/ResourceLocation;"),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", ordinal = 5, remap = false)
  })
  private static void addAdvancementSuggestions(EntitySelectorParser reader, CallbackInfo ci, @Local StringReader stringReader) {
    if (!EntitySelectorParsingConfig.current.showAdvancementsSuggestions) {
      return;
    }
    final CommandContext<?> context = reader.extension$ec().context;
    if (context != null && (!StringReader.isAllowedInUnquotedString(stringReader.peek(-1)) || stringReader.canRead() && StringReader.isAllowedInUnquotedString(stringReader.peek()))) {
      EntitySelectorOptionsExtension.mixinGetAdvancementIdSuggestions(reader, stringReader, context);
    }
  }


  /**
   * 在进度 id 或进度的条件 id 的等号后，读取布尔值前，提供布尔值的建议。
   */
  @Inject(method = "method_9974", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readBoolean()Z", remap = false))
  private static void addAdvancementsValueBooleanSuggestions(EntitySelectorParser reader, CallbackInfo ci) {
    reader.setSuggestions(EntitySelectorOptionsExtension.BOOLEAN_SUGGEST);
  }

  @Inject(method = "method_9974", at = {
      @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;read(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/resources/ResourceLocation;", shift = At.Shift.AFTER),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", ordinal = 5, shift = At.Shift.AFTER, remap = false)
  })
  private static void removeAdvancementSuggestions(EntitySelectorParser reader, CallbackInfo ci, @Local StringReader stringReader) {
    if (!EntitySelectorParsingConfig.current.showAdvancementsSuggestions) {
      return;
    }
    final CommandContext<?> context = reader.extension$ec().context;
    if (context != null && stringReader.canRead()) {
      reader.setSuggestions(EntitySelectorParser.SUGGEST_NOTHING);
    }
  }

  @Inject(method = "method_9974", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", remap = false), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;read(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/resources/ResourceLocation;")))
  private static void getAdvancementInformation_initCriteria(EntitySelectorParser reader, CallbackInfo ci, @Share("criterionMap") LocalRef<Map<String, Boolean>> criterionMap, @Local ResourceLocation advancementId, @Share("advancements") LocalRef<Map<ResourceLocation, Either<Map<String, Boolean>, Boolean>>> advancements) {
    final Object2BooleanLinkedOpenHashMap<String> value = new Object2BooleanLinkedOpenHashMap<>();
    criterionMap.set(value);
    advancements.get().put(advancementId, Either.left(value));
  }

  @Redirect(method = "method_9974", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", remap = false))
  private static String acceptQuotedCriterionName(StringReader instance) throws CommandSyntaxException {
    // 原版的代码中，读取进度条件名称时，只能读取不带引号的字符串，这会无法使用一些含有特殊字符的进度条件名称。
    // 为了解决这样的问题，这里将其调用为可以读取带引号的字符串。
    return EntitySelectorParsingConfig.current.acceptQuotedAdvancementCriterionName ? instance.readString() : instance.readUnquotedString();
  }

  @Inject(method = "method_9974", at = {
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", remap = false),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", ordinal = 4, remap = false)
  })
  private static void addAdvancementCriterionSuggestion(EntitySelectorParser reader, CallbackInfo ci, @Local ResourceLocation advancementId, @Local StringReader stringReader) {
    if (!EntitySelectorParsingConfig.current.showAdvancementsCriterionSuggestions) {
      return;
    }
    final CommandContext<?> context = reader.extension$ec().context;
    if (context == null) return;
    if (StringReader.isAllowedInUnquotedString(stringReader.peek(-1)) && !(stringReader.canRead() && StringReader.isAllowedInUnquotedString(stringReader.peek()))) return;
    if (context.getSource() instanceof final CommandSourceStack serverCommandSource) {
      final int cursor = stringReader.getCursor();
      final ServerAdvancementManager advancementLoader = serverCommandSource.getServer().getAdvancements();
      reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> {
        final AdvancementHolder advancementEntry = advancementLoader.get(advancementId);
        if (advancementEntry != null) {
          // 配合 acceptQuotedCriterionName 方法使用，当进度条件名称含有特殊的字符时，应该建议带有引号的字符串。
          return SharedSuggestionProvider.suggest(advancementEntry.value().criteria().keySet().stream().map(ParsingUtil::quoteStringIfNeeded), suggestionsBuilder.createOffset(cursor));
        } else {
          return Suggestions.empty();
        }
      });
    } else if (context.getSource() instanceof final SharedSuggestionProvider commandSource) {
      reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> commandSource.customSuggestion(context));
    }
  }

  @Inject(method = "method_9974", at = {
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readUnquotedString()Ljava/lang/String;", shift = At.Shift.AFTER, remap = false),
      @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;expect(C)V", shift = At.Shift.AFTER, ordinal = 4, remap = false)
  })
  private static void removeAdvancementCriterionSuggestions(EntitySelectorParser reader, CallbackInfo ci, @Local StringReader stringReader) {
    if (!EntitySelectorParsingConfig.current.showAdvancementsCriterionSuggestions) {
      return;
    }
    final CommandContext<?> context = reader.extension$ec().context;
    if (context != null && stringReader.canRead()) {
      reader.setSuggestions(EntitySelectorParser.SUGGEST_NOTHING);
    }
  }

  @Inject(method = "method_9974", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0))
  private static void getAdvancementInformation_addCriterion(EntitySelectorParser reader, CallbackInfo ci, @Local String criterionName, @Local boolean expectedValue, @Share("criterionMap") LocalRef<Map<String, Boolean>> localRef) {
    localRef.get().put(criterionName, expectedValue);
  }

  @Inject(method = "method_9974", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), slice = @Slice(from = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readBoolean()Z", ordinal = 1, remap = false)))
  private static void getAdvancementInformation_addConstant(EntitySelectorParser reader, CallbackInfo ci, @Local ResourceLocation advancementId, @Local boolean expectedValue, @Share("advancements") LocalRef<Map<ResourceLocation, Either<Map<String, Boolean>, Boolean>>> advancements) {
    advancements.get().put(advancementId, Either.right(expectedValue));
  }

  @ModifyArg(method = "method_9974", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static Predicate<Entity> addAdvancementInformation(Predicate<Entity> predicate, @Share("advancements") LocalRef<Map<ResourceLocation, Either<Map<String, Boolean>, Boolean>>> advancements) {
    final Map<ResourceLocation, Either<Map<String, Boolean>, Boolean>> build = advancements.get();
    return new StaticEntityPredicateWrapper(predicate, new AdvancementsEntityPredicateEntry(build));
  }

  @WrapOperation(method = "method_22824", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;read(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/resources/ResourceLocation;"))
  private static ResourceLocation addPredicateNameSuggestions(StringReader stringReader, Operation<ResourceLocation> original, @Local(argsOnly = true) EntitySelectorParser reader) {
    if (!EntitySelectorParsingConfig.current.showPredicateSuggestions) {
      return original.call(stringReader);
    }
    final CommandContext<?> context = reader.extension$ec().context;
    if (context == null) {
      return original.call(stringReader);
    }
    EntitySelectorOptionsExtension.mixinGetLootConditionIdSuggestions(reader, stringReader, context);
    final ResourceLocation fromCommandInput = original.call(stringReader);
    if (stringReader.canRead()) {
      reader.setSuggestions(EntitySelectorParser.SUGGEST_NOTHING);
    }
    return fromCommandInput;
  }

  @Inject(method = "method_22824", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;read(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/resources/ResourceLocation;"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
  private static void acceptLiteralPredicateInput(EntitySelectorParser reader, CallbackInfo ci, boolean bl) throws CommandSyntaxException {
    if (!EntitySelectorParsingConfig.current.allowLiteralPredicateJson) {
      return;
    }
    final StringReader stringReader = reader.getReader();
    boolean cancel = EntitySelectorOptionsExtension.mixinReadLiteralPredicate(reader, bl, stringReader);
    if (cancel) ci.cancel();
  }

  @Inject(method = "method_22824", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static void modifyPredicateSuggestion(EntitySelectorParser reader, CallbackInfo ci) throws CommandSyntaxException {
    // 如果 reader 后面没有内容，那么提前抛出异常，这是为了避免在输入了不完整的 id 时，由于进行了后面的解析，导致建议的内容被覆盖。
    if (!reader.getReader().canRead() && reader.extension$ec().context != null) {
      final var prev = ((EntitySelectorParserAccessor) reader).getSuggestions();
      reader.setSuggestions((suggestionsBuilder, suggestionsBuilderConsumer) -> {
        final CompletableFuture<Suggestions> prevResult = prev.apply(suggestionsBuilder, suggestionsBuilderConsumer);
        return prevResult.thenCompose(suggestions -> {
          if (suggestions.isEmpty()) {
            return ((EntitySelectorParserAccessor) reader).callSuggestOptionsNextOrClose(suggestionsBuilder, suggestionsBuilderConsumer);
          } else {
            return CompletableFuture.completedFuture(suggestions);
          }
        });
      });
      throw EntitySelectorParser.ERROR_EXPECTED_END_OF_OPTIONS.createWithContext(reader.getReader());
    }
  }


  @ModifyArg(method = "method_22824", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/arguments/selector/EntitySelectorParser;addPredicate(Ljava/util/function/Predicate;)V"))
  private static Predicate<Entity> addPredicateInformation(Predicate<Entity> predicate, @Local boolean inverted, @Local ResourceKey<LootItemCondition> registryKey) {
    return new StaticEntityPredicateWrapper(predicate, new LootTablePredicateEntityPredicateEntry(Holder.Reference.createStandAlone(null, registryKey), inverted));
  }
}
