package pers.solid.ecmd.nbt.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.Tag;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.NbtSourceArgument;
import pers.solid.ecmd.argument.SimpleEnumArgument;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.nbt.data.NbtSource;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.Objects;
import java.util.Optional;

public record GetDataNbtFunction(NbtSource<?> source, Optional<NbtPathArgument.NbtPath> path, NbtConcentrationType concentrationType) implements NbtFunction {
  public static final MapCodec<GetDataNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtSource.CODEC.fieldOf("source").forGetter(GetDataNbtFunction::source),
      NbtPathArgument.NbtPath.CODEC.optionalFieldOf("path").forGetter(GetDataNbtFunction::path),
      NbtConcentrationType.CODEC.optionalFieldOf("concentration_type", NbtConcentrationType.ALL).forGetter(GetDataNbtFunction::concentrationType)
  ).apply(i, GetDataNbtFunction::new));

  @Override
  public String asString() {
    return "from(" + source.asString() + ")";
  }

  @Override
  public NbtFunctionType<GetDataNbtFunction> getType() {
    return Type.GET_DATA_TYPE;
  }

  @Override
  public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    return source.getConcentratedNbts((CommandSourceStack) context.positionProvider, path.orElse(null), concentrationType, RandomSource.create());
  }

  public enum Type implements NbtFunctionType<GetDataNbtFunction> {
    GET_DATA_TYPE;

    @Override
    public MapCodec<GetDataNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static class Parser implements FunctionContentParser.SequentialParams<GetDataNbtFunction> {
    private @Nullable NbtSource<?> nbtSource;
    private @Nullable NbtPathArgument.NbtPath nbtPath;
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
        case 0 -> nbtSource = parseContext.parseAndSuggestArgument(NbtSourceArgument.nbtSource((CommandBuildContext) parseContext.registries()));
        case 1 -> nbtPath = parseContext.parseAndSuggestArgument(NbtPathArgument.nbtPath());
        case 2 -> nbtConcentrationType = parseContext.parseAndSuggestArgument(SimpleEnumArgument.nbtConcentrationType());
      }
    }

    @Override
    public GetDataNbtFunction getParseResult(ParseContext<?> parseContext) {
      Objects.requireNonNull(nbtSource, "nbtSource");
      return new GetDataNbtFunction(nbtSource, Optional.ofNullable(nbtPath), nbtConcentrationType);
    }
  }
}
