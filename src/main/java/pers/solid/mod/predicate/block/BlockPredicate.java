package pers.solid.mod.predicate.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.solid.mod.argument.SuggestedParser;
import pers.solid.mod.command.TestResult;
import pers.solid.mod.predicate.SerializablePredicate;

public interface BlockPredicate extends SerializablePredicate {
  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("argument.ecBlockStatePredicate.cannotParse"));

  @NotNull
  static BlockPredicate parse(SuggestedParser parser) throws CommandSyntaxException {
    CommandSyntaxException exception = null;
    final int cursorOnStart = parser.reader.getCursor();
    int cursorOnEnd = cursorOnStart;
    for (BlockPredicateType<?> type : BlockPredicateType.REGISTRY) {
      try {
        parser.reader.setCursor(cursorOnStart);
        final BlockPredicate parse = type.parse(parser);
        if (parse != null) {
          // keep the current position of the cursor
          return parse;
        }

      } catch (CommandSyntaxException exception1) {
        cursorOnEnd = parser.reader.getCursor();
        exception = exception1;
      }
    }
    parser.reader.setCursor(cursorOnEnd);
    if (exception != null) throw exception;
    throw CANNOT_PARSE.createWithContext(parser.reader);
  }

  boolean test(CachedBlockPosition cachedBlockPosition);

  default TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final boolean test = test(cachedBlockPosition);
    return TestResult.successOrFail(test, cachedBlockPosition.getBlockPos());
  }

  @NotNull BlockPredicateType<?> getType();

  @Contract(mutates = "param1")
  default void writeNbt(NbtCompound nbtCompound) {
    // TODO: 2023/4/24, 024 nbt
  }

  @Override
  default NbtElement asNbt() {
    NbtCompound nbtCompound = new NbtCompound();
    final BlockPredicateType<?> type = getType();
    final Identifier id = BlockPredicateType.REGISTRY.getId(type);
    nbtCompound.putString("type", Preconditions.checkNotNull(id, "Unknown block state predicate type: %s", type).toString());
    writeNbt(nbtCompound);
    return nbtCompound;
  }

  static BlockPredicate fromNbt(NbtCompound nbtCompound) {
    final BlockPredicateType<?> type = BlockPredicateType.REGISTRY.get(new Identifier(nbtCompound.getString("type")));
    Preconditions.checkNotNull(type, "Unknown type: %s", type);
    return type.fromNbt(nbtCompound);
  }
}
