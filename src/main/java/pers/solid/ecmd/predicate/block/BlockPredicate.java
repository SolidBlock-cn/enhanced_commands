package pers.solid.ecmd.predicate.block;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.StringRepresentablePredicate;
import pers.solid.ecmd.util.NbtConvertible;

public interface BlockPredicate extends StringRepresentablePredicate, NbtConvertible, BlockPredicateArgument {
  SimpleCommandExceptionType CANNOT_PARSE = new SimpleCommandExceptionType(Text.translatable("enhancedCommands.argument.block_predicate.cannotParse"));

  static @NotNull BlockPredicate parse(CommandRegistryAccess commandRegistryAccess, String s, ServerCommandSource source) throws CommandSyntaxException {
    return BlockPredicateArgument.parse(commandRegistryAccess, new SuggestedParser(s), false).apply(source);
  }

  boolean test(CachedBlockPosition cachedBlockPosition);

  default TestResult testAndDescribe(CachedBlockPosition cachedBlockPosition) {
    final boolean test = test(cachedBlockPosition);
    return TestResult.successOrFail(test, cachedBlockPosition.getBlockPos());
  }

  @NotNull
  BlockPredicateType<?> getType();

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

  @Override
  default BlockPredicate apply(ServerCommandSource source) {
    return this;
  }
}
