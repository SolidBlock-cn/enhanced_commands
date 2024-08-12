package pers.solid.ecmd.argument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public record StringEnumArgumentType(Collection<String> values, boolean acceptsOtherValues) implements ArgumentType<String>, ArgumentSerializer.ArgumentTypeProperties<StringEnumArgumentType> {
  public static StringEnumArgumentType stringEnum(String... values) {
    return new StringEnumArgumentType(Arrays.asList(values), false);
  }

  public static StringEnumArgumentType stringEnum(Collection<String> values) {
    return new StringEnumArgumentType(values, false);
  }

  public static StringEnumArgumentType stringEnum(Collection<String> values, boolean acceptsOtherValues) {
    return new StringEnumArgumentType(values, acceptsOtherValues);
  }

  @Override
  public String parse(StringReader reader) throws CommandSyntaxException {
    final int cursorBefore = reader.getCursor();
    final String unquotedString = reader.readUnquotedString();
    if (!acceptsOtherValues && !values.contains(unquotedString)) {
      final int cursorAfter = reader.getCursor();
      reader.setCursor(cursorBefore);
      throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader), cursorAfter);
    }
    return unquotedString;
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    return CommandSource.suggestMatching(values, builder);
  }

  @Override
  public Collection<String> getExamples() {
    return ImmutableList.copyOf(Iterables.limit(values, 3));
  }

  @Override
  public StringEnumArgumentType createType(CommandRegistryAccess commandRegistryAccess) {
    return this;
  }

  @Override
  public ArgumentSerializer<StringEnumArgumentType, ?> getSerializer() {
    return Serializer.INSTANCE;
  }

  public enum Serializer implements ArgumentSerializer<StringEnumArgumentType, StringEnumArgumentType> {
    INSTANCE;

    @Override
    public void writePacket(StringEnumArgumentType properties, PacketByteBuf buf) {
      buf.writeCollection(properties.values, PacketCodecs.STRING);
      buf.writeBoolean(properties.acceptsOtherValues);
    }

    @Override
    public StringEnumArgumentType fromPacket(PacketByteBuf buf) {
      return new StringEnumArgumentType(buf.readCollection(ArrayList::new, PacketCodecs.STRING), buf.readBoolean());
    }

    @Override
    public void writeJson(StringEnumArgumentType properties, JsonObject json) {
      final JsonArray array = new JsonArray();
      for (String value : properties.values) {
        array.add(value);
      }
      json.add("values", array);
      json.addProperty("accepts_other_values", properties.acceptsOtherValues);
    }

    @Override
    public StringEnumArgumentType getArgumentTypeProperties(StringEnumArgumentType argumentType) {
      return argumentType;
    }
  }
}
