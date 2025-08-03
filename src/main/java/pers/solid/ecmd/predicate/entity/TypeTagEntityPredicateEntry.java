package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record TypeTagEntityPredicateEntry(TagKey<EntityType<?>> tag, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<TypeTagEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      TagKey.codec(RegistryKeys.ENTITY_TYPE).fieldOf("tag").forGetter(TypeTagEntityPredicateEntry::tag),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(TypeTagEntityPredicateEntry::inverted)
  ).apply(i, TypeTagEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity.getType().isIn(tag) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    final EntityType<?> type = entity.getType();
    final boolean isInTag = type.isIn(tag);
    if (isInTag) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.type.in_tag", displayName, TextUtil.styled(type.getName(), Styles.ACTUAL), TextUtil.literal(tag.id()).styled(Styles.EXPECTED)));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.type.not_in_tag", displayName, TextUtil.styled(type.getName(), Styles.ACTUAL), TextUtil.literal(tag.id()).styled(Styles.EXPECTED)));
    }
  }

  @Override
  public @NotNull EntityPredicateType<TypeTagEntityPredicateEntry> getType() {
    return EntityPredicateTypes.TYPE_TAG;
  }

  @Override
  public String toOptionEntry() {
    return "type=" + (inverted ? "!" : "") + "#" + tag.id().toString();
  }
}
