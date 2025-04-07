package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtFunctionSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.util.parse.ParsingUtil;

public record LiteralNbtSource(CompoundNbtFunction value) implements NbtSource.Single<CompoundNbtFunction>, NbtSourceArgument<CompoundNbtFunction> {
  @Override
  public Text feedbackQuery(NbtElement nbtElement) {
    return Text.translatable("enhanced_commands.nbt.literal.query", NbtHelper.toPrettyPrintedText(nbtElement));
  }

  @Override
  public NbtCompound getNbtFor(CompoundNbtFunction source, @NotNull RegistryWrapper.WrapperLookup registryLookup) {
    return source.apply(null);
  }

  @Override
  public LiteralNbtSource getNbtSource(ServerCommandSource source) throws CommandSyntaxException {
    return this;
  }

  public static LiteralNbtSource handle(CommandRegistryAccess registryAccess, SuggestedParser<?> parser, boolean suggestionsOnly) throws CommandSyntaxException {
    ParsingUtil.expectAndSkipWhitespace(parser.reader);
    final CompoundNbtFunction compoundNbtFunction = new NbtFunctionSuggestedParser<>(parser).parseCompound(false);
    return new LiteralNbtSource(compoundNbtFunction);
  }
}
