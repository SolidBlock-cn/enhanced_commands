package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record TypeTagEntityPredicateEntry(TagKey<EntityType<?>> tag, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<TypeTagEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      TagKey.hashedCodec(Registries.ENTITY_TYPE).fieldOf("tag").forGetter(TypeTagEntityPredicateEntry::tag),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(TypeTagEntityPredicateEntry::inverted)
  ).apply(i, TypeTagEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    return entity.getType().is(tag) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final EntityType<?> type = entity.getType();
    final boolean isInTag = type.is(tag);
    if (inverted) {
      if (isInTag) {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.type.in_tag.fail_inverted", displayName, TextUtil.styled(type.getDescription(), Styles.ACTUAL), TextUtil.literal(tag.location()).withStyle(Styles.EXPECTED)));
      } else {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.type.in_tag.pass_inverted", displayName, TextUtil.styled(type.getDescription(), Styles.ACTUAL), TextUtil.literal(tag.location()).withStyle(Styles.EXPECTED)));
      }
    } else {
      if (isInTag) {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.type.in_tag.pass", displayName, TextUtil.styled(type.getDescription(), Styles.ACTUAL), TextUtil.literal(tag.location()).withStyle(Styles.EXPECTED)));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.type.in_tag.fail", displayName, TextUtil.styled(type.getDescription(), Styles.ACTUAL), TextUtil.literal(tag.location()).withStyle(Styles.EXPECTED)));
      }
    }
  }

  @Override
  public EntityPredicateType<TypeTagEntityPredicateEntry> getType() {
    return EntityPredicateTypes.TYPE_TAG;
  }

  @Override
  public String toOptionEntry() {
    return "type=" + (inverted ? "!" : "") + "#" + tag.location();
  }
}
