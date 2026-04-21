package pers.solid.ecmd.property.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record ComparisonPropertyNamePredicate(String propertyName, Comparator comparator, String valueName) implements PropertyNamePredicate {
  public static final MapCodec<ComparisonPropertyNamePredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply3(ComparisonPropertyNamePredicate::new,
      Codec.STRING.fieldOf("property").forGetter(ComparisonPropertyNamePredicate::propertyName),
      Comparator.FIELD_CODEC.forGetter(ComparisonPropertyNamePredicate::comparator),
      Codec.STRING.fieldOf("value").forGetter(ComparisonPropertyNamePredicate::valueName)));

  @Override
  public String expressAsString() {
    return propertyName + comparator.getSerializedName() + valueName;
  }

  @Override
  public boolean test(BlockState blockState) {
    final StateDefinition<Block, BlockState> stateManager = blockState.getBlock().getStateDefinition();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) return false;
    return comparator.parseAndTest(blockState, property, valueName);
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final StateDefinition<Block, BlockState> stateManager = blockState.getBlock().getStateDefinition();
    final Property<?> property = stateManager.getProperty(propertyName);
    if (property == null) {
      final MutableComponent stateText = blockState.getBlock().getName().withStyle(Styles.TARGET);
      final MutableComponent propertyText = Component.literal(propertyName).withStyle(Styles.EXPECTED);
      if (propertyName.isEmpty()) {
        return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.no_property_this_name_empty", stateText));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.no_property_this_name", stateText, propertyText));
      }
    }
    final boolean test = comparator.parseAndTest(blockState, property, valueName);
    final MutableComponent posText = TextUtil.wrapVector(blockPos);
    final MutableComponent expectedText = TextUtil.literal(this).withStyle(Styles.EXPECTED);
    final MutableComponent actualText = PropertyPredicate.propertyAndValue(blockState, property).withStyle(Styles.ACTUAL);
    if (test) {
      return TestResult.of(true, Component.translatable("enhanced_commands.property_predicate.pass", posText, actualText, expectedText));
    } else if (property.getValue(valueName).isEmpty()) {
      final MutableComponent propertyText = Component.literal(propertyName).withStyle(Styles.TARGET);
      final MutableComponent actualValueText = Component.literal(valueName).withStyle(Styles.ACTUAL);
      if (valueName.isEmpty()) {
        return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.value_not_parsed_empty", propertyText));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.value_not_parsed", propertyText, actualValueText));
      }
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.fail", posText, actualText, expectedText));
    }
  }

  @Override
  public Type getType() {
    return Type.COMPARISON;
  }
}
