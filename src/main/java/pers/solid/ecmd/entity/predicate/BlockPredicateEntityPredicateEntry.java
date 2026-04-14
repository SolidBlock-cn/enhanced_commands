package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.PositionProvider;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;

public record BlockPredicateEntityPredicateEntry(BlockPredicate predicate) implements EntityPredicateEntry {
  public static final MapCodec<BlockPredicateEntityPredicateEntry> CODEC = BlockPredicate.CODEC.fieldOf("predicate").xmap(BlockPredicateEntityPredicateEntry::new, BlockPredicateEntityPredicateEntry::predicate);

  @Override
  public boolean test(Entity entity, ExecutionContext context) {
    return predicate.test(new BlockInWorld(entity.level(), entity.blockPosition(), false), new ExecutionContext(entity.getRandom(), PositionProvider.of(entity), null));
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final TestResult testResult = predicate.testAndDescribe(new BlockInWorld(entity.level(), entity.blockPosition(), false), new ExecutionContext(entity.getRandom(), PositionProvider.of(entity), null));
    if (testResult.successes()) {
      return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.block.pass", displayName, TextUtil.wrapVector(entity.blockPosition())), List.of(testResult));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.block.fail", displayName, TextUtil.wrapVector(entity.blockPosition())), List.of(testResult));
    }
  }

  @Override
  public String toOptionEntry() {
    final String string = predicate.asString();
    return "block=" + (string.startsWith("{") ? "(" + string + ")" : string);
  }

  @Override
  public EntityPredicateType<BlockPredicateEntityPredicateEntry> getType() {
    return EntityPredicateTypes.BLOCK_PREDICATE;
  }
}
