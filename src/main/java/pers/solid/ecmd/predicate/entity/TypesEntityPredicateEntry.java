package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.stream.Collectors;

public record TypesEntityPredicateEntry(List<Either<EntityType<?>, TagKey<EntityType<?>>>> types, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<TypesEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.either(Registries.ENTITY_TYPE.getCodec(), TagKey.codec(RegistryKeys.ENTITY_TYPE)).listOf().fieldOf("types").forGetter(TypesEntityPredicateEntry::types),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(TypesEntityPredicateEntry::inverted)
  ).apply(i, TypesEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    return Iterables.any(types, either -> either.map(type -> type.equals(entity.getType()), tag -> entity.getType().isIn(tag))) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final boolean anyMatch = types.stream().anyMatch(either -> either.map(type -> type.equals(entity.getType()), tag -> entity.getType().isIn(tag)));
    final MutableText actualText = TextUtil.styled(entity.getType().getName(), Styles.ACTUAL);
    final MutableText expectedText = Texts.join(types, Texts.DEFAULT_SEPARATOR_TEXT, either -> either.map(type -> TextUtil.styled(type.getName(), Styles.EXPECTED), tag -> Text.literal("#" + tag.id()).styled(Styles.EXPECTED)));
    if (inverted) {
      if (anyMatch) {
        return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.type.equal_multiple.fail_inverted", displayName, actualText, expectedText));
      } else {
        return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.type.equal_multiple.pass_inverted", displayName, actualText, expectedText));
      }
    } else {
      if (anyMatch) {
        return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.type.equal_multiple.pass", displayName, actualText, expectedText));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.type.equal_multiple.fail", displayName, actualText, expectedText));
      }
    }
  }

  @Override
  public @NotNull EntityPredicateType<TypesEntityPredicateEntry> getType() {
    return EntityPredicateTypes.TYPES;
  }

  @Override
  public String toOptionEntry() {
    return "type=" + (inverted ? "!" : "") + types.stream().map(either -> either.map(type -> Registries.ENTITY_TYPE.getId(type).toString(), tag -> "#" + tag.id())).collect(Collectors.joining("|"));
  }
}
