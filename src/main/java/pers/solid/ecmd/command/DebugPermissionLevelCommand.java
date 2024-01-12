package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.mixin.ServerCommandSourceAccessor;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

public enum DebugPermissionLevelCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(CommandManager.literal("debug:permissionlevel")
        .executes(context -> {
          final ServerCommandSource source = context.getSource();
          final int level = ((ServerCommandSourceAccessor) source).getLevel();
          CommandBridge.sendFeedback(source, () -> TextUtil.literal(level).styled(Styles.RESULT), false);
          return level;
        })
        .then(CommandManager.argument("level", IntegerArgumentType.integer())
            .redirect(dispatcher.getRoot(), context -> context.getSource().withLevel(IntegerArgumentType.getInteger(context, "level")))));
  }
}
