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
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.predicate.nbt.NbtPredicateArgument;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.concurrent.CompletableFuture;

public record NbtPredicateArgumentType(boolean onlyCompounds, CommandRegistryAccess registryAccess) implements ArgumentType<NbtPredicate> {

  public static NbtPredicateArgumentType compound(CommandRegistryAccess registryAccess) {
    return new NbtPredicateArgumentType(true, registryAccess);
  }

  public static NbtPredicateArgumentType element(CommandRegistryAccess registryAccess) {
    return new NbtPredicateArgumentType(false, registryAccess);
  }

  public static NbtPredicate getNbtPredicate(CommandContext<ServerCommandSource> context, String name) {
    return context.getArgument(name, NbtPredicateArgument.class).toAbsolute(context.getSource());
  }

  @Override
  public NbtPredicate parse(StringReader reader) throws CommandSyntaxException {
    final ParseContext<Object> parseContext = new ParseContext<>(registryAccess, reader, false, false);
    final NbtPredicateParser<?> parser = new NbtPredicateParser<>(parseContext);
    return onlyCompounds ? parser.parseCompound(false, false) : parser.parsePredicate(false, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(registryAccess, stringReader, true, false);
    final NbtPredicateParser<S> parser = new NbtPredicateParser<>(parseContext);
    try {
      if (onlyCompounds) {
        parser.parseCompound(false, false);
      } else {
        parser.parsePredicate(false, false);
      }
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parseContext.buildSuggestions(context, builderOffset);
  }


  public enum Serializer implements ArgumentSerializer<NbtPredicateArgumentType, NbtPredicateArgumentType.Properties> {
    INSTANCE;

    @Override
    public void writePacket(Properties properties, PacketByteBuf buf) {
      buf.writeBoolean(properties.onlyCompounds);
    }

    @Override
    public Properties fromPacket(PacketByteBuf buf) {
      return new Properties(buf.readBoolean());
    }

    @Override
    public void writeJson(Properties properties, JsonObject json) {
      json.addProperty("onlyCompounds", properties.onlyCompounds);
    }

    @Override
    public Properties getArgumentTypeProperties(NbtPredicateArgumentType argumentType) {
      return new Properties(argumentType.onlyCompounds);
    }
  }

  public record Properties(boolean onlyCompounds) implements ArgumentSerializer.ArgumentTypeProperties<NbtPredicateArgumentType> {

    @Override
    public NbtPredicateArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
      return new NbtPredicateArgumentType(onlyCompounds, commandRegistryAccess);
    }

    @Override
    public ArgumentSerializer<NbtPredicateArgumentType, ?> getSerializer() {
      return Serializer.INSTANCE;
    }
  }
}
