package pers.solid.ecmd.block.predicate;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.StringIdentifiableCodec;

import java.util.*;

/**
 * To test which a block is exposed in the specified directions and the specified type.
 *
 * @param exposureType The exposure type. By default, it is exposed to empty collision.
 * @param directions   The directions to test exposure.
 */
public record ExposeBlockPredicate(ExposureType exposureType, List<Direction> directions) implements BlockPredicate {
  public static final MapCodec<ExposeBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(ExposeBlockPredicate::new, ExposureType.CODEC.fieldOf("exposure_type").forGetter(ExposeBlockPredicate::exposureType), ExtraCodecs.nonEmptyList(Direction.CODEC.listOf()).optionalFieldOf("directions", List.of(Direction.values())).forGetter(ExposeBlockPredicate::directions)));

  @Override
  public String expressAsString() {
    return "expose(" + exposureType.getSerializedName() + ", " + String.join(" ", Iterables.transform(directions, Direction::getSerializedName)) + ")";
  }

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    for (Direction direction : directions) {
      var offsetCachedBlockPosition = new BlockInWorld(blockInWorld.getLevel(), blockInWorld.getPos().relative(direction), false);
      if (exposureType.test(offsetCachedBlockPosition, direction))
        return true;
    }
    return false;
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    List<TestResult> testResults = new ArrayList<>();
    boolean result = false;
    for (Direction direction : directions) {
      var offsetCachedBlockPosition = new BlockInWorld(blockInWorld.getLevel(), blockInWorld.getPos().relative(direction), false);
      final boolean test = exposureType.test(offsetCachedBlockPosition, direction);
      testResults.add(TestResult.of(test, Component.translatable("enhanced_commands.block_predicate.expose.side." + (test ? "pass" : "fail"), TextUtil.wrapDirection(direction))));
      if (test) {
        result = true;
      }
    }
    if (testResults.size() == 1) {
      return testResults.get(0);
    } else {
      return TestResult.of(result, Component.translatable("enhanced_commands.block_predicate.expose." + (result ? "pass" : "fail")), testResults);
    }
  }

  @Override
  public BlockPredicateType<ExposeBlockPredicate> getType() {
    return BlockPredicateTypes.EXPOSE;
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return Collections.emptyList();
  }

  /**
   * The type to test which the block is exposed.
   */
  public enum ExposureType implements StringRepresentable {
    /**
     * The block is exposed to a block with empty collision shape, such as air, torch or flower.
     */
    EMPTY_COLLISION("empty_collision") {
      @Override
      public boolean test(BlockInWorld offsetCachedBlockPosition, Direction direction) {
        return offsetCachedBlockPosition.getState().getCollisionShape(offsetCachedBlockPosition.getLevel(), offsetCachedBlockPosition.getPos()).isEmpty();
      }
    },
    /**
     * The block is exposed to a block with an empty collision shape at that side, such as a bottom-half slab below it.
     */
    EMPTY_SIDE_COLLISION("empty_side_collision") {
      @Override
      public boolean test(BlockInWorld offsetCachedBlockPosition, Direction direction) {
        return offsetCachedBlockPosition.getState().getCollisionShape(offsetCachedBlockPosition.getLevel(), offsetCachedBlockPosition.getPos()).getFaceShape(direction.getOpposite()).isEmpty();
      }
    },
    /**
     * The block is exposed to air.
     */
    AIR("air") {
      @Override
      public boolean test(BlockInWorld offsetCachedBlockPosition, Direction direction) {
        return offsetCachedBlockPosition.getState().isAir();
      }
    },
    /**
     * The block is exposed to a block with non-full collision shape at that side, such as next to a slab block horizontally.
     */
    INCOMPLETE_SIDE_COLLISION("incomplete_side_collision") {
      @Override
      public boolean test(BlockInWorld offsetCachedBlockPosition, Direction direction) {
        return !Shapes.joinUnoptimized(Shapes.block(), offsetCachedBlockPosition.getState().getCollisionShape(offsetCachedBlockPosition.getLevel(), offsetCachedBlockPosition.getPos()).getFaceShape(direction.getOpposite()), BooleanOp.ONLY_FIRST).isEmpty();
      }
    };
    public static final StringIdentifiableCodec<ExposureType> CODEC = StringIdentifiableCodec.create(ExposureType.values());
    private final String name;

    ExposureType(String name) {
      this.name = name;
    }

    @Override
    public String getSerializedName() {
      return name;
    }

    public abstract boolean test(BlockInWorld offsetCachedBlockPosition, Direction direction);

    public MutableComponent getDisplayName() {
      return Component.translatable("enhanced_commands.exposure_type." + name);
    }
  }

  public static final class Parser implements FunctionContentParser.SequentialParams<ExposeBlockPredicate> {
    private final Set<Direction> directions = new TreeSet<>();
    private @Nullable ExposureType exposureType;

    @Override
    public ExposeBlockPredicate getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(exposureType, "exposureType");
      return new ExposeBlockPredicate(exposureType, directions.isEmpty() ? List.of(Direction.values()) : List.copyOf(directions));
    }

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 2;
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
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
              ParsingUtil.suggestString("all", Component.translatable("enhanced_commands.direction.all"), suggestionsBuilder);
              ParsingUtil.suggestString("horizontal", Component.translatable("enhanced_commands.direction.horizontal"), suggestionsBuilder);
              ParsingUtil.suggestString("vertical", Component.translatable("enhanced_commands.direction.vertical"), suggestionsBuilder);
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
                Direction.Plane.HORIZONTAL.forEach(directions::add);
                continue;
              }
              case "vertical" -> {
                Direction.Plane.VERTICAL.forEach(directions::add);
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
