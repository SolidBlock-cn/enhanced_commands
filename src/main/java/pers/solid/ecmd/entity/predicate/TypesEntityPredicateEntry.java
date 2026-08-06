package pers.solid.ecmd.entity.predicate;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

import java.util.List;
import java.util.stream.Collectors;

public record TypesEntityPredicateEntry(List<Either<EntityType<?>, TagKey<EntityType<?>>>> types, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate, DoesNotRequireValidation {
  public static final MapCodec<TypesEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.either(BuiltInRegistries.ENTITY_TYPE.byNameCodec(), TagKey.hashedCodec(Registries.ENTITY_TYPE)).listOf().fieldOf("types").forGetter(TypesEntityPredicateEntry::types),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(TypesEntityPredicateEntry::inverted)
  ).apply(i, TypesEntityPredicateEntry::new));

  @Override
  public boolean test(Entity entity) {
    return Iterables.any(types, either -> either.map(type -> type.equals(entity.getType()), tag -> entity.getType().is(tag))) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final boolean anyMatch = types.stream().anyMatch(either -> either.map(type -> type.equals(entity.getType()), tag -> entity.getType().is(tag)));
    final MutableComponent actualText = TextUtil.styled(entity.getType().getDescription(), Styles.ACTUAL);
    final MutableComponent expectedText = ComponentUtils.formatList(types, ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR, either -> either.map(type -> TextUtil.styled(type.getDescription(), Styles.EXPECTED), tag -> Component.literal("#" + tag.location()).withStyle(Styles.EXPECTED)));
    if (inverted) {
      if (anyMatch) {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.type.equal_multiple.fail_inverted", displayName, actualText, expectedText));
      } else {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.type.equal_multiple.pass_inverted", displayName, actualText, expectedText));
      }
    } else {
      if (anyMatch) {
        return TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.type.equal_multiple.pass", displayName, actualText, expectedText));
      } else {
        return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.type.equal_multiple.fail", displayName, actualText, expectedText));
      }
    }
  }

  @Override
  public EntityPredicateType<TypesEntityPredicateEntry> getType() {
    return EntityPredicateTypes.TYPES;
  }

  @Override
  public String toOptionEntry() {
    return "type=" + (inverted ? "!" : "") + types.stream().map(either -> either.map(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), tag -> "#" + tag.location())).collect(Collectors.joining("|"));
  }
}
