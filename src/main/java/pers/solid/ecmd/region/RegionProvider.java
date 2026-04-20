package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.util.ExpressionConvertible;
import pers.solid.ecmd.util.PositionProvider;

/**
 * @see net.minecraft.commands.arguments.coordinates.Coordinates
 */
public interface RegionProvider<R extends Region> extends ExpressionConvertible {
  Codec<RegionProvider<?>> CODEC = RegionType.CODEC.dispatch(RegionProvider::getType, RegionType::providerCodec);

  static RegionProvider<?> parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorOnStart = reader.getCursor();
    for (Parser<? extends RegionProvider<?>> argumentParser : RegionParsing.PARSERS) {
      reader.setCursor(cursorOnStart);
      final RegionProvider<?> parse = argumentParser.parse(parseContext.withAllowSparse(true));
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }
    reader.setCursor(cursorOnStart);
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
  }

  R toAbsoluteRegion(PositionProvider positionProvider);

  RegionType<? super R> getType();
}
