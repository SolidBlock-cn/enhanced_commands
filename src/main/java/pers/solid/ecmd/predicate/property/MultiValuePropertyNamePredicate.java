package pers.solid.ecmd.predicate.property;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;

public record MultiValuePropertyNamePredicate(String propertyName, Collection<String> valueNames, boolean inverted) implements PropertyNamePredicate {
  @Override
  public boolean test(BlockState blockState) {
    final StateManager<Block, BlockState> stateManager = blockState.getBlock().getStateManager();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) return false;
    final String actualValue = StateUtil.namePropertyValue(blockState, property);
    return Iterables.any(valueNames, value -> value.equals(actualValue)) != inverted;
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final StateManager<Block, BlockState> stateManager = blockState.getBlock().getStateManager();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) {
      final MutableText nameText = blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_TARGET);
      final MutableText propertyNameText = Text.literal(propertyName).styled(TextUtil.STYLE_FOR_EXPECTED);
      if (propertyName.isEmpty()) {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.no_property_this_name_empty", nameText));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.no_property_this_name", nameText, propertyNameText));
      }
    }
    final Text pos = TextUtil.wrapVector(blockPos);
    final Text actual = PropertyPredicate.propertyAndValue(blockState, property).styled(TextUtil.STYLE_FOR_ACTUAL);
    final Text expected = Texts.join(valueNames, Texts.DEFAULT_SEPARATOR_TEXT, string -> Text.literal(string).styled(TextUtil.STYLE_FOR_EXPECTED));
    final String actualValue = StateUtil.namePropertyValue(blockState, property);
    if (Iterables.any(valueNames, value -> value.equals(actualValue))) {
      if (inverted) {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.value_match_inverted", pos, actual, expected));
      } else {
        return TestResult.of(true, Text.translatable("enhanced_commands.property_predicate.value_match", pos, actual, expected));
      }
    } else {
      if (inverted) {
        return TestResult.of(true, Text.translatable("enhanced_commands.property_predicate.value_mismatch_inverted", pos, actual, expected));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.value_mismatch", pos, actual, expected));
      }
    }
  }

  @Override
  public @NotNull String asString() {
    return propertyName + (inverted ? "!=" : "=") + StringUtils.join(valueNames, "|");
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putString("comparator", inverted ? Comparator.NE.asString() : Comparator.EQ.asString());
    final NbtList list = new NbtList();
    list.addAll(Collections2.transform(valueNames, NbtString::of));
    nbtCompound.put("value", list);
  }
}
