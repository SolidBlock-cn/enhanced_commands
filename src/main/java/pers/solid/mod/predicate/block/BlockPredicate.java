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
import pers.solid.mod.argument.BlockPredicateArgumentParser;
import pers.solid.mod.command.TestResult;
import pers.solid.mod.predicate.SerializablePredicate;

public interface BlockPredicate extends SerializablePredicate {
  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("argument.ecBlockStatePredicate.cannotParse"));

  @NotNull
  static BlockPredicate parse(BlockPredicateArgumentParser parser) throws CommandSyntaxException {
    final int cursorBeforeRead = parser.reader.getCursor();
    CommandSyntaxException exception = null;
    for (BlockPredicateType<?> type : BlockPredicateType.REGISTRY) {
      try {
        final BlockPredicate parse = type.parse(parser);
        if (parse != null) {
          return parse;
        }
      } catch (CommandSyntaxException exception1) {
        exception = exception1;
      }
    }
    if (exception != null) throw exception;
    throw CANNOT_PARSE.createWithContext(parser.reader);
  }

  boolean test(CachedBlockPosition cachedBlockPosition);

  default TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final boolean test = test(cachedBlockPosition);
    return TestResult.successOrFail(test, cachedBlockPosition.getBlockPos());
  }

  BlockPredicateType<?> getType();

  @Contract(mutates = "param1")
  default void writeNbt(NbtCompound nbtCompound) {
    // TODO: 2023/4/24, 024 pending use
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
