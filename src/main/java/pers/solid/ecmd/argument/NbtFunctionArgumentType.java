package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.concurrent.CompletableFuture;

public record NbtFunctionArgumentType(boolean onlyCompounds, CommandRegistryAccess registryAccess) implements ArgumentType<NbtFunction> {

  public static NbtFunctionArgumentType compound(CommandRegistryAccess registryAccess) {
    return new NbtFunctionArgumentType(true, registryAccess);
  }

  public static NbtFunctionArgumentType element(CommandRegistryAccess registryAccess) {
    return new NbtFunctionArgumentType(false, registryAccess);
  }

  public static NbtFunction getNbtFunction(CommandContext<?> context, String name) {
    return context.getArgument(name, NbtFunction.class);
  }

  public static CompoundNbtFunction getCompoundNbtFunction(CommandContext<?> context, String name) {
    return context.getArgument(name, CompoundNbtFunction.class);
  }

  @Override
  public NbtFunction parse(StringReader reader) throws CommandSyntaxException {
    final SuggestedParser<?> suggestedParser = new SuggestedParser<>(reader);
    final NbtFunctionParser<?> parser = new NbtFunctionParser<>(new ParseContext<>(registryAccess, suggestedParser, false, false));
    return onlyCompounds ? parser.parseCompound(false) : parser.parseFunction(false, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final SuggestedParser<S> suggestedParser = new SuggestedParser<>(stringReader);
    final NbtFunctionParser<S> parser = new NbtFunctionParser<>(new ParseContext<>(registryAccess, suggestedParser, false, false));
    try {
      if (onlyCompounds) {
        parser.parseCompound(false);
      } else {
        parser.parseFunction(false, false);
      }
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return suggestedParser.buildSuggestions(context, builderOffset);
  }

  public enum Serializer implements ArgumentSerializer<NbtFunctionArgumentType, NbtFunctionArgumentType.Properties> {
    INSTANCE;

    @Override
    public void writePacket(NbtFunctionArgumentType.Properties properties, PacketByteBuf buf) {
      buf.writeBoolean(properties.onlyCompounds);
    }

    @Override
    public NbtFunctionArgumentType.Properties fromPacket(PacketByteBuf buf) {
      return new Properties(buf.readBoolean());
    }

    @Override
    public void writeJson(NbtFunctionArgumentType.Properties properties, JsonObject json) {
      json.addProperty("onlyCompounds", properties.onlyCompounds);
    }

    @Override
    public NbtFunctionArgumentType.Properties getArgumentTypeProperties(NbtFunctionArgumentType argumentType) {
      return new Properties(argumentType.onlyCompounds);
    }
  }

  public record Properties(boolean onlyCompounds) implements ArgumentSerializer.ArgumentTypeProperties<NbtFunctionArgumentType> {

    @Override
    public NbtFunctionArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
      return new NbtFunctionArgumentType(onlyCompounds, commandRegistryAccess);
    }

    @Override
    public ArgumentSerializer<NbtFunctionArgumentType, ?> getSerializer() {
      return NbtFunctionArgumentType.Serializer.INSTANCE;
    }
  }
}
