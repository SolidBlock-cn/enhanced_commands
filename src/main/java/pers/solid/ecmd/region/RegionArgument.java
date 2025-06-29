package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;

/**
 * @see net.minecraft.command.argument.PosArgument
 */
public interface RegionArgument<R extends Region> extends ExpressionConvertible {
  Codec<RegionArgument<?>> CODEC = RegionType.REGISTRY.getCodec().dispatch(RegionArgument::getType, RegionType::getArgumentCodec);

  @NotNull
  static RegionArgument<?> parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorOnStart = reader.getCursor();
    for (Parser<? extends RegionArgument<?>> argumentParser : RegionTypes.PARSERS) {
      reader.setCursor(cursorOnStart);
      final RegionArgument<?> parse = argumentParser.parse(parseContext.withAllowSparse(true));
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }
    reader.setCursor(cursorOnStart);
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
  }

  R toAbsoluteRegion(PositionProvider positionProvider);

  @NotNull
  RegionType<? super R> getType();
}
