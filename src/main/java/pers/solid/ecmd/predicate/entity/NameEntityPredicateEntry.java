package pers.solid.ecmd.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;

public record NameEntityPredicateEntry(String name, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<NameEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.STRING.fieldOf("name").forGetter(NameEntityPredicateEntry::name),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(NameEntityPredicateEntry::inverted)
  ).apply(i, NameEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    final String actualName = entity.getName().getString();
    return actualName.equals(name) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    final String actualName = entity.getName().getString();
    final MutableText actualNameText = Text.literal(actualName).styled(Styles.ACTUAL);
    if (actualName.equals(name)) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.name.equal", displayName, actualNameText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.empty", displayName, actualNameText, Text.literal(name).styled(Styles.EXPECTED)));
    }
  }

  @Override
  public @NotNull EntityPredicateType<NameEntityPredicateEntry> getType() {
    return EntityPredicateTypes.NAME;
  }

  @Override
  public String toOptionEntry() {
    return "propertyName=" + name;
  }
}
