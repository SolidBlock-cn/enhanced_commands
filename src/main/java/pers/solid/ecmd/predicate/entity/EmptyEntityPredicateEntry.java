package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.TestResult;

public enum EmptyEntityPredicateEntry implements EntityPredicateEntry {
  INSTANCE;

  @Override
  public boolean test(@NotNull Entity entity) {
    return false;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.empty"));
  }

  @Override
  public String toOptionEntry() {
    return null;
  }
}
