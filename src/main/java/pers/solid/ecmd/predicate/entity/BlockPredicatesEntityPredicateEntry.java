package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.block.BlockPredicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record BlockPredicatesEntityPredicateEntry(Map<PosArgument, BlockPredicate> map) implements EntityPredicateEntry {
  @Override
  public boolean test(Entity entity) {
    for (Map.Entry<PosArgument, BlockPredicate> entry : map.entrySet()) {
      final var key = entry.getKey();
      final var value = entry.getValue();
      if (!value.test(new CachedBlockPosition(entity.getWorld(), key.toAbsoluteBlockPos(entity.getCommandSource()), false))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    final List<TestResult> attachments = new ArrayList<>();
    boolean result = true;
    for (Map.Entry<PosArgument, BlockPredicate> entry : map.entrySet()) {
      final var key = entry.getKey();
      final var value = entry.getValue();
      final TestResult testResult = value.testAndDescribe(new CachedBlockPosition(entity.getWorld(), key.toAbsoluteBlockPos(entity.getCommandSource()), false));
      result &= testResult.successes();
    }
    if (result) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.block.pass_multiple", displayName), attachments);
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.block.fail_multiple", displayName), attachments);
    }
  }

  @Override
  public String toOptionEntry() {
    return "block=" + map.entrySet().stream().map(entry -> "<" + entry.getKey().toString() + "> = " + entry.getValue().asString()).collect(Collectors.joining(", ", "{", "}"));
  }
}
