package pers.solid.ecmd.function.block;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.nbt.CompoundNbtFunction;
import pers.solid.ecmd.function.property.PropertyNameFunction;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public interface BlockFunctionArgument extends Function<ServerCommandSource, BlockFunction> {
  static @NotNull BlockFunctionArgument parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final BlockFunctionArgument parseUnit = parseUnit(commandRegistryAccess, parser, suggestionsOnly);
    if (parseUnit instanceof NbtBlockFunction) {
      return parseUnit;
    }
    List<PropertyNameFunction> propertyNameFunctions;
    if (!(parseUnit instanceof PropertyNamesBlockFunction) && parser.reader.canRead(0) && parser.reader.peek(-1) != ']') {
      // 当前面以“]”结尾时，说明已经在其他解析器中读取了属性，此时在这里不再读取任何属性
      // 尝试读取属性
      parser.suggestions.add((context, suggestionsBuilder) -> {
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
    parser.suggestions.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("{", NbtPredicateSuggestedParser.START_OF_COMPOUND);
      }
    });
    if (parser.reader.canRead() && parser.reader.peek() == '{') {
      // 尝试读取 NBT
      nbtFunction = new NbtFunctionSuggestedParser(parser.reader, parser.suggestions).parseCompound(false);
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
