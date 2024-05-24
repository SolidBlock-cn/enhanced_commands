package pers.solid.ecmd.predicate.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

public record ComparisonPropertyPredicate<T extends Comparable<T>>(Property<T> property, Comparator comparator, T value) implements PropertyPredicate<T> {


  @Override
  public @NotNull String asString() {
    return property.getName() + comparator.asString() + property.name(value);
  }

  @Override
  public @NotNull Type getType() {
    return Type.COMPARISON;
  }

  @Override
  public boolean test(BlockState blockState) {
    return blockState.contains(property) && comparator.test(blockState.get(property), value);
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    if (!blockState.contains(property)) {
      final MutableText stateText = blockState.getBlock().getName().styled(Styles.TARGET);
      final String propertyName = property.getName();
      final MutableText propertyText = Text.literal(propertyName).styled(Styles.EXPECTED);
      return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.no_property", stateText, propertyText));
    }
    final boolean test = comparator.test(blockState.get(property), value);
    final MutableText posText = TextUtil.wrapVector(blockPos);
    final MutableText expectedText = TextUtil.literal(this).styled(Styles.EXPECTED);
    final MutableText actualText = PropertyPredicate.propertyAndValue(blockState, property).styled(Styles.ACTUAL);
    if (test) {
      return TestResult.of(true, Text.translatable("enhanced_commands.property_predicate.pass", posText, actualText, expectedText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.fail", posText, actualText, expectedText));
    }
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", property.getName());
    nbtCompound.putString("comparator", comparator.asString());
    nbtCompound.putString("probability", property.name(value));
  }

  public static final Codec<ComparisonPropertyPredicate<?>> CODEC = PropertyCodec.INSTANCE.dispatch("property", ComparisonPropertyPredicate::property, ComparisonPropertyPredicate::getCodecByProperty);

  private static <T extends Comparable<T>> Codec<ComparisonPropertyPredicate<T>> getCodecByProperty(Property<T> property) {
    return RecordCodecBuilder.create(i -> i.apply2((comparator, value) -> new ComparisonPropertyPredicate<>(property, comparator, value),
        Comparator.FIELD_CODEC.forGetter(ComparisonPropertyPredicate::comparator),
        property.getCodec().fieldOf("probability").forGetter(ComparisonPropertyPredicate::value)));
  }
}
