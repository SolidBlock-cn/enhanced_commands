package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Objects;

public record TypeEntityPredicateEntry(EntityType<?> expectedType, boolean hasNegation) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final EntityType<?> actualType = entity.getType();
    final boolean equals = Objects.equals(actualType, expectedType);
    final MutableText actualText = TextUtil.styled(actualType.getName(), TextUtil.STYLE_FOR_ACTUAL);
    if (equals) {
      return TestResult.of(!hasNegation, Text.translatable("enhanced_commands.argument.entity_predicate.type.equal", displayName, actualText));
    } else {
      return TestResult.of(hasNegation, Text.translatable("enhanced_commands.argument.entity_predicate.type.not_equal", displayName, actualText, TextUtil.styled(expectedType.getName(), TextUtil.STYLE_FOR_EXPECTED)));
    }
  }

  @Override
  public String toOptionEntry() {
    return "type=" + (hasNegation ? "!" : "") + Registries.ENTITY_TYPE.getId(expectedType);
  }
}
