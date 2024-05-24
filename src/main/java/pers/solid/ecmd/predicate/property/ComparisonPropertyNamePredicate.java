package pers.solid.ecmd.predicate.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

public record ComparisonPropertyNamePredicate(String propertyName, Comparator comparator, String valueName) implements PropertyNamePredicate {
  public static final Codec<ComparisonPropertyNamePredicate> CODEC = RecordCodecBuilder.create(i -> i.apply3(ComparisonPropertyNamePredicate::new, Codec.STRING.fieldOf("property_name").forGetter(ComparisonPropertyNamePredicate::propertyName), Comparator.FIELD_CODEC.forGetter(ComparisonPropertyNamePredicate::comparator), Codec.STRING.fieldOf("value_name").forGetter(ComparisonPropertyNamePredicate::valueName)));

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
      final MutableText stateText = blockState.getBlock().getName().styled(Styles.TARGET);
      final MutableText propertyText = Text.literal(propertyName).styled(Styles.EXPECTED);
      if (propertyName.isEmpty()) {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.no_property_this_name_empty", stateText));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.no_property_this_name", stateText, propertyText));
      }
    }
    final boolean test = comparator.parseAndTest(blockState, property, valueName);
    final MutableText posText = TextUtil.wrapVector(blockPos);
    final MutableText expectedText = TextUtil.literal(this).styled(Styles.EXPECTED);
    final MutableText actualText = PropertyPredicate.propertyAndValue(blockState, property).styled(Styles.ACTUAL);
    if (test) {
      return TestResult.of(true, Text.translatable("enhanced_commands.property_predicate.pass", posText, actualText, expectedText));
    } else if (property.parse(valueName).isEmpty()) {
      final MutableText propertyText = Text.literal(propertyName).styled(Styles.TARGET);
      final MutableText actualValueText = Text.literal(valueName).styled(Styles.ACTUAL);
      if (valueName.isEmpty()) {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.value_not_parsed_empty", propertyText));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.value_not_parsed", propertyText, actualValueText));
      }
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.fail", posText, actualText, expectedText));
    }
  }

  @Override
  public @NotNull Type getType() {
    return Type.COMPARISON;
  }

  @Override
  public void writeNbt(@NotNull NbtCompound nbtCompound) {
    nbtCompound.putString("property", propertyName);
    nbtCompound.putString("comparator", comparator.asString());
    nbtCompound.putString("probability", valueName);
  }
}
