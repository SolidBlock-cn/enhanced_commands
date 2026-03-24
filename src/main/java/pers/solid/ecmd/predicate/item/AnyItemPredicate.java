package pers.solid.ecmd.predicate.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record AnyItemPredicate(List<ItemPredicate> predicates) implements PredicatesBasedItemPredicate {
  public static final MapCodec<AnyItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ItemPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AnyItemPredicate::predicates)).apply(i, AnyItemPredicate::new));

  @Override
  public boolean test(ItemStack stack, ExecutionContext executionContext) {
    return predicates.stream().anyMatch(p -> p.test(stack, executionContext));
  }

  @Override
  public @NotNull Type getType() {
    return ItemPredicateTypes.ANY_TYPE;
  }

  @Override
  public @NotNull String asString() {
    return "any(" + predicates.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ")) + ")";
  }

  public enum Type implements ItemPredicateType<AnyItemPredicate> {
    ANY_TYPE;

    @Override
    public @NotNull MapCodec<AnyItemPredicate> getCodec() {
      return CODEC;
    }
  }

  public record Parser(List<ItemPredicate> itemPredicates) implements FunctionContentParser.SequentialParams<AnyItemPredicate> {
    public Parser() {
      this(new ArrayList<>());
    }

    @Override
    public AnyItemPredicate getParseResult(ParseContext<?> parseContext) {
      return new AnyItemPredicate(itemPredicates);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      itemPredicates.add(ItemPredicate.parse(parseContext));
    }
  }
}
