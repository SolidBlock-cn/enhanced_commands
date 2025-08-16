package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.parse.FunctionParamsParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Iterator;
import java.util.stream.Stream;

public record CuboidOutlineRegion(BlockCuboidRegion region, int thickness) implements RegionBasedRegion.IntBacked<CuboidOutlineRegion, BlockCuboidRegion> {
  public static final DynamicCommandExceptionType NON_POSITIVE_THICKNESS = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.region.cuboid_outline.non_positive_thickness", o));
  public static final Dynamic2CommandExceptionType TOO_THICK = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.region.cuboid_outline.too_thick", a, b));
  public static final MapCodec<CuboidOutlineRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockCuboidRegion.CODEC.fieldOf("region").forGetter(CuboidOutlineRegion::region), Codec.INT.optionalFieldOf("thickness", 1).forGetter(CuboidOutlineRegion::thickness)).apply(i, CuboidOutlineRegion::new));

  public CuboidOutlineRegion {
    if (thickness <= 0) {
      throw new IllegalArgumentException(NON_POSITIVE_THICKNESS.create(thickness));
    }
    final int maxAcceptableThickness = getMaxAcceptableThickness(region);
    if (thickness > maxAcceptableThickness) {
      throw new IllegalArgumentException(TOO_THICK.create(maxAcceptableThickness, thickness));
    }
  }

  public static int getMaxAcceptableThickness(BlockCuboidRegion blockCuboidRegion) {
    return NumberUtils.min(Math.floorDiv(blockCuboidRegion.maxX() - blockCuboidRegion.minX() + 1, 2), Math.floorDiv(blockCuboidRegion.maxY() - blockCuboidRegion.minY() + 1, 2), Math.floorDiv(blockCuboidRegion.maxZ() - blockCuboidRegion.minZ() + 1, 2));
  }

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    try {
      return region.contains(vec3i) && region.expanded(-thickness).contains(vec3i);
    } catch (IllegalArgumentException illegalArgumentException) {
      // min max wrong
      return true;
    }
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Iterators.concat(Iterators.transform(decompose().iterator(), BlockCuboidRegion::iterator));
  }

  @Override
  public Stream<@NotNull BlockPos> stream() {
    return decompose().flatMap(Region::stream);
  }

  public @NotNull Stream<BlockCuboidRegion> decompose() {
    final Stream<BlockCuboidRegion> walls = new CuboidWallRegion(region, thickness).decompose();
    return Streams.concat(
        Stream.of(new BlockCuboidRegion(region.minX(), region.maxY() - thickness + 1, region.minZ(), region.maxX(), region.maxY(), region.maxZ())),
        walls,
        Stream.of(new BlockCuboidRegion(region.minX(), region.minY(), region.minZ(), region.maxX(), region.minY() + thickness - 1, region.maxZ()))
    );
  }

  @Override
  public CuboidOutlineRegion newRegion(BlockCuboidRegion region) {
    return new CuboidOutlineRegion(region, thickness);
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.CUBOID_OUTLINE;
  }

  @Override
  public long numberOfBlocksAffected() {
    return region.numberOfBlocksAffected() - region.expanded(-thickness).numberOfBlocksAffected();
  }

  @Override
  public @NotNull BlockBox minContainingBlockBox() {
    return region.blockBox();
  }

  @Override
  public @NotNull String asString() {
    return String.format("cuboid_outline(%s %s %s, %s %s %s, %s)", region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ(), thickness);
  }

  @Override
  public @Nullable Box minContainingBox() {
    return region.minContainingBox();
  }

  public enum Type implements RegionType<CuboidOutlineRegion> {
    CUBOID_OUTLINE_TYPE;

    @Override
    public String functionName() {
      return "cuboid_outline";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.region.cuboid_outline");
    }

    @Override
    public FunctionParamsParser<CuboidOutlineRegionArgument> functionParamsParser() {
      return new Parser();
    }

    @Override
    public @NotNull MapCodec<CuboidOutlineRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends CuboidOutlineRegionArgument> getArgumentCodec() {
      return CuboidOutlineRegionArgument.CODEC;
    }
  }

  public static abstract sealed class AbstractParser<R extends RegionArgument<?>> implements FunctionParamsParser<R> permits Parser, CuboidWallRegion.Parser {
    protected EnhancedPosArgument fromPos, toPos;
    protected int thickness = 1;
    protected int cursorBefore = 0, cursorAfter = 0;

    @Override
    public void setCursorBeforeFunctionName(int cursorBeforeFunctionName) {
      this.cursorBefore = cursorBeforeFunctionName;
    }

    @Override
    public R parseAfterLeftParenthesis(ParseContext<?> parseContext) throws CommandSyntaxException {
      final R parsed = FunctionParamsParser.super.parseAfterLeftParenthesis(parseContext);
      cursorAfter = parseContext.reader().getCursor();
      return parsed;
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final EnhancedPosArgumentType type = EnhancedPosArgumentType.blockPos();
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
        fromPos = parseContext.parseAndSuggestArgument(type);
        if (reader.canRead() && Character.isWhitespace(reader.peek())) {
          reader.skipWhitespace();
          // 在有接受到空格后，可直接接受第二个参数
          if (reader.canRead()) {
            final char peek = reader.peek();
            if (peek != ',' && peek != ')') {
              toPos = parseContext.parseAndSuggestArgument(type);
            }
          }
        }
      } else if (toPos == null && paramIndex == 1) {
        toPos = parseContext.parseAndSuggestArgument(type);
      } else if (toPos != null) {
        final int cursorBeforeInt = reader.getCursor();
        thickness = reader.readInt();
        if (thickness <= 0) {
          final int cursorAfterThickness = reader.getCursor();
          reader.setCursor(cursorBeforeInt);
          throw CommandSyntaxExceptionExtension.withCursorEnd(NON_POSITIVE_THICKNESS.createWithContext(reader, thickness), cursorAfterThickness);
        }
      }
    }
  }

  public static final class Parser extends AbstractParser<CuboidOutlineRegionArgument> {
    @Override
    public CuboidOutlineRegionArgument getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new CuboidOutlineRegionArgument(new BlockCuboidRegionArgument(fromPos, toPos), thickness);
    }
  }
}
