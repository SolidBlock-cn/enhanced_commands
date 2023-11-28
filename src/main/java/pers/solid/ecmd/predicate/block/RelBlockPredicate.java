package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.function.Function;

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
    return new TestResult(testResult.successes(), List.of(Text.translatable("enhanced_commands.argument.block_predicate.rel." + (testResult.successes() ? "pass" : "fail"), TextUtil.wrapVector(relPos)).formatted(testResult.successes() ? Formatting.GREEN : Formatting.RED)), List.of(testResult));
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.REL;
  }

  @Override
  public @NotNull String asString() {
    return "rel(%s %s %s, %s)".formatted(relPos.getX(), relPos.getY(), relPos.getZ(), predicate.asString());
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putIntArray("pos", new int[]{relPos.getX(), relPos.getY(), relPos.getZ()});
    nbtCompound.put("predicate", predicate.createNbt());
  }

  public static final class Parser implements FunctionParamsParser<BlockPredicateArgument> {
    private Function<ServerCommandSource, Vec3i> relPos;
    private BlockPredicateArgument blockPredicate;

    @Override
    public BlockPredicateArgument getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      Preconditions.checkNotNull(relPos, "relPos (argument 1)");
      Preconditions.checkNotNull(blockPredicate, "blockPredicate (argument 2)");
      return source -> new RelBlockPredicate(relPos.apply(source), blockPredicate.apply(source));
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
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        relPos = parser.parseAndSuggestVec3i();
      } else if (paramIndex == 1) {
        blockPredicate = BlockPredicateArgument.parse(commandRegistryAccess, parser, suggestionsOnly);
      }
    }
  }

  public enum Type implements BlockPredicateType<RelBlockPredicate> {
    REL_TYPE;

    @Override
    public @NotNull RelBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final int[] intArray = nbtCompound.getIntArray("pos");
      if (intArray.length != 3) {
        throw new IllegalArgumentException("The length of integer array 'pos' must be 3.");
      } else {
        return new RelBlockPredicate(new Vec3i(intArray[0], intArray[1], intArray[2]), BlockPredicate.fromNbt(nbtCompound.getCompound("predicate"), world));
      }
    }
  }
}
