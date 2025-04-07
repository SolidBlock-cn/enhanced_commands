package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;

public record BlockPredicateEntityPredicateEntry(BlockPredicate blockPredicate) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
    return blockPredicate.test(new CachedBlockPosition(entity.getWorld(), entity.getBlockPos(), false));
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    final TestResult testResult = blockPredicate.testAndDescribe(new CachedBlockPosition(entity.getWorld(), entity.getBlockPos(), false));
    if (testResult.successes()) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.block.pass", displayName, TextUtil.wrapVector(entity.getBlockPos())), List.of(testResult));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.block.fail", displayName, TextUtil.wrapVector(entity.getBlockPos())), List.of(testResult));
    }
  }

  @Override
  public @Nullable String toOptionEntry() {
    return "block=" + blockPredicate.asString();
  }
}
