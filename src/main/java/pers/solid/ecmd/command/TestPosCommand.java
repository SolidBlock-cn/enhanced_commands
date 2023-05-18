package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;

public enum TestPosCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    final LiteralArgumentBuilder<ServerCommandSource> literal = CommandManager.literal("testpos");
    for (final var value : EnhancedPosArgumentType.Behavior.values()) {
      literal.then(CommandManager.literal(value.name().toLowerCase())
          .then(CommandManager.argument("pos", new EnhancedPosArgumentType(value, false))
              .executes(context -> {
                final PosArgument pos = EnhancedPosArgumentType.getPos("pos", context);
                final Vec3d absolutePos = pos.toAbsolutePos(context.getSource());
                context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.testpos.result"), true);
                context.getSource().sendFeedback(Text.literal(String.format(" x = %s\n y = %s\n z = %s", absolutePos.x, absolutePos.y, absolutePos.z)).formatted(Formatting.GRAY), true);
                return 1;
              })));
    }
    dispatcher.register(literal);
  }
}
