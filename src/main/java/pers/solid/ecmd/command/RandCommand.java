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
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.function.FailableConsumer;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.mixins.accessor.CommandContextAccessor;
import pers.solid.ecmd.util.Styles;
import pers.solid.ecmd.util.TextUtil;

import java.util.Map;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.NbtPathArgument.nbtPath;
import static pers.solid.ecmd.argument.NbtTargetArgumentType.nbtTarget;
import static pers.solid.ecmd.command.ModCommands.consumerOf;
import static pers.solid.ecmd.command.ModCommands.literalR2;

/**
 * <p>The command used to generate random valueNames. Some usages:</p>
 * <ul>
 *   <li><code>/rand</code> - rand float probability between 0 and 1.</li>
 *   <li><code>/rand boolean</code> - rand boolean with 0.5 probability of true.</li>
 *   <li><code>/rand boolean <var>probability</var></code> - rand boolean with a specified probability of true.</li>
 *   <li><code>/rand float</code> - rand float probability between 0 and 1.</li>
 *   <li><code>/rand float <var>max</var></code> - rand float probability between 0 and <var>max</var>.</li>
 *   <li><code>/rand float <var>min</var> <var>max</var></code> - rand float probability between <var>min</var> and <var>max</var>.</li>
 *   <li><code>/rand int</code> - rand int probability between 0 and 15.</li>
 *   <li><code>/rand int <var>max</var></code> - rand int probability between 0 and <var>max</var>.</li>
 *   <li><code>/rand int <var>min</var> <var>max</var></code> - rand int probability between <var>min</var> and <var>max</var>.</li>
 */
public enum RandCommand implements CommandRegistrationCallback {
  INSTANCE;
  public static final Dynamic2CommandExceptionType MIN_MAX_WRONG = new Dynamic2CommandExceptionType((a, b) -> Component.translatable("enhanced_commands.commands.rand.min_max_wrong", a, b));

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
    dispatcher.register(appendStoreArguments(literal("rand")
        .executes(context -> executeRandFloat(context, 0, 1))
        .then(literal("int")
            .executes(context -> executeRandInt(context, 0, 15))
            .then(argument("max", integer(0))
                .executes(context -> executeRandInt(context, 0, getInteger(context, "max"))))
            .then(argument("min", integer())
                .then(argument("max", integer())
                    .executes(context -> executeRandInt(context, getInteger(context, "min"), getInteger(context, "max"))))))
        .then(literal("float")
            .executes(context -> executeRandFloat(context, 0, 1))
            .then(argument("max", floatArg(0))
                .executes(context -> executeRandFloat(context, 0, getFloat(context, "max"))))
            .then(argument("min", floatArg())
                .then(argument("max", floatArg())
                    .executes(context -> executeRandFloat(context, getFloat(context, "min"), getFloat(context, "max"))))))
        .then(literal("boolean")
            .executes(RandCommand::executeRandBoolean)
            .then(literalR2("store")
                .then(argument("target", nbtTarget(registryAccess))
                    .then(argument("path", nbtPath())
                        .executes(context -> executeRandBoolean(context, consumerOf(context))))))
            .then(argument("probability", floatArg(0, 1))
                .executes(context -> executeRandBoolean(context, getFloat(context, "probability"))))), registryAccess));
  }

  private static int executeRandBoolean(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeRandBoolean(context, optionalNbtConsumer(context));
  }

  private static <T extends Throwable> int executeRandBoolean(CommandContext<CommandSourceStack> context, @Nullable FailableConsumer<Tag, T> nbtConsumer) throws T {
    final RandomSource random = context.getSource().getLevel().getRandom();
    final boolean value = random.nextBoolean();
    context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.rand.boolean." + random.nextInt(10), Component.literal(Boolean.toString(value)).withStyle(Styles.RESULT)), false);
    if (nbtConsumer != null) {
      nbtConsumer.accept(ByteTag.valueOf(value));
    }
    return BooleanUtils.toInteger(value);
  }

  private static int executeRandBoolean(CommandContext<CommandSourceStack> context, float probabilityOfTrue) throws CommandSyntaxException {
    return executeRandBoolean(context, probabilityOfTrue, optionalNbtConsumer(context));
  }

  private static <T extends Throwable> int executeRandBoolean(CommandContext<CommandSourceStack> context, float probabilityOfTrue, @Nullable FailableConsumer<Tag, T> nbtConsumer) throws T {
    final RandomSource random = context.getSource().getLevel().getRandom();
    final boolean value = random.nextFloat() < probabilityOfTrue;
    context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.rand.boolean_with_probability." + random.nextInt(10), TextUtil.literal(probabilityOfTrue), Component.literal(Boolean.toString(value)).withStyle(Styles.RESULT)), false);
    if (nbtConsumer != null) {
      nbtConsumer.accept(ByteTag.valueOf(value));
    }
    return BooleanUtils.toInteger(value);
  }

  private static int executeRandFloat(CommandContext<CommandSourceStack> context, float min, float max) throws CommandSyntaxException {
    return executeRandFloat(context, min, max, optionalNbtConsumer(context));
  }

  private static <T extends Throwable> int executeRandFloat(CommandContext<CommandSourceStack> context, float min, float max, @Nullable FailableConsumer<Tag, T> nbtConsumer) throws CommandSyntaxException, T {
    if (min > max) {
      throw MIN_MAX_WRONG.create(min, max);
    }
    final RandomSource random = context.getSource().getLevel().getRandom();
    final float value = min + (max - min) * random.nextFloat();
    context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.rand.number." + random.nextInt(10), Float.toString(min), Float.toString(max), TextUtil.literal(value).withStyle(Styles.RESULT)), false);
    if (nbtConsumer != null) {
      nbtConsumer.accept(FloatTag.valueOf(value));
    }
    return (int) value;
  }

  private static int executeRandInt(CommandContext<CommandSourceStack> context, int min, int max) throws CommandSyntaxException {
    return executeRandInt(context, min, max, optionalNbtConsumer(context));
  }

  private static <T extends Throwable> int executeRandInt(CommandContext<CommandSourceStack> context, int min, int max, @Nullable FailableConsumer<Tag, T> nbtConsumer) throws CommandSyntaxException, T {
    if (min > max) {
      throw MIN_MAX_WRONG.create(min, max);
    }
    final RandomSource random = context.getSource().getLevel().getRandom();
    final int value = random.nextIntBetweenInclusive(min, max);
    context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.rand.number." + random.nextInt(10), Integer.toString(min), Integer.toString(max), TextUtil.literal(value).withStyle(Styles.RESULT)), false);
    if (nbtConsumer != null) {
      nbtConsumer.accept(IntTag.valueOf(value));
    }
    return value;
  }

  private static <A extends ArgumentBuilder<CommandSourceStack, ?>> A appendStoreArguments(A argumentBuilder, CommandBuildContext registryAccess) {
    for (CommandNode<CommandSourceStack> node : argumentBuilder.getArguments()) {
      appendStoreArguments(node, registryAccess);
    }
    return argumentBuilder;
  }

  private static <N extends CommandNode<CommandSourceStack>> N appendStoreArguments(N node, CommandBuildContext registryAccess) {
    for (CommandNode<CommandSourceStack> child : node.getChildren()) {
      appendStoreArguments(child, registryAccess);
    }
    final Command<CommandSourceStack> command = node.getCommand();
    if (command != null) {
      node.addChild(literal("store")
          .then(argument("target", nbtTarget(registryAccess))
              .then(argument("path", nbtPath())
                  .executes(command))).build());
    }
    return node;
  }

  @SuppressWarnings("unchecked")
  private static @Nullable FailableConsumer<Tag, CommandSyntaxException> optionalNbtConsumer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final Map<String, ParsedArgument<CommandSourceStack, ?>> arguments = ((CommandContextAccessor<CommandSourceStack>) context).getArguments();
    if (arguments.containsKey("target") && arguments.containsKey("path")) {
      return consumerOf(context);
    } else {
      return null;
    }
  }
}
