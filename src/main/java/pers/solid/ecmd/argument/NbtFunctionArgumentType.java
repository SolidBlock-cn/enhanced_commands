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

import java.util.concurrent.CompletableFuture;

public enum NbtFunctionArgumentType implements ArgumentType<NbtFunction>, ArgumentSerializer.ArgumentTypeProperties<NbtFunctionArgumentType> {
  COMPOUND(true), ELEMENT(false);

  private final boolean onlyCompounds;

  NbtFunctionArgumentType(boolean onlyCompounds) {
    this.onlyCompounds = onlyCompounds;
  }

  public static NbtFunction getNbtFunction(CommandContext<?> context, String name) {
    return context.getArgument(name, NbtFunction.class);
  }

  public static CompoundNbtFunction getCompoundNbtFunction(CommandContext<?> context, String name) {
    return context.getArgument(name, CompoundNbtFunction.class);
  }

  @Override
  public NbtFunction parse(StringReader reader) throws CommandSyntaxException {
    final NbtFunctionSuggestedParser parser = new NbtFunctionSuggestedParser(reader);
    return onlyCompounds ? parser.parseCompound(false) : parser.parseFunction(false, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final NbtFunctionSuggestedParser parser = new NbtFunctionSuggestedParser(stringReader);
    try {
      if (onlyCompounds) {
        parser.parseCompound(false);
      } else {
        parser.parseFunction(false, false);
      }
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parser.buildSuggestions(context, builderOffset);
  }

  @Override
  public NbtFunctionArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
    return this;
  }

  @Override
  public ArgumentSerializer<NbtFunctionArgumentType, ?> getSerializer() {
    return Serializer.INSTANCE;
  }

  public enum Serializer implements ArgumentSerializer<NbtFunctionArgumentType, NbtFunctionArgumentType> {
    INSTANCE;

    @Override
    public void writePacket(NbtFunctionArgumentType properties, PacketByteBuf buf) {
      buf.writeBoolean(properties.onlyCompounds);
    }

    @Override
    public NbtFunctionArgumentType fromPacket(PacketByteBuf buf) {
      return buf.readBoolean() ? COMPOUND : ELEMENT;
    }

    @Override
    public void writeJson(NbtFunctionArgumentType properties, JsonObject json) {
      json.addProperty("onlyCompounds", properties.onlyCompounds);
    }

    @Override
    public NbtFunctionArgumentType getArgumentTypeProperties(NbtFunctionArgumentType argumentType) {
      return argumentType;
    }
  }
}
