package pers.solid.ecmd.item.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.ItemPredicateArgumentAccessor;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.StringUtil;

import java.util.Objects;

public record CountItemPredicate(MinMaxBounds.Ints count) implements ItemPredicateEntry, ItemPredicateWithoutContext {
  public static final MapCodec<CountItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(MinMaxBounds.Ints.CODEC.fieldOf("count").forGetter(CountItemPredicate::count)).apply(i, CountItemPredicate::new));

  @Override
  public boolean test(ItemStack stack) {
    return count.matches(stack.getCount());
  }

  @Override
  public ItemPredicateType<CountItemPredicate> getType() {
    return ItemPredicateTypes.COUNT;
  }

  @Override
  public String expressAsString() {
    return "[" + asEntryString() + "]";
  }

  @Override
  public String asEntryString() {
    if (count.min().isPresent() && count.min().equals(count.max())) {
      return ItemPredicateArgumentAccessor.getCOUNT_ID() + "=" + StringUtil.wrapRange(count);
    } else {
      return "count(" + StringUtil.wrapRange(count) + ")";
    }
  }

  public static class Parser implements FunctionContentParser<CountItemPredicate> {
    private @Nullable MinMaxBounds.Ints count;

    @Override
    public CountItemPredicate getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(count, "count");
      return new CountItemPredicate(count);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      count = parseContext.parseAndSuggestArgument(RangeArgument.intRange());
    }
  }
}
