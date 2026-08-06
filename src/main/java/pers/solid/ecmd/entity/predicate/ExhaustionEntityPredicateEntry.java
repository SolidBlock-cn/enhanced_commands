package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;
import pers.solid.ecmd.util.pack.DoesNotRequireValidation;

public record ExhaustionEntityPredicateEntry(BridgeFloatRange exhaustion, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate, DoesNotRequireValidation {
  public static final MapCodec<ExhaustionEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeFloatRange.CODEC.fieldOf("exhaustion").forGetter(ExhaustionEntityPredicateEntry::exhaustion),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(ExhaustionEntityPredicateEntry::inverted)
  ).apply(i, ExhaustionEntityPredicateEntry::new));
  private static final Component CRITERION_NAME = Component.translatable("enhanced_commands.entity_predicate.exhaustion");

  @Override
  public boolean test(Entity entity) {
    return entity instanceof final Player player && exhaustion.test(player.getFoodData().getExhaustionLevel()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (!(entity instanceof final Player player)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.general.not_player", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testFloat(player, player.getFoodData().getExhaustionLevel(), exhaustion, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public EntityPredicateType<ExhaustionEntityPredicateEntry> getType() {
    return EntityPredicateTypes.EXHAUSTION;
  }

  @Override
  public String toOptionEntry() {
    return "exhaustion=" + (inverted ? "!" : "") + exhaustion.expressAsString();
  }
}
