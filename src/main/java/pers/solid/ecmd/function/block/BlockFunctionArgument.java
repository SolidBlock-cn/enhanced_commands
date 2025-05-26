package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableFunction;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtFunctionParser;
import pers.solid.ecmd.argument.NbtPredicateParser;
import pers.solid.ecmd.argument.SimpleBlockFunctionParser;
import pers.solid.ecmd.argument.SimpleBlockParser;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.math.WeightedList;
import pers.solid.ecmd.util.parse.ParseContext;
import pers.solid.ecmd.util.parse.Parser;
import pers.solid.ecmd.util.parse.ParsingUtil;

import java.util.Collections;
import java.util.List;

public interface BlockFunctionArgument extends FailableFunction<ServerCommandSource, BlockFunction, CommandSyntaxException> {
  Text OVERLAY_TOOLTIP = Text.translatable("enhanced_commands.block_function.overlay.symbol_tooltip");
  Text PICK_TOOLTIP = Text.translatable("enhanced_commands.block_function.pick.symbol_tooltip");

  static @NotNull BlockFunctionArgument parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    return parsePick(parseContext);
  }

  static @NotNull BlockFunctionArgument parsePick(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseOverlay(parseContext), functions -> source -> {
      ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
      for (BlockFunctionArgument function : functions) {
        builder.add(function.apply(source));
      }
      return new PickBlockFunction(new WeightedList.Uniform<>(builder.build()));
    }, "|", PICK_TOOLTIP, parseContext);
  }

  static @NotNull BlockFunctionArgument parseOverlay(ParseContext<?> parseContext) throws CommandSyntaxException {
    return ParsingUtil.parseUnifiable(() -> parseCombination(parseContext), functions -> source -> {
      ImmutableList.Builder<BlockFunction> builder = new ImmutableList.Builder<>();
      for (BlockFunctionArgument blockFunctionArgument : functions) {
        builder.add(blockFunctionArgument.apply(source));
      }
      return new OverlayBlockFunction(builder.build());
    }, "*", OVERLAY_TOOLTIP, parseContext);
  }

  static @NotNull <S> BlockFunctionArgument parseCombination(ParseContext<S> parseContext) throws CommandSyntaxException {
    final BlockFunctionArgument parseUnit = parseUnit(parseContext);
    if (parseUnit instanceof NbtBlockFunction) {
      return parseUnit;
    }
    final StringReader reader = parseContext.reader();
    List<PropertyNameFunction> propertyNameFunctions;

    if (!(parseUnit instanceof PropertyNamesBlockFunction) && reader.canRead(0) && reader.peek(-1) != ']') {
      // 当前面以“]”结尾时，说明已经在其他解析器中读取了属性，此时在这里不再读取任何属性
      // 尝试读取属性
      parseContext.addSuggestion((context, builder) -> builder.suggest("[", SimpleBlockParser.START_OF_PROPERTIES).buildFuture());
      if (reader.canRead() && reader.peek() == '[') {
        final SimpleBlockFunctionParser<S> suggestedParser = new SimpleBlockFunctionParser<>(parseContext);
        suggestedParser.parsePropertyNames();
        propertyNameFunctions = suggestedParser.propertyNameFunctions;
      } else {
        propertyNameFunctions = null;
      }
    } else {
      propertyNameFunctions = null;
    }
    CompoundNbtFunction nbtFunction;
    parseContext.addSuggestion((context, suggestionsBuilder) -> suggestionsBuilder.suggest("{", NbtPredicateParser.START_OF_COMPOUND).buildFuture());
    if (reader.canRead() && reader.peek() == '{') {
      // 尝试读取 NBT
      nbtFunction = new NbtFunctionParser<>(parseContext).parseCompound(false);
    } else {
      nbtFunction = null;
    }
    if (propertyNameFunctions != null || nbtFunction != null) {
      return source -> new PropertiesNbtCombinationBlockFunction(parseUnit.apply(source), propertyNameFunctions == null ? null : new PropertyNamesBlockFunction(propertyNameFunctions), nbtFunction == null ? null : new NbtBlockFunction(nbtFunction));
    }
    return parseUnit;
  }

  @NotNull
  static BlockFunctionArgument parseUnit(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorOnStart = reader.getCursor();

    // 强制将 simple 调整到最后再去使用
    for (Parser<BlockFunctionArgument> argumentParser : Iterables.concat(BlockFunctionTypes.PARSERS, Collections.singleton(SimpleBlockFunction.Type.SIMPLE_TYPE))) {
      reader.setCursor(cursorOnStart);
      final BlockFunctionArgument parse = argumentParser.parse(parseContext);
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
