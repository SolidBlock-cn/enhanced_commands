package pers.solid.ecmd.nbt;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.parse.ParsingUtil;

import java.util.concurrent.CompletableFuture;

public final class NbtParserShared {
  public static final Component START_OF_COMPOUND = Component.translatable("enhanced_commands.nbt_predicate.tooltip.start_of_compound");
  public static final Component END_OF_COMPOUND = Component.translatable("enhanced_commands.nbt_predicate.tooltip.end_of_compound");
  public static final Component START_OF_LIST = Component.translatable("enhanced_commands.nbt_predicate.tooltip.start_of_list");
  public static final Component END_OF_LIST = Component.translatable("enhanced_commands.nbt_predicate.tooltip.end_of_list");
  public static final Component SEPARATE = Component.translatable("enhanced_commands.nbt_predicate.tooltip.separate");

  private NbtParserShared() {
  }

  public static boolean parseColonOrEqual(boolean mustExpectSign, boolean equalsForDefault, StringReader reader, int cursorBeforeSign, SimpleCommandExceptionType signExpected) throws CommandSyntaxException {
    boolean isUsingEqual = equalsForDefault;
    if (!reader.canRead()) {
      reader.setCursor(cursorBeforeSign);
      throw signExpected.createWithContext(reader);
    }
    if (reader.peek() == ':') {
      isUsingEqual = false;
      reader.skip();
      reader.skipWhitespace();
    } else if (reader.peek() == '=') {
      isUsingEqual = true;
      reader.skip();
      reader.skipWhitespace();
    } else if (mustExpectSign) {
      reader.setCursor(cursorBeforeSign);
      throw signExpected.createWithContext(reader);
    }
    return isUsingEqual;
  }

  public static CompletableFuture<Suggestions> suggestCompoundStart(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("{", START_OF_COMPOUND, suggestionsBuilder).buildFuture();
  }

  public static CompletableFuture<Suggestions> suggestCompoundSeparate(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString(",", SEPARATE, suggestionsBuilder).buildFuture();
  }

  public static CompletableFuture<Suggestions> suggestCompoundEnd(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("}", END_OF_COMPOUND, suggestionsBuilder).buildFuture();
  }

  public static CompletableFuture<Suggestions> suggestListEnd(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString("]", END_OF_LIST, suggestionsBuilder).buildFuture();
  }

  public static CompletableFuture<Suggestions> suggestListSeparate(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder) {
    return ParsingUtil.suggestString(",", SEPARATE, suggestionsBuilder).buildFuture();
  }
}
