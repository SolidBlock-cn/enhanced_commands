package pers.solid.ecmd.predicate.property;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.Styles;

public record ExistencePropertyPredicate<T extends Comparable<T>>(Property<T> property, boolean exists) implements PropertyPredicate<T> {
  @Override
  public @NotNull String asString() {
    return property.getName() + (exists ? "=*" : "!=*");
  }

  @Override
  public boolean test(BlockState blockState) {
    return blockState.contains(property) == exists;
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final String propertyName = property.getName();
    final boolean actualExists = blockState.contains(property);
    final boolean successes = actualExists == exists;
    final MutableText blockText = blockState.getBlock().getName().styled(Styles.TARGET);
    final MutableText propertyNameText = Text.literal(propertyName).styled(Styles.TARGET);
    if (successes) {
      if (actualExists) {
        return TestResult.of(true, Text.translatable("enhanced_commands.property_predicate.property_pass_exists", blockText, propertyNameText));
      } else {
        return TestResult.of(true, Text.translatable("enhanced_commands.property_predicate.property_pass_absent", blockText, propertyNameText));
      }
    } else if (actualExists) {
      return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.property_fail_exists", blockText, propertyNameText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.property_fail_absent", blockText, propertyNameText));
    }
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", property.getName());
    nbtCompound.putBoolean("exists", exists);
  }
}
