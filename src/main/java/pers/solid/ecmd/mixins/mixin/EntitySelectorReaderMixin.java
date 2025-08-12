package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.command.FloatRangeArgument;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.mixins.ext.EntitySelectorReaderExtension;
import pers.solid.ecmd.predicate.entity.*;
import pers.solid.ecmd.util.bridge.BridgeIntRange;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

@Mixin(EntitySelectorReader.class)
public abstract class EntitySelectorReaderMixin implements EntitySelectorReaderExtension {
  @Shadow
  @Final
  private StringReader reader;
  @Shadow
  private FloatRangeArgument pitchRange;
  @Shadow
  private FloatRangeArgument yawRange;
  @Shadow
  private NumberRange.IntRange levelRange;

  @Shadow
  private int limit;
  @Shadow
  private BiConsumer<Vec3d, List<? extends Entity>> sorter;
  @Shadow
  private BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> suggestionProvider;

  @Shadow
  protected abstract CompletableFuture<Suggestions> suggestSelectorRest(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer);

  @Shadow
  protected abstract CompletableFuture<Suggestions> suggestOpen(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> consumer);

  @Shadow
  private boolean hasLimit;

  @Shadow
  @Nullable
  private Double x;

  @Shadow
  @Nullable
  private Double y;

  @Shadow
  @Nullable
  private Double z;

  @Shadow
  @Nullable
  private Double dy;

  @Shadow
  @Nullable
  private Double dx;

  @Shadow
  @Nullable
  private Double dz;

  @Shadow
  @Final
  public static BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> DEFAULT_SUGGESTION_PROVIDER;

  @Shadow
  @Final
  private List<Predicate<Entity>> predicates;

  @Inject(method = "readArguments", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readString()Ljava/lang/String;", remap = false), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorOptions;getHandler(Lnet/minecraft/command/EntitySelectorReader;Ljava/lang/String;I)Lnet/minecraft/command/EntitySelectorOptions$SelectorHandler;")), locals = LocalCapture.CAPTURE_FAILSOFT)
  private void setCursorBeforeOptionName(CallbackInfo ci, int i) {
    extension$ec().cursorBeforeOptionName = i; // cursor
  }

  @Inject(method = "readArguments", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorOptions;getHandler(Lnet/minecraft/command/EntitySelectorReader;Ljava/lang/String;I)Lnet/minecraft/command/EntitySelectorOptions$SelectorHandler;"))
  private void setCursorAfterOptionName(CallbackInfo ci) {
    extension$ec().cursorAfterOptionName = reader.getCursor(); // cursor
  }

  @WrapWithCondition(method = "readArguments", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;suggestionProvider:Ljava/util/function/BiFunction;", ordinal = 0), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorOptions$SelectorHandler;handle(Lnet/minecraft/command/EntitySelectorReader;)V")))
  private boolean putOffNextSuggestion(EntitySelectorReader instance, BiFunction<SuggestionsBuilder, Consumer<SuggestionsBuilder>, CompletableFuture<Suggestions>> value, @Local String optionName) {
    if (EntitySelectorOptionsExtension.INCOMPLETE_SUGGESTIONS.contains(optionName) && suggestionProvider != DEFAULT_SUGGESTION_PROVIDER) {
      suggestionProvider = EntitySelectorOptionsExtension.getPutOffSuggestions(suggestionProvider, value);
      return false;
    } else {
      return true;
    }
  }

  @ModifyExpressionValue(method = "readAtVariable", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;read()C", remap = false))
  private char setAtVariable(char c) {
    if (extension$ec().atVariable == null) {
      extension$ec().atVariable = Character.toString(c);
    }
    return c;
  }

  @Inject(method = "readAtVariable", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorReader;setEntityType(Lnet/minecraft/entity/EntityType;)V"))
  private void setImplicitEntityType(CallbackInfo ci) {
    if (!"a".equals(extension$ec().atVariable)) {
      extension$ec().implicitEntityType = true;
      extension$ec().implicitNonPlayers = true;
    }
  }

  @SuppressWarnings("unchecked")
  @ModifyArg(method = "buildPredicate", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;pitchRange:Lnet/minecraft/command/FloatRangeArgument;", ordinal = 1), to = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;yawRange:Lnet/minecraft/command/FloatRangeArgument;")))
  private Object buildPredicateDescriptionsForPitch(Object e) {
    final Float min = pitchRange.min();
    final Float max = pitchRange.max();
    return new StaticEntityPredicateWrapper((Predicate<Entity>) e, new RotationPredicateEntry.Pitch(min == null ? 0f : min, max == null ? 359f : max));
  }

  @SuppressWarnings("unchecked")
  @ModifyArg(method = "buildPredicate", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;yawRange:Lnet/minecraft/command/FloatRangeArgument;"), to = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;levelRange:Lnet/minecraft/predicate/NumberRange$IntRange;")))
  private Object buildPredicateDescriptionsForYaw(Object e) {
    final Float min = yawRange.min();
    final Float max = yawRange.max();
    return new StaticEntityPredicateWrapper((Predicate<Entity>) e, new RotationPredicateEntry.Yaw(min == null ? 0f : min, max == null ? 359f : max));
  }

  @SuppressWarnings("unchecked")
  @ModifyArg(method = "buildPredicate", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;levelRange:Lnet/minecraft/predicate/NumberRange$IntRange;")))
  private Object buildPredicateDescriptionsForLevel(Object e) {
    return new StaticEntityPredicateWrapper((Predicate<Entity>) e, new LevelEntityPredicateEntry(BridgeIntRange.fromVanilla(levelRange), false));
  }

  /**
   * 当实体选择器类型为 @pets 时，添加一个类型为 owner 的谓词，该谓词会使用到 {@link EntitySelectorReaderExtras#collectorOf} 字段。
   */
  @Inject(method = "build", at = @At(value = "INVOKE", target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;"))
  private void modifyPredicatesAtBuild(CallbackInfoReturnable<EntitySelector> cir) {
    final EntitySelectorReaderExtras extras = extension$ec();
    if (EntitySelectorTypeExtras.PETS.equals(extras.atVariable)) {
      // 插入在 alive 谓词的后面（使用 @pets 时，默认都是 alive 的），从而与转化回的结果要一致。
      predicates.add(predicates.isEmpty() ? 0 : 1, new DynamicEntityPredicateWrapper(new OwnerEntityPredicateEntry(extras.collectorOf == null ? SenderOnlyEntityPredicate.INSTANCE : EntityPredicate.simplifiedBySelector(extras.collectorOf), false), extras.contextWrapper));
    }
  }

  /**
   * 在运行 {@link EntitySelectorReader#build()} 时，将 {@link EntitySelectorReader#x}、{@link EntitySelectorReader#y}、{@link EntitySelectorReader#z} 等数据存储在 {@link EntitySelector} 的相关字段中，而非仅以 {@link Function} 的形式呈现，从而实现序列化与反序列化。
   *
   * @see PositionOffsetInfo
   * @see EntitySelectorExtras#positionOffsetInfo
   */
  @ModifyReturnValue(method = "build", at = @At("RETURN"))
  private EntitySelector recordMoreInfoAtBuild(EntitySelector original) {
    final EntitySelectorExtras extras = EntitySelectorExtras.getOf(original);
    final EntitySelectorReaderExtras selfExtras = extension$ec();
    extras.contextWrapper = selfExtras.contextWrapper;
    extras.collector = selfExtras.atVariable != null ? EntitySelectorCollector.CODEC.byId(selfExtras.atVariable) : null;
    extras.collectorOf = selfExtras.collectorOf;
    original.extension$ec().positionOffsetInfo = PositionOffsetInfo.of(x, y, z);
    if (this.dx == null && this.dy == null && this.dz == null) {
      original.extension$ec().dxDyDz = null;
    } else {
      original.extension$ec().dxDyDz = new Vec3d(dx == null ? 0 : dx, dy == null ? 0 : dy, dz == null ? 0 : dz);
    }
    return original;
  }


  /**
   * 如果通过 {@code gamemode}、{@code level} 等选项将 {@link EntitySelectorReader#setIncludesNonPlayers(boolean)} 为 {@code false}，那么 {@code implicitNonPlayers} 就应该是 {@code false}。需要注意的是，在 {@link EntitySelectorReader#readArguments()} 中读取 {@code @p} 等参数时，是直接修改的字段，没有调用此方法。
   */
  @Inject(method = "setIncludesNonPlayers", at = @At("TAIL"))
  private void setExplicitNonPlayer(boolean includesNonPlayers, CallbackInfo ci) {
    extension$ec().implicitNonPlayers = false;
  }

  /**
   * 在读取 {@code a}、{@code e} 等参数之前，读取其他可能的参数，并作为 {@code @e} 应对。
   */
  @WrapOperation(method = "readAtVariable", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;read()C", remap = false))
  private char readMoreNames(StringReader instance, Operation<Character> original, @Share("cursorBeforeVariable") LocalIntRef ref) {
    final int cursorBeforeUnquoted = reader.getCursor();
    ref.set(cursorBeforeUnquoted);
    final String extraName = reader.readUnquotedString();
    if (EntitySelectorTypeExtras.EXTRA_NAMES.containsKey(extraName)) {
      extension$ec().atVariable = extraName;
      return 'e';
    } else if (extraName.length() > 1) {
      // 此时读到的 extraName 是无效的多于一个字符。
      extension$ec().atVariable = extraName;
      return '?';
    }
    reader.setCursor(cursorBeforeUnquoted);
    return original.call(instance);
  }

  /**
   * 根据读取到的增强的参数类型，应用相应的特殊的限制。例如，读取到 {@code @f} 时，设置 {@code limit=1,sort=furthest}。
   */
  @Inject(method = "readAtVariable", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", shift = At.Shift.AFTER))
  private void appendAdditionalLimitations(CallbackInfo ci) {
    final String atVariable = extension$ec().atVariable;
    if (EntitySelectorTypeExtras.EXTRA_LIMITS.containsKey(atVariable)) {
      limit = EntitySelectorTypeExtras.EXTRA_LIMITS.getInt(atVariable);
    }
    if (EntitySelectorTypeExtras.FORCE_ONE_LIMIT.contains(atVariable)) {
      limit = 1;
      hasLimit = true;
    }
    if (EntitySelectorTypeExtras.EXTRA_SORTERS.containsKey(atVariable)) {
      sorter = EntitySelectorTypeExtras.EXTRA_SORTERS.get(atVariable);
    }
    if (EntitySelectorTypeExtras.EXTRA_READER_ATTRIBUTES.containsKey(atVariable)) {
      EntitySelectorTypeExtras.EXTRA_READER_ATTRIBUTES.get(atVariable).accept((EntitySelectorReader) (Object) this);
    }
  }

  /**
   * 在一些特定情况下，会将初始的实体谓词设置为 {@code Entity::isAlive} 以避免选择死亡的实体。此 mixin 使该谓词可序列化。
   */
  @SuppressWarnings("unchecked")
  @ModifyArg(method = "readAtVariable", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
  private Object wrapAlivePredicate(Object e) {
    return new StaticEntityPredicateWrapper((Predicate<Entity>) e, AliveEntityPredicate.INSTANCE);
  }

  /**
   * 在提供变量类型的建议时，排除不匹配的建议。原版中由于只有一个字符，故没有考虑到这个问题。
   */
  @WrapOperation(method = "suggestSelector(Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;suggest(Ljava/lang/String;Lcom/mojang/brigadier/Message;)Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;", remap = false))
  private static SuggestionsBuilder suggestSelectorsCautiously(SuggestionsBuilder instance, String text, Message tooltip, Operation<SuggestionsBuilder> original) {
    return ParsingUtil.suggestString(text, tooltip, instance);
  }

  /**
   * 除了建议原版的几种变量类型（{@code @a}、{@code @e} 等）之外，还要建议本模组中提供的额外类型。
   */
  @Inject(method = "suggestSelector(Lcom/mojang/brigadier/suggestion/SuggestionsBuilder;)V", at = @At("TAIL"))
  private static void suggestMoreSelectors(SuggestionsBuilder builder, CallbackInfo ci) {
    CommandSource.suggestMatching(EntitySelectorTypeExtras.EXTRA_NAMES.entrySet(), builder, entry -> "@" + entry.getKey(), Map.Entry::getValue);
  }

  /**
   * 在没有读取玩所有的实体选择器类型之前，不需要急于提供方括号的建议，只有已经完全输入后，再提供方括号的建议。
   */
  @Inject(method = "readAtVariable", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;suggestionProvider:Ljava/util/function/BiFunction;", shift = At.Shift.AFTER), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;predicates:Ljava/util/List;"), to = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;skip()V", remap = false)), locals = LocalCapture.CAPTURE_FAILSOFT)
  private void modifiedSetSuggestOpen(CallbackInfo ci, @Share("cursorBeforeVariable") LocalIntRef ref) {
    suggestionProvider = (builder, consumer) -> suggestSelectorRest(builder.createOffset(ref.get()), consumer).thenCombine(suggestOpen(builder, consumer), (suggestions, suggestions2) -> suggestions.isEmpty() ? suggestions2 : suggestions);
  }

  /**
   * 报告 UNKNOWN_SELECTOR_EXCEPTION 时，使用的参数不应该是 {@code "@" + c}，而有可能是本模组中含有多个字符的名称。
   */
  @ModifyExpressionValue(method = "readAtVariable", at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(C)Ljava/lang/String;"))
  private String injectedUnknownSelectorException(String original) {
    return extension$ec().atVariable;
  }

  @ModifyExpressionValue(method = "readAtVariable", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;createWithContext(Lcom/mojang/brigadier/ImmutableStringReader;Ljava/lang/Object;)Lcom/mojang/brigadier/exceptions/CommandSyntaxException;", remap = false), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;UNKNOWN_SELECTOR_EXCEPTION:Lcom/mojang/brigadier/exceptions/DynamicCommandExceptionType;")))
  private CommandSyntaxException modifyUnknownSelectorException(CommandSyntaxException original) {
    return CommandSyntaxExceptionExtension.addCursorEnd(original, extension$ec().atVariable);
  }

}
