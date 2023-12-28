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

public record ComparisonPropertyNamePredicate(String propertyName, Comparator comparator, String valueName) implements PropertyNamePredicate {
  @Override
  public @NotNull String asString() {
    return propertyName + comparator.asString() + valueName;
  }

  @Override
  public boolean test(BlockState blockState) {
    final StateManager<Block, BlockState> stateManager = blockState.getBlock().getStateManager();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) return false;
    return comparator.parseAndTest(blockState, property, valueName);
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final StateManager<Block, BlockState> stateManager = blockState.getBlock().getStateManager();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) {
      final MutableText stateText = blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET);
      final MutableText propertyText = Text.literal(propertyName).styled(TextUtil.STYLE_FOR_EXPECTED);
      if (propertyName.isEmpty()) {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.no_property_this_name_empty", stateText));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.no_property_this_name", stateText, propertyText));
      }
    }
    if (property.parse(valueName).isEmpty()) {
      final MutableText propertyText = Text.literal(propertyName).styled(TextUtil.STYLE_FOR_TARGET);
      final MutableText actualValueText = Text.literal(valueName).styled(TextUtil.STYLE_FOR_ACTUAL);
      if (valueName.isEmpty()) {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.value_not_parsed_empty", propertyText));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.value_not_parsed", propertyText, actualValueText));
      }
    }
    final boolean test = comparator.parseAndTest(blockState, property, valueName);
    final MutableText posText = TextUtil.wrapVector(blockPos);
    final MutableText expectedText = TextUtil.literal(this).styled(TextUtil.STYLE_FOR_EXPECTED);
    final MutableText actualText = PropertyPredicate.propertyAndValue(blockState, property).styled(TextUtil.STYLE_FOR_ACTUAL);
    if (test) {
      return TestResult.of(true, Text.translatable("enhanced_commands.property_predicate.pass", posText, actualText, expectedText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.fail", posText, actualText, expectedText));
    }
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putString("comparator", comparator.asString());
    nbtCompound.putString("value", valueName);
  }
}
