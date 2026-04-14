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
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.nbt.predicate.NbtPredicateParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.concurrent.CompletableFuture;

public record NbtPredicateArgument(boolean onlyCompounds, CommandBuildContext commandBuildContext) implements ArgumentType<NbtPredicate> {

  public static NbtPredicateArgument compound(CommandBuildContext commandBuildContext) {
    return new NbtPredicateArgument(true, commandBuildContext);
  }

  public static NbtPredicateArgument element(CommandBuildContext commandBuildContext) {
    return new NbtPredicateArgument(false, commandBuildContext);
  }

  public static NbtPredicate getNbtPredicate(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, NbtPredicate.class);
  }

  @Override
  public NbtPredicate parse(StringReader reader) throws CommandSyntaxException {
    final ParseContext<Object> parseContext = new ParseContext<>(commandBuildContext, reader, false, false);
    return onlyCompounds ? NbtPredicateParser.parseCompound(parseContext, false) : NbtPredicateParser.parseNbtPredicate(parseContext, false, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(commandBuildContext, stringReader, true, false);
    try {
      if (onlyCompounds) {
        NbtPredicateParser.parseCompound(parseContext, false);
      } else {
        NbtPredicateParser.parseNbtPredicate(parseContext, false, false);
      }
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parseContext.buildSuggestions(context, builderOffset);
  }


  public enum Info implements ArgumentTypeInfo<NbtPredicateArgument, Template> {
    INSTANCE;

    @Override
    public void serializeToNetwork(NbtPredicateArgument.Template template, FriendlyByteBuf buf) {
      buf.writeBoolean(template.onlyCompounds);
    }

    @Override
    public NbtPredicateArgument.Template deserializeFromNetwork(FriendlyByteBuf buf) {
      return new NbtPredicateArgument.Template(buf.readBoolean());
    }

    @Override
    public void serializeToJson(NbtPredicateArgument.Template template, JsonObject json) {
      json.addProperty("onlyCompounds", template.onlyCompounds);
    }

    @Override
    public NbtPredicateArgument.Template unpack(NbtPredicateArgument argumentType) {
      return new NbtPredicateArgument.Template(argumentType.onlyCompounds);
    }
  }

  public record Template(boolean onlyCompounds) implements ArgumentTypeInfo.Template<NbtPredicateArgument> {

    @Override
    public NbtPredicateArgument instantiate(CommandBuildContext commandRegistryAccess) {
      return new NbtPredicateArgument(onlyCompounds, commandRegistryAccess);
    }

    @Override
    public ArgumentTypeInfo<NbtPredicateArgument, ?> type() {
      return Info.INSTANCE;
    }
  }
}
