package pers.solid.ecmd.region;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;

import java.util.Iterator;
import java.util.function.Function;

public record SingleBlockPosRegion(Vec3i pos) implements IntBackedRegion {
  public static final MapCodec<SingleBlockPosRegion> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(Vec3i.CODEC.fieldOf("pos").forGetter(SingleBlockPosRegion::pos)).apply(i, SingleBlockPosRegion::new));

  @Override
  public boolean contains(@NotNull Vec3i vec3i) {
    return this.pos.equals(vec3i);
  }

  @Override
  public @NotNull Type getType() {
    return RegionTypes.SINGLE;
  }

  @Override
  public SingleBlockPosRegion transformedInt(Function<Vec3i, Vec3i> transformation) {
    return new SingleBlockPosRegion(transformation.apply(pos));
  }

  @Override
  public long numberOfBlocksAffected() {
    return 1;
  }

  @Override
  public @NotNull String asString() {
    return "single(%s %s %s)".formatted(pos.getX(), pos.getY(), pos.getZ());
  }

  @Override
  public @NotNull Box minContainingBox() {
    return new Box(new BlockPos(pos));
  }

  @Override
  public @NotNull BlockBox minContainingBlockBox() {
    return BlockBox.create(pos, pos);
  }

  @Override
  public @NotNull Iterator<BlockPos> iterator() {
    return Iterators.singletonIterator(new BlockPos(pos));
  }

  public enum Type implements RegionType<SingleBlockPosRegion> {
    INSTANCE;

    @Override
    public String functionName() {
      return "single";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.region.single");
    }

    @Override
    public FunctionParamsParser<RegionArgument> functionParamsParser() {
      return FunctionParser.INSTANCE;
    }

    @Override
    public @NotNull MapCodec<SingleBlockPosRegion> getCodec() {
      return CODEC;
    }
  }

  /**
   * 直接将坐标形式的内容解析为区域，例如 {@code 1 2 3} 等价于 {@code single(1 2 3)}，{@code ~~~} 等价于 {@code single(~~~)}。
   */
  public enum BareParser implements Parser<RegionArgument> {
    INSTANCE;

    @Override
    public RegionArgument parse(ParseContext<?> parseContext) throws CommandSyntaxException {
      final StringReader reader = parseContext.reader();
      final int cursorBeforeParse = reader.getCursor();
      final EnhancedPosArgumentType argumentType = EnhancedPosArgumentType.blockPos();
      parseContext.addSuggestion((context, builder) -> {
        final SuggestionsBuilder builderOffset = builder.createOffset(cursorBeforeParse);
        return argumentType.listSuggestions(context, builderOffset);
      });
      if (reader.canRead()) {
        final char peek = reader.peek();
        if (StringReader.isAllowedNumber(peek) || peek == '~' || peek == '^') {
          final PosArgument posArgument = argumentType.parse(reader);
          return source -> new SingleBlockPosRegion(posArgument.toAbsoluteBlockPos(source));
        }
      }
      return null;
    }
  }

  public enum FunctionParser implements FunctionParamsParser<RegionArgument> {
    INSTANCE;
    private PosArgument posArgument;

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 1;
    }

    @Override
    public RegionArgument getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      final PosArgument posArgument1 = posArgument;
      posArgument = null;
      return source -> new SingleBlockPosRegion(posArgument1.toAbsoluteBlockPos(source));
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      ArgumentType<PosArgument> argumentType = EnhancedPosArgumentType.blockPos();
      posArgument = parseContext.parseAndSuggestArgument(argumentType);
    }
  }
}
