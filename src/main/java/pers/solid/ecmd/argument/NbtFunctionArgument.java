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
import pers.solid.ecmd.nbt.function.CompoundNbtFunction;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.nbt.function.NbtFunctionParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.concurrent.CompletableFuture;

public record NbtFunctionArgument(boolean onlyCompounds, CommandBuildContext commandBuildContext) implements ArgumentType<NbtFunction> {

  public static NbtFunctionArgument compound(CommandBuildContext commandBuildContext) {
    return new NbtFunctionArgument(true, commandBuildContext);
  }

  public static NbtFunctionArgument element(CommandBuildContext commandBuildContext) {
    return new NbtFunctionArgument(false, commandBuildContext);
  }

  public static NbtFunction getNbtFunction(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, NbtFunction.class);
  }

  public static CompoundNbtFunction getCompoundNbtFunction(CommandContext<?> context, String name) {
    return context.getArgument(name, CompoundNbtFunction.class);
  }

  @Override
  public NbtFunction parse(StringReader reader) throws CommandSyntaxException {
    final ParseContext<Object> parseContext = new ParseContext<>(commandBuildContext, reader, false, false);
    final NbtFunctionParser<?> parser = new NbtFunctionParser<>(parseContext);
    return onlyCompounds ? parser.parsePreferringCompound(false, false) : parser.parseNbtFunction(false, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(commandBuildContext, stringReader, false, false);
    final NbtFunctionParser<S> parser = new NbtFunctionParser<>(parseContext);
    try {
      if (onlyCompounds) {
        parser.parseCompound(false);
      } else {
        parser.parseNbtFunction(false, false);
      }
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parseContext.buildSuggestions(context, builderOffset);
  }

  public enum Info implements ArgumentTypeInfo<NbtFunctionArgument, Template> {
    INSTANCE;

    @Override
    public void serializeToNetwork(NbtFunctionArgument.Template template, FriendlyByteBuf buf) {
      buf.writeBoolean(template.onlyCompounds);
    }

    @Override
    public NbtFunctionArgument.Template deserializeFromNetwork(FriendlyByteBuf buf) {
      return new NbtFunctionArgument.Template(buf.readBoolean());
    }

    @Override
    public void serializeToJson(NbtFunctionArgument.Template template, JsonObject json) {
      json.addProperty("onlyCompounds", template.onlyCompounds);
    }

    @Override
    public NbtFunctionArgument.Template unpack(NbtFunctionArgument argumentType) {
      return new NbtFunctionArgument.Template(argumentType.onlyCompounds);
    }
  }

  public record Template(boolean onlyCompounds) implements ArgumentTypeInfo.Template<NbtFunctionArgument> {

    @Override
    public NbtFunctionArgument instantiate(CommandBuildContext commandRegistryAccess) {
      return new NbtFunctionArgument(onlyCompounds, commandRegistryAccess);
    }

    @Override
    public ArgumentTypeInfo<NbtFunctionArgument, ?> type() {
      return Info.INSTANCE;
    }
  }
}
