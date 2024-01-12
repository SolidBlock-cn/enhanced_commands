package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.commons.lang3.function.FailableConsumer;
import pers.solid.ecmd.argument.NbtTargetArgumentType;
import pers.solid.ecmd.mixin.CommandContextAccessor;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.EnhancedRedirectModifier;
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
    if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
      DebugOpCommand.INSTANCE.register(dispatcher, registryAccess, environment);
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
    MirrorCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    MoveCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    OutlineCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RandCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RegionSelectionCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    RotateCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    SeparatedExecuteCommand.register(dispatcher, registryAccess);
    StackCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TasksCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestArgCommand.INSTANCE.register(dispatcher, registryAccess, environment);
    TestForCommands.INSTANCE.register(dispatcher, registryAccess, environment);
    TpRelCommand.INSTANCE.register(dispatcher, registryAccess, environment);
  }

  public static <S> LiteralCommandNode<S> registerWithArgumentModification(CommandDispatcher<S> dispatcher, LiteralArgumentBuilder<S> directBuilder, LiteralArgumentBuilder<S> indirectBuilder, CommandNode<S> then, RedirectModifier<S> redirectModifier) {
    final LiteralCommandNode<S> register = dispatcher.register(directBuilder.then(then));
    dispatcher.register(indirectBuilder.forward(then, redirectModifier, false));
    return register;
  }

  public static final EnhancedRedirectModifier.Constant<ServerCommandSource> REGION_ARGUMENTS_MODIFIER = (arguments, previousArguments, source) -> {
    final ServerPlayerEntity player = source.getPlayerOrThrow();
    final RegionArgument regionArgument = ((ServerPlayerEntityExtension) player).ec$getOrEvaluateActiveRegionOrThrow();
    arguments.put("region", new ParsedArgument<>(0, 0, regionArgument));
  };

  public static LiteralCommandNode<ServerCommandSource> registerWithRegionArgumentModification(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> directBuilder, LiteralArgumentBuilder<ServerCommandSource> indirectBuilder, CommandNode<ServerCommandSource> regionArgument) {
    final Command<ServerCommandSource> directCommand = regionArgument.getCommand();
    if (directCommand != null && indirectBuilder.getCommand() == null) {
      indirectBuilder.executes(context -> {
        ((CommandContextAccessor<?>) context).getArguments().put("region", new ParsedArgument<>(0, 0, ((ServerPlayerEntityExtension) context.getSource().getPlayerOrThrow()).ec$getOrEvaluateActiveRegionOrThrow()));
        return directCommand.run(context);
      });
    }
    return registerWithArgumentModification(dispatcher, directBuilder, indirectBuilder, regionArgument, REGION_ARGUMENTS_MODIFIER);
  }

  public static LiteralCommandNode<ServerCommandSource> registerWithRegionArgumentModification(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> directBuilder, LiteralArgumentBuilder<ServerCommandSource> indirectBuilder, ArgumentBuilder<ServerCommandSource, ?> regionArgument) {
    return registerWithRegionArgumentModification(dispatcher, directBuilder, indirectBuilder, regionArgument.build());
  }

  public static FailableConsumer<NbtElement, CommandSyntaxException> consumerOf(CommandContext<ServerCommandSource> context, String targetArgName, String pathArgName) throws CommandSyntaxException {
    final NbtTarget target = NbtTargetArgumentType.getNbtTarget(context, targetArgName);
    final NbtPathArgumentType.NbtPath path = NbtPathArgumentType.getNbtPath(context, pathArgName);
    return nbt -> target.modifyNbt(path, nbt);
  }

  public static FailableConsumer<NbtElement, CommandSyntaxException> consumerOf(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return consumerOf(context, "target", "path");
  }
}
