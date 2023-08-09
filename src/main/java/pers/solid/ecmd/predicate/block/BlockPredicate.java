package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.SerializablePredicate;
import pers.solid.ecmd.util.NbtConvertible;

public interface BlockPredicate extends SerializablePredicate, NbtConvertible {
  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_predicate.cannotParse"));

  @NotNull
  static BlockPredicate parse(SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    CommandSyntaxException exception = null;
    final int cursorOnStart = parser.reader.getCursor();
    int cursorOnEnd = cursorOnStart;
    for (BlockPredicateType<?> type : BlockPredicateType.REGISTRY) {
      try {
        parser.reader.setCursor(cursorOnStart);
        final BlockPredicate parse = type.parse(parser, suggestionsOnly);
        if (parse != null) {
          // keep the current position of the cursor
          return parse;
        }

      } catch (
          CommandSyntaxException exception1) {
        cursorOnEnd = parser.reader.getCursor();
        exception = exception1;
      }
    }
    parser.reader.setCursor(cursorOnEnd);
    if (exception != null)
      throw exception;
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

  static BlockPredicate fromNbt(NbtCompound nbtCompound) {
    final BlockPredicateType<?> type = BlockPredicateType.REGISTRY.get(new Identifier(nbtCompound.getString("type")));
    Preconditions.checkNotNull(type, "Unknown block predicate type: %s", type);
    return type.fromNbt(nbtCompound);
  }
}
