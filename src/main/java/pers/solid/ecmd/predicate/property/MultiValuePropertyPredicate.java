package pers.solid.ecmd.predicate.property;

import com.google.common.collect.Collections2;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;
import java.util.stream.Collectors;

public record MultiValuePropertyPredicate<T extends Comparable<T>>(Property<T> property, Collection<T> values, boolean inverted) implements PropertyPredicate<T> {
  @Override
  public boolean test(BlockState blockState) {
    return blockState.contains(property) && values.contains(blockState.get(property)) != inverted;
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final String propertyName = property.getName();
    if (!blockState.contains(property)) {
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
    final Text expected = Texts.join(values, Texts.DEFAULT_SEPARATOR_TEXT, value -> Text.literal(property.name(value)).styled(TextUtil.STYLE_FOR_EXPECTED));
    final T actualValue = blockState.get(property);
    if (values.contains(actualValue)) {
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
    return property.getName() + (inverted ? "!=" : "=") + values.stream().map(property::name).collect(Collectors.joining("|"));
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", property.getName());
    nbtCompound.putString("comparator", inverted ? Comparator.NE.asString() : Comparator.EQ.asString());
    final NbtList nbtList = new NbtList();
    nbtList.addAll(Collections2.transform(values, input -> NbtString.of(property.name(input))));
    nbtCompound.put("value", nbtList);
  }
}
