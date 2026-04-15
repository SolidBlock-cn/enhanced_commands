package pers.solid.ecmd.item.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.ExpressionConvertible;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record AllItemPredicate(List<ItemPredicate> predicates) implements PredicatesBasedItemPredicate {
  public static final MapCodec<AllItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(ItemPredicate.CODEC.listOf().fieldOf("predicates").forGetter(AllItemPredicate::predicates)).apply(i, AllItemPredicate::new));

  @Override
  public boolean test(ItemStack stack, ExecutionContext executionContext) {
    return predicates.stream().allMatch(p -> p.test(stack, executionContext));
  }

  @Override
  public ItemPredicateType<AllItemPredicate> getType() {
    return ItemPredicateTypes.ALL_TYPE;
  }

  @Override
  public String asString() {
    return "all(" + predicates.stream().map(ExpressionConvertible::asString).collect(Collectors.joining(", ")) + ")";
  }

  public record Parser(List<ItemPredicate> itemPredicates) implements FunctionContentParser.SequentialParams<AllItemPredicate> {
    public Parser() {
      this(new ArrayList<>());
    }

    @Override
    public AllItemPredicate getParseResult(ParseContext<?> parseContext) {
      return new AllItemPredicate(itemPredicates);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      itemPredicates.add(ItemPredicate.parse(parseContext));
    }
  }
}
