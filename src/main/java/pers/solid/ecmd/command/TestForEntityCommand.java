package pers.solid.ecmd.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.EntityPredicateArgument;
import pers.solid.ecmd.entity.predicate.EntityPredicate;
import pers.solid.ecmd.util.ExecutionContext;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TestResult;
import pers.solid.ecmd.util.TextUtil;

import java.util.Collection;

public enum TestForEntityCommand implements TestForCommands.Entry {
  INSTANCE;

  @Override
  public void addArguments(LiteralArgumentBuilder<CommandSourceStack> testForBuilder, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    testForBuilder.then(Commands.literal("entity")
        .then(Commands.argument("entities", EntityArgument.entities())
            .executes(context -> executeShowEntities(EntityArgument.getOptionalEntities(context, "entities"), context))
            .then(Commands.argument("predicate", EntityPredicateArgument.entityPredicate(commandBuildContext))
                .executes(context -> executeTestPredicate(EntityArgument.getOptionalEntities(context, "entities"), EntityPredicateArgument.getEntityPredicate(context, "predicate"), context)))));
  }

  private int executeTestPredicate(Collection<? extends Entity> entities, EntityPredicate predicate, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final int size = entities.size();
    if (size == 0) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.entity.none").withStyle(ChatFormatting.RED), false);
      return 0;
    } else if (size == 1) {
      final Entity entity = entities.iterator().next();
      final ExecutionContext executionContext = new ExecutionContext(context.getSource());
      final TestResult testResult = predicate.testAndDescribe(entity, executionContext);
      testResult.sendMessage(context.getSource());
      return BooleanUtils.toInteger(testResult.successes());
    } else {
      final int passes = Iterables.size(Iterables.filter(entities, entity -> predicate.test(entity, new ExecutionContext(context.getSource()))));
      final MutableComponent exampleEntity = TextUtil.styled(entities.iterator().next().getDisplayName(), Styles.TARGET);
      if (passes == size) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.entity.all_pass", size, exampleEntity).enhanced$$().withStyle(Styles.TRUE), false);
      } else if (passes == 0) {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.entity.none_pass", size, exampleEntity).enhanced$$().withStyle(Styles.FALSE), false);
      } else {
        context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.entity.partially_pass", size, exampleEntity, passes).enhanced$$().withStyle(Styles.MEDIUM), false);
      }
      return passes;
    }
  }

  private int executeShowEntities(Collection<? extends Entity> entities, CommandContext<CommandSourceStack> context) {
    final int size = entities.size();
    if (size == 0) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.entity.none").withStyle(ChatFormatting.RED), false);
    } else if (size == 1) {
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.entity.single", TextUtil.styled(entities.iterator().next().getDisplayName(), Styles.RESULT)), false);
    } else if (size < 9) {
      @NotNull CommandSourceStack source = context.getSource();
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.entity.several", ComponentUtils.formatList(entities, entity -> TextUtil.styled(entity.getDisplayName(), Styles.RESULT)), TextUtil.literal(size).withStyle(Styles.RESULT)), false);
    } else {
      @NotNull CommandSourceStack source = context.getSource();
      source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testfor.entity.many", ComponentUtils.formatList(ImmutableList.copyOf(Iterables.limit(entities, 10)), entity -> TextUtil.styled(entity.getDisplayName(), Styles.RESULT)), TextUtil.literal(size).withStyle(Styles.RESULT)), false);
    }
    return size;
  }
}
