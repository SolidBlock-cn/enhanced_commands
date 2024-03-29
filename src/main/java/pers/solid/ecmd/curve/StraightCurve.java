package pers.solid.ecmd.curve;

import com.google.common.collect.AbstractIterator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.NbtUtil;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.StringUtil;

import java.util.Iterator;
import java.util.function.Function;
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
  public @NotNull StraightCurve transformed(Function<Vec3d, Vec3d> transformation) {
    return new StraightCurve(transformation.apply(from), transformation.apply(to));
  }

  @Override
  public @NotNull String asString() {
    return "straight(%s, %s)".formatted(StringUtil.wrapPosition(from), StringUtil.wrapPosition(to));
  }

  @Override
  public @NotNull Box minContainingBox() {
    return new Box(from, to);
  }

  @Override
  public @NotNull CurveType<StraightCurve> getType() {
    return Type.INSTANCE;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.put("from", NbtUtil.fromVec3d(from));
    nbtCompound.put("to", NbtUtil.fromVec3d(to));
  }

  public enum Type implements CurveType<StraightCurve> {
    INSTANCE;

    @Override
    public @NotNull StraightCurve fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      return new StraightCurve(
          NbtUtil.toVec3d(nbtCompound.getCompound("from")),
          NbtUtil.toVec3d(nbtCompound.getCompound("to"))
      );
    }
  }

  private static final class Parser implements FunctionParamsParser<CurveArgument<StraightCurve>> {
    private PosArgument from, to;
    private boolean usingKeyword = false;

    @Override
    public CurveArgument<StraightCurve> getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) throws CommandSyntaxException {
      if (from == null || to == null) {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
      }
      return source -> new StraightCurve(from.toAbsolutePos(source), to.toAbsolutePos(source));
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final StringReader reader = parser.reader;
      final EnhancedPosArgumentType argumentType = EnhancedPosArgumentType.posPreferringCenteredInt();
      if (paramIndex == 0) {
        parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("from", suggestionsBuilder));
        final int cursorBeforeKeyword = reader.getCursor();
        final String unquotedString = reader.readUnquotedString();
        if (unquotedString.equals("from")) {
          parser.suggestionProviders.clear();
          usingKeyword = true;
          ParsingUtil.expectAndSkipWhitespace(reader);
          from = parser.parseAndSuggestArgument(argumentType);
          ParsingUtil.expectAndSkipWhitespace(reader);
          final int cursorBeforeKeyword2 = reader.getCursor();
          parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString("to", suggestionsBuilder));
          if (reader.readUnquotedString().equals("to")) {
            parser.suggestionProviders.clear();
            ParsingUtil.expectAndSkipWhitespace(reader);
            to = parser.parseAndSuggestArgument(argumentType);
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
          from = parser.parseAndSuggestArgument(argumentType);
        }
      } else if (paramIndex == 1) {
        to = parser.parseAndSuggestArgument(argumentType);
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
