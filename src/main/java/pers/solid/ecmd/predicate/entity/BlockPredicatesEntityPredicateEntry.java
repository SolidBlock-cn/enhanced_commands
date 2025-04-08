package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.block.BlockPredicateContext;
import pers.solid.ecmd.util.TestResult;

import java.util.Map;
import java.util.stream.Collectors;

public record BlockPredicatesEntityPredicateEntry(Map<PosArgument, BlockPredicate> map) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
    for (Map.Entry<PosArgument, BlockPredicate> entry : map.entrySet()) {
      final var key = entry.getKey();
      final var value = entry.getValue();
      if (!value.test(new CachedBlockPosition(entity.getWorld(), key.toAbsoluteBlockPos(entity.getCommandSource()), false), new BlockPredicateContext(entity.getRandom(), null))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    final ImmutableList.Builder<TestResult> attachments = new ImmutableList.Builder<>();
    boolean result = true;
    for (Map.Entry<PosArgument, BlockPredicate> entry : map.entrySet()) {
      final var key = entry.getKey();
      final var value = entry.getValue();
      final TestResult testResult = value.testAndDescribe(new CachedBlockPosition(entity.getWorld(), key.toAbsoluteBlockPos(entity.getCommandSource()), false), new BlockPredicateContext(entity.getRandom(), null));
      attachments.add(testResult);
      result &= testResult.successes();
    }
    if (result) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.block.pass_multiple", displayName), attachments.build());
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.block.fail_multiple", displayName), attachments.build());
    }
  }

  @Override
  public @Nullable String toOptionEntry() {
    return "block=" + map.entrySet().stream().map(entry -> "<" + entry.getKey().toString() + "> = " + entry.getValue().asString()).collect(Collectors.joining(", ", "{", "}"));
  }
}
