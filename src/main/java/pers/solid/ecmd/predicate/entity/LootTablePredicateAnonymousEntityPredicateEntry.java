package pers.solid.ecmd.predicate.entity;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.entity.Entity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.LootBridge;

import java.util.Optional;

public record LootTablePredicateAnonymousEntityPredicateEntry(LootCondition lootCondition, boolean inverted) implements EntityPredicateEntry {
  @Override
  public TestResult testAndDescribe(Entity entity, Text displayName) throws CommandSyntaxException {
    if (!(entity.getWorld() instanceof final ServerWorld serverWorld)) {
      return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.not_on_server", displayName));
    } else {
      LootContext lootContext = new LootContext.Builder(new LootContextParameterSet.Builder(serverWorld)
          .add(LootContextParameters.THIS_ENTITY, entity)
          .add(LootContextParameters.ORIGIN, entity.getPos())
          .build(LootContextTypes.SELECTOR))
          .build(Optional.empty());
      final boolean test = lootCondition.test(lootContext);
      if (inverted ^ test) {
        return TestResult.of(true, Text.translatable("enhanced_commands.entity_predicate.predicate.pass_anonymous", displayName, TextUtil.literal(test).styled(Styles.ACTUAL)));
      } else {
        return TestResult.of(false, Text.translatable("enhanced_commands.entity_predicate.predicate.fail_anonymous", displayName, TextUtil.literal(test).styled(Styles.ACTUAL), TextUtil.literal(!inverted).styled(Styles.EXPECTED)));
      }
    }
  }

  @Override
  public boolean test(@NotNull Entity entity) {
    final LootContext context = LootBridge.createContextForEntity(entity, (ServerWorld) entity.getWorld());
    return lootCondition.test(context);
  }

  @Override
  public String toOptionEntry() {
    return "predicate=" + (inverted ? "!" : "") + LootCondition.CODEC.encodeStart(JsonOps.INSTANCE, lootCondition).getOrThrow();
  }
}
