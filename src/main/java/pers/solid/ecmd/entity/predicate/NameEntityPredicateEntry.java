package pers.solid.ecmd.entity.predicate;

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
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) {
    final String actualName = entity.getName().getString();
    final MutableComponent actualNameText = Component.literal(actualName).withStyle(Styles.ACTUAL);
    if (actualName.equals(name)) {
      return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.name.equal", displayName, actualNameText));
    } else {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.empty", displayName, actualNameText, Component.literal(name).withStyle(Styles.EXPECTED)));
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
