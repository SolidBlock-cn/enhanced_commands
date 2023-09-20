package pers.solid.ecmd.curve;

import com.google.common.collect.AbstractIterator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.GeoUtil;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 直线段，由两个点连接而成的直线。语法为 {@code straight(<from>, <to>)} 或 {@code straight(from <from> to <to>)}。
 */
public record StraightCurve(Vec3d from, Vec3d to) implements Curve {
  @Override
  public @NotNull Stream<BlockPos> streamBlockPos() {
    final BlockPos fromBlockPos = BlockPos.ofFloored(from);
    final BlockPos toBlockPos = BlockPos.ofFloored(to);
    if (fromBlockPos.equals(toBlockPos)) {
      return Stream.of(fromBlockPos);
    }
    final BlockPos d = toBlockPos.subtract(fromBlockPos);
    if (d.getX() == 0 && (d.getY() == 0 || d.getZ() == 0) || (d.getY() == 0 && d.getZ() == 0)) {
      // 如果至少两个坐标轴之差为零，那么绘制直线。
      return BlockPos.stream(fromBlockPos, toBlockPos);
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

    final int dMaxFrom = fromBlockPos.getComponentAlongAxis(maxAxis);
    final int dMaxTo = toBlockPos.getComponentAlongAxis(maxAxis);
    final IntStream initialStream = dMaxTo > dMaxFrom ? IntStream.rangeClosed(dMaxFrom, dMaxTo) : IntStream.rangeClosed(-dMaxFrom, -dMaxTo).map(operand -> -operand);
    final BlockPos.Mutable mutable = new BlockPos.Mutable();
    final double grad1 = (to.getComponentAlongAxis(otherAxis1) - from.getComponentAlongAxis(otherAxis1)) / (to.getComponentAlongAxis(maxAxis) - from.getComponentAlongAxis(maxAxis));
    final double grad2 = (to.getComponentAlongAxis(otherAxis2) - from.getComponentAlongAxis(otherAxis2)) / (to.getComponentAlongAxis(maxAxis) - from.getComponentAlongAxis(maxAxis));
    return initialStream.mapToObj(value -> {
      // 例如，假如 maxAxis 为 x，componentAxis 为 y，则：
      // (y - fromY) / (x - fromX) = (toY - fromX) / (toX - fromX)
      final double componentAxis1 = grad1 * (value + 0.5d - from.getComponentAlongAxis(maxAxis)) + from.getComponentAlongAxis(otherAxis1);
      final double componentAxis2 = grad2 * (value + 0.5d - from.getComponentAlongAxis(maxAxis)) + from.getComponentAlongAxis(otherAxis2);
      return switch (maxAxis) {
        case X -> mutable.set(value, componentAxis1, componentAxis2);
        case Y -> mutable.set(componentAxis1, value, componentAxis2);
        case Z -> mutable.set(componentAxis1, componentAxis2, value);
      };
    });
  }

  @Override
  public @NotNull Iterator<Vec3d> iteratePoints(Number interval) {
    final Vec3d relVec = to.subtract(from);
    final double totalLength = relVec.length();
    final Vec3d unitVec = relVec.multiply(1 / totalLength);
    return new AbstractIterator<>() {
      private double walkedLength = 0;

      @Override
      protected Vec3d computeNext() {
        if (walkedLength > totalLength) {
          return endOfData();
        }
        final Vec3d next = from.add(unitVec.multiply(walkedLength));
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
  public @NotNull Curve moved(@NotNull Vec3d relativePos) {
    return new StraightCurve(from.add(relativePos), to.add(relativePos));
  }

  @Override
  public @NotNull Curve rotated(@NotNull Vec3d pivot, @NotNull BlockRotation blockRotation) {
    return new StraightCurve(GeoUtil.rotate(from, blockRotation, pivot), GeoUtil.rotate(to, blockRotation, pivot));
  }

  @Override
  public @NotNull Curve mirrored(@NotNull Vec3d pivot, Direction.@NotNull Axis axis) {
    return new StraightCurve(GeoUtil.mirror(from, axis, pivot), GeoUtil.mirror(to, axis, pivot));
  }

  @Override
  public @NotNull String asString() {
    return "line(%s, %s)".formatted(StringUtil.wrapPosition(from), StringUtil.wrapPosition(to));
  }

  @Override
  public @NotNull CurveType<StraightCurve> getCurveType() {
    return Type.INSTANCE;
  }

  public enum Type implements CurveType<StraightCurve> {
    INSTANCE;

    @Override
    public @Nullable CurveArgument<StraightCurve> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  private static final class Parser implements FunctionLikeParser<CurveArgument<StraightCurve>> {
    private PosArgument from, to;
    private boolean usingKeyword = false;

    @Override
    public @NotNull String functionName() {
      return "straight";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhancedCommands.argument.curve.straight");
    }

    @Override
    public CurveArgument<StraightCurve> getParseResult(SuggestedParser parser) throws CommandSyntaxException {
      if (from == null || to == null) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
      }
      return source -> new StraightCurve(from.toAbsolutePos(source), to.toAbsolutePos(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      final EnhancedPosArgumentType argumentType = new EnhancedPosArgumentType(EnhancedPosArgumentType.Behavior.PREFER_INT, false);
      if (paramIndex == 0) {
        parser.suggestionProviders.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("from", suggestionsBuilder));
        final int cursorBeforeKeyword = reader.getCursor();
        final String unquotedString = reader.readUnquotedString();
        if (unquotedString.equals("from")) {
          parser.suggestionProviders.clear();
          usingKeyword = true;
          StringUtil.expectAndSkipWhitespace(reader);
          from = SuggestionUtil.suggestParserFromType(argumentType, parser, suggestionsOnly);
          StringUtil.expectAndSkipWhitespace(reader);
          final int cursorBeforeKeyword2 = reader.getCursor();
          parser.suggestionProviders.add((context, suggestionsBuilder) -> SuggestionUtil.suggestString("to", suggestionsBuilder));
          if (reader.readUnquotedString().equals("to")) {
            parser.suggestionProviders.clear();
            StringUtil.expectAndSkipWhitespace(reader);
            to = SuggestionUtil.suggestParserFromType(argumentType, parser, suggestionsOnly);
          } else {
            reader.setCursor(cursorBeforeKeyword2);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, "to");
          }
        } else {
          reader.setCursor(cursorBeforeKeyword);
          if (reader.canRead() && !CommandSource.shouldSuggest(reader.getRemaining().toLowerCase(), "from")) {
            // 避免在输入了部分坐标后仍建议输入 “from” 的情况
            parser.suggestionProviders.clear();
          }
          from = SuggestionUtil.suggestParserFromType(argumentType, parser, suggestionsOnly);
        }
      } else if (paramIndex == 1) {
        to = SuggestionUtil.suggestParserFromType(argumentType, parser, suggestionsOnly);
      }
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return usingKeyword ? 1 : 2;
    }
  }
}
