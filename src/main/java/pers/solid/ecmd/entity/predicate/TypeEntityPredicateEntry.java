package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.Objects;

public record TypeEntityPredicateEntry(EntityType<?> entityType, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate, DoesNotRequireValidation {
  public static final MapCodec<TypeEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity_type").forGetter(TypeEntityPredicateEntry::entityType),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(TypeEntityPredicateEntry::inverted)
  ).apply(i, TypeEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    return Objects.equals(entityType, entity.getType()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final EntityType<?> actualType = entity.getType();
    final boolean equals = Objects.equals(actualType, entityType);
    final MutableComponent actualText = TextUtil.styled(actualType.getDescription(), Styles.ACTUAL);
    if (inverted) {
      if (equals) {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.type.equal.fail_inverted", displayName, actualText));
      } else {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.type.equal.pass_inverted", displayName, actualText, TextUtil.styled(entityType.getDescription(), Styles.EXPECTED)));
      }
    } else {
      if (equals) {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.type.equal.pass", displayName, actualText));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.type.equal.fail", displayName, actualText, TextUtil.styled(entityType.getDescription(), Styles.EXPECTED)));
      }
    }
  }

  @Override
  public EntityPredicateType<TypeEntityPredicateEntry> getType() {
    return EntityPredicateTypes.TYPE;
  }

  @Override
  public String toOptionEntry() {
    return "type=" + (inverted ? "!" : "") + DefaultNamespace.MINECRAFT.toSimplerString(BuiltInRegistries.ENTITY_TYPE.getKey(entityType));
  }
}
