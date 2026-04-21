package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

public record FoodEntityPredicateEntry(BridgeIntRange food, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<FoodEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeIntRange.CODEC.fieldOf("food").forGetter(FoodEntityPredicateEntry::food),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(FoodEntityPredicateEntry::inverted)
  ).apply(i, FoodEntityPredicateEntry::new));
  private static final Component CRITERION_NAME = Component.translatable("enhanced_commands.entity_predicate.food");

  @Override
  public boolean test(Entity entity) {
    return entity instanceof final Player player && food.test(player.getFoodData().getFoodLevel()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    if (!(entity instanceof final Player player)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.general.not_player", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testInt(player, player.getFoodData().getFoodLevel(), food, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public EntityPredicateType<FoodEntityPredicateEntry> getType() {
    return EntityPredicateTypes.FOOD;
  }

  @Override
  public String toOptionEntry() {
    return "food=" + (inverted ? "!" : "") + food.expressAsString();
  }
}
