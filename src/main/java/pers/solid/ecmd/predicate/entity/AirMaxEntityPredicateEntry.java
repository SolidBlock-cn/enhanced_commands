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
import pers.solid.ecmd.util.TextUtil;

public record AirMaxEntityPredicateEntry(boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<AirMaxEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(AirMaxEntityPredicateEntry::inverted)
  ).apply(i, AirMaxEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    return (entity.getAir() == entity.getMaxAir()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) {
    final int actualAir = entity.getAir();
    final MutableText actualHealthText = TextUtil.literal(actualAir).styled(Styles.ACTUAL);
    final int maxAir = entity.getMaxAir();
    final MutableText maxHealthText = TextUtil.literal(maxAir);
    if (maxAir == actualAir) {
      return TestResult.of(!inverted, Text.translatable("enhanced_commands.entity_predicate.air.is_max", displayName, actualHealthText));
    } else {
      return TestResult.of(inverted, Text.translatable("enhanced_commands.entity_predicate.air.is_not_max", displayName, actualHealthText, maxHealthText));
    }
  }

  @Override
  public @NotNull String toOptionEntry() {
    return "air=" + (inverted ? "!" : "") + "max";
  }

  @Override
  public @NotNull EntityPredicateType<AirMaxEntityPredicateEntry> getType() {
    return EntityPredicateTypes.AIR_MAX;
  }
}
