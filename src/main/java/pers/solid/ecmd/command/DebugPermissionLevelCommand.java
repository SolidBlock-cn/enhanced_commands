package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import pers.solid.ecmd.mixins.accessor.ServerCommandSourceAccessor;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

public enum DebugPermissionLevelCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(CommandManager.literal("debug:permissionlevel")
        .executes(context -> {
          final ServerCommandSource source = context.getSource();
          final int level = ((ServerCommandSourceAccessor) source).getLevel();
          source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.debug:permissionlevel.result", TextUtil.literal(level).styled(Styles.RESULT)), false);
          return level;
        })
        .then(CommandManager.argument("level", IntegerArgumentType.integer())
            .redirect(dispatcher.getRoot(), context -> context.getSource().withLevel(IntegerArgumentType.getInteger(context, "level")))));
  }
}
