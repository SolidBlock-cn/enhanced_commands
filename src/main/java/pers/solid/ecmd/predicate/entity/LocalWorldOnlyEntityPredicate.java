package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record LocalWorldOnlyEntityPredicate(@NotNull World sourceWorld) implements SpecialEntityPredicate {
  @Override
  public boolean test(@NotNull Entity entity) {
    return entity.getWorld().equals(sourceWorld);
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    final World world = entity.getWorld();
    if (world.equals(sourceWorld)) {
      return (TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.local_world.true", displayName, TextUtil.literal(world.getRegistryKey().getValue()).styled(Styles.ACTUAL))));
    } else {
      return (TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.local_world.false", displayName, TextUtil.literal(world.getRegistryKey().getValue()).styled(Styles.ACTUAL), TextUtil.literal(sourceWorld.getRegistryKey().getValue()).styled(Styles.EXPECTED))));
    }
  }
}
