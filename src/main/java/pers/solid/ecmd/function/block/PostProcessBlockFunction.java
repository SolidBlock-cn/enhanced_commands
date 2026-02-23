package pers.solid.ecmd.function.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public record PostProcessBlockFunction(@NotNull List<@NotNull Direction> directions) implements BlockFunction {
  public static final List<Direction> ALL_DIRECTIONS = List.of(Direction.values());
  public static final MapCodec<PostProcessBlockFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      ExtraCodecs.nonEmptyList(Direction.CODEC.listOf()).optionalFieldOf("directions", ALL_DIRECTIONS).forGetter(PostProcessBlockFunction::directions)
  ).apply(i, PostProcessBlockFunction::new));

  /**
   * @see Block#updateFromNeighbourShapes(BlockState, LevelAccessor, BlockPos)
   */
  @Override
  public @NotNull BlockState getModifiedState(BlockState blockState, BlockState origState, Level world, BlockPos pos, MutableObject<CompoundTag> blockEntityData, BlockFunctionContext context) {
    BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

    for (Direction direction : directions) {
      mutable.setWithOffset(pos, direction);
      blockState = blockState.updateShape(world, world, pos, direction, mutable, world.getBlockState(mutable), context.random);
    }

    return blockState;
  }

  @Override
  public @NotNull Type getType() {
    return BlockFunctionTypes.POST_PROCESS;
  }

  @Override
  public @NotNull String asString() {
    return "postprocess(" + (ALL_DIRECTIONS.equals(directions) ? "" : directions.stream().map(Direction::getSerializedName).collect(Collectors.joining(" "))) + ")";
  }

  public enum Type implements BlockFunctionType<PostProcessBlockFunction> {
    POST_PROCESS_TYPE;

    @Override
    public @NotNull MapCodec<PostProcessBlockFunction> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionLikeParser.SequentialParams<PostProcessBlockFunction> {
    private final Set<@NotNull Direction> directions = new TreeSet<>();

    @Override
    public PostProcessBlockFunction getParseResult(ParseContext<?> parseContext) {
      return new PostProcessBlockFunction(directions.isEmpty() ? List.of(Direction.values()) : List.copyOf(directions));
    }

    @Override
    public int minSequentialParamsCount() {
      return 0;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 1;
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
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
