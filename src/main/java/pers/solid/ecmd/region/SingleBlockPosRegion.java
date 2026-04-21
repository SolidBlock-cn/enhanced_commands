package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

public record SingleBlockPosRegion(Vec3i pos) implements IntBackedRegion, CuboidRegion {
  public static final MapCodec<SingleBlockPosRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Vec3i.CODEC.fieldOf("pos").forGetter(SingleBlockPosRegion::pos)).apply(i, SingleBlockPosRegion::new));

  @Override
  public boolean contains(Vec3i vec3i) {
    return this.pos.equals(vec3i);
  }

  @Override
  public RegionType<SingleBlockPosRegion> getType() {
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
  public String expressAsString() {
    return "single(%s %s %s)".formatted(pos.getX(), pos.getY(), pos.getZ());
  }

  @Override
  public AABB minContainingBox() {
    return new AABB(new BlockPos(pos));
  }

  @Override
  public BoundingBox minContainingBlockBox() {
    return BoundingBox.fromCorners(pos, pos);
  }

  @Override
  public Iterator<BlockPos> iterator() {
    return Iterators.singletonIterator(new BlockPos(pos));
  }

  /**
   * 直接将坐标形式的内容解析为区域，例如 {@code 1 2 3} 等价于 {@code single(1 2 3)}，{@code ~~~} 等价于 {@code single(~~~)}。
   */
  public enum BareParser implements pers.solid.ecmd.parse.Parser<SingleBlockPosRegionProvider> {
    INSTANCE;

    @Override
    public @Nullable SingleBlockPosRegionProvider parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final int cursorBeforeParse = reader.getCursor();
      final EnhancedPosArgument argumentType = EnhancedPosArgument.blockPos();
      parseContext.addSuggestion((context, builder) -> {
        final SuggestionsBuilder builderOffset = builder.createOffset(cursorBeforeParse);
        return argumentType.listSuggestions(context, builderOffset);
      });
      if (reader.canRead()) {
        final char peek = reader.peek();
        if (StringReader.isAllowedNumber(peek) || peek == '~' || peek == '^') {
          final EnhancedCoordinates posArgument = argumentType.parse(reader);
          return new SingleBlockPosRegionProvider(posArgument);
        }
      }
      return null;
    }
  }

  public enum FunctionParser implements FunctionContentParser.SequentialParams<RegionProvider<SingleBlockPosRegion>> {
    INSTANCE;
    private @Nullable EnhancedCoordinates posArgument;

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 1;
    }

    @Override
    public RegionProvider<SingleBlockPosRegion> getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(posArgument, "posArgument");
      return new SingleBlockPosRegionProvider(posArgument);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      ArgumentType<EnhancedCoordinates> argumentType = EnhancedPosArgument.blockPos();
      posArgument = parseContext.parseAndSuggestArgument(argumentType);
    }
  }
}
