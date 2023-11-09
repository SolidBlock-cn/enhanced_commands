package pers.solid.ecmd.predicate.property;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record ValueNamePropertyPredicate(String propertyName, Comparator comparator, String valueName) implements PropertyNamePredicate {
  @Override
  public @NotNull String asString() {
    return propertyName + comparator.asString() + valueName;
  }

  @Override
  public boolean test(BlockState blockState) {
    final StateManager<Block, BlockState> stateManager = blockState.getBlock().getStateManager();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null)
      return false;
    return comparator.parseAndTest(blockState, property, valueName);
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final StateManager<Block, BlockState> stateManager = blockState.getBlock().getStateManager();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) {
      return TestResult.of(false, Text.translatable(propertyName.isEmpty() ? "enhanced_commands.argument.block_predicate.no_property_this_name_empty" : "enhanced_commands.argument.block_predicate.no_property_this_name", blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), Text.literal(propertyName).styled(TextUtil.STYLE_FOR_EXPECTED)));
    }
    if (property.parse(valueName).isEmpty()) {
      return TestResult.of(false, Text.translatable(valueName.isEmpty() ? "enhanced_commands.argument.block_predicate.property_value_not_parsed_empty" : "enhanced_commands.argument.block_predicate.property_value_not_parsed", Text.literal(propertyName).styled(TextUtil.STYLE_FOR_TARGET), Text.literal(valueName).styled(TextUtil.STYLE_FOR_ACTUAL)));
    }
    final boolean test = comparator.parseAndTest(blockState, property, valueName);
    if (test) {
      return TestResult.of(true, Text.translatable("enhanced_commands.argument.block_predicate.property_test_pass", TextUtil.literal(ValueNamePropertyPredicate.this).styled(TextUtil.STYLE_FOR_EXPECTED)));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.argument.block_predicate.property_not_this_value", TextUtil.wrapVector(blockPos), propertyAndValue(blockState, property).styled(TextUtil.STYLE_FOR_ACTUAL), TextUtil.literal(ValueNamePropertyPredicate.this).styled(TextUtil.STYLE_FOR_EXPECTED)));
    }
  }

  @NotNull
  private static <T extends Comparable<T>> MutableText propertyAndValue(BlockState blockState, Property<T> property) {
    return Text.literal(property.getName() + "=" + property.name(blockState.get(property)));
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putString("comparator", comparator.asString());
    nbtCompound.putString("value", valueName);
  }
}
