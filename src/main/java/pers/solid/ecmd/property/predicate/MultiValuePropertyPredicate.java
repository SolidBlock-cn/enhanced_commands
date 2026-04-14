package pers.solid.ecmd.property.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.List;
import java.util.stream.Collectors;

public record MultiValuePropertyPredicate<T extends Comparable<T>>(Property<T> property, List<T> values, boolean inverted) implements PropertyPredicate<T> {
  private static <T extends Comparable<T>> MapCodec<MultiValuePropertyPredicate<T>> getCodecByProperty(Property<T> property) {
    return RecordCodecBuilder.mapCodec(i -> i.apply2(
        (values, inverted) -> new MultiValuePropertyPredicate<>(property, values, inverted),
        property.codec().listOf().optionalFieldOf("values", ImmutableList.of()).forGetter(MultiValuePropertyPredicate::values),
        Codec.BOOL.optionalFieldOf("inverted", false).forGetter(MultiValuePropertyPredicate::inverted)));
  }

  public static MapCodec<MultiValuePropertyPredicate<?>> getCodec(Block block) {
    return CodecUtil.propertyForBlock(block.getStateDefinition()).dispatchMap("property", MultiValuePropertyPredicate::property, MultiValuePropertyPredicate::getCodecByProperty);
  }

  @Override
  public @NotNull Type getType() {
    return Type.MULTI_VALUE;
  }

  @Override
  public boolean test(BlockState blockState) {
    return blockState.hasProperty(property) && values.contains(blockState.getValue(property)) != inverted;
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final String propertyName = property.getName();
    if (!blockState.hasProperty(property)) {
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
    final Component expected = ComponentUtils.formatList(values, ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR, value -> Component.literal(property.getName(value)).withStyle(Styles.EXPECTED));
    final T actualValue = blockState.getValue(property);
    if (values.contains(actualValue)) {
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
  public @NotNull String asString() {
    return property.getName() + (inverted ? "!=" : "=") + values.stream().map(property::getName).collect(Collectors.joining("|"));
  }
}
