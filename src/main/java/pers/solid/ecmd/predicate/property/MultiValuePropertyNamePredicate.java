package pers.solid.ecmd.predicate.property;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;

public record MultiValuePropertyNamePredicate(String propertyName, List<String> valueNames, boolean inverted) implements PropertyNamePredicate {
  public static final Codec<MultiValuePropertyNamePredicate> CODEC = RecordCodecBuilder.create(i -> i.apply3(MultiValuePropertyNamePredicate::new,
      Codec.STRING.fieldOf("property").forGetter(MultiValuePropertyNamePredicate::propertyName),
      Codec.STRING.listOf().optionalFieldOf("values", ImmutableList.of()).forGetter(MultiValuePropertyNamePredicate::valueNames),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(MultiValuePropertyNamePredicate::inverted)));

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
      final MutableText nameText = blockState.getBlock().getName().styled(Styles.TARGET);
      final MutableText propertyNameText = Text.literal(propertyName).styled(Styles.EXPECTED);
      if (propertyName.isEmpty()) {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.no_property_this_name_empty", nameText));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.no_property_this_name", nameText, propertyNameText));
      }
    }
    final Text pos = TextUtil.wrapVector(blockPos);
    final Text actual = PropertyPredicate.propertyAndValue(blockState, property).styled(Styles.ACTUAL);
    final Text expected = Texts.join(valueNames, Texts.DEFAULT_SEPARATOR_TEXT, string -> Text.literal(string).styled(Styles.EXPECTED));
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
  public @NotNull Type getType() {
    return Type.MULTI_VALUE;
  }

  @Override
  public @NotNull String asString() {
    return propertyName + (inverted ? "!=" : "=") + StringUtils.join(valueNames, "|");
  }
}
