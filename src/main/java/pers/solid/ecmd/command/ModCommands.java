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
import pers.solid.ecmd.argument.NbtTargetArgumentType;
import pers.solid.ecmd.configs.CommandsConfig;
import pers.solid.ecmd.mixins.accessor.ExecuteCommandAccessor;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.region.ActiveRegionArgument;

import java.util.Collections;
import java.util.function.Predicate;

public enum ModCommands implements CommandRegistrationCallback {
  INSTANCE;
  public static final Predicate<CommandSourceStack> REQUIRES_PERMISSION_2 = source -> source.hasPermission(2);

  public static LiteralArgumentBuilder<CommandSourceStack> literalR2(String literal) {
    return Commands.literal(literal).requires(REQUIRES_PERMISSION_2);
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    if (CommandsConfig.CURRENT.enableDebugCommands) {
      DebugDeOpCommand.INSTANCE.register(dispatcher, registryAccess, environment);
      DebugIgnoreBoundaryCommand.INSTANCE.register(dispatcher, registryAccess, environment);
      DebugOpCommand.INSTANCE.register(dispatcher, registryAccess, environment);
      DebugPermissionLevelCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    }

    ActiveRegionCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    AirCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    ConvertBlockCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    ConvertBlocksCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    DrawCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    FillReplaceCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    FireCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    FoodCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    GameModeAliasCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    HealthCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    HistoryCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    MirrorCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    MoonCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    MoveCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    NbtCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    OutlineCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    PileCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    PostProcessCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RandCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RegionSelectionCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RotateCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    SeparatedExecuteCommand.register(dispatcher, registryAccess);
    StackCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TameCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TasksCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestArgCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestForCommands.INSTANCE.register(dispatcher, registryAccess, environment);
    TpRelCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    UndoCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    EnhancedWeatherCommand.INSTANCE.register(dispatcher, registryAccess, environment);
  }

  public static <S> LiteralCommandNode<S> registerWithArgumentModification(CommandDispatcher<S> dispatcher, LiteralArgumentBuilder<S> directBuilder, LiteralArgumentBuilder<S> indirectBuilder, CommandNode<S> then, RedirectModifier<S> redirectModifier) {
    final LiteralCommandNode<S> register = dispatcher.register(directBuilder.then(then));
    dispatcher.register(indirectBuilder.forward(then, redirectModifier, false));
    return register;
  }

  public static final RedirectModifier<CommandSourceStack> REGION_ARGUMENTS_MODIFIER = context -> {
    final CommandSourceStack source = context.getSource();
    source.addExtraArgument$ec("region", ActiveRegionArgument.INSTANCE);
    return Collections.singleton(source);
  };

  public static LiteralCommandNode<CommandSourceStack> registerWithRegionArgumentModification(CommandDispatcher<CommandSourceStack> dispatcher, LiteralArgumentBuilder<CommandSourceStack> directBuilder, LiteralArgumentBuilder<CommandSourceStack> indirectBuilder, CommandNode<CommandSourceStack> regionArgument) {
    final Command<CommandSourceStack> directCommand = regionArgument.getCommand();
    if (directCommand != null && indirectBuilder.getCommand() == null) {
      indirectBuilder.executes(context -> {
        final CommandSourceStack source = context.getSource();
        source.addExtraArgument$ec("region", ActiveRegionArgument.INSTANCE);
        return directCommand.run(context);
      });
    }
    return registerWithArgumentModification(dispatcher, directBuilder, indirectBuilder, regionArgument, REGION_ARGUMENTS_MODIFIER);
  }

  public static LiteralCommandNode<CommandSourceStack> registerWithRegionArgumentModification(CommandDispatcher<CommandSourceStack> dispatcher, LiteralArgumentBuilder<CommandSourceStack> directBuilder, LiteralArgumentBuilder<CommandSourceStack> indirectBuilder, ArgumentBuilder<CommandSourceStack, ?> regionArgument) {
    return registerWithRegionArgumentModification(dispatcher, directBuilder, indirectBuilder, regionArgument.build());
  }

  public static FailableConsumer<Tag, CommandSyntaxException> consumerOf(CommandContext<CommandSourceStack> context, String targetArgName, String pathArgName) throws CommandSyntaxException {
    final NbtTarget<?> target = NbtTargetArgumentType.getNbtTarget(context, targetArgName);
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
