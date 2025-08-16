package pers.solid.ecmd.region;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.parse.Parser;
import pers.solid.ecmd.parse.ParsingUtil;

public enum ActiveRegionType implements RegionType<Region>, Parser<ActiveRegionArgument> {
  TYPE;

  private static final MapCodec<Region> CODEC = MapCodec.assumeMapUnsafe(Codec.of(Encoder.error("Cannot encode"), Decoder.error("Region NBT cannot hold this type of region")));

  @Override
  public ActiveRegionArgument parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    parseContext.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("$", Text.translatable("enhanced_commands.region.active_region"), suggestionsBuilder).buildFuture());
    final StringReader reader = parseContext.reader();
    if (reader.canRead() && reader.peek() == '$') {
      reader.skip();
      parseContext.clearSuggestion();
      return ActiveRegionArgument.INSTANCE;
    } else {
      return null;
    }
  }

  @Override
  public @NotNull MapCodec<Region> getCodec() {
    return CODEC;
  }

  @Override
  public @NotNull MapCodec<? extends ActiveRegionArgument> getArgumentCodec() {
    return ActiveRegionArgument.CODEC;
  }
}
