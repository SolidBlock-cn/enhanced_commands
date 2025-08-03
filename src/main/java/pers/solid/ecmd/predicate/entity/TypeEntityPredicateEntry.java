package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Objects;

public record TypeEntityPredicateEntry(EntityType<?> entityType, boolean inverted) implements EntityPredicateEntry, SpecialEntityPredicate, StaticEntityPredicate {
  public static final MapCodec<TypeEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Registries.ENTITY_TYPE.getCodec().fieldOf("entity_type").forGetter(TypeEntityPredicateEntry::entityType),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(TypeEntityPredicateEntry::inverted)
  ).apply(i, TypeEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    return Objects.equals(entityType, entity.getType()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    final EntityType<?> actualType = entity.getType();
    final boolean equals = Objects.equals(actualType, entityType);
    final MutableText actualText = TextUtil.styled(actualType.getName(), Styles.ACTUAL);
    if (equals) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.type.equal", displayName, actualText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.type.not_equal", displayName, actualText, TextUtil.styled(entityType.getName(), Styles.EXPECTED)));
    }
  }

  @Override
  public @NotNull EntityPredicateType<TypeEntityPredicateEntry> getType() {
    return EntityPredicateTypes.TYPE;
  }

  @Override
  public String toOptionEntry() {
    return "type=" + (inverted ? "!" : "") + Registries.ENTITY_TYPE.getId(entityType);
  }
}
