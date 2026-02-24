package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableInt;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.nbt.*;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.nbt.NbtSource;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.util.ExecutionContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.NbtPathArgument.getPath;
import static net.minecraft.commands.arguments.NbtPathArgument.nbtPath;
import static pers.solid.ecmd.argument.KeywordArgsArgument.getKeywordArgs;
import static pers.solid.ecmd.argument.NbtFunctionArgument.getNbtFunction;
import static pers.solid.ecmd.argument.NbtPredicateArgument.getNbtPredicate;
import static pers.solid.ecmd.argument.NbtSourceArgument.getNbtSource;
import static pers.solid.ecmd.argument.NbtSourceArgument.nbtSource;
import static pers.solid.ecmd.argument.NbtTargetArgument.getNbtTarget;
import static pers.solid.ecmd.argument.NbtTargetArgument.nbtTarget;
import static pers.solid.ecmd.argument.SimpleEnumArgument.nbtConcentrationType;

public enum NbtCommand implements CommandRegistrationCallback {
  INSTANCE;

  private static int executeGet(NbtSource<?> nbtSource, NbtConcentrationType nbtConcentrationType, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    return nbtSource.executeQuery(source, null, 1, nbtConcentrationType, source.getLevel().getRandom());
  }

  private static int executeGetInPath(NbtSource<?> nbtSource, NbtPathArgument.NbtPath nbtPath, NbtConcentrationType nbtConcentrationType, double scale, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    return nbtSource.executeQuery(source, nbtPath, scale, nbtConcentrationType, source.getLevel().getRandom());
  }

  private static <T> int executeSet(NbtTarget<T> target, NbtPathArgument.NbtPath nbtPath, NbtFunction nbtFunction, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final Collection<T> values = target.values(source);
    final ExecutionContext executionContext = new ExecutionContext(source);
    for (T value : values) {
      target.transformNbtInPathFor(source, value, nbtPath, nbtFunction.asJavaFunction(executionContext));
    }
    source.sendFeedback$ecBridge(() -> target.feedbackModify(values), true);
    return 1; // 应该修改为执行成功数量
  }

  private static <T> int executeMerge(NbtTarget<T> target, NbtFunction nbtFunction, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final Collection<T> values = target.values(source);
    final ExecutionContext executionContext = new ExecutionContext(source);
    final Collection<T> successValues = new ArrayList<>();
    for (T value : values) {
      target.transformNbtFor(context.getSource(), value, nbtCompound -> {
        final CompoundTag old = nbtCompound.copy();
        final CompoundTag applied = nbtFunction.apply(nbtCompound, executionContext) instanceof final CompoundTag newCompound ? newCompound : nbtCompound;
        if (!applied.equals(old)) {
          successValues.add(value);
        }
        return applied;
      });
    }
    source.sendFeedback$ecBridge(() -> target.feedbackModify(successValues), true);
    return successValues.size(); // 应该修改为执行成功数量
  }

  private static int executeApply(NbtFunction nbtFunction, IntFunction<Component> message, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeApply(getNbtTarget(context, "target"), getPath(context, "path"), nbtFunction, message, context);
  }

  private static <T> int executeApply(NbtTarget<T> target, NbtPathArgument.NbtPath nbtPath, NbtFunction nbtFunction, IntFunction<Component> message, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final Collection<T> values = target.values(source);
    final ExecutionContext executionContext = new ExecutionContext(source);
    final MutableInt success = new MutableInt();
    for (T value : values) {
      target.transformNbtInPathFor(source, value, nbtPath, nbtElement -> {
        final Tag old = nbtElement.copy();
        final Tag applied = nbtFunction.apply(nbtElement, executionContext);
        if (!applied.equals(old)) {
          success.increment();
          return applied;
        }
        return nbtElement;
      });
    }
    source.sendFeedback$ecBridge(() -> message.apply(success.intValue()), true);
    return success.intValue();
  }

  private static <T> int executeReplace(NbtTarget<T> target, NbtPredicate nbtPredicate, NbtFunction nbtFunction, CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    return executeApply(new ReplaceNbtFunction(nbtPredicate, nbtFunction), success -> Component.translatable("enhanced_commands.commands.nbt.replace.success", success), context);
  }

  private static int executeStringReplace(CommandContext<CommandSourceStack> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final String targetString = StringArgumentType.getString(context, "targetString");
    final String replacement = StringArgumentType.getString(context, "replacement");
    final StringReplaceNbtFunction nbtFunction = new StringReplaceNbtFunction(targetString, replacement, keywordArgs.getBoolean("recursive"), keywordArgs.getBoolean("lenient"), Optional.empty());
    return executeApply(nbtFunction, success -> Component.translatable("enhanced_commands.commands.nbt.string_replace.success", success), context);
  }

  private static int executeRegexReplace(CommandContext<CommandSourceStack> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final Pattern pattern = RegexArgument.getRegex(context, "regex");
    final String replacement = StringArgumentType.getString(context, "replacement");
    final RegexReplaceNbtFunction nbtFunction = new RegexReplaceNbtFunction(pattern, replacement, keywordArgs.getBoolean("recursive"), keywordArgs.getBoolean("lenient"), Optional.empty());
    return executeApply(nbtFunction, success -> Component.translatable("enhanced_commands.commands.nbt.regex_replace.success", success), context);
  }

  private static int executeSubstring(Optional<Integer> endIndex, CommandContext<CommandSourceStack> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final int startIndex = getInteger(context, "start_index");
    final SubstringNbtFunction nbtFunction = new SubstringNbtFunction(startIndex, endIndex, keywordArgs.getBoolean("lenient"), Optional.empty());
    return executeApply(nbtFunction, success -> Component.translatable("enhanced_commands.commands.nbt.substring.success", success), context);
  }

  private static <T> int executeRemove(NbtTarget<T> target, NbtPathArgument.NbtPath nbtPath, CommandSourceStack source) throws CommandSyntaxException {
    final MutableInt success = new MutableInt();
    target.transformNbt(source, input -> {
      final int remove = nbtPath.remove(input);
      success.add(remove);
      return input;
    });
    source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.nbt.remove.success", success.intValue()).enhanced$$(), true);
    return success.intValue();
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> commandDispatcher, CommandBuildContext commandRegistryAccess, Commands.CommandSelection registrationEnvironment) {
    final KeywordArgsArgument transformKeywordArgs = KeywordArgsArgument.builder()
        .addOptionalArg("affect_only", BlockPredicateArgument.blockPredicate(commandRegistryAccess), null)
        .addOptionalArg("recursive", BoolArgumentType.bool(), false)
        .addOptionalArg("lenient", BoolArgumentType.bool(), false)
        .build();
    final KeywordArgsArgument substringKeywordArgs = KeywordArgsArgument.builder()
        .addOptionalArg("affect_only", BlockPredicateArgument.blockPredicate(commandRegistryAccess), null)
        .addOptionalArg("lenient", BoolArgumentType.bool(), false)
        .build();

    commandDispatcher.register(literal("nbt")
        .then(literal("get")
            .then(argument("source", nbtSource(commandRegistryAccess))
                .executes(context -> executeGet(getNbtSource(context, "source"), NbtConcentrationType.ALL, context))
                .then(argument("path", nbtPath())
                    .executes(context -> executeGetInPath(getNbtSource(context, "source"), getPath(context, "path"), NbtConcentrationType.ALL, 1, context))
                    .then(argument("keyword_args", KeywordArgsArgument.builder()
                        .addOptionalArg("scale", doubleArg(), 1d)
                        .addOptionalArg("concentration_type", nbtConcentrationType(), NbtConcentrationType.ALL)
                        .build())
                        .executes(context -> {
                          final KeywordArgs keywordArgs = getKeywordArgs(context, "keyword_args");
                          return executeGetInPath(getNbtSource(context, "source"), getPath(context, "path"), keywordArgs.getArg("concentration_type"), keywordArgs.getDouble("scale"), context);
                        })))))
        .then(literal("set")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("path", nbtPath())
                    .then(argument("nbt_function", NbtFunctionArgument.element(commandRegistryAccess))
                        .executes(context -> executeSet(getNbtTarget(context, "target"), getPath(context, "path"), getNbtFunction(context, "nbt_function"), context))))))
        .then(literal("merge")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("nbt_function", NbtFunctionArgument.compound(commandRegistryAccess))
                    .executes(context -> executeMerge(getNbtTarget(context, "target"), getNbtFunction(context, "nbt_function"), context)))))
        .then(literal("replace")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("path", nbtPath())
                    .then(argument("nbt_predicate", NbtPredicateArgument.element(commandRegistryAccess))
                        .then(argument("nbt_function", NbtFunctionArgument.element(commandRegistryAccess))
                            .executes(context -> executeReplace(getNbtTarget(context, "target"), getNbtPredicate(context, "nbt_predicate"), getNbtFunction(context, "nbt_function"), context)))))))
        .then(literal("string_replace")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("path", nbtPath())
                    .then(argument("targetString", string())
                        .then(argument("replacement", string())
                            .executes(context -> executeStringReplace(context, transformKeywordArgs.defaultArgs()))
                            .then(argument("keyword_args", transformKeywordArgs)
                                .executes(context -> executeStringReplace(context, getKeywordArgs(context, "keyword_args")))))))))
        .then(literal("regex_replace")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("path", nbtPath())
                    .then(argument("regex", RegexArgument.REGEX)
                        .then(argument("replacement", string())
                            .executes(context -> executeRegexReplace(context, transformKeywordArgs.defaultArgs()))
                            .then(argument("keyword_args", transformKeywordArgs)
                                .executes(context -> executeRegexReplace(context, getKeywordArgs(context, "keyword_args")))))))))
        .then(literal("substring")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("path", nbtPath())
                    .then(argument("start_index", integer())
                        .executes(context -> executeSubstring(Optional.empty(), context, substringKeywordArgs.defaultArgs()))
                        .then(argument("keyword_args", substringKeywordArgs)
                            .executes(context -> executeSubstring(Optional.empty(), context, getKeywordArgs(context, "keyword_args"))))
                        .then(argument("end_index", integer())
                            .executes(context -> executeSubstring(Optional.of(getInteger(context, "end_index")), context, substringKeywordArgs.defaultArgs()))
                            .then(argument("keyword_args", substringKeywordArgs)
                                .executes(context -> executeSubstring(Optional.of(getInteger(context, "end_index")), context, getKeywordArgs(context, "keyword_args")))))))))
        .then(literal("remove")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("path", nbtPath())
                    .executes(context -> executeRemove(getNbtTarget(context, "target"), getPath(context, "path"), context.getSource()))))));
  }
}
