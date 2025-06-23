package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EnhancedPosArgument;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

public interface CuboidRegion extends Region {
  MapCodec<CuboidRegion> CODEC = Codec.BOOL.dispatchMap("block", r -> r instanceof BlockCuboidRegion, isBlock -> isBlock ? BlockCuboidRegion.CODEC : PreciseCuboidRegion.CODEC);

  enum Type implements RegionType<CuboidRegion> {
    CUBOID_TYPE;

    @Override
    public String functionName() {
      return "cuboid";
    }

    @Override
    public Text tooltip() {
      return Text.translatable("enhanced_commands.region.cuboid");
    }

    @Override
    public FunctionParamsParser<? extends RegionArgument<? extends CuboidRegion>> functionParamsParser() {
      return new PreciseCuboidRegion.Parser();
    }

    @Override
    public @NotNull MapCodec<CuboidRegion> getCodec() {
      return CODEC;
    }

    @Override
    public @NotNull MapCodec<? extends RegionArgument<? extends CuboidRegion>> getArgumentCodec() {
      return CuboidRegionArgument.CODEC;
    }
  }

  final class Parser implements FunctionParamsParser<CuboidRegionArgument<?>> {
    private EnhancedPosArgument from;
    private EnhancedPosArgument to;

    @Override
    public CuboidRegionArgument<?> getParseResult(ParseContext<?> parseContext) {
      if (to == null) {
        if (EnhancedPosArgument.isInt(from)) {
          return new SingleBlockPosRegionArgument(from);
        }
      }
      if (EnhancedPosArgument.isInt(from) && EnhancedPosArgument.isInt(to)) {
        return new BlockCuboidRegionArgument(from, to);
      }
      return new PreciseCuboidRegionArgument(from, to);
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      final EnhancedPosArgumentType type = EnhancedPosArgumentType.posPreferringCenteredInt();
      final StringReader reader = parseContext.reader();
      if (paramIndex == 0) {
        from = parseContext.parseAndSuggestArgument(type);
        if (reader.canRead() && Character.isWhitespace(reader.peek())) {
          reader.skipWhitespace();
          // 在有接受到空格后，可直接接受第二个参数
          if (reader.canRead()) {
            final char peek = reader.peek();
            if (peek != ',' && peek != ')') {
              to = parseContext.parseAndSuggestArgument(type);
            }
          }
        }
      } else if (paramIndex == 1) {
        to = parseContext.parseAndSuggestArgument(type);
      }
    }

    @Override
    public int minParamsCount() {
      return (to != null || EnhancedPosArgument.isInt(from)) ? 1 : 2;
    }

    @Override
    public int maxParamsCount() {
      // 如果接受到了以空格区分的参数，那么不需要接受第二个参数了。
      return to != null ? 1 : 2;
    }
  }
}
