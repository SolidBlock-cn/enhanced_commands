package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;

import java.util.List;

public record RelBlockPredicate(@NotNull Vec3i relPos, @NotNull BlockPredicate predicate) implements BlockPredicate {

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    final BlockPos pos = cachedBlockPosition.getBlockPos().add(relPos);
    return predicate.test(new CachedBlockPosition(cachedBlockPosition.getWorld(), pos, false));
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final BlockPos pos = cachedBlockPosition.getBlockPos().add(relPos);
    final TestResult testResult = predicate.testAndDescribe(new CachedBlockPosition(cachedBlockPosition.getWorld(), pos, false));
    return new TestResult(testResult.successes(), List.of(Text.translatable("blockPredicate.rel." + (testResult.successes() ? "pass" : "fail"), EnhancedCommands.wrapBlockPos(relPos)).formatted(testResult.successes() ? Formatting.GREEN : Formatting.RED)), List.of(testResult));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.REL;
  }

  @Override
  public @NotNull String asString() {
    return "rel(%s %s %s, %s)".formatted(relPos.getX(), relPos.getY(), relPos.getZ(), predicate.asString());
  }

  public static final class Parser implements FunctionLikeParser<RelBlockPredicate> {
    private Vec3i relPos;
    private BlockPredicate blockPredicate;

    @Override
    public @NotNull String functionName() {
      return "rel";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("blockPredicate.rel");
    }

    @Override
    public RelBlockPredicate getParseResult() {
      Preconditions.checkNotNull(relPos, "relPos (argument 1)");
      Preconditions.checkNotNull(blockPredicate, "blockPredicate (argument 2)");
      return new RelBlockPredicate(relPos, blockPredicate);
    }

    @Override
    public int minParamsCount() {
      return 2;
    }

    @Override
    public int maxParamsCount() {
      return 2;
    }

    @Override
    public void parseParameter(SuggestedParser parser, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        parser.suggestions.clear();
        final int x, y, z;
        x = parser.reader.readInt();
        parser.reader.skipWhitespace();
        y = parser.reader.readInt();
        parser.reader.skipWhitespace();
        z = parser.reader.readInt();
        relPos = new BlockPos(x, y, z);
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicate.parse(parser);
      }
    }
  }

  public enum Type implements BlockPredicateType<RelBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(SuggestedParser parser) throws CommandSyntaxException {
      return new Parser().parse(parser);
    }
  }
}
