package pers.solid.ecmd.item.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.accessor.ItemPredicateArgumentAccessor;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.StringUtil;

public record CountItemPredicate(MinMaxBounds.Ints count) implements ItemPredicateEntry, ItemPredicateWithoutContext {
  public static final MapCodec<CountItemPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(MinMaxBounds.Ints.CODEC.fieldOf("count").forGetter(CountItemPredicate::count)).apply(i, CountItemPredicate::new));

  @Override
  public boolean test(ItemStack stack) {
    return count.matches(stack.getCount());
  }

  @Override
  public @NotNull Type getType() {
    return ItemPredicateTypes.COUNT;
  }

  @Override
  public @NotNull String asString() {
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

  public enum Type implements ItemPredicateType<CountItemPredicate> {
    COUNT_TYPE;

    @Override
    public @NotNull MapCodec<CountItemPredicate> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionContentParser<CountItemPredicate> {
    private MinMaxBounds.Ints count;

    @Override
    public CountItemPredicate getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new CountItemPredicate(count);
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      count = parseContext.parseAndSuggestArgument(RangeArgument.intRange());
    }
  }
}
