package pers.solid.ecmd.mixins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.command.FloatRangeArgument;
import net.minecraft.entity.Entity;
import net.minecraft.predicate.NumberRange;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pers.solid.ecmd.mixins.ext.EntitySelectorReaderExtension;
import pers.solid.ecmd.predicate.entity.*;
import pers.solid.ecmd.util.ParsingUtil;

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
  protected abstract Predicate<Entity> rotationPredicate(FloatRangeArgument angleRange, ToDoubleFunction<Entity> entityToAngle);

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

  @Inject(method = "readArguments", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;readString()Ljava/lang/String;", remap = false), slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorOptions;getHandler(Lnet/minecraft/command/EntitySelectorReader;Ljava/lang/String;I)Lnet/minecraft/command/EntitySelectorOptions$SelectorHandler;")), locals = LocalCapture.CAPTURE_FAILSOFT)
  private void setCursorBeforeOptionName(CallbackInfo ci, int i) {
    extension$ec().cursorBeforeOptionName = i; // cursor
  }

  @Inject(method = "readArguments", at = @At(value = "INVOKE", target = "Lnet/minecraft/command/EntitySelectorOptions;getHandler(Lnet/minecraft/command/EntitySelectorReader;Ljava/lang/String;I)Lnet/minecraft/command/EntitySelectorOptions$SelectorHandler;"))
  private void setCursorAfterOptionName(CallbackInfo ci) {
    extension$ec().cursorAfterOptionName = reader.getCursor(); // cursor
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

  @Inject(method = "buildPredicate", at = @At("HEAD"))
  private void buildPredicateDescriptions(CallbackInfo ci) {
    if (pitchRange != FloatRangeArgument.ANY) {
      extension$ec().addDescription(source -> new RotationPredicateEntry(pitchRange, "pitch", Entity::getPitch, rotationPredicate(pitchRange, Entity::getPitch)));
    }

    if (yawRange != FloatRangeArgument.ANY) {
      extension$ec().addDescription(source -> new RotationPredicateEntry(yawRange, "yaw", Entity::getYaw, rotationPredicate(yawRange, Entity::getYaw)));
    }

    if (!levelRange.isDummy()) {
      extension$ec().addDescription(source -> new LevelEntityPredicateEntry(levelRange, false));
    }
  }

  /**
   * 在结束时对对象进行修改，使之接受本模组中的受实体命令源影响的谓词。
   */
  @Inject(method = "build", at = @At("RETURN"))
  private void buildExtraPredicate(CallbackInfoReturnable<EntitySelector> cir) {
    final EntitySelector returnValue = cir.getReturnValue();
    final EntitySelectorExtras extras = EntitySelectorExtras.getOf(returnValue);
    extras.predicateFunctions = extension$ec().predicateFunctions;
    extras.predicateDescriptions = extension$ec().predicateDescriptions;
    extras.collector = extension$ec().atVariable != null ? EntitySelectorCollector.NAMES.get(extension$ec().atVariable) : null;
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
  private char readMoreNames(StringReader instance, Operation<Character> original) {
    final int cursorBeforeUnquoted = reader.getCursor();
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
  @Inject(method = "readAtVariable", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;predicates:Ljava/util/List;", shift = At.Shift.AFTER))
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
  @Inject(method = "readAtVariable", at = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;suggestionProvider:Ljava/util/function/BiFunction;", shift = At.Shift.AFTER), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/command/EntitySelectorReader;predicates:Ljava/util/List;"), to = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/StringReader;skip()V", remap = false)))
  private void modifiedSetSuggestOpen(CallbackInfo ci) {
    suggestionProvider = (builder, consumer) -> suggestSelectorRest(builder /* todo 检查此处的 cursor */, consumer).thenCombine(suggestOpen(builder, consumer), (suggestions, suggestions2) -> suggestions.isEmpty() ? suggestions2 : suggestions);
  }

  @ModifyExpressionValue(method = "readAtVariable", at = @At(value = "INVOKE", target = "Ljava/lang/String;valueOf(C)Ljava/lang/String;"))
  private String injectedUnknownSelectorException(String original) {
    return extension$ec().atVariable;
  }
}
