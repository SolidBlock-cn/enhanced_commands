package pers.solid.ecmd.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record NameExistencePropertyPredicate(String propertyName, boolean exists) implements PropertyNamePredicate {
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
    return new TestResult(successes, Text.translatable("enhanced_commands.argument.block_predicate.property_name_" + string + "_" + string2, Text.literal(propertyName).styled(TextUtil.STYLE_FOR_TARGET)).formatted(formatting));
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putBoolean("exists", exists);
  }
}
