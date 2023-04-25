package pers.solid.mod.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import pers.solid.mod.command.TestResult;
import pers.solid.mod.predicate.SerializablePredicate;

public interface PropertyNameEntry extends SerializablePredicate {
  boolean test(BlockState blockState);

  TestResult testAndDescribe(BlockState blockState, BlockPos blockPos);

  String propertyName();
}
