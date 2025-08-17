package pers.solid.ecmd.function.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.NbtSourceArgumentType;
import pers.solid.ecmd.argument.SimpleEnumArgumentType;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.nbt.NbtSource;
import pers.solid.ecmd.parse.FunctionLikeParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Optional;

public record GetDataNbtFunction(NbtSource<?> source, Optional<NbtPathArgumentType.NbtPath> path, NbtConcentrationType concentrationType) implements NbtFunction {
  public static final MapCodec<GetDataNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtSource.CODEC.fieldOf("source").forGetter(GetDataNbtFunction::source),
      NbtPathArgumentType.NbtPath.CODEC.optionalFieldOf("path").forGetter(GetDataNbtFunction::path),
      NbtConcentrationType.CODEC.optionalFieldOf("concentration_type", NbtConcentrationType.ALL).forGetter(GetDataNbtFunction::concentrationType)
  ).apply(i, GetDataNbtFunction::new));

  @Override
  public @NotNull String asString() {
    return "from(" + source.asString() + ")";
  }

  @Override
  public @NotNull NbtFunctionType<GetDataNbtFunction> getType() {
    return Type.GET_DATA_TYPE;
  }

  @Override
  public @NotNull NbtElement apply(@Nullable NbtElement nbtElement, ExecutionContext context) throws CommandSyntaxException {
    return source.getConcentratedNbts((ServerCommandSource) context.positionProvider, path.orElse(null), concentrationType, Random.create());
  }

  public enum Type implements NbtFunctionType<GetDataNbtFunction> {
    GET_DATA_TYPE;

    @Override
    public MapCodec<GetDataNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionLikeParser.SequentialParams<GetDataNbtFunction> {
    private NbtSource<?> nbtSource;
    private @Nullable NbtPathArgumentType.NbtPath nbtPath;
    private NbtConcentrationType nbtConcentrationType = NbtConcentrationType.ALL;

    @Override
    public int minSequentialParamsCount() {
      return 1;
    }

    @Override
    public int maxSequentialParamsCount() {
      return 3;
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      switch (paramIndex) {
        case 0 -> nbtSource = parseContext.parseAndSuggestArgument(NbtSourceArgumentType.nbtSource(parseContext.registryAccess()));
        case 1 -> nbtPath = parseContext.parseAndSuggestArgument(NbtPathArgumentType.nbtPath());
        case 2 -> nbtConcentrationType = parseContext.parseAndSuggestArgument(SimpleEnumArgumentType.nbtConcentrationType());
      }
    }

    @Override
    public GetDataNbtFunction getParseResult(ParseContext<?> parseContext) throws CommandSyntaxException {
      return new GetDataNbtFunction(nbtSource, Optional.ofNullable(nbtPath), nbtConcentrationType);
    }
  }
}
