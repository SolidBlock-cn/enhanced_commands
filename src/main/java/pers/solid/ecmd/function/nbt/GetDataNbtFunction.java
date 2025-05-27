package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.NbtSourceArgumentType;
import pers.solid.ecmd.argument.SimpleEnumArgumentType;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.nbt.NbtSource;
import pers.solid.ecmd.nbt.NbtSourceArgument;
import pers.solid.ecmd.util.mixin.MixinShared;
import pers.solid.ecmd.util.parse.FunctionParamsParser;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.Optional;

public record GetDataNbtFunction(NbtSource<?> nbtSource, Optional<NbtPathArgumentType.NbtPath> path, NbtConcentrationType concentrationType) implements NbtFunction {
  public static final MapCodec<GetDataNbtFunction> CODEC = MapCodec.unit(null).flatXmap(o -> DataResult.error(() -> "not implemented yet"), function -> null);

  @Override
  public @NotNull String asString(boolean requirePrefix) {
    return "from(" + nbtSource + ")";
  }

  @Override
  public NbtFunctionType<GetDataNbtFunction> getType() {
    return null;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement) throws CommandSyntaxException {
    // todo consider using context
    return nbtSource.getConcentratedNbts(path.orElse(null), MixinShared.getCommandRegistryAccess(), concentrationType, Random.create());
  }

  public enum Type implements NbtFunctionType<GetDataNbtFunction> {
    GET_DATA_TYPE;

    @Override
    public MapCodec<GetDataNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionParamsParser<NbtFunctionArgument> {
    private NbtSourceArgument<?> nbtSourceArgument;
    private @Nullable NbtPathArgumentType.NbtPath nbtPath;
    private NbtConcentrationType nbtConcentrationType = NbtConcentrationType.ALL;

    @Override
    public int minParamsCount() {
      return 1;
    }

    @Override
    public int maxParamsCount() {
      return 3;
    }

    @Override
    public void parseParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      switch (paramIndex) {
        case 0 -> nbtSourceArgument = parseContext.parseAndSuggestArgument(NbtSourceArgumentType.nbtSource(parseContext.registryAccess()));
        case 1 -> nbtPath = parseContext.parseAndSuggestArgument(NbtPathArgumentType.nbtPath());
        case 2 -> nbtConcentrationType = parseContext.parseAndSuggestArgument(SimpleEnumArgumentType.nbtConcentrationType());
      }
    }

    @Override
    public NbtFunctionArgument getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return source -> new GetDataNbtFunction(nbtSourceArgument.getNbtSource(source), Optional.ofNullable(nbtPath), nbtConcentrationType);
    }
  }
}
