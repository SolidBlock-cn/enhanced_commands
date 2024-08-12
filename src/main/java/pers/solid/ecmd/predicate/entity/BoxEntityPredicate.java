package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record BoxEntityPredicate(Box offsetBox) implements SpecialEntityPredicate {
  @Override
  public boolean test(@NotNull Entity entity) {
    return offsetBox.intersects(entity.getBoundingBox());
  }

  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    final boolean intersects = offsetBox.intersects(entity.getBoundingBox());
    final Text expectedText = TextUtil.wrapVector(offsetBox.getMinPos()).styled(Styles.EXPECTED).append(" - ").append(TextUtil.wrapVector(offsetBox.getMaxPos()));
    if (intersects) {
      return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.box.true", displayName, expectedText));
    } else {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.box.false", displayName, expectedText));
    }
  }
}
