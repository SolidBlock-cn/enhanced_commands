package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

public record NameEntityPredicateEntry(String expectedName, boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
    final String actualName = entity.getName().getString();
    return actualName.equals(expectedName) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final String actualName = entity.getName().getString();
    final MutableText actualNameText = Text.literal(actualName).styled(Styles.ACTUAL);
    if (actualName.equals(expectedName)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.name.equal", displayName, actualNameText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.empty", displayName, actualNameText, Text.literal(expectedName).styled(Styles.EXPECTED)));
    }
  }

  @Override
  public String toOptionEntry() {
    return "propertyName=" + expectedName;
  }
}
