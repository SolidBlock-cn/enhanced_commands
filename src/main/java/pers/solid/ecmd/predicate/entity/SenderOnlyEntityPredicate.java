package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public record SenderOnlyEntityPredicate(@Nullable Entity sender) implements SpecialEntityPredicate {
  @Override
  public boolean test(@NotNull Entity entity) {
    return entity.equals(sender);
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, Text displayName) throws CommandSyntaxException {
    if (entity.equals(sender)) {
      return (TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.sender.true", displayName)));
    } else if (sender != null) {
      return (TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.sender.false", displayName, TextUtil.styled(sender.getDisplayName(), Styles.EXPECTED))));
    } else {
      return (TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.sender.false_without_sender", displayName)));
    }
  }
}
