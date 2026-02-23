package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.ecmd.mixins.accessor.ServerWorldAccessor;
import pers.solid.ecmd.util.Styles;

import static net.minecraft.commands.Commands.literal;

public enum EnhancedWeatherCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    dispatcher.register(literal("weather")
        .executes(context -> executeQuery(context.getSource(), (byte) 0))
        .then(literal("query")
            .executes(context -> executeQuery(context.getSource(), (byte) 0))
            .then(literal("clear_time")
                .executes(context -> executeQuery(context.getSource(), (byte) 1)))
            .then(literal("rain_time")
                .executes(context -> executeQuery(context.getSource(), (byte) 2)))
            .then(literal("thunder_time")
                .executes(context -> executeQuery(context.getSource(), (byte) 1)))));
  }

  private static int executeQuery(CommandSourceStack source, byte returns) {
    final ServerLevel world = source.getLevel();
    final ServerLevelData properties = ((ServerWorldAccessor) world).getServerLevelData();
    source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.weather.query", describeWeather(properties.isRaining(), properties.isThundering()).withStyle(Styles.RESULT), properties.getClearWeatherTime(), properties.getRainTime(), properties.getThunderTime()), false);
    return switch (returns) {
      case 1 -> properties.getClearWeatherTime();
      case 2 -> properties.getRainTime();
      case 3 -> properties.getThunderTime();
      case -1 -> BooleanUtils.toInteger(properties.isRaining());
      case -2 -> BooleanUtils.toInteger(properties.isThundering());
      default -> 1;
    };
  }

  private static MutableComponent describeWeather(boolean raining, boolean thundering) {
    if (raining) {
      if (thundering) {
        return Component.translatable("enhanced_commands.commands.weather.query.thunder");
      } else {
        return Component.translatable("enhanced_commands.commands.weather.query.rain");
      }
    } else {
      if (thundering) {
        return Component.translatable("enhanced_commands.commands.weather.query.thunder_without_rain");
      } else {
        return Component.translatable("enhanced_commands.commands.weather.query.clear");
      }
    }
  }
}
