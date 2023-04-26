package pers.solid.mod.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.Nullable;
import pers.solid.mod.EnhancedCommands;
import pers.solid.mod.argument.ArgumentParser;
import pers.solid.mod.command.TestResult;

public record RandBlockPredicate(float value) implements BlockPredicate {
  @Override
  public String asString() {
    return "rand(" + value + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    return RandomUtils.nextFloat(0, 1) < value;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final float nextFloat = RandomUtils.nextFloat(0, 1);
    final MutableText o1 = Text.literal(String.valueOf(nextFloat)).styled(EnhancedCommands.STYLE_FOR_ACTUAL);
    final MutableText o2 = Text.literal(String.valueOf(value)).styled(EnhancedCommands.STYLE_FOR_EXPECTED);
    if (nextFloat < value) {
      return new TestResult(true, Text.translatable("blockPredicate.probability.pass", o1, o2).formatted(Formatting.GREEN));
    } else {
      return new TestResult(false, Text.translatable("blockPredicate.probability.fail", o1, o2).formatted(Formatting.RED));
    }
  }

  @Override
  public BlockPredicateType<?> getType() {
    return BlockPredicateTypes.RAND;
  }

  public static final class Parser implements FunctionLikeParser<RandBlockPredicate> {
    private float value;

    @Override
    public String functionName() {
      return "rand";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("blockPredicate.probability");
    }

    @Override
    public RandBlockPredicate getParseResult() {
      return new RandBlockPredicate(value);
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    public int maxParamsCount() {
      return 1;
    }

    @Override
    public void parseParameter(ArgumentParser parser) throws CommandSyntaxException {
      value = parser.reader.readFloat();
      if (value > 1) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().createWithContext(parser.reader, value, 1);
      }
      if (value < 0) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooLow().createWithContext(parser.reader, value, 0);
      }
    }
  }

  public enum Type implements BlockPredicateType<RandBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(ArgumentParser parser) throws CommandSyntaxException {
      return new Parser().parse(parser);
    }
  }
}
