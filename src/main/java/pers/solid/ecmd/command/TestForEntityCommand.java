package pers.solid.ecmd.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.ecmd.argument.EntityPredicateArgumentType;
import pers.solid.ecmd.predicate.entity.EntityPredicate;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.Collection;

public enum TestForEntityCommand implements TestForCommands.Entry {
  INSTANCE;

  @Override
  public void addArguments(LiteralArgumentBuilder<ServerCommandSource> testForBuilder, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    testForBuilder.then(CommandManager.literal("entity")
        .then(CommandManager.argument("entities", EntityArgumentType.entities())
            .executes(context -> executeShowEntities(EntityArgumentType.getOptionalEntities(context, "entities"), context))
            .then(CommandManager.argument("predicate", EntityPredicateArgumentType.entityPredicate(registryAccess))
                .executes(context -> executeTestPredicate(EntityArgumentType.getOptionalEntities(context, "entities"), EntityPredicateArgumentType.getEntityPredicate(context, "predicate"), context)))));
  }

  private int executeTestPredicate(Collection<? extends Entity> entities, EntityPredicate predicate, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final int size = entities.size();
    if (size == 0) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.none").formatted(Formatting.RED), false);
      return 0;
    } else if (size == 1) {
      final Entity entity = entities.iterator().next();
      final TestResult testResult = predicate.testAndDescribe(entity);
      testResult.sendMessage(context.getSource());
      return BooleanUtils.toInteger(testResult.successes());
    } else {
      final int passes = Iterables.size(Iterables.filter(entities, predicate::test));
      final MutableText exampleEntity = TextUtil.styled(entities.iterator().next().getDisplayName(), Styles.TARGET);
      if (passes == size) {
        CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.testfor.entity.all_pass", size, exampleEntity).styled(Styles.TRUE), false);
      } else if (passes == 0) {
        CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.testfor.entity.none_pass", size, exampleEntity).styled(Styles.FALSE), false);
      } else {
        CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.testfor.entity.partially_pass", size, exampleEntity, passes).styled(Styles.MEDIUM), false);
      }
      return passes;
    }
  }

  private int executeShowEntities(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
    final int size = entities.size();
    if (size == 0) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.none").formatted(Formatting.RED), false);
    } else if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.single", TextUtil.styled(entities.iterator().next().getDisplayName(), Styles.RESULT)), false);
    } else if (size < 9) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.several", Texts.join(entities, entity -> TextUtil.styled(entity.getDisplayName(), Styles.RESULT)), TextUtil.literal(size).styled(Styles.RESULT)), false);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.many", Texts.join(ImmutableList.copyOf(Iterables.limit(entities, 10)), entity -> TextUtil.styled(entity.getDisplayName(), Styles.RESULT)), TextUtil.literal(size).styled(Styles.RESULT)), false);
    }
    return size;
  }
}
