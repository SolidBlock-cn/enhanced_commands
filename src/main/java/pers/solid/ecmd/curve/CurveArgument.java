package pers.solid.ecmd.curve;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.util.PositionProvider;

public interface CurveArgument<T extends Curve> {
  Codec<CurveArgument<?>> CODEC = CurveType.REGISTRY.byNameCodec().dispatch(CurveArgument::getType, CurveType::getArgumentCodec);

  @NotNull
  static CurveArgument<?> parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final int cursorOnStart = parseContext.reader().getCursor();
    for (Parser<CurveArgument<?>> parser : CurveTypes.PARSERS) {
      parseContext.reader().setCursor(cursorOnStart);
      final CurveArgument<?> parse = parser.parse(parseContext);
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }
    parseContext.reader().setCursor(cursorOnStart);
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parseContext.reader());
  }

  T toAbsoluteRegion(PositionProvider positionProvider) throws CommandSyntaxException;

  @NotNull
  CurveType<? super T> getType();
}
