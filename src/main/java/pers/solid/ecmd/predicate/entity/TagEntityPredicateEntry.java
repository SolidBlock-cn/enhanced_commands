package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

import java.util.Set;

public record TagEntityPredicateEntry(@NotNull String tag, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<TagEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.fieldOf("tag").forGetter(TagEntityPredicateEntry::tag),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(TagEntityPredicateEntry::inverted)
  ).apply(i, TagEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    if (tag.isEmpty()) {
      return entity.getTags().isEmpty() != inverted;
    } else {
      return entity.getTags().contains(tag) != inverted;
    }
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) {
    final Set<String> commandTags = entity.getTags();
    if (tag.isEmpty()) {
      // 检测实体是否没有任何标签
      if (commandTags.isEmpty()) {
        return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.tag.empty", displayName));
      } else {
        return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.tag.any", displayName));
      }
    } else {
      // 检测实体是否拥有指定的标签
      final MutableComponent tagNameText = Component.literal(tag).withStyle(Styles.EXPECTED);
      if (commandTags.contains(tag)) {
        return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.tag.contains", displayName, tagNameText));
      } else {
        return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.tag.not_contains", displayName, tagNameText));
      }
    }
  }

  @Override
  public @NotNull EntityPredicateType<TagEntityPredicateEntry> getType() {
    return EntityPredicateTypes.TAG;
  }

  @Override
  public String toOptionEntry() {
    return "tag=" + (inverted ? "!" : "") + tag;
  }
}
