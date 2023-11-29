package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.ApiStatus;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

public interface EntityPredicateEntry {
  @ApiStatus.NonExtendable
  default TestResult testAndDescribe(Entity entity) throws CommandSyntaxException {
    return testAndDescribe(entity, TextUtil.styled(entity.getDisplayName(), TextUtil.STYLE_FOR_TARGET));
  }

  TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException;

  String toOptionEntry();
}
