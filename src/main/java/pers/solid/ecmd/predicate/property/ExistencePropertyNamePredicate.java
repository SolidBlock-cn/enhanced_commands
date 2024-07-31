package pers.solid.ecmd.predicate.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

public record ExistencePropertyNamePredicate(String propertyName, boolean exists) implements PropertyNamePredicate {
  public static final MapCodec<ExistencePropertyNamePredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(ExistencePropertyNamePredicate::new, Codec.STRING.fieldOf("property").forGetter(ExistencePropertyNamePredicate::propertyName), Codec.BOOL.optionalFieldOf("exists", false).forGetter(ExistencePropertyNamePredicate::exists)));

  @Override
  public @NotNull String asString() {
    return propertyName + (exists ? "=*" : "!=*");
  }

  @Override
  public boolean test(BlockState blockState) {
    return (blockState.getBlock().getStateManager().getProperty(propertyName) != null) == exists;
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final boolean actualExists = blockState.getBlock().getStateManager().getProperty(propertyName) != null;
    final boolean successes = actualExists == exists;
    final MutableText blockText = blockState.getBlock().getName().styled(Styles.TARGET);
    final MutableText propertyNameText = Text.literal(propertyName).styled(Styles.TARGET);
    if (successes) {
      if (actualExists) {
        return TestResult.of(true, Text.translatable("enhanced_commands.property_predicate.name_pass_exists", blockText, propertyNameText));
      } else {
        return TestResult.of(true, Text.translatable("enhanced_commands.property_predicate.name_pass_absent", blockText, propertyNameText));
      }
    } else if (actualExists) {
      return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.name_fail_exists", blockText, propertyNameText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.property_predicate.name_fail_absent", blockText, propertyNameText));
    }
  }

  @Override
  public @NotNull Type getType() {
    return Type.EXISTENCE;
  }
}
