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
import pers.solid.ecmd.predicate.nbt.NbtPredicate;

import java.util.concurrent.CompletableFuture;

public enum NbtPredicateArgumentType implements ArgumentType<NbtPredicate>, ArgumentSerializer.ArgumentTypeProperties<NbtPredicateArgumentType> {
  COMPOUND(true), ELEMENT(false);

  private final boolean onlyCompounds;

  NbtPredicateArgumentType(boolean onlyCompounds) {
    this.onlyCompounds = onlyCompounds;
  }

  public static NbtPredicate getNbtPredicate(CommandContext<?> context, String name) {
    return context.getArgument(name, NbtPredicate.class);
  }

  @Override
  public NbtPredicate parse(StringReader reader) throws CommandSyntaxException {
    final NbtPredicateSuggestedParser<?> parser = new NbtPredicateSuggestedParser<>(reader);
    return onlyCompounds ? parser.parseCompound(false, false) : parser.parsePredicate(false, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final NbtPredicateSuggestedParser<S> parser = new NbtPredicateSuggestedParser<>(stringReader);
    try {
      if (onlyCompounds) {
        parser.parseCompound(false, false);
      } else {
        parser.parsePredicate(false, false);
      }
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parser.buildSuggestions(context, builderOffset);
  }

  @Override
  public NbtPredicateArgumentType createType(CommandRegistryAccess registryAccess) {
    return this;
  }

  @Override
  public ArgumentSerializer<NbtPredicateArgumentType, ?> getSerializer() {
    return Serializer.INSTANCE;
  }

  public enum Serializer implements ArgumentSerializer<NbtPredicateArgumentType, NbtPredicateArgumentType> {
    INSTANCE;

    @Override
    public void writePacket(NbtPredicateArgumentType properties, PacketByteBuf buf) {
      buf.writeBoolean(properties.onlyCompounds);
    }

    @Override
    public NbtPredicateArgumentType fromPacket(PacketByteBuf buf) {
      return buf.readBoolean() ? COMPOUND : ELEMENT;
    }

    @Override
    public void writeJson(NbtPredicateArgumentType properties, JsonObject json) {
      json.addProperty("onlyCompounds", properties.onlyCompounds);
    }

    @Override
    public NbtPredicateArgumentType getArgumentTypeProperties(NbtPredicateArgumentType argumentType) {
      return argumentType;
    }
  }
}
