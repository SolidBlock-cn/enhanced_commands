package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Set;

public record TagEntityPredicateEntry(@NotNull String tagName, boolean hasNegation) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final Set<String> commandTags = entity.getCommandTags();
    if (tagName.isEmpty()) {
      // 检测实体是否没有任何标签
      if (commandTags.isEmpty()) {
        return TestResult.of(!hasNegation, Text.translatable("enhanced_commands.entity_predicate.tag.empty", displayName));
      } else {
        return TestResult.of(hasNegation, Text.translatable("enhanced_commands.entity_predicate.tag.any", displayName));
      }
    } else {
      // 检测实体是否拥有指定的标签
      final MutableText tagNameText = Text.literal(tagName).styled(TextUtil.STYLE_FOR_EXPECTED);
      if (commandTags.contains(tagName)) {
        return TestResult.of(!hasNegation, Text.translatable("enhanced_commands.entity_predicate.tag.contains", displayName, tagNameText));
      } else {
        return TestResult.of(hasNegation, Text.translatable("enhanced_commands.entity_predicate.tag.not_contains", displayName, tagNameText));
      }
    }
  }

  @Override
  public String toOptionEntry() {
    return "tag=" + (hasNegation ? "!" : "") + tagName;
  }
}
