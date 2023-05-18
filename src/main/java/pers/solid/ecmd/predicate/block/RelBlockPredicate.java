package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.SuggestionUtil;

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
    public void parseParameter(SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        final EnhancedPosArgumentType type = new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.INT_ONLY, true);
        final PosArgument argument = SuggestionUtil.suggestParserFromType(type, parser, suggestionsOnly);
        relPos = argument.toAbsoluteBlockPos(new ServerCommandSource(null, Vec3d.ZERO, Vec2f.ZERO, null, 0, null, null, null, null));
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicate.parse(parser, suggestionsOnly);
      }
    }
  }

  public enum Type implements BlockPredicateType<RelBlockPredicate> {
    INSTANCE;

    @Override
    public @Nullable BlockPredicate parse(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(parser, suggestionsOnly);
    }
  }
}
