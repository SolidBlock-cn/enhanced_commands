package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.regionselection.RegionSelection;
import pers.solid.ecmd.regionselection.RegionSelectionType;
import pers.solid.ecmd.regionselection.WandEvent;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static pers.solid.ecmd.argument.OmittedRegistryEntryArgumentType.omittedRegistryEntry;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum RegionSelectionCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    final Command<CommandSourceStack> executesWithoutParam = context -> {
      final ServerPlayer player = context.getSource().getPlayerOrException();
      player.addItem(WandEvent.createWandStack());
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.regionselection.build_now", Component.keybind("key.attack").withStyle(ChatFormatting.GRAY), Component.keybind("key.use").withStyle(ChatFormatting.GRAY)), false);
      return 1;
    };
    final LiteralCommandNode<CommandSourceStack> regionselection
        = dispatcher.register(literalR2("regionselection")
        .executes(executesWithoutParam)
        .then(literal("pos1")
            .executes(context -> executeSetPoint(BlockPos.containing(context.getSource().getPosition()).getCenter(), context, 1))
            .then(argument("pos", EnhancedPosArgumentType.posPreferringCenteredInt())
                .executes(context -> executeSetPoint(EnhancedPosArgumentType.getPos(context, "pos"), context, 1))))
        .then(literal("pos2")
            .executes(context -> executeSetPoint(BlockPos.containing(context.getSource().getPosition()).getCenter(), context, 2))
            .then(argument("pos", EnhancedPosArgumentType.posPreferringCenteredInt())
                .executes(context -> executeSetPoint(EnhancedPosArgumentType.getPos(context, "pos"), context, 2))))
        .then(literal("type")
            .then(argument("type", omittedRegistryEntry(commandBuildContext, RegionSelectionType.REGISTRY_KEY))
                .executes(context -> {
                  final ServerPlayer player = context.getSource().getPlayerOrException();
                  final Holder.Reference<RegionSelectionType> registryEntry = ResourceArgument.getResource(context, "type", RegionSelectionType.REGISTRY_KEY);
                  final RegionSelectionType type = registryEntry.value();
                  player.switchRegionSelectionType$ec(type);
                  context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.regionselection.changed", TextUtil.literal(registryEntry.key().location()).withStyle(Styles.RESULT)), true);
                  return 1;
                }))));
    dispatcher.register(literalR2("rs")
        .executes(executesWithoutParam)
        .redirect(regionselection));
  }

  public static int executeSetPoint(Vec3 pos, CommandContext<CommandSourceStack> context, int type) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final ServerPlayer player = source.getPlayerOrException();
    final RegionSelection regionSelection = player.getOrResetRegionSelection$ec();
    final Supplier<Component> textSupplier = switch (type) {
      case 1 -> regionSelection.clickFirstPoint(pos, player);
      case 2 -> regionSelection.clickSecondPoint(pos, player);
      default -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
    };
    player.syncActiveRegion$ec();
    source.sendFeedback$ecBridge(textSupplier, true);
    return 1;
  }
}
