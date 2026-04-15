package pers.solid.ecmd.curve;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.util.PositionProvider;

public interface CurveProvider<T extends Curve> {
  Codec<CurveProvider<?>> CODEC = CurveType.REGISTRY.byNameCodec().dispatch(CurveProvider::getType, CurveType::providerCodec);

  static CurveProvider<?> parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final int cursorOnStart = parseContext.reader().getCursor();
    for (Parser<? extends CurveProvider<?>> parser : CurveParsing.PARSERS) {
      parseContext.reader().setCursor(cursorOnStart);
      final CurveProvider<?> parse = parser.parse(parseContext);
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }
    parseContext.reader().setCursor(cursorOnStart);
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parseContext.reader());
  }

  T toAbsoluteRegion(PositionProvider positionProvider);

  CurveType<? super T> getType();
}
