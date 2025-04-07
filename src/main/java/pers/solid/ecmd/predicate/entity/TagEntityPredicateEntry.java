package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

import java.util.Set;

public record TagEntityPredicateEntry(@NotNull String tagName, boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(@NotNull Entity entity) {
    if (tagName.isEmpty()) {
      return entity.getCommandTags().isEmpty() != inverted;
    } else {
      return entity.getCommandTags().contains(tagName) != inverted;
    }
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final Set<String> commandTags = entity.getCommandTags();
    if (tagName.isEmpty()) {
      // 检测实体是否没有任何标签
      if (commandTags.isEmpty()) {
        return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.tag.empty", displayName));
      } else {
        return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.tag.any", displayName));
      }
    } else {
      // 检测实体是否拥有指定的标签
      final MutableText tagNameText = Text.literal(tagName).styled(Styles.EXPECTED);
      if (commandTags.contains(tagName)) {
        return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.tag.contains", displayName, tagNameText));
      } else {
        return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.tag.not_contains", displayName, tagNameText));
      }
    }
  }

  @Override
  public @Nullable String toOptionEntry() {
    return "tag=" + (inverted ? "!" : "") + tagName;
  }
}
