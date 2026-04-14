package pers.solid.ecmd.nbt.function;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.parse.FunctionContentParser;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public record OverlayNbtFunction(List<NbtFunction> functions) implements NbtFunction {
  public static final MapCodec<OverlayNbtFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      NbtFunction.CODEC.listOf().fieldOf("functions").forGetter(OverlayNbtFunction::functions)
  ).apply(i, OverlayNbtFunction::new));

  @Override
  public String asString() {
    return functions.stream().map(NbtFunction::asString).collect(Collectors.joining(", ", "overlay(", ")"));
  }

  @Override
  public Type getType() {
    return NbtFunctionTypes.OVERLAY;
  }

  @Override
  public Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    for (NbtFunction function : functions) {
      nbtElement = function.apply(nbtElement, context);
    }
    if (nbtElement == null) {
      throw new SimpleCommandExceptionType(Component.translatable("enhanced_commands.nbt_function.overlay.empty")).create();
    }
    return nbtElement;
  }

  public enum Type implements NbtFunctionType<OverlayNbtFunction> {
    OVERLAY_TYPE;

    @Override
    public MapCodec<OverlayNbtFunction> getCodec() {
      return CODEC;
    }
  }

  public static final class Parser implements FunctionContentParser.SequentialParams<OverlayNbtFunction> {
    private final List<NbtFunction> nbtFunctions = new ArrayList<>();

    @Override
    public OverlayNbtFunction getParseResult(ParseContext<?> parseContext) {
      final ImmutableList.Builder<NbtFunction> builder = new ImmutableList.Builder<>();
      for (NbtFunction nbtFunction : nbtFunctions) {
        builder.add(nbtFunction);
      }
      return new OverlayNbtFunction(builder.build());
    }

    @Override
    public void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException {
      nbtFunctions.add(NbtFunction.parse(parseContext, false, false));
    }
  }
}
