package pers.solid.ecmd.mixins.general;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ComponentPredicateParser;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.parsing.packrat.*;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import net.minecraft.util.parsing.packrat.commands.StringReaderTerms;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.item.predicate.ItemPredicate;
import pers.solid.ecmd.item.predicate.ItemPredicateParsing;
import pers.solid.ecmd.item.predicate.ReferenceItemPredicate;
import pers.solid.ecmd.parse.IdWithDefaultNamespaceParseRule;
import pers.solid.ecmd.parse.PackratTermFromParser;
import pers.solid.ecmd.parse.ReferenceEntryLookupRule;
import pers.solid.ecmd.util.extension.ComponentPredicateParserContextExtension;

import java.util.List;
import java.util.Optional;

@Mixin(ComponentPredicateParser.class)
public abstract class ComponentPredicateParserMixin {

  /**
   * 修改字典中名为 {@code test} 的 term（例如 <code>oak_planks[<u>count=1</u>, <u>item_name</u>]</code>），使其支持函数语法。
   */
  @ModifyExpressionValue(method = "createGrammar", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/parsing/packrat/Term;alternative([Lnet/minecraft/util/parsing/packrat/Term;)Lnet/minecraft/util/parsing/packrat/Term;"), slice = @Slice(from = @At(value = "CONSTANT", args = "intValue=126")))
  private static <T, C, P> Term<StringReader> modifyAlternativeTermForTest(Term<StringReader> original, ComponentPredicateParser.Context<T, C, P> context) {
    @SuppressWarnings("unchecked") final ComponentPredicateParserContextExtension<T> contextExtension = (ComponentPredicateParserContextExtension<T>) context;
    if (!contextExtension.supportsItemPredicate$enhanced_commands()) {
      return original;
    }
    final Atom<ItemPredicate> atomFunctionGrammar = Atom.of("enhanced_commands:function_grammar");
    final Atom<List<T>> atomParentheses = Atom.of("enhanced_commands:parentheses");
    return Term.alternative(original, Term.named(atomFunctionGrammar), Term.named(atomParentheses));
  }

  /**
   * 修改字典中名为 {@code test} 的 rule，使之支持对函数式语法进行适当处理。
   */
  @Inject(method = "method_58493", at = @At("HEAD"), cancellable = true)
  private static <T, C, P> void injectedTestRuleAction(Atom<P> atom, Atom<Tag> atom2, ComponentPredicateParser.Context<T, C, P> context, Atom<C> atom3, ParseState<StringReader> parseState, Scope scope, CallbackInfoReturnable<Optional<T>> cir) {
    final Atom<ItemPredicate> atomFunctionGrammar = Atom.of("enhanced_commands:function_grammar");
    final Atom<List<T>> atomParentheses = Atom.of("enhanced_commands:parentheses");
    final @Nullable ItemPredicate functionGrammarValue = scope.get(atomFunctionGrammar);
    @SuppressWarnings("unchecked") final ComponentPredicateParserContextExtension<T> contextExtension = (ComponentPredicateParserContextExtension<T>) context;
    if (functionGrammarValue != null) {
      final Optional<T> returnValue = Optional.of(contextExtension.convertFromItemPredicate$enhanced_commands(functionGrammarValue));
      cir.setReturnValue(returnValue);
    }

    final List<T> parentheses = scope.get(atomParentheses);
    if (parentheses != null) {
      final Optional<T> optional = Optional.of(contextExtension.combine$enhanced_commands(parentheses));
      cir.setReturnValue(optional);
    }
  }

  /**
   * 修改字典中名为 {@code top} 的规则，将其改为名为 {@code enhanced_commands:vanilla_grammar} 的规则，{@code top} 规则会单独处理。
   */
  @ModifyArg(method = "createGrammar", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/parsing/packrat/Dictionary;put(Lnet/minecraft/util/parsing/packrat/Atom;Lnet/minecraft/util/parsing/packrat/Term;Lnet/minecraft/util/parsing/packrat/Rule$SimpleRuleAction;)V", ordinal = 0), slice = @Slice(from = @At(value = "NEW", target = "()Lnet/minecraft/util/parsing/packrat/Dictionary;")), index = 0)
  private static <T> Atom<T> topRuleIsActuallyUnion(Atom<T> atom) {
    return Atom.of("enhanced_commands:vanilla_grammar");
  }

  /**
   * 在返回结果之前，给字典加入更多值，使其支持增强的解析模式。
   */
  @Inject(method = "createGrammar", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/parsing/packrat/Dictionary;put(Lnet/minecraft/util/parsing/packrat/Atom;Lnet/minecraft/util/parsing/packrat/Term;Lnet/minecraft/util/parsing/packrat/Rule$SimpleRuleAction;)V", ordinal = 0), slice = @Slice(from = @At(value = "NEW", target = "()Lnet/minecraft/util/parsing/packrat/Dictionary;")))
  private static <T, C, P> void putMoreRuleIntoDictionary(ComponentPredicateParser.Context<T, C, P> context, CallbackInfoReturnable<Grammar<List<T>>> cir, @Local Dictionary<StringReader> dictionary) {
    final Atom<List<List<T>>> atomIntersect = Atom.of("enhanced_commands:intersect");
    final Atom<List<List<T>>> atomUnion = Atom.of("enhanced_commands:union");
    final Atom<List<T>> atomNegation = Atom.of("enhanced_commands:negation");
    final Atom<List<T>> atomReference = Atom.of("enhanced_commands:reference");
    final Atom<Holder.Reference<ItemPredicate>> atomItemPredicateId = Atom.of("enhanced_commands:reference_id");
    final Atom<List<T>> atomVanillaGrammar = Atom.of("enhanced_commands:vanilla_grammar");
    final Atom<List<T>> atomUnit = Atom.of("enhanced_commands:unit");
    final Atom<List<T>> atomTop = Atom.of("top");
    final Atom<List<T>> atomParentheses = Atom.of("enhanced_commands:parentheses");
    final Atom<ItemPredicate> atomFunctionGrammar = Atom.of("enhanced_commands:function_grammar");
    final Atom<ResourceLocation> atomIdInMod = Atom.of("enhanced_commands:id");

    @SuppressWarnings("unchecked") final ComponentPredicateParserContextExtension<T> contextExtension = (ComponentPredicateParserContextExtension<T>) context;
    dictionary.put(atomFunctionGrammar, new PackratTermFromParser<>(contextExtension.registries$enhanced_commands(), atomFunctionGrammar, ItemPredicateParsing.FUNCTIONS_PARSER), scope -> scope.getOrThrow(atomFunctionGrammar));
    dictionary.put(atomParentheses, Term.sequence(
        StringReaderTerms.character('('),
        Term.named(atomTop),
        StringReaderTerms.character(')')
    ), scope -> scope.getOrThrow(atomTop));
    dictionary.put(atomNegation, Term.sequence(
        StringReaderTerms.character('!'),
        Term.named(atomUnit)
    ), scope -> List.of(context.negate(contextExtension.combine$enhanced_commands(scope.getOrThrow(atomUnit)))));
    dictionary.put(atomReference, Term.sequence(
        StringReaderTerms.character('$'),
        Term.named(atomItemPredicateId)
    ), scope -> List.of(contextExtension.convertFromItemPredicate$enhanced_commands(new ReferenceItemPredicate(scope.getOrThrow(atomItemPredicateId)))));
    dictionary.put(atomIdInMod, IdWithDefaultNamespaceParseRule.ENHANCED_COMMANDS);
    dictionary.put(atomItemPredicateId, new ReferenceEntryLookupRule<>(atomIdInMod, context, ItemPredicate.REGISTRY_KEY));
    dictionary.put(atomUnit, Term.alternative(
        Term.named(atomFunctionGrammar),
        Term.named(atomVanillaGrammar),
        Term.named(atomParentheses),
        Term.named(atomNegation),
        Term.named(atomReference)
    ), scope -> Optional.ofNullable(scope.get(atomFunctionGrammar)).map(t -> List.of(contextExtension.convertFromItemPredicate$enhanced_commands(t))).orElseGet(() -> scope.getAnyOrThrow(atomVanillaGrammar, atomParentheses, atomNegation, atomReference)));

    dictionary.put(atomIntersect, Term.sequence(
        Term.named(atomUnit),
        Term.optional(Term.sequence(
            StringReaderTerms.character('&'),
            Term.named(atomIntersect)
        ))
    ), scope -> {
      final List<T> unit = scope.getOrThrow(atomUnit);
      return Optional.ofNullable(scope.get(atomIntersect)).map(t -> {
        t.add(unit);
        return t;
      }).orElseGet(() -> Lists.newArrayList(List.of(unit)));
    });

    dictionary.put(atomUnion, Term.sequence(
        Term.named(atomIntersect),
        Term.optional(Term.sequence(
            StringReaderTerms.character('|'),
            Term.named(atomUnion)
        ))
    ), scope -> {
      final List<List<T>> intersect = scope.getOrThrow(atomIntersect);
      final List<T> intersected;
      if (intersect.size() == 1) {
        intersected = intersect.get(0);
      } else {
        intersected = List.of(contextExtension.allOf$enhanced_commands(intersect.reversed().stream().map(contextExtension::combine$enhanced_commands).toList()));
      }
      return Optional.ofNullable(scope.get(atomUnion)).map(t -> {
        t.add(intersected);
        return t;
      }).orElseGet(() -> Lists.newArrayList(List.of(intersected)));
    });

    dictionary.put(atomTop, Term.named(atomUnion), scope -> {
      final List<List<T>> union = scope.getOrThrow(atomUnion);
      if (union.size() == 1) {
        return union.get(0);
      } else {
        return List.of(context.anyOf(union.reversed().stream().map(contextExtension::combine$enhanced_commands).toList()));
      }
    });
  }

  @Mixin(ComponentPredicateParser.Context.class)
  public interface ContextMixin<T> extends ComponentPredicateParserContextExtension<T> {
  }
}
