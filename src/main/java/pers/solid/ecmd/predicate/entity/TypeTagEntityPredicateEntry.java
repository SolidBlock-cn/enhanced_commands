package pers.solid.ecmd.predicate.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record TypeTagEntityPredicateEntry(TagKey<EntityType<?>> tagKey, boolean hasNegation) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) {
    final EntityType<?> type = entity.getType();
    final boolean isInTag = type.isIn(tagKey);
    if (isInTag) {
      return TestResult.of(!hasNegation, Text.translatable("enhanced_commands.argument.entity_predicate.type.in_tag", displayName, TextUtil.styled(type.getName(), TextUtil.STYLE_FOR_ACTUAL), TextUtil.literal(tagKey.id()).styled(TextUtil.STYLE_FOR_EXPECTED)));
    } else {
      return TestResult.of(hasNegation, Text.translatable("enhanced_commands.argument.entity_predicate.type.not_in_tag", displayName, TextUtil.styled(type.getName(), TextUtil.STYLE_FOR_ACTUAL), TextUtil.literal(tagKey.id()).styled(TextUtil.STYLE_FOR_EXPECTED)));
    }
  }

  @Override
  public String toOptionEntry() {
    return "type=" + (hasNegation ? "!" : "") + "#" + tagKey.id().toString();
  }
}
