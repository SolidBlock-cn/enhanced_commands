package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.regionselection.RegionSelectionType;
import pers.solid.ecmd.regionselection.WandEvent;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

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
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.regionselection.build_now", Text.keybind("key.attack").formatted(Formatting.GRAY), Text.keybind("key.use").formatted(Formatting.GRAY)), false);
      return 1;
    };
    final LiteralCommandNode<ServerCommandSource> regionselection
        = dispatcher.register(literalR2("regionselection")
        .executes(executesWithoutParam)
        .then(literal("pos1")
            .executes(context -> executeSetPoint(BlockPos.ofFloored(context.getSource().getPosition()).toCenterPos(), context, 1))
            .then(argument("pos", EnhancedPosArgumentType.posPreferringCenteredInt())
                .executes(context -> executeSetPoint(EnhancedPosArgumentType.getPos(context, "pos"), context, 1))))
        .then(literal("pos2")
            .executes(context -> executeSetPoint(BlockPos.ofFloored(context.getSource().getPosition()).toCenterPos(), context, 2))
            .then(argument("pos", EnhancedPosArgumentType.posPreferringCenteredInt())
                .executes(context -> executeSetPoint(EnhancedPosArgumentType.getPos(context, "pos"), context, 2))))
        .then(literal("type")
            .then(argument("type", omittedRegistryEntry(registryAccess, RegionSelectionType.REGISTRY_KEY))
                .executes(context -> {
                  final ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                  final RegistryEntry.Reference<RegionSelectionType> registryEntry = RegistryEntryReferenceArgumentType.getRegistryEntry(context, "type", RegionSelectionType.REGISTRY_KEY);
                  final RegionSelectionType type = registryEntry.value();
                  player.switchRegionSelectionType$ec(type);
                  context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.regionselection.changed", TextUtil.literal(registryEntry.registryKey().getValue()).styled(Styles.RESULT)), true);
                  return 1;
                }))));
    dispatcher.register(literalR2("rs")
        .executes(executesWithoutParam)
        .redirect(regionselection));
  }

  public static int executeSetPoint(Vec3d pos, CommandContext<ServerCommandSource> context, int type) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final ServerPlayerEntity player = source.getPlayerOrThrow();
    final RegionSelection regionSelection = player.getOrResetRegionSelection$ec();
    final Supplier<Text> textSupplier = switch (type) {
      case 1 -> regionSelection.clickFirstPoint(pos, player);
      case 2 -> regionSelection.clickSecondPoint(pos, player);
      default -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
    };
    player.syncActiveRegion$ec();
    source.sendFeedback$ecBridge(textSupplier, true);
    return 1;
  }
}
