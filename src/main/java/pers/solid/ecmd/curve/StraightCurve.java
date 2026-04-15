package pers.solid.ecmd.curve;

import com.google.common.collect.AbstractIterator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.StringUtil;

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 直线段，由两个点连接而成的直线。语法为 {@code straight(<from>, <to>)} 或 {@code straight(from <from> to <to>)}。
 */
public record StraightCurve(Vec3 from, Vec3 to) implements Curve {
  public static final MapCodec<StraightCurve> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Vec3.CODEC.fieldOf("from").forGetter(StraightCurve::from), Vec3.CODEC.fieldOf("to").forGetter(StraightCurve::to)).apply(i, StraightCurve::new));

  @Override
  public Stream<BlockPos> streamBlockPos() {
    final BlockPos fromBlockPos = BlockPos.containing(from);
    final BlockPos toBlockPos = BlockPos.containing(to);
    if (fromBlockPos.equals(toBlockPos)) {
      return Stream.of(fromBlockPos);
    }
    final BlockPos d = toBlockPos.subtract(fromBlockPos);
    if (d.getX() == 0 && (d.getY() == 0 || d.getZ() == 0) || (d.getY() == 0 && d.getZ() == 0)) {
      // 如果至少两个坐标轴之差为零，那么绘制直线。
      return BlockPos.betweenClosedStream(fromBlockPos, toBlockPos);
    }

    final int dx = Math.abs(d.getX());
    final int dy = Math.abs(d.getY());
    final int dz = Math.abs(d.getZ());
    final int dMax = NumberUtils.max(dx, dy, dz);

    final Direction.Axis maxAxis, otherAxis1, otherAxis2;
    if (dMax == dx) {
      maxAxis = Direction.Axis.X;
      otherAxis1 = Direction.Axis.Y;
      otherAxis2 = Direction.Axis.Z;
    } else if (dMax == dy) {
      maxAxis = Direction.Axis.Y;
      otherAxis1 = Direction.Axis.X;
      otherAxis2 = Direction.Axis.Z;
    } else {
      maxAxis = Direction.Axis.Z;
      otherAxis1 = Direction.Axis.X;
      otherAxis2 = Direction.Axis.Y;
    }

    final int dMaxFrom = fromBlockPos.get(maxAxis);
    final int dMaxTo = toBlockPos.get(maxAxis);
    final IntStream initialStream = dMaxTo > dMaxFrom ? IntStream.rangeClosed(dMaxFrom, dMaxTo) : IntStream.rangeClosed(-dMaxFrom, -dMaxTo).map(operand -> -operand);
    final BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
    final double grad1 = (to.get(otherAxis1) - from.get(otherAxis1)) / (to.get(maxAxis) - from.get(maxAxis));
    final double grad2 = (to.get(otherAxis2) - from.get(otherAxis2)) / (to.get(maxAxis) - from.get(maxAxis));
    return initialStream.mapToObj(value -> {
      // 例如，假如 maxAxis 为 x，componentAxis 为 y，则：
      // (y - fromY) / (x - fromX) = (toY - fromX) / (toX - fromX)
      final double componentAxis1 = grad1 * (value + 0.5d - from.get(maxAxis)) + from.get(otherAxis1);
      final double componentAxis2 = grad2 * (value + 0.5d - from.get(maxAxis)) + from.get(otherAxis2);
      return switch (maxAxis) {
        case X -> mutable.set(value, componentAxis1, componentAxis2);
        case Y -> mutable.set(componentAxis1, value, componentAxis2);
        case Z -> mutable.set(componentAxis1, componentAxis2, value);
      };
    });
  }

  @Override
  public Iterator<Vec3> iteratePoints(Number interval) {
    final Vec3 relVec = to.subtract(from);
    final double totalLength = relVec.length();
    final Vec3 unitVec = relVec.scale(1 / totalLength);
    return new AbstractIterator<>() {
      private double walkedLength = 0;

      @Override
      protected Vec3 computeNext() {
        if (walkedLength > totalLength) {
          return endOfData();
        }
        final Vec3 next = from.add(unitVec.scale(walkedLength));
        walkedLength += interval.doubleValue();
        return next;
      }
    };
  }

  @Override
  public double length() {
    return to.subtract(from).length();
  }

  @Override
  public StraightCurve transformed(Function<Vec3, Vec3> transformation) {
    return new StraightCurve(transformation.apply(from), transformation.apply(to));
  }

  @Override
  public String asString() {
    return "straight(%s, %s)".formatted(StringUtil.wrapVector(from), StringUtil.wrapVector(to));
  }

  @Override
  public AABB minContainingBox() {
    return new AABB(from, to);
  }

  @Override
  public CurveType<StraightCurve> getType() {
    return CurveTypes.STRAIGHT;
  }

  protected static final class Parser implements FunctionContentParser.SequentialParams<CurveProvider<StraightCurve>> {
    private @Nullable EnhancedCoordinates from, to;
    private boolean usingKeyword = false;

    @Override
    public CurveProvider<StraightCurve> getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      if (from == null || to == null) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
      }
      return new StraightCurveProvider(from, to);
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final EnhancedPosArgument argumentType = EnhancedPosArgument.posPreferringCenteredInt();
      if (paramIndex == 0) {
        {
          if (reader.canRead() && !SharedSuggestionProvider.matchesSubStr(reader.getRemaining().toLowerCase(), "from")) {
            // 避免在输入了部分坐标后仍建议输入 “from” 的情况
            parseContext.clearSuggestion();
          }
          from = parseContext.parseAndSuggestArgument(argumentType);
        }
      } else if (paramIndex == 1) {
        to = parseContext.parseAndSuggestArgument(argumentType);
      }
    }

    @Override
    public void parseWithinParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final EnhancedPosArgument argumentType = EnhancedPosArgument.posPreferringCenteredInt();
      parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("from", suggestionsBuilder).buildFuture());
      final int cursorBeforeKeyword = reader.getCursor();
      final String unquotedString = reader.readUnquotedString();
      if (unquotedString.equals("from")) {
        parseContext.clearSuggestion();
        usingKeyword = true;
        ParsingUtil.expectAndSkipWhitespace(reader);
        from = parseContext.parseAndSuggestArgument(argumentType);
        ParsingUtil.expectAndSkipWhitespace(reader);
        final int cursorBeforeKeyword2 = reader.getCursor();
        parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("to", suggestionsBuilder).buildFuture());
        if (reader.readUnquotedString().equals("to")) {
          parseContext.clearSuggestion();
          ParsingUtil.expectAndSkipWhitespace(reader);
          to = parseContext.parseAndSuggestArgument(argumentType);
        } else {
          reader.setCursor(cursorBeforeKeyword2);
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, "to");
        }
      } else {
        reader.setCursor(cursorBeforeKeyword);
        parseSequentialParameters(parseContext);
      }
    }

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }
  }
}
