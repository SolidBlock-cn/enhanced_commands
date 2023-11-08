package pers.solid.ecmd.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.Collection;

public enum TestForEntityCommand implements TestForCommands.Entry {
  INSTANCE;

  @Override
  public void addArguments(LiteralArgumentBuilder<ServerCommandSource> testForBuilder, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    testForBuilder.then(CommandManager.literal("entity")
        .then(CommandManager.argument("entities", EntityArgumentType.entities())
            .executes(context -> executeShowEntities(EntityArgumentType.getOptionalEntities(context, "entities"), context))));
  }

  private int executeShowEntities(Collection<? extends Entity> entities, CommandContext<ServerCommandSource> context) {
    final int size = entities.size();
    if (size == 0) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.none").formatted(Formatting.RED), true);
    } else if (size == 1) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.single", Text.empty().append(entities.iterator().next().getDisplayName()).styled(TextUtil.STYLE_FOR_RESULT)), true);
    } else if (size < 9) {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.several", Texts.join(entities, entity -> Text.empty().append(entity.getDisplayName()).styled(TextUtil.STYLE_FOR_RESULT)), TextUtil.literal(size).styled(TextUtil.STYLE_FOR_RESULT)), true);
    } else {
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.testfor.entity.many", Texts.join(ImmutableList.copyOf(Iterables.limit(entities, 10)), entity -> Text.empty().append(entity.getDisplayName()).styled(TextUtil.STYLE_FOR_RESULT)), TextUtil.literal(size).styled(TextUtil.STYLE_FOR_RESULT)), true);
    }
    return size;
  }
}
