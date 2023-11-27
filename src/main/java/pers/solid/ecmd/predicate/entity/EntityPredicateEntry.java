package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

public interface EntityPredicateEntry {
  @ApiStatus.NonExtendable
  default TestResult testAndDescribe(Entity entity) {
    return testAndDescribe(entity, TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET));
  }

  TestResult testAndDescribe(Entity entity, Text displayName);

  String toOptionEntry();
}
