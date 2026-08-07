package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.function.BlockFunctionContext;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;

public record TestBlockFunctionBlockPredicate(BlockFunction blockFunction) implements BlockPredicate {
  public static final MapCodec<TestBlockFunctionBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(BlockFunction.CODEC.fieldOf("block_function").forGetter(TestBlockFunctionBlockPredicate::blockFunction)).apply(i, TestBlockFunctionBlockPredicate::new));

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext executionContext) {
    final BlockState modifiedState;
    try {
      modifiedState = blockFunction.getModifiedState(blockInWorld.getState(), blockInWorld.getState(), executionContext.positionProvider.getWorld$ec(), blockInWorld.getPos(), null, new BlockFunctionContext(3, 0, executionContext.random, executionContext.positionProvider, executionContext.getSeed()));
    } catch (CommandSyntaxException e) {
      return false;
    }
    return modifiedState == blockInWorld.getState();
  }

  @Override
  public BlockPredicateType<TestBlockFunctionBlockPredicate> getType() {
    return BlockPredicateTypes.TEST_BLOCK_FUNCTION;
  }

  @Override
  public String expressAsString() {
    return "test_block_function(" + blockFunction.expressAsString() + ")";
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(blockFunction);
  }
}
