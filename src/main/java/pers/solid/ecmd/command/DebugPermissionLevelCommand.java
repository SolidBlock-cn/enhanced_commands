package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import pers.solid.ecmd.mixins.accessor.ServerCommandSourceAccessor;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

public enum DebugPermissionLevelCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(Commands.literal("debug:permissionlevel")
        .executes(context -> {
          final CommandSourceStack source = context.getSource();
          final int level = ((ServerCommandSourceAccessor) source).getPermissionLevel();
          source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.debug:permissionlevel.result", TextUtil.literal(level).withStyle(Styles.RESULT)), false);
          return level;
        })
        .then(Commands.argument("level", IntegerArgumentType.integer())
            .redirect(dispatcher.getRoot(), context -> context.getSource().withPermission(IntegerArgumentType.getInteger(context, "level")))));
  }
}
