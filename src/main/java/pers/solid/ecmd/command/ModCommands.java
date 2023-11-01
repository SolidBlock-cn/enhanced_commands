package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import pers.solid.ecmd.argument.RegionArgumentType;
import pers.solid.ecmd.mixin.CommandContextAccessor;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.mixin.EnhancedRedirectModifier;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.function.Predicate;

public enum ModCommands implements CommandRegistrationCallback {
  INSTANCE;
  public static final Predicate<ServerCommandSource> REQUIRES_PERMISSION_2 = source -> source.hasPermissionLevel(2);

  public static LiteralArgumentBuilder<ServerCommandSource> literalR2(String literal) {
    return CommandManager.literal(literal).requires(REQUIRES_PERMISSION_2);
  }

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    ActiveRegionCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    DrawCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    FillReplaceCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    GameModeAliasCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    MirrorCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    MoveCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    OutlineCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RandCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RegionBuilderCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RotateCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    SeparatedExecuteCommand.register(dispatcher, registryAccess);
    StackCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TasksCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestArgCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestForCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TpRelCommand.INSTANCE.register(dispatcher, registryAccess, environment);
  }

  public static <S> LiteralCommandNode<S> registerWithArgumentModification(CommandDispatcher<S> dispatcher, LiteralArgumentBuilder<S> directBuilder, LiteralArgumentBuilder<S> indirectBuilder, ArgumentBuilder<S, ?> then, RedirectModifier<S> redirectModifier) {
    return registerWithArgumentModification(dispatcher, directBuilder, indirectBuilder, then.build(), redirectModifier);
  }

  public static <S> LiteralCommandNode<S> registerWithArgumentModification(CommandDispatcher<S> dispatcher, LiteralArgumentBuilder<S> directBuilder, LiteralArgumentBuilder<S> indirectBuilder, CommandNode<S> then, RedirectModifier<S> redirectModifier) {
    final LiteralCommandNode<S> register = dispatcher.register(directBuilder.then(then));
    dispatcher.register(indirectBuilder.forward(then, redirectModifier, false));
    return register;
  }

  public static final EnhancedRedirectModifier.Constant<ServerCommandSource> REGION_ARGUMENTS_MODIFIER = (arguments, previousArguments, source) -> {
    final ServerPlayerEntity player = source.getPlayerOrThrow();
    final RegionArgument<?> regionArgument = ((ServerPlayerEntityExtension) player).ec$getOrEvaluateActiveRegionOrThrow(source);
    arguments.put("region", new ParsedArgument<>(0, 0, regionArgument));
  };

  public static LiteralCommandNode<ServerCommandSource> registerWithRegionArgumentModification(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, LiteralArgumentBuilder<ServerCommandSource> directBuilder, LiteralArgumentBuilder<ServerCommandSource> indirectBuilder, CommandNode<ServerCommandSource> then) {
    return registerWithArgumentModification(dispatcher, directBuilder, indirectBuilder, CommandManager.argument("region", RegionArgumentType.region(commandRegistryAccess)).then(then), REGION_ARGUMENTS_MODIFIER);
  }

  public static LiteralCommandNode<ServerCommandSource> registerWithRegionArgumentModificationDefaults(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, LiteralArgumentBuilder<ServerCommandSource> directBuilder, LiteralArgumentBuilder<ServerCommandSource> indirectBuilder, CommandNode<ServerCommandSource> then, Command<ServerCommandSource> executesWithoutArguments) {
    return registerWithArgumentModification(dispatcher, directBuilder, indirectBuilder.executes(context -> {
      ((CommandContextAccessor<?>) context).getArguments().put("region", new ParsedArgument<>(0, 0, ((ServerPlayerEntityExtension) context.getSource().getPlayerOrThrow()).ec$getOrEvaluateActiveRegionOrThrow(context.getSource())));
      return executesWithoutArguments.run(context);
    }), CommandManager.argument("region", RegionArgumentType.region(commandRegistryAccess)).then(then).executes(executesWithoutArguments), REGION_ARGUMENTS_MODIFIER);
  }

  public static LiteralCommandNode<ServerCommandSource> registerWithRegionArgumentModification(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, LiteralArgumentBuilder<ServerCommandSource> directBuilder, LiteralArgumentBuilder<ServerCommandSource> indirectBuilder, ArgumentBuilder<ServerCommandSource, ?> then) {
    return registerWithRegionArgumentModification(dispatcher, commandRegistryAccess, directBuilder, indirectBuilder, then.build());
  }
}
