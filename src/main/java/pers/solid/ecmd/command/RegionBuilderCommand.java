package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
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
import pers.solid.ecmd.argument.OmittedRegistryEntryArgumentType;
import pers.solid.ecmd.regionbuilder.RegionBuilderType;
import pers.solid.ecmd.regionbuilder.WandEvent;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum RegionBuilderCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final Command<ServerCommandSource> executesWithoutParam = context -> {
      final ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
      player.giveItemStack(WandEvent.createWandStack());
      CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhancedCommands.commands.regionbuilder.build_now", Text.keybind("key.attack").formatted(Formatting.GRAY), Text.keybind("key.use").formatted(Formatting.GRAY)), true);
      return 1;
    };
    final LiteralCommandNode<ServerCommandSource> regionBuilderNode = dispatcher.register(literalR2("regionbuilder")
        .executes(executesWithoutParam)
        .then(CommandManager.argument("type", OmittedRegistryEntryArgumentType.omittedRegistryEntry(registryAccess, RegionBuilderType.REGISTRY_KEY))
            .executes(context -> {
              final ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
              final RegistryEntry.Reference<RegionBuilderType> registryEntry = RegistryEntryArgumentType.getRegistryEntry(context, "type", RegionBuilderType.REGISTRY_KEY);
              final RegionBuilderType type = registryEntry.value();
              ((ServerPlayerEntityExtension) player).ec$switchRegionBuilderType(type);
              CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhancedCommands.commands.regionbuilder.changed", TextUtil.literal(registryEntry.registryKey().getValue()).styled(TextUtil.STYLE_FOR_RESULT)), true);
              return 1;
            })));
    dispatcher.register(literalR2("rb")
        .executes(executesWithoutParam)
        .redirect(regionBuilderNode));
  }
}
