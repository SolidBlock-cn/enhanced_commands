package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import pers.solid.ecmd.regionbuilder.RegionBuilderType;
import pers.solid.ecmd.regionbuilder.WandEvent;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.mixin.ServerPlayerEntityExtension;

public enum RegionBuilderCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(CommandManager.literal("regionbuilder")
        .executes(context -> {
          final ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
          player.giveItemStack(WandEvent.createWandStack());
          CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhancedCommands.commands.regionbuilder.build_now", Text.keybind("key.attack").formatted(Formatting.GRAY), Text.keybind("key.use").formatted(Formatting.GRAY)), true);
          return 1;
        })
        .then(CommandManager.argument("type", RegistryEntryArgumentType.registryEntry(registryAccess, RegionBuilderType.REGISTRY_KEY))
            .executes(context -> {
              final ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
              final RegistryEntry.Reference<RegionBuilderType> registryEntry = RegistryEntryArgumentType.getRegistryEntry(context, "type", RegionBuilderType.REGISTRY_KEY);
              final RegionBuilderType type = registryEntry.value();
              ((ServerPlayerEntityExtension) player).ec$switchRegionBuilderType(type);
              CommandBridge.sendFeedback(context.getSource(), () -> Text.translatable("enhancedCommands.commands.regionbuilder.changed", TextUtil.literal(registryEntry.registryKey().getValue()).styled(TextUtil.STYLE_FOR_RESULT)), true);
              return 1;
            })));
  }
}
