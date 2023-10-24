package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import pers.solid.ecmd.argument.RegionArgumentType;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.mixin.EnhancedRedirectModifier;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.function.Predicate;

public enum ModCommands implements CommandRegistrationCallback {
  INSTANCE;
  public static final Predicate<ServerCommandSource> REQUIRES_PERMISSION_2 = source -> source.hasPermissionLevel(2);

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    ActiveRegionCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    DrawCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    GameModeAliasCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RandCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    SeparatedExecuteCommand.register(dispatcher, registryAccess);
    SetBlocksCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TasksCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestArgCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestForCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TpRelCommand.INSTANCE.register(dispatcher, registryAccess, environment);
  }

  public static <S> LiteralCommandNode<S> registerWithArgumentModification(CommandDispatcher<S> dispatcher, LiteralArgumentBuilder<S> directBuilder, LiteralArgumentBuilder<S> indirectBuilder, ArgumentBuilder<S, ?> omittedBuilder, ArgumentBuilder<S, ?> then, RedirectModifier<S> redirectModifier) {
    final CommandNode<S> node = omittedBuilder.then(then).build();
    final LiteralCommandNode<S> register = dispatcher.register(directBuilder.then(node));
    dispatcher.register(indirectBuilder.forward(node, redirectModifier, false));
    return register;
  }

  public static final DynamicCommandExceptionType PLAYER_HAS_NO_ACTIVE_REGION = new DynamicCommandExceptionType(o -> Text.translatable("enhancedCommands.argument.region.no_active_region", o));

  public static final EnhancedRedirectModifier.ArgumentsModifier<ServerCommandSource> REGION_ARGUMENTS_MODIFIER = (arguments, source) -> {
    final ServerPlayerEntity player = source.getPlayerOrThrow();
    final RegionArgument<?> regionArgument = ((ServerPlayerEntityExtension) player).ec_getActiveRegion();
    if (regionArgument == null) {
      throw PLAYER_HAS_NO_ACTIVE_REGION.create(player.getName());
    }
    arguments.put("region", new ParsedArgument<>(0, 0, regionArgument));
  };

  public static LiteralCommandNode<ServerCommandSource> registerWithRegionArgumentModification(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> directBuilder, LiteralArgumentBuilder<ServerCommandSource> indirectBuilder, ArgumentBuilder<ServerCommandSource, ?> then, CommandRegistryAccess commandRegistryAccess) {
    return registerWithArgumentModification(dispatcher, directBuilder, indirectBuilder, RequiredArgumentBuilder.argument("region", RegionArgumentType.region(commandRegistryAccess)), then, EnhancedRedirectModifier.of(REGION_ARGUMENTS_MODIFIER));
  }

  public static LiteralCommandNode<net.minecraft.server.command.ServerCommandSource> registerWithRegionArgumentModification(CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher, String name, ArgumentBuilder<net.minecraft.server.command.ServerCommandSource, ?> then, CommandRegistryAccess commandRegistryAccess) {
    return registerWithRegionArgumentModification(dispatcher, LiteralArgumentBuilder.literal(name), LiteralArgumentBuilder.literal("/" + name), then, commandRegistryAccess);
  }
}
