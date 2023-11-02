package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionLikeParser;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.Iterator;
import java.util.stream.Stream;

public record CuboidOutlineRegion(BlockCuboidRegion region, int thickness) implements RegionBasedRegion.IntBacked<CuboidOutlineRegion, BlockCuboidRegion> {
  public static final DynamicCommandExceptionType NON_POSITIVE_THICKNESS = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.argument.region.cuboid_outline.non_positive_thickness", o));
  public static final Dynamic2CommandExceptionType TOO_THICK = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.argument.region.cuboid_outline.too_thick", a, b));

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
  public Stream<BlockPos> stream() {
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
    public @Nullable RegionArgument<CuboidOutlineRegion> parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
      return new Parser().parse(commandRegistryAccess, parser, suggestionsOnly);
    }
  }

  public static abstract sealed class AbstractParser<T extends Region> implements FunctionLikeParser<RegionArgument<T>> permits Parser, CuboidWallRegion.Parser {
    protected PosArgument fromPos, toPos;
    protected int thickness = 1;


    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      final EnhancedPosArgumentType type = EnhancedPosArgumentType.blockPos();
      if (paramIndex == 0) {
        fromPos = ParsingUtil.suggestParserFromType(type, parser, suggestionsOnly);
        if (parser.reader.canRead() && Character.isWhitespace(parser.reader.peek())) {
          parser.reader.skipWhitespace();
          // 在有接受到空格后，可直接接受第二个参数
          if (parser.reader.canRead()) {
            final char peek = parser.reader.peek();
            if (peek != ',' && peek != ')') {
              toPos = ParsingUtil.suggestParserFromType(type, parser, suggestionsOnly);
            }
          }
        }
      } else if (toPos == null && paramIndex == 1) {
        toPos = ParsingUtil.suggestParserFromType(type, parser, suggestionsOnly);
      } else if (toPos != null) {
        final int cursorBeforeInt = parser.reader.getCursor();
        thickness = parser.reader.readInt();
        if (thickness < 0) {
          parser.reader.setCursor(cursorBeforeInt);
          throw NON_POSITIVE_THICKNESS.createWithContext(parser.reader, thickness);
        }
      }
    }
  }

  public static final class Parser extends AbstractParser<CuboidOutlineRegion> {
    @Override
    public @NotNull String functionName() {
      return "cuboid_outline";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.argument.region.cuboid_outline");
    }

    @Override
    public RegionArgument<CuboidOutlineRegion> getParseResult(SuggestedParser parser) {
      return source -> new CuboidOutlineRegion(new BlockCuboidRegion(fromPos.toAbsoluteBlockPos(source), toPos.toAbsoluteBlockPos(source)), thickness);
    }
  }
}
