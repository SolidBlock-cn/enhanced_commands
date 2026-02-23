package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import pers.solid.ecmd.argument.SimpleEnumArgumentType;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.enums.CommandEnumType;
import pers.solid.ecmd.util.enums.MoonPhase;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public enum MoonCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final LiteralCommandNode<CommandSourceStack> moonNode = dispatcher.register(literal("moon")
        .executes(MoonCommand::executeGetPhase)
        .then(literal("get")
            .executes(MoonCommand::executeGetPhase))
        .then(literal("set")
            .then(argument("moon_phase", SimpleEnumArgumentType.simpleEnum(CommandEnumType.MOON_PHASE))
                .executes(MoonCommand::executeSetPhase)))
        .then(literal("rotate")
            .executes(context -> executeRotatePhase(context, 1))
            .then(argument("steps", IntegerArgumentType.integer())
                .executes(context -> executeRotatePhase(context, IntegerArgumentType.getInteger(context, "steps")))))
        .then(addConditionArguments(dispatcher.getRoot(), literal("if"), true, commandBuildContext))
        .then(addConditionArguments(dispatcher.getRoot(), literal("unless"), false, commandBuildContext)));

    dispatcher.register(literal("jadeplate").redirect(moonNode));
  }

  private static int executeGetPhase(CommandContext<CommandSourceStack> context) {
    final CommandSourceStack source = context.getSource();
    final ServerLevel world = source.getLevel();
    final int moonPhase = world.getMoonPhase();
    final MoonPhase moonPhaseValue = MoonPhase.byNumericId(moonPhase);
    source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.moon.phase.get", TextUtil.styled(moonPhaseValue.displayName, Styles.RESULT)), false);
    return moonPhase;
  }

  private static int executeSetPhase(CommandContext<CommandSourceStack> context) {
    final MoonPhase moonPhase = context.getArgument("moon_phase", MoonPhase.class);
    final CommandSourceStack source = context.getSource();
    final ServerLevel world = source.getLevel();
    final long timeOfDay = world.getDayTime();
    final int actualMoonPhase = world.getMoonPhase();
    world.setDayTime(timeOfDay + (moonPhase.ordinal() - actualMoonPhase) * 24000L);
    source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.moon.phase.set.success", TextUtil.styled(moonPhase.displayName, Styles.RESULT)), true);
    return 1;
  }

  private static int executeRotatePhase(CommandContext<CommandSourceStack> context, int steps) {
    final CommandSourceStack source = context.getSource();
    final ServerLevel world = source.getLevel();
    final long timeOfDay = world.getDayTime();
    world.setDayTime(timeOfDay + Math.floorMod(steps, 8) * 24000L);
    final MoonPhase moonPhase = MoonPhase.byNumericId(world.getMoonPhase());
    source.sendFeedback$ecBridge(() -> steps >= 0 ? Component.translatable("enhanced_commands.commands.moon.phase.rotate.next", TextUtil.literal(steps).withStyle(Styles.TARGET), TextUtil.styled(moonPhase.displayName, Styles.RESULT)) : Component.translatable("enhanced_commands.commands.moon.phase.rotate.previous", TextUtil.literal(-steps).withStyle(Styles.TARGET), TextUtil.styled(moonPhase.displayName, Styles.RESULT)), true);
    return 1;
  }

  private static <X extends ArgumentBuilder<CommandSourceStack, X>> X addConditionArguments(CommandNode<CommandSourceStack> root, X argumentBuilder, boolean positive, CommandBuildContext commandRegistryAccess) {
    return argumentBuilder
        .then(literal("phase")
            .then(ModCommands.addConditionLogic(root, argument("moon_phase", SimpleEnumArgumentType.simpleEnum(CommandEnumType.MOON_PHASE)), positive, context -> context.getSource().getLevel().getMoonPhase() == context.getArgument("moon_phase", MoonPhase.class).ordinal())))
        .then(literal("size")
            .then(ModCommands.addConditionLogic(root, argument("size", RangeArgument.floatRange()), positive, context -> RangeArgument.Floats.getRange(context, "size").matches(context.getSource().getLevel().getMoonBrightness()))));
  }
}
