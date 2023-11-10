package pers.solid.ecmd.command;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import pers.solid.ecmd.argument.EntityPredicateArgumentType;
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

  private int executeTestPredicate(Collection<? extends Entity> entities, Predicate<Entity> predicate, CommandContext<ServerCommandSource> context) {
    final int size = entities.size();
    if (size == 0) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.none").formatted(Formatting.RED), false);
      return 0;
    } else if (size == 1) {
      final Entity entity = entities.iterator().next();
      final boolean test = predicate.test(entity);
      if (test) {
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.pass", entity.getDisplayName().copy().styled(TextUtil.STYLE_FOR_TARGET)).formatted(Formatting.GREEN), false);
        return 1;
      } else {
        CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.fail", entity.getDisplayName().copy().styled(TextUtil.STYLE_FOR_TARGET)).formatted(Formatting.RED), false);
        return 0;
      }
    } else {
      final int passes = Iterables.size(Iterables.filter(entities, predicate));
      final MutableText exampleEntity = entities.iterator().next().getDisplayName().copy().styled(TextUtil.STYLE_FOR_TARGET);
      if (passes == size) {
        CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.testfor.entity.all_pass", size, exampleEntity).formatted(Formatting.GREEN), false);
      } else if (passes == 0) {
        CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.testfor.entity.none_pass", size, exampleEntity).formatted(Formatting.RED), false);
      } else {
        CommandBridge.sendFeedback(context, () -> TextUtil.enhancedTranslatable("enhanced_commands.commands.testfor.entity.partially_pass", size, exampleEntity, passes).formatted(Formatting.YELLOW), false);
      }
      return passes;
    }
  }

  private int executeShowEntities(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
    final int size = entities.size();
    if (size == 0) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.none").formatted(Formatting.RED), false);
    } else if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.single", Text.empty().append(entities.iterator().next().getDisplayName()).styled(TextUtil.STYLE_FOR_RESULT)), false);
    } else if (size < 9) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.several", Texts.join(entities, entity -> Text.empty().append(entity.getDisplayName()).styled(TextUtil.STYLE_FOR_RESULT)), TextUtil.literal(size).styled(TextUtil.STYLE_FOR_RESULT)), false);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.many", Texts.join(ImmutableList.copyOf(Iterables.limit(entities, 10)), entity -> Text.empty().append(entity.getDisplayName()).styled(TextUtil.STYLE_FOR_RESULT)), TextUtil.literal(size).styled(TextUtil.STYLE_FOR_RESULT)), false);
    }
    return size;
  }
}
