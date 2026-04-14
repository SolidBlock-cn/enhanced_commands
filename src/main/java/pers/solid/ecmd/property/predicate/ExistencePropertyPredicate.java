package pers.solid.ecmd.property.predicate;

import com.mojang.serialization.Codec;
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
import pers.solid.ecmd.util.codec.CodecUtil;

public record ExistencePropertyPredicate<T extends Comparable<T>>(Property<T> property, boolean exists) implements PropertyPredicate<T> {
  public static MapCodec<ExistencePropertyPredicate<?>> getCodec(Block block) {
    return RecordCodecBuilder.mapCodec(i -> i.apply2(ExistencePropertyPredicate::new, CodecUtil.propertyForBlock(block.getStateDefinition()).fieldOf("property").forGetter(ExistencePropertyPredicate::property), Codec.BOOL.optionalFieldOf("exists", false).forGetter(ExistencePropertyPredicate::exists)));
  }

  @Override
  public String asString() {
    return property.getName() + (exists ? "=*" : "!=*");
  }

  @Override
  public Type getType() {
    return Type.EXISTENCE;
  }

  @Override
  public boolean test(BlockState blockState) {
    return blockState.hasProperty(property) == exists;
  }

  @Override
  public TestResult testAndDescribe(BlockState blockState, BlockPos blockPos) {
    final String propertyName = property.getName();
    final boolean actualExists = blockState.hasProperty(property);
    final boolean successes = actualExists == exists;
    final MutableComponent blockText = blockState.getBlock().getName().withStyle(Styles.TARGET);
    final MutableComponent propertyNameText = Component.literal(propertyName).withStyle(Styles.TARGET);
    if (successes) {
      if (actualExists) {
        return TestResult.of(true, Component.translatable("enhanced_commands.property_predicate.property_pass_exists", blockText, propertyNameText));
      } else {
        return TestResult.of(true, Component.translatable("enhanced_commands.property_predicate.property_pass_absent", blockText, propertyNameText));
      }
    } else if (actualExists) {
      return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.property_fail_exists", blockText, propertyNameText));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.property_predicate.property_fail_absent", blockText, propertyNameText));
    }
  }
}
