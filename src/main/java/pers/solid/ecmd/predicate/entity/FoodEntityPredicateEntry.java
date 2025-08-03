package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

public record FoodEntityPredicateEntry(BridgeIntRange food, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<FoodEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeIntRange.CODEC.fieldOf("food").forGetter(FoodEntityPredicateEntry::food),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(FoodEntityPredicateEntry::inverted)
  ).apply(i, FoodEntityPredicateEntry::new));
  private static final Text CRITERION_NAME = Text.translatable("enhanced_commands.entity_predicate.food");

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity instanceof final PlayerEntity player && food.test(player.getHungerManager().getFoodLevel()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    if (!(entity instanceof final PlayerEntity player)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.general.not_player", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testInt(player, player.getHungerManager().getFoodLevel(), food, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public @NotNull EntityPredicateType<FoodEntityPredicateEntry> getType() {
    return EntityPredicateTypes.FOOD;
  }

  @Override
  public @NotNull String toOptionEntry() {
    return "food=" + (inverted ? "!" : "") + food.asString();
  }
}
