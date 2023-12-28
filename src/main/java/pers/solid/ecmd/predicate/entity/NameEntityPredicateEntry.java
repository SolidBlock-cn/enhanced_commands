package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record NameEntityPredicateEntry(String expectedName, boolean hasNegation) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final String actualName = entity.getName().getString();
    final MutableText actualNameText = Text.literal(actualName).styled(TextUtil.STYLE_FOR_ACTUAL);
    if (actualName.equals(expectedName)) {
      return TestResult.of(!hasNegation, Text.translatable("enhanced_commands.entity_predicate.name.equal", displayName, actualNameText));
    } else {
      return TestResult.of(hasNegation, Text.translatable("enhanced_commands.entity_predicate.name.not_equal", displayName, actualNameText, Text.literal(expectedName).styled(TextUtil.STYLE_FOR_EXPECTED)));
    }
  }

  @Override
  public String toOptionEntry() {
    return "name=" + expectedName;
  }
}
