package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeIntRange;

public record LevelEntityPredicateEntry(BridgeIntRange level, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<LevelEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeIntRange.CODEC.fieldOf("level").forGetter(LevelEntityPredicateEntry::level),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(LevelEntityPredicateEntry::inverted)
  ).apply(i, LevelEntityPredicateEntry::new));
  private static final Component CRITERION_NAME = Component.translatable("enhanced_commands.entity_predicate.level");

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity instanceof Player player && level.test(player.experienceLevel) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) {
    if (!(entity instanceof Player player)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.general.not_player", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testInt(player, player.experienceLevel, level, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public @NotNull EntityPredicateType<LevelEntityPredicateEntry> getType() {
    return EntityPredicateTypes.LEVEL;
  }

  @Override
  public String toOptionEntry() {
    return "level=" + (inverted ? "!" : "") + level.asString();
  }
}
