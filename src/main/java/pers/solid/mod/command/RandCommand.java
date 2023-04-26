package pers.solid.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomUtils;
import pers.solid.mod.EnhancedCommands;

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
public final class RandCommand {
  public static final Dynamic2CommandExceptionType MIN_MAX_WRONG = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhancedCommands.rand.min_max_wrong", a, b));

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
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
    final boolean value = RandomUtils.nextBoolean();
    context.getSource().sendFeedback(Text.translatable("enhancedCommands.rand.boolean." + RandomUtils.nextInt(0, 9), Text.literal(Boolean.toString(value)).styled(EnhancedCommands.STYLE_FOR_RESULT)), true);
    return BooleanUtils.toInteger(value);
  }

  private static int executeRandBoolean(CommandContext<ServerCommandSource> context, float probabilityOfTrue) {
    final boolean value = RandomUtils.nextFloat(0, 1) < probabilityOfTrue;
    context.getSource().sendFeedback(Text.translatable("enhancedCommands.rand.boolean_with_probability." + RandomUtils.nextInt(0, 9), Text.literal(Float.toString(probabilityOfTrue)), Text.literal(Boolean.toString(value)).styled(EnhancedCommands.STYLE_FOR_RESULT)), true);
    return BooleanUtils.toInteger(value);
  }

  private static int executeRandFloat(CommandContext<ServerCommandSource> context, float min, float max) throws CommandSyntaxException {
    if (min > max) {
      throw MIN_MAX_WRONG.create(min, max);
    }
    final float value = RandomUtils.nextFloat(min, max);
    context.getSource().sendFeedback(Text.translatable("enhancedCommands.rand.number." + RandomUtils.nextInt(0, 9), Float.toString(min), Float.toString(max), Text.literal(Float.toString(value)).styled(EnhancedCommands.STYLE_FOR_RESULT)), true);
    return (int) value;
  }

  private static int executeRandInt(CommandContext<ServerCommandSource> context, int min, int max) throws CommandSyntaxException {
    if (min > max) {
      throw MIN_MAX_WRONG.create(min, max);
    }
    final int value = RandomUtils.nextInt(min, max);
    context.getSource().sendFeedback(Text.translatable("enhancedCommands.rand.number." + RandomUtils.nextInt(0, 9), Integer.toString(min), Integer.toString(max), Text.literal(Integer.toString(value)).styled(EnhancedCommands.STYLE_FOR_RESULT)), true);
    return value;
  }
}
