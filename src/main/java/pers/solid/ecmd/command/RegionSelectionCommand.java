package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.regionselection.RegionSelectionType;
import pers.solid.ecmd.regionselection.WandEvent;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.OmittedRegistryEntryArgumentType.omittedRegistryEntry;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum RegionSelectionCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final Command<ServerCommandSource> executesWithoutParam = context -> {
      final ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
      player.giveItemStack(WandEvent.createWandStack());
      CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhanced_commands.commands.regionselection.build_now", Text.keybind("key.attack").formatted(Formatting.GRAY), Text.keybind("key.use").formatted(Formatting.GRAY)), false);
      return 1;
    };
    final LiteralCommandNode<ServerCommandSource> regionselection
        = dispatcher.register(literalR2("regionselection")
        .executes(executesWithoutParam)
        .then(literal("pos1")
            .executes(context -> executeSetPoint(BlockPos.ofFloored(context.getSource().getPosition()), context, 1))
            .then(argument("pos", EnhancedPosArgumentType.blockPos())
                .executes(context -> executeSetPoint(EnhancedPosArgumentType.getBlockPos(context, "pos"), context, 1))))
        .then(literal("pos2")
            .executes(context -> executeSetPoint(BlockPos.ofFloored(context.getSource().getPosition()), context, 2))
            .then(argument("pos", EnhancedPosArgumentType.blockPos())
                .executes(context -> executeSetPoint(EnhancedPosArgumentType.getBlockPos(context, "pos"), context, 2))))
        .then(literal("type")
            .then(argument("type", omittedRegistryEntry(registryAccess, RegionSelectionType.REGISTRY_KEY))
                .executes(context -> {
                  final ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                  final RegistryEntry.Reference<RegionSelectionType> registryEntry = RegistryEntryArgumentType.getRegistryEntry(context, "type", RegionSelectionType.REGISTRY_KEY);
                  final RegionSelectionType type = registryEntry.value();
                  ((ServerPlayerEntityExtension) player).ec$switchRegionSelectionType(type);
                  CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhanced_commands.commands.regionselection.changed", TextUtil.literal(registryEntry.registryKey().getValue()).styled(TextUtil.STYLE_FOR_RESULT)), true);
                  return 1;
                }))));
    dispatcher.register(literalR2("rs")
        .executes(executesWithoutParam)
        .redirect(regionselection));
  }

  public static int executeSetPoint(BlockPos blockPos, CommandContext<ServerCommandSource> context, int type) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final ServerPlayerEntity player = source.getPlayerOrThrow();
    final RegionSelection regionSelection = ((ServerPlayerEntityExtension) player).ec$getOrResetRegionSelection();
    final Supplier<Text> textSupplier = switch (type) {
      case 1 -> regionSelection.clickFirstPoint(blockPos, player);
      case 2 -> regionSelection.clickSecondPoint(blockPos, player);
      default -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
    };
    CommandBridge.sendFeedback(source, textSupplier, true);
    return 1;
  }
}
