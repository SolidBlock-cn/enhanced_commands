package pers.solid.ecmd.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;

import java.util.concurrent.CompletableFuture;

public record NbtPredicateArgumentType(boolean onlyCompounds, CommandBuildContext commandBuildContext) implements ArgumentType<NbtPredicate> {

  public static NbtPredicateArgumentType compound(CommandBuildContext commandBuildContext) {
    return new NbtPredicateArgumentType(true, commandBuildContext);
  }

  public static NbtPredicateArgumentType element(CommandBuildContext commandBuildContext) {
    return new NbtPredicateArgumentType(false, commandBuildContext);
  }

  public static NbtPredicate getNbtPredicate(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, NbtPredicate.class);
  }

  @Override
  public NbtPredicate parse(StringReader reader) throws CommandSyntaxException {
    final ParseContext<Object> parseContext = new ParseContext<>(commandBuildContext, reader, false, false);
    final NbtPredicateParser<?> parser = new NbtPredicateParser<>(parseContext);
    return onlyCompounds ? parser.parseCompound(false, false) : parser.parsePredicate(false, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(commandBuildContext, stringReader, true, false);
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


  public enum Serializer implements ArgumentTypeInfo<NbtPredicateArgumentType, NbtPredicateArgumentType.Properties> {
    INSTANCE;

    @Override
    public void serializeToNetwork(Properties properties, FriendlyByteBuf buf) {
      buf.writeBoolean(properties.onlyCompounds);
    }

    @Override
    public @NotNull Properties deserializeFromNetwork(FriendlyByteBuf buf) {
      return new Properties(buf.readBoolean());
    }

    @Override
    public void serializeToJson(Properties properties, JsonObject json) {
      json.addProperty("onlyCompounds", properties.onlyCompounds);
    }

    @Override
    public @NotNull Properties unpack(NbtPredicateArgumentType argumentType) {
      return new Properties(argumentType.onlyCompounds);
    }
  }

  public record Properties(boolean onlyCompounds) implements ArgumentTypeInfo.Template<NbtPredicateArgumentType> {

    @Override
    public @NotNull NbtPredicateArgumentType instantiate(CommandBuildContext commandRegistryAccess) {
      return new NbtPredicateArgumentType(onlyCompounds, commandRegistryAccess);
    }

    @Override
    public @NotNull ArgumentTypeInfo<NbtPredicateArgumentType, ?> type() {
      return Serializer.INSTANCE;
    }
  }
}
