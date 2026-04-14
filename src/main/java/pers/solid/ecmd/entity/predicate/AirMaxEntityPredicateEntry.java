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
import pers.solid.ecmd.util.TextUtil;

public record AirMaxEntityPredicateEntry(boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<AirMaxEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(AirMaxEntityPredicateEntry::inverted)
  ).apply(i, AirMaxEntityPredicateEntry::new));

  @Override
  public boolean test(@NotNull Entity entity) {
    return (entity.getAirSupply() == entity.getMaxAirSupply()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) {
    final int actualAir = entity.getAirSupply();
    final MutableComponent actualHealthText = TextUtil.literal(actualAir).withStyle(Styles.ACTUAL);
    final int maxAir = entity.getMaxAirSupply();
    final MutableComponent maxHealthText = TextUtil.literal(maxAir);
    if (maxAir == actualAir) {
      return TestResult.of(!inverted, Component.translatable("enhanced_commands.entity_predicate.air.is_max", displayName, actualHealthText));
    } else {
      return TestResult.of(inverted, Component.translatable("enhanced_commands.entity_predicate.air.is_not_max", displayName, actualHealthText, maxHealthText));
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
