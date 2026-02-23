package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.bridge.BridgeFloatRange;

public record SaturationEntityPredicateEntry(BridgeFloatRange saturation, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<SaturationEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeFloatRange.CODEC.fieldOf("saturation").forGetter(SaturationEntityPredicateEntry::saturation),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(SaturationEntityPredicateEntry::inverted)
  ).apply(i, SaturationEntityPredicateEntry::new));
  private static final Component CRITERION_NAME = Component.translatable("enhanced_commands.entity_predicate.saturation");

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity instanceof final Player player && saturation.test(player.getFoodData().getSaturationLevel()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Component displayName) throws CommandSyntaxException {
    if (!(entity instanceof final Player player)) {
      return TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.general.not_player", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testFloat(player, player.getFoodData().getSaturationLevel(), saturation, CRITERION_NAME, displayName, inverted);
    }
  }

  @Override
  public @NotNull EntityPredicateType<SaturationEntityPredicateEntry> getType() {
    return EntityPredicateTypes.SATURATION;
  }

  @Override
  public String toOptionEntry() {
    return "saturation=" + (inverted ? "!" : "") + saturation.asString();
  }
}
