package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.FunctionParamsParser;
import pers.solid.ecmd.util.ParsingUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.*;

/**
 * To test which a block is exposed in the specified directions and the specified type.
 *
 * @param exposureType The exposure type. By default, it is exposed to empty collision.
 * @param directions   The directions to test exposure.
 */
public record ExposeBlockPredicate(@NotNull ExposureType exposureType, @NotNull Iterable<@NotNull Direction> directions) implements BlockPredicate {
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

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("exposure_type", exposureType.asString());
    final NbtList nbtList = new NbtList();
    nbtCompound.put("directions", nbtList);
    for (Direction direction : directions) {
      nbtList.add(NbtString.of(direction.asString()));
    }

    // 如果只有一个元素，那么没有必须存储为列表。
    if (nbtList.size() == 1) {
      nbtCompound.remove("directions");
      nbtCompound.put("directions", nbtList.get(0));
    }
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
      return Text.translatable("enhanced_commands.exposure_type." + name);
    }
  }

  public static final class Parser implements FunctionParamsParser<ExposeBlockPredicate> {
    private ExposureType exposureType;
    private final Set<@NotNull Direction> directions = new TreeSet<>();

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

  public enum Type implements BlockPredicateType<ExposeBlockPredicate> {
    EXPOSE_TYPE;

    @Override
    public @NotNull ExposeBlockPredicate fromNbt(@NotNull NbtCompound nbtCompound, @NotNull World world) {
      final String typeName = nbtCompound.getString("exposure_type");
      final ExposureType type = ExposureType.CODEC.byId(typeName);
      Preconditions.checkNotNull(type, "Unknown exposure type: %", typeName);
      final List<Direction> directions;
      if (nbtCompound.contains("directions", NbtElement.STRING_TYPE)) {
        directions = Collections.singletonList(Direction.byName(nbtCompound.getString("directions")));
      } else {
        directions = nbtCompound.getList("directions", NbtElement.STRING_TYPE).stream().map(nbtElement -> Direction.byName(nbtElement.asString())).filter(Objects::nonNull).toList();
      }
      return new ExposeBlockPredicate(type, directions);
    }
  }
}
