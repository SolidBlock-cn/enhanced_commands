package pers.solid.ecmd.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.function.FailableConsumer;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixin.CommandContextAccessor;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.Map;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.command.argument.NbtPathArgumentType.nbtPath;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.NbtTargetArgumentType.nbtTarget;
import static pers.solid.ecmd.command.ModCommands.consumerOf;

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
    dispatcher.register(appendStoreArguments(literal("rand")
        .executes(context -> executeRandFloat(context, 0, 1))
        .then(literal("int")
            .executes(context -> executeRandInt(context, 0, 15))
            .then(argument("max", integer(0))
                .executes(context -> executeRandInt(context, 0, getInteger(context, "max"))))
            .then(argument("min", integer(0))
                .then(argument("max", integer(0))
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
            .then(literal("store")
                .then(argument("target", nbtTarget(registryAccess))
                    .then(argument("path", nbtPath())
                        .executes(context -> executeRandBoolean(context, consumerOf(context))))))
            .then(argument("probability", floatArg(0, 1))
                .executes(context -> executeRandBoolean(context, getFloat(context, "probability"))))), registryAccess));
  }

  private static int executeRandBoolean(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeRandBoolean(context, optionalNbtConsumer(context));
  }

  private static <T extends Throwable> int executeRandBoolean(CommandContext<ServerCommandSource> context, @Nullable FailableConsumer<NbtElement, T> nbtConsumer) throws T {
    final Random random = context.getSource().getWorld().getRandom();
    final boolean value = random.nextBoolean();
    CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.rand.boolean." + random.nextInt(10), Text.literal(Boolean.toString(value)).styled(TextUtil.STYLE_FOR_RESULT)), false);
    if (nbtConsumer != null) {
      nbtConsumer.accept(NbtByte.of(value));
    }
    return BooleanUtils.toInteger(value);
  }

  private static int executeRandBoolean(CommandContext<ServerCommandSource> context, float probabilityOfTrue) throws CommandSyntaxException {
    return executeRandBoolean(context, probabilityOfTrue, optionalNbtConsumer(context));
  }

  private static <T extends Throwable> int executeRandBoolean(CommandContext<ServerCommandSource> context, float probabilityOfTrue, @Nullable FailableConsumer<NbtElement, T> nbtConsumer) throws T {
    final Random random = context.getSource().getWorld().getRandom();
    final boolean value = random.nextFloat() < probabilityOfTrue;
    CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.rand.boolean_with_probability." + random.nextInt(10), TextUtil.literal(probabilityOfTrue), Text.literal(Boolean.toString(value)).styled(TextUtil.STYLE_FOR_RESULT)), false);
    if (nbtConsumer != null) {
      nbtConsumer.accept(NbtByte.of(value));
    }
    return BooleanUtils.toInteger(value);
  }

  private static int executeRandFloat(CommandContext<ServerCommandSource> context, float min, float max) throws CommandSyntaxException {
    return executeRandFloat(context, min, max, optionalNbtConsumer(context));
  }

  private static <T extends Throwable> int executeRandFloat(CommandContext<ServerCommandSource> context, float min, float max, @Nullable FailableConsumer<NbtElement, T> nbtConsumer) throws CommandSyntaxException, T {
    if (min > max) {
      throw MIN_MAX_WRONG.create(min, max);
    }
    final Random random = context.getSource().getWorld().getRandom();
    final float value = min + (max - min) * random.nextFloat();
    CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.rand.number." + random.nextInt(10), Float.toString(min), Float.toString(max), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_RESULT)), false);
    if (nbtConsumer != null) {
      nbtConsumer.accept(NbtFloat.of(value));
    }
    return (int) value;
  }

  private static int executeRandInt(CommandContext<ServerCommandSource> context, int min, int max) throws CommandSyntaxException {
    return executeRandInt(context, min, max, optionalNbtConsumer(context));
  }

  private static <T extends Throwable> int executeRandInt(CommandContext<ServerCommandSource> context, int min, int max, @Nullable FailableConsumer<NbtElement, T> nbtConsumer) throws CommandSyntaxException, T {
    if (min > max) {
      throw MIN_MAX_WRONG.create(min, max);
    }
    final Random random = context.getSource().getWorld().getRandom();
    final int value = random.nextBetween(min, max);
    CommandBridge.sendFeedback(context, () -> Text.translatable("enhanced_commands.commands.rand.number." + random.nextInt(10), Integer.toString(min), Integer.toString(max), TextUtil.literal(value).styled(TextUtil.STYLE_FOR_RESULT)), false);
    if (nbtConsumer != null) {
      nbtConsumer.accept(NbtInt.of(value));
    }
    return value;
  }

  private static <A extends ArgumentBuilder<ServerCommandSource, ?>> A appendStoreArguments(A argumentBuilder, CommandRegistryAccess registryAccess) {
    for (CommandNode<ServerCommandSource> node : argumentBuilder.getArguments()) {
      appendStoreArguments(node, registryAccess);
    }
    return argumentBuilder;
  }

  private static <N extends CommandNode<ServerCommandSource>> N appendStoreArguments(N node, CommandRegistryAccess registryAccess) {
    for (CommandNode<ServerCommandSource> child : node.getChildren()) {
      appendStoreArguments(child, registryAccess);
    }
    final Command<ServerCommandSource> command = node.getCommand();
    if (command != null) {
      node.addChild(literal("store")
          .then(argument("target", nbtTarget(registryAccess))
              .then(argument("path", nbtPath())
                  .executes(command))).build());
    }
    return node;
  }

  private static @Nullable FailableConsumer<NbtElement, CommandSyntaxException> optionalNbtConsumer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final Map<String, ParsedArgument<ServerCommandSource, ?>> arguments = ((CommandContextAccessor<ServerCommandSource>) context).getArguments();
    if (arguments.containsKey("target") && arguments.containsKey("path")) {
      return consumerOf(context);
    } else {
      return null;
    }
  }
}
