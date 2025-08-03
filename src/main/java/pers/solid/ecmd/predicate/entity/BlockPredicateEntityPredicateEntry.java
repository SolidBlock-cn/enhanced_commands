package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;

public record BlockPredicateEntityPredicateEntry(BlockPredicate blockPredicate) implements EntityPredicateEntry {
  public static final MapCodec<BlockPredicateEntityPredicateEntry> CODEC = BlockPredicate.CODEC.fieldOf("block_predicate").xmap(BlockPredicateEntityPredicateEntry::new, BlockPredicateEntityPredicateEntry::blockPredicate);

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    return blockPredicate.test(new CachedBlockPosition(entity.getWorld(), entity.getBlockPos(), false), new ExecutionContext(entity.getRandom(), entity.getCommandSource(), null));
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final TestResult testResult = blockPredicate.testAndDescribe(new CachedBlockPosition(entity.getWorld(), entity.getBlockPos(), false), new ExecutionContext(entity.getRandom(), entity.getCommandSource(), null));
    if (testResult.successes()) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.block.pass", displayName, TextUtil.wrapVector(entity.getBlockPos())), List.of(testResult));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.block.fail", displayName, TextUtil.wrapVector(entity.getBlockPos())), List.of(testResult));
    }
  }

  @Override
  public @NotNull String toOptionEntry() {
    return "block=" + blockPredicate.asString();
  }

  @Override
  public @NotNull EntityPredicateType<BlockPredicateEntityPredicateEntry> getType() {
    return EntityPredicateTypes.BLOCK_PREDICATE;
  }
}
