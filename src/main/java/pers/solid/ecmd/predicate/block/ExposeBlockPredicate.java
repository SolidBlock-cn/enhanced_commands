package pers.solid.ecmd.predicate.block;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.ParsingUtil;

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
  public boolean test(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
    for (Direction direction : directions) {
      var offsetCachedBlockPosition = new CachedBlockPosition(cachedBlockPosition.getWorld(), cachedBlockPosition.getBlockPos().offset(direction), false);
      if (exposureType.test(offsetCachedBlockPosition, direction))
        return true;
    }
    return false;
  }

  @Override
  public TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition, ExecutionContext context) {
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
  public @NotNull Type getType() {
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
    public static final StringIdentifiableCodec<ExposureType> CODEC = StringIdentifiableCodec.create(ExposureType.values());
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
    public ExposeBlockPredicate getParseResult(ParseContext<?> parseContext) {
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
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
        parseContext.clearSuggestion();
        exposureType = parseContext.parseAndSuggestEnums(ExposureType.values(), ExposureType::getDisplayName, ExposureType.CODEC);
      } else if (paramIndex == 1) {
        do {
          parseContext.clearSuggestion();
          reader.skipWhitespace();
          if (directions.isEmpty()) {
            parseContext.addSuggestion((context, suggestionsBuilder) -> {
              ParsingUtil.suggestString("all", Text.translatable("enhanced_commands.direction.all"), suggestionsBuilder);
              ParsingUtil.suggestString("horizontal", Text.translatable("enhanced_commands.direction.horizontal"), suggestionsBuilder);
              ParsingUtil.suggestString("vertical", Text.translatable("enhanced_commands.direction.vertical"), suggestionsBuilder);
              return suggestionsBuilder.buildFuture();
            });
          }
          parseContext.addSuggestion((context, builder) -> ParsingUtil.suggestDirections(builder));
          final int cursorBeforeReadString = reader.getCursor();
          final String id = reader.readString();
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
            reader.setCursor(cursorBeforeReadString);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
          }
          directions.add(direction);
        } while (reader.canRead() && Character.isWhitespace(reader.peek()));
        if (directions.isEmpty()) {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
        }
      }
    }
  }
}
