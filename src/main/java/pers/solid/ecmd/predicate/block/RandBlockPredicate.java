package pers.solid.ecmd.predicate.block;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.TextUtil;

/**
 * <p>The predicate that passes only a probability test is passed.</p>
 * <h2>Syntax</h2>
 * <ul>
 *   <li>{@code rand(<value>)} - passes under a specified probability.</li>
 *   <li>{@code rand(<value>, <predicate>)} - passes when both probability test and another block predicate passes. Identical to {@code all(rand(value), <predicate>)}.</li>
 *   </ul>
 */
public record RandBlockPredicate(float value, @Nullable BlockPredicate predicate, Random random) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    if (predicate == null) {
      return "rand(" + value + ")";
    } else {
      return "rand(" + value + ", " + predicate.asString() + ")";
    }
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    if (predicate == null) {
      return random.nextFloat() < value;
    } else {
      return random.nextFloat() < value && predicate.test(cachedBlockPosition);
    }
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final float nextFloat = RandomUtils.nextFloat(0, 1);
    final MutableText o1 = Text.literal(String.valueOf(nextFloat)).styled(TextUtil.STYLE_FOR_ACTUAL);
    final MutableText o2 = Text.literal(String.valueOf(value)).styled(TextUtil.STYLE_FOR_EXPECTED);
    if (nextFloat < value) {
      return new TestResult(true, Text.translatable("enhanced_commands.argument.block_predicate.probability.pass", o1, o2).formatted(Formatting.GREEN));
    } else {
      return new TestResult(false, Text.translatable("enhanced_commands.argument.block_predicate.probability.fail", o1, o2).formatted(Formatting.RED));
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.RAND;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putFloat("value", value);
    if (predicate != null) {
      nbtCompound.put("predicate", predicate.createNbt());
    } else {
      nbtCompound.remove("predicate");
    }
  }

  public static final class Parser implements FunctionLikeParser<BlockPredicateArgument> {
    private float value;
    private @Nullable BlockPredicateArgument predicate;

    @Override
    public @NotNull String functionName() {
      return "rand";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.block_predicate.probability");
    }

    @Override
    public BlockPredicateArgument getParseResult(SuggestedParser parser) {
      return source -> new RandBlockPredicate(value, predicate == null ? null : predicate.apply(source), source.getWorld().getRandom());
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    public int maxParamsCount() {
      return 2;
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        value = parser.reader.readFloat();
        if (value > 1) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooHigh().createWithContext(parser.reader, value, 1);
        }
        if (value < 0) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.floatTooLow().createWithContext(parser.reader, value, 0);
        }
      } else if (paramIndex == 1) {
        predicate = BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      }
    }
  }

  public enum Type implements BlockPredicateType<RandBlockPredicate> {
    RAND_TYPE;

    @Override
    public @NotNull RandBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      if (nbtCompound.contains("predicate", NbtElement.COMPOUND_TYPE)) {
        return new RandBlockPredicate(nbtCompound.getFloat("value"), BlockPredicate.fromNbt(nbtCompound.getCompound("predicate"), world), world.getRandom());
      } else {
        return new RandBlockPredicate(nbtCompound.getFloat("value"), null, world.getRandom());
      }
    }

    @Override
    public @Nullable BlockPredicateArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }
}
