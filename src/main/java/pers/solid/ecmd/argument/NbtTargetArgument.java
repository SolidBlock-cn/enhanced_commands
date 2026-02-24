package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.CommandContextAccessor;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.nbt.BlockNbtData;
import pers.solid.ecmd.nbt.NbtDataRegistry;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.predicate.block.BlockPredicate;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record NbtTargetArgument(CommandBuildContext commandBuildContext) implements ArgumentType<NbtTarget<?>> {
  public static NbtTargetArgument nbtTarget(CommandBuildContext commandBuildContext) {
    return new NbtTargetArgument(commandBuildContext);
  }

  public static NbtTarget<?> getNbtTarget(CommandContext<CommandSourceStack> context, String name, @Nullable BlockPredicate blockPredicate) throws CommandSyntaxException {
    final NbtTarget<?> argument = context.getArgument(name, NbtTarget.class);
    if (argument instanceof BlockNbtData blockNbtData && blockPredicate != null) {
      return new BlockNbtData(blockNbtData.region(), blockPredicate);
    }
    return argument;
  }

  public static NbtTarget<?> getNbtTarget(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
    return getNbtTarget(context, name, ((CommandContextAccessor<?>) context).getArguments().containsKey("keyword_args") ? KeywordArgsArgument.getKeywordArgs(context, "keyword_args").getArg("affect_only") : null);
  }

  @Override
  public NbtTarget<?> parse(StringReader reader) throws CommandSyntaxException {
    final int cursorBeforeString = reader.getCursor();
    final String s = reader.readUnquotedString();
    final NbtTarget<?> nbtTargetArgument = NbtDataRegistry.handleTarget(s, new ParseContext<>(commandBuildContext, reader, false, true));
    if (nbtTargetArgument == null) {
      final int cursorAfterString = reader.getCursor();
      reader.setCursor(cursorBeforeString);
      throw CommandSyntaxExceptionExtension.withCursorEnd(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader), cursorAfterString);
    } else {
      return nbtTargetArgument;
    }
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    final StringReader reader = new StringReader(builder.getInput());
    reader.setCursor(builder.getStart());
    final int cursorBeforeString = reader.getCursor();
    final String s = reader.readUnquotedString();
    final NbtTarget<?> nbtTargetArgument;
    final ParseContext<S> parseContext = new ParseContext<>(commandBuildContext, reader, true, true);
    try {
      nbtTargetArgument = NbtDataRegistry.handleTarget(s, parseContext);
      if (nbtTargetArgument == null) {
        reader.setCursor(cursorBeforeString);
        return SharedSuggestionProvider.suggest(NbtDataRegistry.streamTargetTypes(), builder);
      }
    } catch (CommandSyntaxException ignored) {
    }
    return parseContext.buildSuggestions(context, builder.createOffset(reader.getCursor()));
  }

  private static final Collection<String> EXAMPLES = List.of("block ~ ~1 ~", "blocks sphere(5)", "entity @s", "entity Solid", "entity @e[type=pig,limit=1]", "entities @a", "entities @e", "store x");

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
