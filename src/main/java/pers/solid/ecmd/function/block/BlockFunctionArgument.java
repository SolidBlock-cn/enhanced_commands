package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.Parser;
import pers.solid.ecmd.util.ParsingUtil;

import java.util.Collections;
import java.util.List;

public interface BlockFunctionArgument extends FailableFunction<ServerCommandSource, BlockFunction, CommandSyntaxException> {
  Text OVERLAY_TOOLTIP = Text.translatable("enhanced_commands.block_function.overlay.symbol_tooltip");
  Text PICK_TOOLTIP = Text.translatable("enhanced_commands.block_function.pick.symbol_tooltip");

  static @NotNull BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    return parse(commandRegistryAccess, parser, suggestionsOnly, true);
  }

  static @NotNull BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return parsePick(commandRegistryAccess, parser, suggestionsOnly, allowsSparse);
  }

  static @NotNull BlockFunctionArgument parsePick(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseOverlay(commandRegistryAccess, parser, suggestionsOnly, allowsSparse), functions -> source -> {
      ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
      for (BlockFunctionArgument function : functions) {
        builder.add(function.apply(source));
      }
      return new PickBlockFunction.Uniform(builder.build());
    }, "|", PICK_TOOLTIP, parser, allowsSparse);
  }

  static @NotNull BlockFunctionArgument parseOverlay(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseCombination(commandRegistryAccess, parser, suggestionsOnly, allowsSparse), functions -> source -> {
      ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
      for (BlockFunctionArgument blockFunctionArgument : functions) {
        builder.add(blockFunctionArgument.apply(source));
      }
      return new OverlayBlockFunction(builder.build());
    }, "*", OVERLAY_TOOLTIP, parser, allowsSparse);
  }

  static @NotNull BlockFunctionArgument parseCombination(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    final BlockFunctionArgument parseUnit = parseUnit(commandRegistryAccess, parser, suggestionsOnly, allowsSparse);
    if (parseUnit instanceof NbtBlockFunction) {
      return parseUnit;
    }
    List<PropertyNameFunction> propertyNameFunctions;
    if (!(parseUnit instanceof PropertyNamesBlockFunction) && parser.reader.canRead(0) && parser.reader.peek(-1) != ']') {
      // 当前面以“]”结尾时，说明已经在其他解析器中读取了属性，此时在这里不再读取任何属性
      // 尝试读取属性
      parser.suggestionProviders.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest("[", SimpleBlockSuggestedParser.START_OF_PROPERTIES);
        }
      });
      if (parser.reader.canRead() && parser.reader.peek() == '[') {
        final SimpleBlockFunctionSuggestedParser suggestedParser = new SimpleBlockFunctionSuggestedParser(commandRegistryAccess, parser);
        suggestedParser.parsePropertyNames();
        propertyNameFunctions = suggestedParser.propertyNameFunctions;
      } else {propertyNameFunctions = null;}
    } else {propertyNameFunctions = null;}
    CompoundNbtFunction nbtFunction;
    parser.suggestionProviders.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("{", NbtPredicateSuggestedParser.START_OF_COMPOUND);
      }
    });
    if (parser.reader.canRead() && parser.reader.peek() == '{') {
      // 尝试读取 NBT
      nbtFunction = new NbtFunctionSuggestedParser(parser.reader, parser.suggestionProviders).parseCompound(false);
    } else {nbtFunction = null;}
    if (propertyNameFunctions != null || nbtFunction != null) {
      return source -> new PropertiesNbtCombinationBlockFunction(parseUnit.apply(source), propertyNameFunctions == null ? null : new PropertyNamesBlockFunction(propertyNameFunctions), nbtFunction == null ? null : new NbtBlockFunction(nbtFunction));
    }
    return parseUnit;
  }

  @NotNull
  static BlockFunctionArgument parseUnit(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    final StringReader reader = parser.reader;
    final int cursorOnStart = reader.getCursor();

    // 强制将 simple 调整到最后再去使用
    for (Parser<BlockFunctionArgument> argumentParser : Iterables.concat(BlockFunctionTypes.PARSERS, Collections.singleton(SimpleBlockFunction.Type.SIMPLE_TYPE))) {
      reader.setCursor(cursorOnStart);
      final BlockFunctionArgument parse = argumentParser.parse(commandRegistryAccess, parser, suggestionsOnly, allowsSparse);
      if (parse != null) {
        return parse;
      }
    }
    reader.setCursor(cursorOnStart);
    throw BlockFunction.CANNOT_PARSE.createWithContext(reader);
  }

  @Override
  BlockFunction apply(ServerCommandSource source) throws CommandSyntaxException;
}
