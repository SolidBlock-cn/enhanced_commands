package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record AngleArgumentType(boolean returnRadians, double min, double max) implements ArgumentType<Double>, ArgumentTypeInfo.Template<AngleArgumentType> {
  public static AngleArgumentType angle(boolean returnRadians, double minimum, double maximum) {
    return new AngleArgumentType(returnRadians, minimum, maximum);
  }

  public static AngleArgumentType angle(boolean returnRadians) {
    return new AngleArgumentType(returnRadians, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  public static double getAngle(CommandContext<?> context, String name) {
    return context.getArgument(name, Double.class);
  }

  @Override
  public Double parse(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeAngle = reader.getCursor();
    final double result = new ParseContext<>(reader).parseAndSuggestAngle(returnRadians);
    final int cursorAfterAngle = reader.getCursor();
    if (result < min) {
      reader.setCursor(cursorBeforeAngle);
      throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, result, min + (returnRadians ? "rad" : "deg")), cursorAfterAngle);
    }
    if (result > max) {
      reader.setCursor(cursorBeforeAngle);
      throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooHigh().createWithContext(reader, result, max + (returnRadians ? "rad" : "deg")), cursorAfterAngle);
    }
    return result;
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parser = new ParseContext<>(stringReader);
    try {
      parser.parseAndSuggestAngle(returnRadians);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parser.buildSuggestions(context, builderOffset);
  }

  @Override
  public @NotNull String toString() {
    if (min == Double.NEGATIVE_INFINITY && max == Double.POSITIVE_INFINITY) {
      return "angle(radians = " + returnRadians + ")";
    } else {
      return "angle(radians = %s, min = %s, max = %s)".formatted(Boolean.toString(returnRadians), Double.toString(min), Double.toString(max));
    }
  }

  @Override
  public Collection<String> getExamples() {
    return List.of("0", "30deg", "0.5rad", "0.5turn");
  }

  @Override
  public @NotNull AngleArgumentType instantiate(CommandBuildContext registryAccess) {
    return this;
  }

  @Override
  public @NotNull Serializer type() {
    return Serializer.INSTANCE;
  }

  public static class Serializer implements ArgumentTypeInfo<AngleArgumentType, AngleArgumentType> {
    public static final Serializer INSTANCE = new Serializer();

    private Serializer() {
    }

    @Override
    public void serializeToNetwork(AngleArgumentType properties, FriendlyByteBuf buf) {
      buf.writeBoolean(properties.returnRadians);
      boolean hasMin = properties.min != Double.NEGATIVE_INFINITY;
      boolean hasMax = properties.max != Double.POSITIVE_INFINITY;
      buf.writeByte(ArgumentUtils.createNumberFlags(hasMin, hasMax));
      if (hasMin) {
        buf.writeDouble(properties.min);
      }

      if (hasMax) {
        buf.writeDouble(properties.max);
      }
    }

    @Override
    public @NotNull AngleArgumentType deserializeFromNetwork(FriendlyByteBuf buf) {
      final boolean returnRadians = buf.readBoolean();
      byte b = buf.readByte();
      double d = ArgumentUtils.numberHasMin(b) ? buf.readDouble() : Double.NEGATIVE_INFINITY;
      double e = ArgumentUtils.numberHasMax(b) ? buf.readDouble() : Double.POSITIVE_INFINITY;
      return new AngleArgumentType(returnRadians, d, e);
    }

    @Override
    public void serializeToJson(AngleArgumentType properties, JsonObject json) {
      json.addProperty("radians", properties.returnRadians);
      if (properties.min != Double.NEGATIVE_INFINITY) {
        json.addProperty("min", properties.min);
      }

      if (properties.max != Double.POSITIVE_INFINITY) {
        json.addProperty("max", properties.max);
      }
    }

    @Override
    public @NotNull AngleArgumentType unpack(AngleArgumentType argumentType) {
      return argumentType;
    }
  }
}
