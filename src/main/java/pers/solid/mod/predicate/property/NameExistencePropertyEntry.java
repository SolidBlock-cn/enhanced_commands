package pers.solid.mod.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.mod.EnhancedCommands;
import pers.solid.mod.command.TestResult;

public record NameExistencePropertyEntry(String propertyName, boolean exists) implements PropertyNameEntry {
  @Override
  public @NotNull String asString() {
    return propertyName + (exists ? "=*" : "!=*");
  }

  @Override
  public boolean test(BlockState blockState) {
    return (blockState.getBlock().getStateManager().getProperty(propertyName) != null) == exists;
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final boolean actualExists = blockState.getBlock().getStateManager().getProperty(propertyName) != null;
    final boolean successes = actualExists == exists;
    final String string = successes ? "pass" : "fail";
    final String string2 = actualExists ? "exists" : "does_not_exist";
    final Formatting formatting = successes ? Formatting.GREEN : Formatting.RED;
    return new TestResult(successes, Text.translatable("blockPredicate.property_name_" + string + "_" + string2, Text.literal(propertyName).styled(EnhancedCommands.STYLE_FOR_TARGET)).formatted(formatting));
  }
}
