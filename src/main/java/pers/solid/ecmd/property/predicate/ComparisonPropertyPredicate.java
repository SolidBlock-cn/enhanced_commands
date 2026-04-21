package pers.solid.ecmd.property.predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.codec.CodecUtil;

public record ComparisonPropertyPredicate<T extends Comparable<T>>(Property<T> property, Comparator comparator, T value) implements PropertyPredicate<T> {
  public static MapCodec<ComparisonPropertyPredicate<?>> getCodec(Block block) {
    return CodecUtil.propertyForBlock(block.getStateDefinition()).dispatchMap("property", ComparisonPropertyPredicate::property, ComparisonPropertyPredicate::getCodecByProperty);
  }

  private static <T extends Comparable<T>> MapCodec<ComparisonPropertyPredicate<T>> getCodecByProperty(Property<T> property) {
    return RecordCodecBuilder.mapCodec(i -> i.apply2((comparator, value) -> new ComparisonPropertyPredicate<>(property, comparator, value),
        Comparator.FIELD_CODEC.forGetter(ComparisonPropertyPredicate::comparator),
        property.codec().fieldOf("value").forGetter(ComparisonPropertyPredicate::value)));
  }

  @Override
  public String expressAsString() {
    return property.getName() + comparator.getSerializedName() + property.getName(value);
  }

  @Override
  public Type getType() {
    return Type.COMPARISON;
  }

  @Override
  public boolean test(BlockState blockState) {
    return blockState.hasProperty(property) && comparator.test(blockState.getValue(property), value);
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    if (!blockState.hasProperty(property)) {
      final MutableComponent stateText = blockState.getBlock().getName().withStyle(Styles.TARGET);
      final String propertyName = property.getName();
      final MutableComponent propertyText = Component.literal(propertyName).withStyle(Styles.EXPECTED);
      return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.no_property", stateText, propertyText));
    }
    final boolean test = comparator.test(blockState.getValue(property), value);
    final MutableComponent posText = TextUtil.wrapVector(blockPos);
    final MutableComponent expectedText = TextUtil.literal(this).withStyle(Styles.EXPECTED);
    final MutableComponent actualText = PropertyPredicate.propertyAndValue(blockState, property).withStyle(Styles.ACTUAL);
    if (test) {
      return TestResult.of(true, Component.translatable("enhanced_commands.property_predicate.pass", posText, actualText, expectedText));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.fail", posText, actualText, expectedText));
    }
  }
}
