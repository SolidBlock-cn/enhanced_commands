package pers.solid.ecmd.entity.predicate;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

public enum SenderOnlyEntityPredicate implements SpecialEntityPredicate {
  INSTANCE;
  public static final MapCodec<SenderOnlyEntityPredicate> CODEC = MapCodec.unit(INSTANCE);

  @Override
  public boolean test(Entity entity, ExecutionContext context) {
    return entity.equals(context.positionProvider.getEntity$ec());
  }

  @Override
  public TestResult testAndDescribe(Entity entity, ExecutionContext context, Component displayName) {
    final Entity sender = context.positionProvider.getEntity$ec();
    if (entity.equals(sender)) {
      return (TestResult.of(true, Component.translatable("enhanced_commands.entity_predicate.sender.true", displayName)));
    } else if (sender != null) {
      return (TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.sender.false", displayName, TextUtil.styled(sender.getDisplayName(), Styles.EXPECTED))));
    } else {
      return (TestResult.of(false, Component.translatable("enhanced_commands.entity_predicate.sender.false_without_sender", displayName)));
    }
  }

  @Override
  public EntityPredicateType<SenderOnlyEntityPredicate> getType() {
    return EntityPredicateTypes.SENDER_ONLY;
  }

  @Override
  public String expressAsString() {
    return "@s";
  }
}
