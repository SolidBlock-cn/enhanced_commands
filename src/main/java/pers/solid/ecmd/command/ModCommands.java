package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.FailableConsumer;
import pers.solid.ecmd.argument.NbtTargetArgument;
import pers.solid.ecmd.config.CommandsConfig;
import pers.solid.ecmd.mixins.accessor.ExecuteCommandAccessor;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.region.ActiveRegionProvider;

import java.util.Collections;
import java.util.function.Predicate;

public enum ModCommands implements CommandRegistrationCallback {
  INSTANCE;
  public static final Predicate<CommandSourceStack> REQUIRES_PERMISSION_2 = source -> source.hasPermission(2);

  public static LiteralArgumentBuilder<CommandSourceStack> literalR2(String literal) {
    return Commands.literal(literal).requires(REQUIRES_PERMISSION_2);
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    if (CommandsConfig.current.enableDebugCommands) {
      DebugDeOpCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
      DebugIgnoreBoundaryCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
      DebugOpCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
      DebugPermissionLevelCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    }

    ActiveRegionCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    AirCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    ConvertBlockCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    ConvertBlocksCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    DrawCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    EnhancedCommandsConfigCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    FillReplaceCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    FireCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    FoodCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    GameModeAliasCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    HealthCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    HistoryCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    if (CommandsConfig.current.enableMirrorCommand) {
      MirrorCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    }
    MoonCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    if (CommandsConfig.current.enableMoveCommand) {
      MoveCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    }
    NbtCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    OutlineCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    PileCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    PostProcessCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    RandCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    RegionSelectionCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    RotateCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    SeparatedExecuteCommand.register(dispatcher, commandBuildContext);
    if (CommandsConfig.current.enableStackCommand) {
      StackCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    }
    TameCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    TasksCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    TestArgCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    TestForCommands.INSTANCE.register(dispatcher, commandBuildContext, environment);
    TpRelCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    UndoCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
    EnhancedWeatherCommand.INSTANCE.register(dispatcher, commandBuildContext, environment);
  }

  public static <S> LiteralCommandNode<S> registerWithArgumentModification(CommandDispatcher<S> dispatcher, LiteralArgumentBuilder<S> directBuilder, LiteralArgumentBuilder<S> indirectBuilder, CommandNode<S> then, RedirectModifier<S> redirectModifier) {
    final LiteralCommandNode<S> register = dispatcher.register(directBuilder.then(then));
    dispatcher.register(indirectBuilder.forward(then, redirectModifier, false));
    return register;
  }

  public static final RedirectModifier<CommandSourceStack> REGION_ARGUMENTS_MODIFIER = context -> {
    final CommandSourceStack source = context.getSource();
    source.addExtraArgument$ec("region", ActiveRegionProvider.INSTANCE);
    return Collections.singleton(source);
  };

  public static LiteralCommandNode<CommandSourceStack> registerWithRegionArgumentModification(CommandDispatcher<CommandSourceStack> dispatcher, LiteralArgumentBuilder<CommandSourceStack> directBuilder, LiteralArgumentBuilder<CommandSourceStack> indirectBuilder, CommandNode<CommandSourceStack> regionArgument) {
    final Command<CommandSourceStack> directCommand = regionArgument.getCommand();
    if (directCommand != null && indirectBuilder.getCommand() == null) {
      indirectBuilder.executes(context -> {
        final CommandSourceStack source = context.getSource();
        source.addExtraArgument$ec("region", ActiveRegionProvider.INSTANCE);
        return directCommand.run(context);
      });
    }
    return registerWithArgumentModification(dispatcher, directBuilder, indirectBuilder, regionArgument, REGION_ARGUMENTS_MODIFIER);
  }

  public static LiteralCommandNode<CommandSourceStack> registerWithRegionArgumentModification(CommandDispatcher<CommandSourceStack> dispatcher, LiteralArgumentBuilder<CommandSourceStack> directBuilder, LiteralArgumentBuilder<CommandSourceStack> indirectBuilder, ArgumentBuilder<CommandSourceStack, ?> regionArgument) {
    return registerWithRegionArgumentModification(dispatcher, directBuilder, indirectBuilder, regionArgument.build());
  }

  public static FailableConsumer<Tag, CommandSyntaxException> consumerOf(CommandContext<CommandSourceStack> context, String targetArgName, String pathArgName) throws CommandSyntaxException {
    final NbtTarget<?> target = NbtTargetArgument.getNbtTarget(context, targetArgName);
    final NbtPathArgument.NbtPath path = NbtPathArgument.getPath(context, pathArgName);
    return nbt -> target.setNbtInPath(context.getSource(), path, nbt);
  }

  public static FailableConsumer<Tag, CommandSyntaxException> consumerOf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return consumerOf(context, "target", "path");
  }

  /**
   * 因原方法涉及非公开类，无法通过 mixin 取代，故需要重新做一遍方法。
   */
  public static ArgumentBuilder<CommandSourceStack, ?> addConditionLogic(CommandNode<CommandSourceStack> root, ArgumentBuilder<CommandSourceStack, ?> builder, boolean positive, Predicate<CommandContext<CommandSourceStack>> condition) {
    return builder.fork(root, (context) -> ExecuteCommandAccessor.callExpect(context, positive, condition.test(context))).executes((context) -> {
      if (positive == condition.test(context)) {
        context.getSource().sendSuccess(() -> Component.translatable("commands.execute.conditional.pass"), false);
        return 1;
      } else {
        throw ExecuteCommandAccessor.getERROR_CONDITIONAL_FAILED().create();
      }
    });
  }

}
