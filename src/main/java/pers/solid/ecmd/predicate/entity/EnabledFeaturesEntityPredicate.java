package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TestResult;

public record EnabledFeaturesEntityPredicate(@NotNull FeatureSet featureSet) implements SpecialEntityPredicate {
  @Override
  public boolean test(@NotNull Entity entity) {
    return entity.getType().isEnabled(featureSet);
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    if (test(entity)) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.enabled_feature.true", displayName));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.enabled_feature.false", displayName));
    }
  }
}
