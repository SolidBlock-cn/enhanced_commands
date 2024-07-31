package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * To test which a block is exposed in the specified directions and the specified type.
 *
 * @param exposureType The exposure type. By default, it is exposed to empty collision.
 * @param directions   The directions to test exposure.
 */
public record ExposeBlockPredicate(@NotNull ExposureType exposureType, @NotNull List<@NotNull Direction> directions) implements BlockPredicate {
  public static final MapCodec<ExposeBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(ExposeBlockPredicate::new, ExposureType.CODEC.fieldOf("exposure_type").forGetter(ExposeBlockPredicate::exposureType), Direction.CODEC.listOf().optionalFieldOf("directions", List.of(Direction.values())).forGetter(ExposeBlockPredicate::directions)));

  @Override
  public @NotNull String asString() {
    return "expose(" + exposureType.asString() + ", " + String.join(" ", Iterables.transform(directions, Direction::asString)) + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    for (Direction direction : directions) {
      var offsetCachedBlockPosition = new CachedBlockPosition(cachedBlockPosition.getWorld(), cachedBlockPosition.getBlockPos().offset(direction), false);
      if (exposureType.test(offsetCachedBlockPosition, direction))
        return true;
    }
    return false;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    List<TestResult> testResults = new ArrayList<>();
    boolean result = false;
    for (Direction direction : directions) {
      var offsetCachedBlockPosition = new CachedBlockPosition(cachedBlockPosition.getWorld(), cachedBlockPosition.getBlockPos().offset(direction), false);
      final boolean test = exposureType.test(offsetCachedBlockPosition, direction);
      testResults.add(TestResult.of(test, Text.translatable("enhanced_commands.block_predicate.expose.side." + (test ? "pass" : "fail"), TextUtil.wrapDirection(direction))));
      if (test) {
        result = true;
      }
    }
    if (testResults.size() == 1) {
      return testResults.get(0);
    } else {
      return TestResult.of(result, Text.translatable("enhanced_commands.block_predicate.expose." + (result ? "pass" : "fail")), testResults);
    }
  }

  @Override
  public @NotNull BlockPredicateType<?> getType() {
    return BlockPredicateTypes.EXPOSE;
  }

  /**
   * The type to test which the block is exposed.
   */
  public enum ExposureType implements StringIdentifiable {
    /**
     * The block is exposed to a block with empty collision shape, such as air, torch or flower.
     */
    EMPTY_COLLISION("empty_collision") {
      @Override
      public boolean test(CachedBlockPosition offsetCachedBlockPosition, Direction direction) {
        return offsetCachedBlockPosition.getBlockState().getCollisionShape(offsetCachedBlockPosition.getWorld(), offsetCachedBlockPosition.getBlockPos()).isEmpty();
      }
    },
    /**
     * The block is exposed to a block with an empty collision shape at that side, such as a bottom-half slab below it.
     */
    EMPTY_SIDE_COLLISION("empty_side_collision") {
      @Override
      public boolean test(CachedBlockPosition offsetCachedBlockPosition, Direction direction) {
        return offsetCachedBlockPosition.getBlockState().getCollisionShape(offsetCachedBlockPosition.getWorld(), offsetCachedBlockPosition.getBlockPos()).getFace(direction.getOpposite()).isEmpty();
      }
    },
    /**
     * The block is exposed to air.
     */
    AIR("air") {
      @Override
      public boolean test(CachedBlockPosition offsetCachedBlockPosition, Direction direction) {
        return offsetCachedBlockPosition.getBlockState().isAir();
      }
    },
    /**
     * The block is exposed to a block with non-full collision shape at that side, such as next to a slab block horizontally.
     */
    INCOMPLETE_SIDE_COLLISION("incomplete_side_collision") {
      @Override
      public boolean test(CachedBlockPosition offsetCachedBlockPosition, Direction direction) {
        return !VoxelShapes.combine(VoxelShapes.fullCube(), offsetCachedBlockPosition.getBlockState().getCollisionShape(offsetCachedBlockPosition.getWorld(), offsetCachedBlockPosition.getBlockPos()).getFace(direction.getOpposite()), BooleanBiFunction.ONLY_FIRST).isEmpty();
      }
    };
    public static final EnumCodec<ExposureType> CODEC = StringIdentifiable.createCodec(ExposureType::values);
    private final String name;

    ExposureType(String name) {
      this.name = name;
    }

    @Override
    public String asString() {
      return name;
    }

    public abstract boolean test(CachedBlockPosition offsetCachedBlockPosition, Direction direction);

    public MutableText getDisplayName() {
      return Text.translatable("enhanced_commands.exposure_type." + name);
    }
  }

  public enum Type implements BlockPredicateType<ExposeBlockPredicate> {
    EXPOSE_TYPE;

    @Override
    public @NotNull MapCodec<ExposeBlockPredicate> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionParamsParser<ExposeBlockPredicate> {
    private final Set<@NotNull Direction> directions = new TreeSet<>();
    private ExposureType exposureType;

    @Override
    public ExposeBlockPredicate getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) {
      return new ExposeBlockPredicate(exposureType, directions.isEmpty() ? List.of(Direction.values()) : List.copyOf(directions));
    }

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 2;
    }

    @Override
    public void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException {
      if (paramIndex == 0) {
        parser.suggestionProviders.clear();
        exposureType = parser.parseAndSuggestEnums(ExposureType.values(), ExposureType::getDisplayName, ExposureType.CODEC);
      } else if (paramIndex == 1) {
        do {
          parser.suggestionProviders.clear();
          parser.reader.skipWhitespace();
          if (directions.isEmpty()) {
            parser.suggestionProviders.add((context, suggestionsBuilder) -> {
              ParsingUtil.suggestString("all", Text.translatable("enhanced_commands.direction.all"), suggestionsBuilder);
              ParsingUtil.suggestString("horizontal", Text.translatable("enhanced_commands.direction.horizontal"), suggestionsBuilder);
              ParsingUtil.suggestString("vertical", Text.translatable("enhanced_commands.direction.vertical"), suggestionsBuilder);
            });
          }
          parser.suggestionProviders.add((context, builder) -> ParsingUtil.suggestDirections(builder));
          final int cursorBeforeReadString = parser.reader.getCursor();
          final String id = parser.reader.readString();
          if (id.isEmpty())
            break;
          if (directions.isEmpty()) {
            switch (id) {
              case "all" -> {
                Direction.stream().forEach(directions::add);
                continue;
              }
              case "horizontal" -> {
                Direction.Type.HORIZONTAL.forEach(directions::add);
                continue;
              }
              case "vertical" -> {
                Direction.Type.VERTICAL.forEach(directions::add);
                continue;
              }
            }
          }
          final Direction direction = Direction.byName(id);
          if (direction == null) {
            parser.reader.setCursor(cursorBeforeReadString);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
          }
          directions.add(direction);
        } while (parser.reader.canRead() && Character.isWhitespace(parser.reader.peek()));
        if (directions.isEmpty()) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
        }
      }
    }
  }
}
