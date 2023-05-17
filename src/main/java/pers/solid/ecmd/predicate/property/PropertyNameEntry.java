package pers.solid.ecmd.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.predicate.SerializablePredicate;

public interface PropertyNameEntry extends SerializablePredicate {
  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  String propertyName();
}
