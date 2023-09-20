package pers.solid.ecmd.function.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;
import pers.solid.ecmd.util.SuggestionUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public interface BlockFunctionArgument extends Function<ServerCommandSource, BlockFunction> {
  Text OVERLAY_TOOLTIP = Text.translatable("enhancedCommands.argument.block_function.overlay.symbol_tooltip");
  Text PICK_TOOLTIP = Text.translatable("enhancedCommands.argument.block_function.pick.symbol_tooltip");

  static @NotNull BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    return parse(commandRegistryAccess, parser, suggestionsOnly, true);
  }

  static @NotNull BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return parsePick(commandRegistryAccess, parser, suggestionsOnly, allowsSparse);
  }

  static @NotNull BlockFunctionArgument parsePick(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return SuggestionUtil.parseUnifiable(() -> parseOverlay(commandRegistryAccess, parser, suggestionsOnly, allowsSparse), functions -> source -> new PickBlockFunction.Uniform(ImmutableList.copyOf(Lists.transform(functions, function -> function.apply(source)))), "|", PICK_TOOLTIP, parser, allowsSparse);
  }

  static @NotNull BlockFunctionArgument parseOverlay(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowsSparse) throws CommandSyntaxException {
    return SuggestionUtil.parseUnifiable(() -> parseCombination(commandRegistryAccess, parser, suggestionsOnly), functions -> source -> new OverlayBlockFunction(ImmutableList.copyOf(Lists.transform(functions, function -> function.apply(source)))), "*", OVERLAY_TOOLTIP, parser, allowsSparse);
  }

  static @NotNull BlockFunctionArgument parseCombination(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final BlockFunctionArgument parseUnit = parseUnit(commandRegistryAccess, parser, suggestionsOnly);
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
  static BlockFunctionArgument parseUnit(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final int cursorOnStart = parser.reader.getCursor();

    final Stream<BlockFunctionType<?>> stream = commandRegistryAccess.createWrapper(BlockFunctionType.REGISTRY_KEY).streamEntries().map(RegistryEntry.Reference::value);
    // 强制将 simple 调整到最后再去使用
    Iterable<BlockFunctionType<?>> iterable = Iterables.concat(stream.filter(type -> type != BlockFunctionTypes.SIMPLE)::iterator, Collections.singleton(BlockFunctionTypes.SIMPLE));
    for (BlockFunctionType<?> type : iterable) {
      parser.reader.setCursor(cursorOnStart);
      final BlockFunctionArgument parse = type.parse(commandRegistryAccess, parser, suggestionsOnly);
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }

    }
    parser.reader.setCursor(cursorOnStart);
    throw BlockFunction.CANNOT_PARSE.createWithContext(parser.reader);
  }
}
