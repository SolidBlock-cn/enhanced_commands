package pers.solid.ecmd.predicate.entity;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import pers.solid.ecmd.command.TestResult;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.List;
import java.util.stream.Collectors;

public record TypesEntityPredicateEntry(List<Either<EntityType<?>, TagKey<EntityType<?>>>> values, boolean inverted) implements EntityPredicateEntry {
  @Override
  public boolean test(Entity entity) {
    return Iterables.any(values, either -> either.map(type -> type.equals(entity.getType()), tag -> entity.getType().isIn(tag))) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    final boolean anyMatch = values.stream().anyMatch(either -> either.map(type -> type.equals(entity.getType()), tag -> entity.getType().isIn(tag)));
    final MutableText actualText = TextUtil.styled(entity.getType().getName(), Styles.ACTUAL);
    final MutableText expectedText = Texts.join(values, Texts.DEFAULT_SEPARATOR_TEXT, either -> either.map(type -> TextUtil.styled(type.getName(), Styles.EXPECTED), tag -> Text.literal("#" + tag.id()).styled(Styles.EXPECTED)));
    if (anyMatch) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.type.true_multiple", displayName, actualText, expectedText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.type.false_multiple", displayName, actualText, expectedText));
    }
  }

  @Override
  public String toOptionEntry() {
    return "type=" + (inverted ? "!" : "") + values.stream().map(either -> either.map(type -> Registries.ENTITY_TYPE.getId(type).toString(), tag -> "#" + tag.id())).collect(Collectors.joining("|"));
  }
}
