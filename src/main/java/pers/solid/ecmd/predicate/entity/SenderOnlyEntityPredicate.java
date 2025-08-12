package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public enum SenderOnlyEntityPredicate implements SpecialEntityPredicate {
  INSTANCE;
  public static final MapCodec<SenderOnlyEntityPredicate> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(@NotNull Entity entity, @NotNull ExecutionContext context) {
    return entity.equals(context.positionProvider.getEntity$ec());
  }

  @Override
  public TestResult testAndDescribe(@NotNull Entity entity, @NotNull ExecutionContext context, Text displayName) throws CommandSyntaxException {
    final Entity sender = context.positionProvider.getEntity$ec();
    if (entity.equals(sender)) {
      return (TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.sender.true", displayName)));
    } else if (sender != null) {
      return (TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.sender.false", displayName, TextUtil.styled(sender.getDisplayName(), Styles.EXPECTED))));
    } else {
      return (TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.sender.false_without_sender", displayName)));
    }
  }

  @Override
  public @NotNull EntityPredicateType<SenderOnlyEntityPredicate> getType() {
    return EntityPredicateTypes.SENDER_ONLY;
  }

  @Override
  public @NotNull String asString() {
    return "@s";
  }
}
