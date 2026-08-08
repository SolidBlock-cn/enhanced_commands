package pers.solid.ecmd.block.predicate;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.mixins.accessor.BlockInWorldAccessor;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.List;

public record BlockFunctionResultBlockPredicate(BlockFunction tryApply, BlockPredicate predicate) implements BlockPredicate {
  public static final MapCodec<BlockFunctionResultBlockPredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BlockFunction.CODEC.fieldOf("try_apply").forGetter(BlockFunctionResultBlockPredicate::tryApply),
      BlockPredicate.CODEC.fieldOf("predicate").forGetter(BlockFunctionResultBlockPredicate::predicate)
  ).apply(i, BlockFunctionResultBlockPredicate::new));

  @Override
  public boolean test(BlockInWorld blockInWorld, ExecutionContext context) {
    final BlockState modifiedState;
    try {
      modifiedState = tryApply.getModifiedState(blockInWorld.getState(), blockInWorld.getState(), context.positionProvider.getWorld$ec(), blockInWorld.getPos(), new MutableObject<>(), context);
    } catch (CommandSyntaxException e) {
      return false;
    }

    final BlockInWorld newBlockInWorld = new BlockInWorld(blockInWorld.getLevel(), blockInWorld.getPos(), false);
    ((BlockInWorldAccessor) newBlockInWorld).setState(modifiedState);
    ((BlockInWorldAccessor) newBlockInWorld).setCachedEntity(true);
    return predicate.test(newBlockInWorld, context);
  }

  @Override
  public BlockPredicateType<BlockFunctionResultBlockPredicate> getType() {
    return BlockPredicateTypes.TEST_BLOCK_FUNCTION_RESULT;
  }

  @Override
  public String expressAsString() {
    return "block-function-result(" + tryApply.expressAsString() + ", " + predicate.expressAsString() + ")";
  }

  @Override
  public Iterable<? extends @Nullable Object> membersToValidate() {
    return List.of(tryApply);
  }
}
