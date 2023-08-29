package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.NbtPredicateSuggestedParser;
import pers.solid.ecmd.argument.SimpleBlockPredicateSuggestedParser;
import pers.solid.ecmd.argument.SimpleBlockSuggestedParser;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.StringRepresentablePredicate;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.predicate.property.PropertyNamePredicate;
import pers.solid.ecmd.util.NbtConvertible;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public interface BlockPredicate extends StringRepresentablePredicate, NbtConvertible {
  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_predicate.cannotParse"));

  static @NotNull BlockPredicate parse(CommandRegistryAccess commandRegistryAccess, String s) throws CommandSyntaxException {
    return parse(commandRegistryAccess, new SuggestedParser(new StringReader(s)), false);
  }

  static @NotNull BlockPredicate parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final BlockPredicate parseUnit = parseUnit(commandRegistryAccess, parser, suggestionsOnly);
    if (parseUnit instanceof NbtPredicate) {
      return parseUnit;
    }
    List<PropertyNamePredicate> propertyNamePredicates = null;
    if (!(parseUnit instanceof PropertyNamesBlockPredicate) && parser.reader.canRead(0) && parser.reader.peek(-1) != ']') {
      // 当前面以“]”结尾时，说明已经在其他解析器中读取了属性，此时在这里不再读取任何属性
      // 尝试读取属性
      parser.suggestions.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest("[", SimpleBlockSuggestedParser.START_OF_PROPERTIES);
        }
      });
      if (parser.reader.canRead() && parser.reader.peek() == '[') {
        final SimpleBlockPredicateSuggestedParser suggestedParser = new SimpleBlockPredicateSuggestedParser(commandRegistryAccess, parser);
        suggestedParser.parsePropertyNames();
        propertyNamePredicates = suggestedParser.propertyNamePredicates;
      }
    }
    NbtPredicate nbtPredicate = null;
    parser.suggestions.add((context, suggestionsBuilder) -> {
      if (suggestionsBuilder.getRemaining().isEmpty()) {
        suggestionsBuilder.suggest("{", NbtPredicateSuggestedParser.START_OF_COMPOUND);
      }
    });
    if (parser.reader.canRead() && parser.reader.peek() == '{') {
      // 尝试读取 NBT
      nbtPredicate = new NbtPredicateSuggestedParser(parser.reader, parser.suggestions).parseCompound(false, false);
    }
    if (propertyNamePredicates != null || nbtPredicate != null) {
      return new PropertiesNbtCombinationBlockPredicate(parseUnit, propertyNamePredicates == null ? null : new PropertyNamesBlockPredicate(propertyNamePredicates), nbtPredicate == null ? null : new NbtBlockPredicate(nbtPredicate));
    }
    return parseUnit;
  }

  @NotNull
  static BlockPredicate parseUnit(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final int cursorOnStart = parser.reader.getCursor();
    final Stream<BlockPredicateType<?>> stream = commandRegistryAccess.createWrapper(BlockPredicateType.REGISTRY_KEY).streamEntries().map(RegistryEntry.Reference::value);
    Iterable<BlockPredicateType<?>> iterable = Iterables.concat(stream.filter(type -> type != BlockPredicateTypes.SIMPLE)::iterator, Collections.singleton(BlockPredicateTypes.SIMPLE));
    for (BlockPredicateType<?> type : iterable) {
      parser.reader.setCursor(cursorOnStart);
      final BlockPredicate parse = type.parse(commandRegistryAccess, parser, suggestionsOnly);
      if (parse != null) {
        // keep the current position of the cursor
        return parse;
      }
    }
    parser.reader.setCursor(cursorOnStart);
    throw CANNOT_PARSE.createWithContext(parser.reader);
  }

  boolean test(CachedBlockPosition cachedBlockPosition);

  default TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final boolean test = test(cachedBlockPosition);
    return TestResult.successOrFail(test, cachedBlockPosition.getBlockPos());
  }

  @NotNull BlockPredicateType<?> getType();

  @Override
  default NbtCompound createNbt() {
    final NbtCompound nbt = NbtConvertible.super.createNbt();
    final BlockPredicateType<?> type = getType();
    final Identifier id = BlockPredicateType.REGISTRY.getId(type);
    nbt.putString("type", Preconditions.checkNotNull(id, "Unknown block predicate type: %s", type).toString());
    return nbt;
  }

  /**
   * 从 NBT 中获取一个 BlockPredicate 对象。会先从这个 NBT 中获取 type，并从注册表中获取。如果这个 type 不正确，或者里面的参数不正确，会直接抛出错误。
   */
  static BlockPredicate fromNbt(NbtCompound nbtCompound) {
    final BlockPredicateType<?> type = BlockPredicateType.REGISTRY.get(new Identifier(nbtCompound.getString("type")));
    Preconditions.checkNotNull(type, "Unknown block predicate type: %s", type);
    return type.fromNbt(nbtCompound);
  }
}
