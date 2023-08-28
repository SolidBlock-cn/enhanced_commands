package pers.solid.ecmd.predicate.property;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
      return new TestResult(false, Text.translatable(propertyName.isEmpty() ? "enhancedCommands.argument.block_predicate.no_property_this_name_empty" : "enhancedCommands.argument.block_predicate.no_property_this_name", blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET), Text.literal(propertyName).styled(TextUtil.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
    }
    if (property.parse(valueName).isEmpty()) {
      return new TestResult(false, Text.translatable(valueName.isEmpty() ? "enhancedCommands.argument.block_predicate.property_value_not_parsed_empty" : "enhancedCommands.argument.block_predicate.property_value_not_parsed", Text.literal(propertyName).styled(TextUtil.STYLE_FOR_TARGET), Text.literal(valueName).styled(TextUtil.STYLE_FOR_ACTUAL)).formatted(Formatting.RED));
    }
    final boolean test = comparator.parseAndTest(blockState, property, valueName);
    if (test) {
      return new TestResult(true, Text.translatable("enhancedCommands.argument.block_predicate.property_test_pass", Text.literal(asString()).styled(TextUtil.STYLE_FOR_EXPECTED)).formatted(Formatting.GREEN));
    } else {
      return new TestResult(false, Text.translatable("enhancedCommands.argument.block_predicate.property_not_this_value", TextUtil.wrapBlockPos(blockPos), propertyAndValue(blockState, property).styled(TextUtil.STYLE_FOR_ACTUAL), Text.literal(asString()).styled(TextUtil.STYLE_FOR_EXPECTED)).formatted(Formatting.RED));
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
