package pers.solid.ecmd.region;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

public enum ActiveRegionType implements RegionType<Region>, Parser<RegionArgument> {
  TYPE;

  private static final MapCodec<Region> CODEC = MapCodec.assumeMapUnsafe(Codec.of(Encoder.error("Cannot encode"), Decoder.error("Region NBT cannot hold this type of region")));

  @Override
  public RegionArgument parse(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException {
    parser.addSuggestion((context, suggestionsBuilder) -> ParsingUtil.suggestString("$", Text.translatable("enhanced_commands.region.active_region"), suggestionsBuilder).buildFuture());
    if (parser.reader.canRead() && parser.reader.peek() == '$') {
      parser.reader.skip();
      parser.clearSuggestion();
      return source -> ((ServerPlayerEntityExtension) source.getPlayerOrThrow()).ec$getOrEvaluateActiveRegionOrThrow();
    } else {
      return null;
    }
  }

  @Override
  public @NotNull MapCodec<Region> getCodec() {
    return CODEC;
  }
}
