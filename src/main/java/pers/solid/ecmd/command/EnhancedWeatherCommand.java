package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.level.ServerWorldProperties;
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.ecmd.mixins.accessor.ServerWorldAccessor;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.bridge.CommandBridge;

import static net.minecraft.server.command.CommandManager.literal;

public enum EnhancedWeatherCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
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

  private static int executeQuery(ServerCommandSource source, byte returns) {
    final ServerWorld world = source.getWorld();
    final ServerWorldProperties properties = ((ServerWorldAccessor) world).getWorldProperties();
    CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.weather.query", describeWeather(properties.isRaining(), properties.isThundering()).styled(Styles.RESULT), properties.getClearWeatherTime(), properties.getRainTime(), properties.getThunderTime()), false);
    return switch (returns) {
      case 1 -> properties.getClearWeatherTime();
      case 2 -> properties.getRainTime();
      case 3 -> properties.getThunderTime();
      case -1 -> BooleanUtils.toInteger(properties.isRaining());
      case -2 -> BooleanUtils.toInteger(properties.isThundering());
      default -> 1;
    };
  }

  private static MutableText describeWeather(boolean raining, boolean thundering) {
    if (raining) {
      if (thundering) {
        return Text.translatable("enhanced_commands.commands.weather.query.thunder");
      } else {
        return Text.translatable("enhanced_commands.commands.weather.query.rain");
      }
    } else {
      if (thundering) {
        return Text.translatable("enhanced_commands.commands.weather.query.thunder_without_rain");
      } else {
        return Text.translatable("enhanced_commands.commands.weather.query.clear");
      }
    }
  }
}
