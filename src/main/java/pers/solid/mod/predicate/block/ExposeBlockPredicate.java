package pers.solid.mod.predicate.block;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.NotNull;
import pers.solid.mod.EnhancedCommands;
import pers.solid.mod.argument.SuggestedParser;
import pers.solid.mod.command.TestResult;
import pers.solid.mod.util.SuggestionUtil;

import java.util.*;

/**
 * To test which a block is exposed in the specified directions and the specified type.
 *
 * @param exposureType The exposure type. By default, it is exposed to empty collision.
 * @param directions   The directions to test exposure.
 */
public record ExposeBlockPredicate(@NotNull ExposureType exposureType, @NotNull Iterable<Direction> directions) implements BlockPredicate {
  @Override
  public @NotNull String asString() {
    return "expose(" + exposureType.asString() + ", " + String.join(" ", Iterables.transform(directions, Direction::asString)) + ")";
  }

  @Override
  public boolean test(CachedBlockPosition cachedBlockPosition) {
    for (Direction direction : directions) {
      var offsetCachedBlockPosition = new CachedBlockPosition(cachedBlockPosition.getWorld(), cachedBlockPosition.getBlockPos().offset(direction), false);
      if (exposureType.test(offsetCachedBlockPosition, direction)) return true;
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
      testResults.add(new TestResult(test, Text.translatable("blockPredicate.expose.side." + (test ? "pass":"fail"), EnhancedCommands.wrapDirection(direction)).formatted(test ? Formatting.GREEN : Formatting.RED)));
      if (test) {
        result = true;
      }
    }
    if (testResults.size() == 1) {
      return testResults.get(0);
    } else {
      return new TestResult(result, List.of(Text.translatable("blockPredicate.expose." + (result ? "pass" : "fail")).formatted(result ? Formatting.GREEN : Formatting.RED)), testResults);
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
    EMPTY_SIDE_COLLISION("empty_collision_side") {
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
    private final String name;
    public static final Codec<ExposureType> CODEC = StringIdentifiable.createCodec(ExposureType::values);

    ExposureType(String name) {
      this.name = name;
    }

    @Override
    public String asString() {
      return name;
    }

    public abstract boolean test(CachedBlockPosition offsetCachedBlockPosition, Direction direction);

    public MutableText getDisplayName() {
      return Text.translatable("enhancedCommands.exposureType." + name);
    }
  }

  public static final class Parser implements FunctionLikeParser<ExposeBlockPredicate> {
    private ExposureType exposureType;
    private final Set<@NotNull Direction> directions = new TreeSet<>();

    @Override
    public @NotNull String functionName() {
      return "expose";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("blockPredicate.expose");
    }

    @Override
    public ExposeBlockPredicate getParseResult() {
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
    public void parseParameter(SuggestedParser parser, int paramIndex) throws CommandSyntaxException {
      if (paramIndex == 0) {
        parser.suggestions.clear();
        parser.suggestions.add(suggestionsBuilder -> SuggestionUtil.suggestMatchingEnumWithTooltip(Arrays.asList(ExposureType.values()), ExposureType::getDisplayName, suggestionsBuilder));
        final int cursorBeforeReadString = parser.reader.getCursor();
        final String id = parser.reader.readString();
        exposureType = ExposureType.CODEC.byId(id);
        if (exposureType == null) {
          parser.reader.setCursor(cursorBeforeReadString);
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
        }
      } else if (paramIndex == 1) {
        do {
          parser.suggestions.clear();
          parser.reader.skipWhitespace();
          if (directions.isEmpty()) {
            parser.suggestions.add(suggestionsBuilder -> {
              SuggestionUtil.suggestString("all", Text.translatable("enhancedCommands.direction.all"), suggestionsBuilder);
              SuggestionUtil.suggestString("horizontal", Text.translatable("enhancedCommands.direction.horizontal"), suggestionsBuilder);
              SuggestionUtil.suggestString("vertical", Text.translatable("enhancedCommands.direction.vertical"), suggestionsBuilder);
            });
          }
          parser.suggestions.add(SuggestionUtil::suggestDirections);
          final int cursorBeforeReadString = parser.reader.getCursor();
          final String id = parser.reader.readString();
          if (id.isEmpty()) break;
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

  public enum Type implements BlockPredicateType<ExposeBlockPredicate> {
    INSTANCE;

    @Override
    public @NotNull ExposeBlockPredicate parse(SuggestedParser parser) throws CommandSyntaxException {
      return new Parser().parse(parser);
    }
  }
}
