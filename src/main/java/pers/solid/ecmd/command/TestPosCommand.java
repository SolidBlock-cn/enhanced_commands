package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.VanillaWrappedArgumentType;
import pers.solid.ecmd.util.bridge.CommandBridge;

public enum TestPosCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final LiteralArgumentBuilder<ServerCommandSource> literal = CommandManager.literal("testpos");
    final Command<ServerCommandSource> execution = context -> {
      final PosArgument pos = EnhancedPosArgumentType.getPos(context, "pos");
      final Vec3d absolutePos = pos.toAbsolutePos(context.getSource());
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.testpos.result"), true);
      CommandBridge.sendFeedback(context, () -> Text.literal(String.format(" x = %s\n y = %s\n z = %s", absolutePos.x, absolutePos.y, absolutePos.z)).formatted(Formatting.GRAY), true);
      return 1;
    };
    for (final var value : EnhancedPosArgumentType.Behavior.values()) {
      literal.then(CommandManager.literal(value.name().toLowerCase())
          .then(CommandManager.argument("pos", new EnhancedPosArgumentType(value, false))
              .executes(execution)));
    }

    // 由于传入客户端的数据包并不会告知这个参数类型是强制使用了原版的，因此需要在这里手动指定 suggestionProvider
    literal.then(CommandManager.literal("vanilla_vec3")
            .then(CommandManager.argument("pos", new VanillaWrappedArgumentType<>(new Vec3ArgumentType(true)))
                .executes(execution)))
        .then(CommandManager.literal("vanilla_vec3_accurate")
            .then(CommandManager.argument("pos", new VanillaWrappedArgumentType<>(new Vec3ArgumentType(false)))
                .executes(execution)))
        .then(CommandManager.literal("vanilla_block_pos")
            .then(CommandManager.argument("pos", new VanillaWrappedArgumentType<>(new BlockPosArgumentType()))
                .executes(execution)));
    dispatcher.register(literal);
  }
}
