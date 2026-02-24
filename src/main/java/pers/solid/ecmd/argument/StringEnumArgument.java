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
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public record StringEnumArgument(Collection<String> values, boolean acceptsOtherValues) implements ArgumentType<String>, ArgumentTypeInfo.Template<StringEnumArgument> {
  public static StringEnumArgument stringEnum(String... values) {
    return new StringEnumArgument(Arrays.asList(values), false);
  }

  public static StringEnumArgument stringEnum(Collection<String> values) {
    return new StringEnumArgument(values, false);
  }

  public static StringEnumArgument stringEnum(Collection<String> values, boolean acceptsOtherValues) {
    return new StringEnumArgument(values, acceptsOtherValues);
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
    return SharedSuggestionProvider.suggest(values, builder);
  }

  @Override
  public Collection<String> getExamples() {
    return ImmutableList.copyOf(Iterables.limit(values, 3));
  }

  @Override
  public @NotNull StringEnumArgument instantiate(CommandBuildContext commandRegistryAccess) {
    return this;
  }

  @Override
  public @NotNull ArgumentTypeInfo<StringEnumArgument, ?> type() {
    return Info.INSTANCE;
  }

  public enum Info implements ArgumentTypeInfo<StringEnumArgument, StringEnumArgument> {
    INSTANCE;

    @Override
    public void serializeToNetwork(StringEnumArgument properties, FriendlyByteBuf buf) {
      buf.writeCollection(properties.values, ByteBufCodecs.STRING_UTF8);
      buf.writeBoolean(properties.acceptsOtherValues);
    }

    @Override
    public @NotNull StringEnumArgument deserializeFromNetwork(FriendlyByteBuf buf) {
      return new StringEnumArgument(buf.readCollection(ArrayList::new, ByteBufCodecs.STRING_UTF8), buf.readBoolean());
    }

    @Override
    public void serializeToJson(StringEnumArgument properties, JsonObject json) {
      final JsonArray array = new JsonArray();
      for (String value : properties.values) {
        array.add(value);
      }
      json.add("values", array);
      json.addProperty("accepts_other_values", properties.acceptsOtherValues);
    }

    @Override
    public @NotNull StringEnumArgument unpack(StringEnumArgument argumentType) {
      return argumentType;
    }
  }
}
