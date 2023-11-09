package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;

import java.util.Iterator;
import java.util.function.Function;

public record SingleBlockPosRegion(Vec3i vec3i) implements IntBackedRegion {
  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return this.vec3i.equals(vec3i);
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.SINGLE;
  }

  @Override
  public SingleBlockPosRegion transformedInt(Function<Vec3i, Vec3i> transformation) {
    return new SingleBlockPosRegion(transformation.apply(vec3i));
  }

  @Override
  public long numberOfBlocksAffected() {
    return 1;
  }

  @Override
  public @NotNull String asString() {
    return "single(%s %s %s)".formatted(vec3i.getX(), vec3i.getY(), vec3i.getZ());
  }

  @Override
  public @NotNull Box minContainingBox() {
    return new Box(new BlockPos(vec3i));
  }

  @Override
  public @NotNull BlockBox minContainingBlockBox() {
    return BlockBox.create(vec3i, vec3i);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Iterators.singletonIterator(new BlockPos(vec3i));
  }

  public enum Type implements RegionType<SingleBlockPosRegion> {
    INSTANCE;

    @Override
    public @Nullable RegionArgument<SingleBlockPosRegion> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return Parser.INSTANCE.parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  public enum Parser implements FunctionLikeParser<RegionArgument<SingleBlockPosRegion>> {
    INSTANCE;
    private PosArgument posArgument;

    @Override
    public @NotNull String functionName() {
      return "single";
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.region.single");
    }

    @Override
    public RegionArgument<SingleBlockPosRegion> getParseResult(SuggestedParser parser) throws CommandSyntaxException {
      final PosArgument posArgument1 = posArgument;
      posArgument = null;
      return source -> new SingleBlockPosRegion(posArgument1.toAbsoluteBlockPos(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      ArgumentType<PosArgument> argumentType = EnhancedPosArgumentType.blockPos();
      posArgument = parser.parseAndSuggestArgument(argumentType);
    }
  }
}
