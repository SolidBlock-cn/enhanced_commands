package pers.solid.ecmd.property.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

public record ExistencePropertyNamePredicate(String propertyName, boolean exists) implements PropertyNamePredicate {
  public static final MapCodec<ExistencePropertyNamePredicate> CODEC = RecordCodecBuilder.mapCodec(i -> i.apply2(ExistencePropertyNamePredicate::new, Codec.STRING.fieldOf("property").forGetter(ExistencePropertyNamePredicate::propertyName), Codec.BOOL.optionalFieldOf("exists", false).forGetter(ExistencePropertyNamePredicate::exists)));

  @Override
  public String expressAsString() {
    return propertyName + (exists ? "=*" : "!=*");
  }

  @Override
  public boolean test(BlockState blockState) {
    return (blockState.getBlock().getStateDefinition().getProperty(propertyName) != null) == exists;
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final boolean actualExists = blockState.getBlock().getStateDefinition().getProperty(propertyName) != null;
    final boolean successes = actualExists == exists;
    final MutableComponent blockText = blockState.getBlock().getName().withStyle(Styles.TARGET);
    final MutableComponent propertyNameText = Component.literal(propertyName).withStyle(Styles.TARGET);
    if (successes) {
      if (actualExists) {
        return TestResult.of(true, Component.translatable("enhanced_commands.property_predicate.name_pass_exists", blockText, propertyNameText));
      } else {
        return TestResult.of(true, Component.translatable("enhanced_commands.property_predicate.name_pass_absent", blockText, propertyNameText));
      }
    } else if (actualExists) {
      return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.name_fail_exists", blockText, propertyNameText));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.name_fail_absent", blockText, propertyNameText));
    }
  }

  @Override
  public Type getType() {
    return Type.EXISTENCE;
  }
}
