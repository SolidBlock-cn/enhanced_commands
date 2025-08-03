package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public enum LocalWorldOnlyEntityPredicate implements SpecialEntityPredicate {
  INSTANCE;
  public static final MapCodec<LocalWorldOnlyEntityPredicate> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    return entity.getWorld().equals(context.positionProvider.world$ec());
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final World world = entity.getWorld();
    final World sourceWorld = context.positionProvider.world$ec();
    if (world.equals(sourceWorld)) {
      return (TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.local_world.true", displayName, TextUtil.literal(world.getRegistryKey().getValue()).styled(Styles.ACTUAL))));
    } else {
      return (TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.local_world.false", displayName, TextUtil.literal(world.getRegistryKey().getValue()).styled(Styles.ACTUAL), Text.literal(sourceWorld == null ? "<unknown>" : sourceWorld.getRegistryKey().getValue().toString()).styled(Styles.EXPECTED))));
    }
  }

  @Override
  public @NotNull EntityPredicateType<LocalWorldOnlyEntityPredicate> getType() {
    return EntityPredicateTypes.LOCAL_WORLD;
  }

  @Override
  public @NotNull String asString() {
    return "<local world>";
  }
}
