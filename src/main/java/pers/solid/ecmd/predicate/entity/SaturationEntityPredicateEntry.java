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
import pers.solid.ecmd.util.bridge.BridgeFloatRange;

public record SaturationEntityPredicateEntry(BridgeFloatRange saturation, boolean inverted) implements EntityPredicateEntry, StaticEntityPredicate {
  public static final MapCodec<SaturationEntityPredicateEntry> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BridgeFloatRange.CODEC.fieldOf("saturation").forGetter(SaturationEntityPredicateEntry::saturation),
      Codec.BOOL.optionalFieldOf("inverted", false).forGetter(SaturationEntityPredicateEntry::inverted)
  ).apply(i, SaturationEntityPredicateEntry::new));
  private static final Text CRITERION_NAME = Text.translatable("enhanced_commands.entity_predicate.saturation");

  @Override
  public boolean test(@NotNull Entity entity) {
    return entity instanceof final PlayerEntity player && saturation.test(player.getHungerManager().getSaturationLevel()) != inverted;
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    if (!(entity instanceof final PlayerEntity player)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.general.not_player", displayName, CRITERION_NAME));
    } else {
      return EntityPredicateEntry.testFloat(player, player.getHungerManager().getSaturationLevel(), saturation, CRITERION_NAME, displayName, inverted);
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
