package pers.solid.ecmd.mixins.general;

import com.google.common.base.Predicates;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ComponentPredicateParser;
import net.minecraft.nbt.Tag;
import net.minecraft.util.parsing.packrat.Atom;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.Term;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pers.solid.ecmd.parse.PackratTermFromParser;
import pers.solid.ecmd.predicate.item.ItemPredicate;
import pers.solid.ecmd.predicate.item.ItemPredicateParsing;

import java.util.Optional;

@Mixin(ComponentPredicateParser.class)
public abstract class ComponentPredicateParserMixin {

  @ModifyExpressionValue(method = "createGrammar", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/parsing/packrat/Term;alternative([Lnet/minecraft/util/parsing/packrat/Term;)Lnet/minecraft/util/parsing/packrat/Term;"), slice = @Slice(from = @At(value = "CONSTANT", args = "intValue=126")))
  private static Term<StringReader> modifyAlternativeTermForTest(Term<StringReader> original) {
    Atom<ItemPredicate> atomFunctionGrammar = Atom.of("enhanced_commands:function_grammar");
    return Term.alternative(new PackratTermFromParser<>(atomFunctionGrammar, ItemPredicateParsing.FUNCTIONS_PARSER), original);
  }

  @Inject(method = "method_58493", at = @At("HEAD"), cancellable = true)
  private static <T, C, P> void injectedTestRuleAction(Atom<P> atom, Atom<Tag> atom2, ComponentPredicateParser.Context<T, C, P> context, Atom<C> atom3, ParseState<StringReader> parseState, Scope scope, CallbackInfoReturnable<Optional<T>> cir) {
    Atom<ItemPredicate> atomFunctionGrammar = Atom.of("enhanced_commands:function_grammar");
    final ItemPredicate functionGrammarValue = scope.get(atomFunctionGrammar);
    if (functionGrammarValue != null) {
      @SuppressWarnings("unchecked") final Optional<T> returnValue = Optional.of((T) ItemPredicate.vanillaWrapper(Predicates.alwaysTrue(), functionGrammarValue));
      cir.setReturnValue(returnValue);
    }
  }
}
