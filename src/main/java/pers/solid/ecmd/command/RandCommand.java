package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * <p>The command used to generate random values. Some usages:</p>
 * <ul>
 *   <li><code>/rand</code> - rand float value between 0 and 1.</li>
 *   <li><code>/rand boolean</code> - rand boolean with 0.5 probability of true.</li>
 *   <li><code>/rand boolean <var>probability</var></code> - rand boolean with a specified probability of true.</li>
 *   <li><code>/rand float</code> - rand float value between 0 and 1.</li>
 *   <li><code>/rand float <var>max</var></code> - rand float value between 0 and <var>max</var>.</li>
 *   <li><code>/rand float <var>min</var> <var>max</var></code> - rand float value between <var>min</var> and <var>max</var>.</li>
 *   <li><code>/rand int</code> - rand int value between 0 and 15.</li>
 *   <li><code>/rand int <var>max</var></code> - rand int value between 0 and <var>max</var>.</li>
 *   <li><code>/rand int <var>min</var> <var>max</var></code> - rand int value between <var>min</var> and <var>max</var>.</li>
 */
public enum RandCommand implements CommandRegistrationCallback {
  INSTANCE;
  public static final Dynamic2CommandExceptionType MIN_MAX_WRONG = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.commands.rand.min_max_wrong", a, b));

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literal("rand")
        .executes(context -> executeRandFloat(context, 0, 1))
        .then(literal("int")
            .executes(context -> executeRandInt(context, 0, 15))
            .then(argument("max", IntegerArgumentType.integer(0))
                .executes(context -> executeRandInt(context, 0, getInteger(context, "max"))))
            .then(argument("min", IntegerArgumentType.integer(0))
                .then(argument("max", IntegerArgumentType.integer(0))
                    .executes(context -> executeRandInt(context, getInteger(context, "min"), getInteger(context, "max"))))))
        .then(literal("float")
            .executes(context -> executeRandFloat(context, 0, 1))
            .then(argument("max", floatArg(0))
                .executes(context -> executeRandFloat(context, 0, getFloat(context, "max"))))
            .then(argument("min", floatArg(0))
                .then(argument("max", floatArg(0))
                    .executes(context -> executeRandFloat(context, getFloat(context, "min"), getFloat(context, "max"))))))
        .then(literal("boolean")
            .executes(RandCommand::executeRandBoolean)
            .then(argument("probability", floatArg(0, 1))
                .executes(context -> executeRandBoolean(context, getFloat(context, "probability"))))));
  }

  private static int executeRandBoolean(CommandContext<ServerCommandSource> context) {
    final Random random = context.getSource().getWorld().getRandom();
    final boolean value = random.nextBoolean();
    CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.rand.boolean." + random.nextInt(10), Text.literal(Boolean.toString(value)).styled(TextUtil.STYLE_FOR_RESULT)), true);
    return BooleanUtils.toInteger(value);
  }

  private static int executeRandBoolean(CommandContext<ServerCommandSource> context, float probabilityOfTrue) {
    final Random random = context.getSource().getWorld().getRandom();
    final boolean value = random.nextFloat() < probabilityOfTrue;
    CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.rand.boolean_with_probability." + random.nextInt(10), TextUtil.literal(probabilityOfTrue), Text.literal(Boolean.toString(value)).styled(TextUtil.STYLE_FOR_RESULT)), true);
    return BooleanUtils.toInteger(value);
  }

  private static int executeRandFloat(CommandContext<ServerCommandSource> context, float min, float max) throws CommandSyntaxException {
    if (min > max) {
      throw MIN_MAX_WRONG.create(min, max);
    }
    final Random random = context.getSource().getWorld().getRandom();
    final float value = min + (max - min) * random.nextFloat();
    CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.rand.number." + random.nextInt(10), Float.toString(min), Float.toString(max), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_RESULT)), true);
    return (int) value;
  }

  private static int executeRandInt(CommandContext<ServerCommandSource> context, int min, int max) throws CommandSyntaxException {
    if (min > max) {
      throw MIN_MAX_WRONG.create(min, max);
    }
    final Random random = context.getSource().getWorld().getRandom();
    final int value = random.nextBetween(min, max);
    CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.rand.number." + random.nextInt(10), Integer.toString(min), Integer.toString(max), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_RESULT)), true);
    return value;
  }
}
