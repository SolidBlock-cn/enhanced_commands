package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.nbt.NbtSource;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.predicate.block.ExecutionContext;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.command.argument.NbtPathArgumentType.getNbtPath;
import static net.minecraft.command.argument.NbtPathArgumentType.nbtPath;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.KeywordArgsArgumentType.getKeywordArgs;
import static pers.solid.ecmd.argument.NbtFunctionArgumentType.getNbtFunction;
import static pers.solid.ecmd.argument.NbtPredicateArgumentType.getNbtPredicate;
import static pers.solid.ecmd.argument.NbtSourceArgumentType.getNbtSource;
import static pers.solid.ecmd.argument.NbtSourceArgumentType.nbtSource;
import static pers.solid.ecmd.argument.NbtTargetArgumentType.getNbtTarget;
import static pers.solid.ecmd.argument.NbtTargetArgumentType.nbtTarget;
import static pers.solid.ecmd.argument.SimpleEnumArgumentType.nbtConcentrationType;

public enum NbtCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> commandDispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
    final KeywordArgsArgumentType transformKeywordArgs = KeywordArgsArgumentType.builder()
        .addOptionalArg("affect_only", BlockPredicateArgumentType.blockPredicate(commandRegistryAccess), null)
        .addOptionalArg("recursively", BoolArgumentType.bool(), false)
        .build();

    commandDispatcher.register(literal("nbt")
        .then(literal("get")
            .then(argument("source", nbtSource(commandRegistryAccess))
                .executes(context -> executeGet(getNbtSource(context, "source"), NbtConcentrationType.ALL, context))
                .then(argument("path", nbtPath())
                    .executes(context -> executeGetInPath(getNbtSource(context, "source"), getNbtPath(context, "path"), NbtConcentrationType.ALL, 1, context))
                    .then(argument("keyword_args", KeywordArgsArgumentType.builder()
                        .addOptionalArg("scale", doubleArg(), 1d)
                        .addOptionalArg("concentration_type", nbtConcentrationType(), NbtConcentrationType.ALL)
                        .build())
                        .executes(context -> {
                          final KeywordArgs keywordArgs = getKeywordArgs(context, "keyword_args");
                          return executeGetInPath(getNbtSource(context, "source"), getNbtPath(context, "path"), keywordArgs.getArg("concentration_type"), keywordArgs.getDouble("scale"), context);
                        })))))
        .then(literal("set")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("path", nbtPath())
                    .then(argument("nbt_function", NbtFunctionArgumentType.element(commandRegistryAccess))
                        .executes(context -> executeSet(getNbtTarget(context, "target"), getNbtPath(context, "path"), getNbtFunction(context, "nbt_function"), context))))))
        .then(literal("merge")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("nbt_function", NbtFunctionArgumentType.compound(commandRegistryAccess))
                    .executes(context -> executeMerge(getNbtTarget(context, "target"), getNbtFunction(context, "nbt_function"), context)))))
        .then(literal("replace")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("nbt_predicate", NbtPredicateArgumentType.element(commandRegistryAccess))
                    .then(argument("nbt_function", NbtFunctionArgumentType.element(commandRegistryAccess))
                        .executes(context -> executeReplace(getNbtTarget(context, "target"), getNbtPredicate(context, "nbt_predicate"), getNbtFunction(context, "nbt_function"), context))))))
        .then(literal("string")
            .then(literal("replace")
                .then(argument("target", nbtTarget(commandRegistryAccess))
                    .then(argument("path", nbtPath())
                        .then(argument("targetString", string())
                            .then(argument("replacement", string())
                                .executes(context -> executeStringReplace(context, transformKeywordArgs.defaultArgs()))
                                .then(argument("keyword_args", transformKeywordArgs)
                                    .executes(context -> executeStringReplace(context, getKeywordArgs(context, "keyword_args"))))))))))
        .then(literal("regex")
            .then(literal("replace")
                .then(argument("target", nbtTarget(commandRegistryAccess))
                    .then(argument("path", nbtPath())
                        .then(argument("regex", RegexArgumentType.REGEX)
                            .then(argument("replacement", string())
                                .executes(context -> executeStringRegexReplace(context, transformKeywordArgs.defaultArgs()))
                                .then(argument("keyword_args", transformKeywordArgs)
                                    .executes(context -> executeStringRegexReplace(context, getKeywordArgs(context, "keyword_args")))))))))));
  }

  private static int executeGet(NbtSource<?> nbtSource, NbtConcentrationType nbtConcentrationType, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    return nbtSource.executeQuery(source, null, 1, nbtConcentrationType, source.getWorld().getRandom());
  }

  private static int executeGetInPath(NbtSource<?> nbtSource, NbtPathArgumentType.NbtPath nbtPath, NbtConcentrationType nbtConcentrationType, double scale, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    return nbtSource.executeQuery(source, nbtPath, scale, nbtConcentrationType, source.getWorld().getRandom());
  }

  private static <T> int executeSet(NbtTarget<T> target, NbtPathArgumentType.NbtPath nbtPath, NbtFunction nbtFunction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final Collection<T> values = target.values(source);
    for (T value : values) {
      target.transformNbtInPathFor(source, value, nbtPath, nbtFunction.asJavaFunction(new ExecutionContext(source)));
    }
    source.sendFeedback$ecBridge(() -> target.feedbackModify(values), true);
    return 1; // 应该修改为执行成功数量
  }

  private static <T> int executeMerge(NbtTarget<T> target, NbtFunction nbtFunction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final Collection<T> values = target.values(source);
    for (T value : values) {
      target.transformNbtFor(context.getSource(), value, nbtCompound -> nbtFunction.apply(nbtCompound, new ExecutionContext(source)) instanceof final NbtCompound newCompound ? newCompound : nbtCompound);
    }
    source.sendFeedback$ecBridge(() -> target.feedbackModify(values), true);
    return 1; // 应该修改为执行成功数量
  }

  private static <T> int executeReplace(NbtTarget<T> target, NbtPredicate nbtPredicate, NbtFunction nbtFunction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final Collection<T> values = target.values(source);
    for (T value : values) {
      target.transformNbtFor(source, value, nbtCompound -> nbtFunction.recursivelyApply(nbtCompound, nbtPredicate, new ExecutionContext(source)) instanceof final NbtCompound newCompound ? newCompound : nbtCompound);
    }
    source.sendFeedback$ecBridge(() -> target.feedbackModify(values), true);
    return 1; // 应该修改为执行成功数量
  }

  private int executeTransform(NbtTarget<?> target, NbtPathArgumentType.NbtPath path, FailableFunction<@Nullable NbtElement, @Nullable NbtElement, CommandSyntaxException> operation, Supplier<Text> message, boolean recursively, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    if (recursively) {
      target.transformNbtInPath(source, path, nbtElement -> NbtFunction.recursivelyApply(operation, nbtElement, null));
    } else {
      target.transformNbtInPath(source, path, operation);
    }
    source.sendFeedback$ecBridge(message, true);
    return 1;
  }

  private int executeStringReplace(CommandContext<ServerCommandSource> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final String targetString = StringArgumentType.getString(context, "targetString");
    final String replacement = StringArgumentType.getString(context, "replacement");
    final MutableInt mutableInt = new MutableInt();
    return executeTransform(NbtTargetArgumentType.getNbtTarget(context, "target"), getNbtPath(context, "path"), nbtElement -> {
      if (nbtElement instanceof NbtString nbtString) {
        final String original = nbtString.asString();
        final String replaced = original.replace(targetString, replacement);
        if (!replaced.equals(original)) {
          mutableInt.increment();
          return NbtString.of(replaced);
        }
      }
      return null;
    }, () -> Text.translatable("enhanced_commands.commands.nbt.string.replace.success", mutableInt.toString()), keywordArgs.getBoolean("recursively"), context);
  }

  private int executeStringRegexReplace(CommandContext<ServerCommandSource> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final Pattern pattern = RegexArgumentType.getRegex(context, "regex");
    final String replacement = StringArgumentType.getString(context, "replacement");
    final MutableInt mutableInt = new MutableInt();
    return executeTransform(NbtTargetArgumentType.getNbtTarget(context, "target"), getNbtPath(context, "path"), nbtElement -> {
      if (nbtElement instanceof NbtString nbtString) {
        final String original = nbtString.asString();
        try {
          final String replaced = pattern.matcher(original).replaceAll(replacement);
          if (!replaced.equals(original)) {
            mutableInt.increment();
            return NbtString.of(replaced);
          }
        } catch (RuntimeException e) {
          EnhancedCommands.LOGGER.debug("An error occurred when regex-replacing NBT:", e);
          return null;
        }
      }
      return null;
    }, () -> Text.translatable("enhanced_commands.commands.nbt.string.replace.success", mutableInt.toString()), keywordArgs.getBoolean("recursively"), context);
  }
}
