package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.parse.FunctionParamsParser;

import java.util.Iterator;
import java.util.function.Function;

public record SingleBlockPosRegion(Vec3i pos) implements IntBackedRegion {
  public static final MapCodec<SingleBlockPosRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Vec3i.CODEC.fieldOf("pos").forGetter(SingleBlockPosRegion::pos)).apply(i, SingleBlockPosRegion::new));

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return this.pos.equals(vec3i);
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.SINGLE;
  }

  @Override
  public SingleBlockPosRegion transformedInt(Function<Vec3i, Vec3i> transformation) {
    return new SingleBlockPosRegion(transformation.apply(pos));
  }

  @Override
  public long numberOfBlocksAffected() {
    return 1;
  }

  @Override
  public @NotNull String asString() {
    return "single(%s %s %s)".formatted(pos.getX(), pos.getY(), pos.getZ());
  }

  @Override
  public @NotNull Box minContainingBox() {
    return new Box(new BlockPos(pos));
  }

  @Override
  public @NotNull BlockBox minContainingBlockBox() {
    return BlockBox.create(pos, pos);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Iterators.singletonIterator(new BlockPos(pos));
  }

  public enum Type implements RegionType<SingleBlockPosRegion> {
    INSTANCE;

    @Override
    public String functionName() {
      return "single";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.region.single");
    }

    @Override
    public FunctionParamsParser<RegionArgument> functionParamsParser() {
      return Parser.INSTANCE;
    }

    @Override
    public @NotNull MapCodec<SingleBlockPosRegion> getCodec() {
      return CODEC;
    }
  }

  public enum Parser implements FunctionParamsParser<RegionArgument> {
    INSTANCE;
    private PosArgument posArgument;

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }

    @Override
    public RegionArgument getParseResult(CommandRegistryAccess registryAccess, SuggestedParser<?> parser) throws CommandSyntaxException {
      final PosArgument posArgument1 = posArgument;
      posArgument = null;
      return source -> new SingleBlockPosRegion(posArgument1.toAbsoluteBlockPos(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      ArgumentType<PosArgument> argumentType = EnhancedPosArgumentType.blockPos();
      posArgument = parser.parseAndSuggestArgument(argumentType);
    }
  }
}
