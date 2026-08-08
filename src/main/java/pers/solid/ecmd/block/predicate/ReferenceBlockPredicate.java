package pers.solid.ecmd.block.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.pack.ReferenceEntry;

import java.util.List;

public record ReferenceBlockPredicate(Holder.Reference<BlockPredicate> reference) implements BlockPredicate, ReferenceEntry<BlockPredicate> {
  public static final MapCodec<ReferenceBlockPredicate> CODEC = ReferenceEntry.createCodec(DefaultNamespace.MINECRAFT.idCodec(true), BlockPredicate.REGISTRY_KEY, ReferenceBlockPredicate::new);
  public static final PrefixedIdParser<ReferenceBlockPredicate, BlockPredicate> PREFIXED_ID_PARSER = new PrefixedIdParser<>('$', Component.translatable("enhanced_commands.block_predicate.reference"), BlockPredicate.REGISTRY_KEY, ReferenceBlockPredicate::new);

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext context) {
    return value().test(blockInWorld, context);
  }

  @Override
  public TestResult testAndDescribe(BlockInWorld blockInWorld, ExecutionContext context) {
    final TestResult testResult = value().testAndDescribe(blockInWorld, context);
    final MutableComponent posText = TextUtil.wrapVector(blockInWorld.getPos());
    final MutableComponent idText = TextUtil.literal(identifier()).withStyle(Styles.EXPECTED);
    if (testResult.successes()) {
      return TestResult.of(true, Component.translatable("enhanced_commands.block_predicate.reference.pass", posText, idText), List.of(testResult));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.block_predicate.reference.fail", posText, idText), List.of(testResult));
    }
  }

  @Override
  public BlockPredicateType<ReferenceBlockPredicate> getType() {
    return BlockPredicateTypes.REFERENCE;
  }

  @Override
  public String expressAsString() {
    return "$" + DefaultNamespace.ENHANCED_COMMANDS.toSimplerString(identifier());
  }
}
