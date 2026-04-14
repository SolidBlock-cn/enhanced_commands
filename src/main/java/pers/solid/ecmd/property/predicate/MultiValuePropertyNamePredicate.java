package pers.solid.ecmd.property.predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.StateUtil;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;

public record MultiValuePropertyNamePredicate(String propertyName, List<String> valueNames, boolean inverted) implements PropertyNamePredicate {
  public static final MapCodec<MultiValuePropertyNamePredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(MultiValuePropertyNamePredicate::new,
      Codec.STRING.fieldOf("property").forGetter(MultiValuePropertyNamePredicate::propertyName),
      Codec.STRING.listOf().optionalFieldOf("values", ImmutableList.of()).forGetter(MultiValuePropertyNamePredicate::valueNames),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(MultiValuePropertyNamePredicate::inverted)));

  @Override
  public boolean test(BlockState blockState) {
    final StateDefinition<Block, BlockState> stateManager = blockState.getBlock().getStateDefinition();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) return false;
    final String actualValue = StateUtil.namePropertyValue(blockState, property);
    return Iterables.any(valueNames, value -> value.equals(actualValue)) != inverted;
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final StateDefinition<Block, BlockState> stateManager = blockState.getBlock().getStateDefinition();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) {
      final MutableComponent nameText = blockState.getBlock().getName().withStyle(Styles.TARGET);
      final MutableComponent propertyNameText = Component.literal(propertyName).withStyle(Styles.EXPECTED);
      if (propertyName.isEmpty()) {
        return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.no_property_this_name_empty", nameText));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.no_property_this_name", nameText, propertyNameText));
      }
    }
    final Component pos = TextUtil.wrapVector(blockPos);
    final Component actual = PropertyPredicate.propertyAndValue(blockState, property).withStyle(Styles.ACTUAL);
    final Component expected = ComponentUtils.formatList(valueNames, ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR, string -> Component.literal(string).withStyle(Styles.EXPECTED));
    final String actualValue = StateUtil.namePropertyValue(blockState, property);
    if (Iterables.any(valueNames, value -> value.equals(actualValue))) {
      if (inverted) {
        return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.value_match_inverted", pos, actual, expected));
      } else {
        return TestResult.of(true, Component.translatable("enhanced_commands.property_predicate.value_match", pos, actual, expected));
      }
    } else {
      if (inverted) {
        return TestResult.of(true, Component.translatable("enhanced_commands.property_predicate.value_mismatch_inverted", pos, actual, expected));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.value_mismatch", pos, actual, expected));
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
