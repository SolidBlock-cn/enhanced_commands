package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.argument.NbtFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.util.ParsingUtil;

public record LiteralNbtSource(CompoundNbtFunction nbtFunction) implements NbtSource.Single, NbtSourceArgument {
  @Override
  public Text feedbackQuery(NbtElement nbtElement, NbtConcentrationType nbtConcentrationType) {
    return Text.translatable("enhanced_commands.nbt.literal.query", NbtHelper.toPrettyPrintedText(nbtElement));
  }

  @Override
  public NbtCompound getNbt() {
    return nbtFunction.apply(null);
  }

  @Override
  public LiteralNbtSource getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return this;
  }

  public static LiteralNbtSource handle(CommandRegistryAccess registryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final CompoundNbtFunction compoundNbtFunction = new NbtFunctionSuggestedParser(parser.reader, parser.suggestionProviders).parseCompound(false);
    return new LiteralNbtSource(compoundNbtFunction);
  }
}
