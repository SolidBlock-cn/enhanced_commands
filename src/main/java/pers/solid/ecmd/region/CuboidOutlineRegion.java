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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedCoordinates;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;

import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

public record CuboidOutlineRegion(BlockCuboidRegion region, int thickness) implements RegionBasedRegion.IntBacked<CuboidOutlineRegion, BlockCuboidRegion> {
  public static final DynamicCommandExceptionType NON_POSITIVE_THICKNESS = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.region.cuboid_outline.non_positive_thickness", o));
  public static final Dynamic2CommandExceptionType TOO_THICK = new Dynamic2CommandExceptionType((a, b) -> Component.translatable("enhanced_commands.region.cuboid_outline.too_thick", a, b));
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
  public boolean contains(Vec3i vec3i) {
    try {
      return region.contains(vec3i) && region.expanded(-thickness).contains(vec3i);
    } catch (IllegalArgumentException illegalArgumentException) {
      // min max wrong
      return true;
    }
  }

  @Override
  public Iterator<BlockPos> iterator() {
    return Iterators.concat(Iterators.transform(decompose().iterator(), BlockCuboidRegion::iterator));
  }

  @Override
  public Stream<BlockPos> stream() {
    return decompose().flatMap(Region::stream);
  }

  public Stream<BlockCuboidRegion> decompose() {
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
  public RegionType<CuboidOutlineRegion> getType() {
    return RegionTypes.CUBOID_OUTLINE;
  }

  @Override
  public long numberOfBlocksAffected() {
    return region.numberOfBlocksAffected() - region.expanded(-thickness).numberOfBlocksAffected();
  }

  @Override
  public BoundingBox minContainingBlockBox() {
    return region.blockBox();
  }

  @Override
  public String asString() {
    return String.format("cuboid_outline(%s %s %s, %s %s %s, %s)", region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ(), thickness);
  }

  @Override
  public @Nullable AABB minContainingBox() {
    return region.minContainingBox();
  }

  public static abstract sealed class AbstractParser<R extends RegionProvider<?>> implements FunctionContentParser.SequentialParams<R> permits Parser, CuboidWallRegion.Parser {
    protected @Nullable EnhancedCoordinates fromPos, toPos;
    protected int thickness = 1;
    protected int cursorBeforeFunctionName = 0, cursorAfterParenthesis = 0;

    @Override
    public void onBeforeParentheses(String functionName, int cursorBeforeFunctionName, int cursorAfterFunctionName) {
      this.cursorBeforeFunctionName = cursorBeforeFunctionName;
    }

    @Override
    public void onAfterParentheses(int cursorAfterParentheses) {
      this.cursorAfterParenthesis = cursorAfterParentheses;
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final EnhancedPosArgument type = EnhancedPosArgument.blockPos();
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
          throw EnhancedCommandSyntaxException.withCursorEnd(NON_POSITIVE_THICKNESS.createWithContext(reader, thickness), cursorAfterThickness);
        }
      }
    }
  }

  public static final class Parser extends AbstractParser<CuboidOutlineRegionProvider> {
    @Override
    public CuboidOutlineRegionProvider getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(fromPos, "fromPos");
      Objects.requireNonNull(toPos, "toPos");
      return new CuboidOutlineRegionProvider(new BlockCuboidRegionProvider(fromPos, toPos), thickness);
    }
  }
}
