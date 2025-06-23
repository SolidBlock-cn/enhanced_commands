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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.apache.commons.lang3.mutable.MutableInt;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.nbt.*;
import pers.solid.ecmd.math.NbtConcentrationType;
import pers.solid.ecmd.nbt.NbtSource;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.predicate.block.ExecutionContext;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
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
    final ExecutionContext executionContext = new ExecutionContext(source);
    for (T value : values) {
      target.transformNbtInPathFor(source, value, nbtPath, nbtFunction.asJavaFunction(executionContext));
    }
    source.sendFeedback$ecBridge(() -> target.feedbackModify(values), true);
    return 1; // 应该修改为执行成功数量
  }

  private static <T> int executeMerge(NbtTarget<T> target, NbtFunction nbtFunction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final Collection<T> values = target.values(source);
    final ExecutionContext executionContext = new ExecutionContext(source);
    final Collection<T> successValues = new ArrayList<>();
    for (T value : values) {
      target.transformNbtFor(context.getSource(), value, nbtCompound -> {
        final NbtCompound old = nbtCompound.copy();
        final NbtCompound applied = nbtFunction.apply(nbtCompound, executionContext) instanceof final NbtCompound newCompound ? newCompound : nbtCompound;
        if (!applied.equals(old)) {
          successValues.add(value);
        }
        return applied;
      });
    }
    source.sendFeedback$ecBridge(() -> target.feedbackModify(successValues), true);
    return successValues.size(); // 应该修改为执行成功数量
  }

  private static int executeApply(NbtFunction nbtFunction, IntFunction<Text> message, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeApply(getNbtTarget(context, "target"), getNbtPath(context, "path"), nbtFunction, message, context);
  }

  private static <T> int executeApply(NbtTarget<T> target, NbtPathArgumentType.NbtPath nbtPath, NbtFunction nbtFunction, IntFunction<Text> message, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final Collection<T> values = target.values(source);
    final ExecutionContext executionContext = new ExecutionContext(source);
    final MutableInt success = new MutableInt();
    for (T value : values) {
      target.transformNbtInPathFor(source, value, nbtPath, nbtElement -> {
        final NbtElement old = nbtElement.copy();
        final NbtElement applied = nbtFunction.apply(nbtElement, executionContext);
        if (!applied.equals(old)) {
          success.increment();
        }
        return nbtElement;
      });
    }
    source.sendFeedback$ecBridge(() -> message.apply(success.intValue()), true);
    return success.intValue();
  }

  private static <T> int executeReplace(NbtTarget<T> target, NbtPredicate nbtPredicate, NbtFunction nbtFunction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    return executeApply(new ReplaceNbtFunction(nbtPredicate, nbtFunction), success -> Text.translatable("enhanced_commands.commands.nbt.replace.success", success), context);
  }

  private static int executeStringReplace(CommandContext<ServerCommandSource> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final String targetString = StringArgumentType.getString(context, "targetString");
    final String replacement = StringArgumentType.getString(context, "replacement");
    final StringReplaceNbtFunction nbtFunction = new StringReplaceNbtFunction(targetString, replacement, keywordArgs.getBoolean("recursive"), keywordArgs.getBoolean("lenient"), Optional.empty());
    return executeApply(nbtFunction, success -> Text.translatable("enhanced_commands.commands.nbt.string_replace.success", success), context);
  }

  private static int executeRegexReplace(CommandContext<ServerCommandSource> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final Pattern pattern = RegexArgumentType.getRegex(context, "regex");
    final String replacement = StringArgumentType.getString(context, "replacement");
    final RegexReplaceNbtFunction nbtFunction = new RegexReplaceNbtFunction(pattern, replacement, keywordArgs.getBoolean("recursive"), keywordArgs.getBoolean("lenient"), Optional.empty());
    return executeApply(nbtFunction, success -> Text.translatable("enhanced_commands.commands.nbt.regex_replace.success", success), context);
  }

  private static int executeSubstring(OptionalInt endIndex, CommandContext<ServerCommandSource> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    final int startIndex = getInteger(context, "startIndex");
    final SubstringNbtFunction nbtFunction = new SubstringNbtFunction(startIndex, endIndex, keywordArgs.getBoolean("lenient"), Optional.empty());
    return executeApply(nbtFunction, success -> Text.translatable("enhanced_commands.commands.nbt.substring.success", success), context);
  }

  private static <T> int executeRemove(NbtTarget<T> target, NbtPathArgumentType.NbtPath nbtPath, ServerCommandSource source) throws CommandSyntaxException {
    final MutableInt success = new MutableInt();
    target.transformNbt(source, input -> {
      final int remove = nbtPath.remove(input);
      success.add(remove);
      return input;
    });
    source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.nbt.remove.success", success.intValue()).enhanced$$(), true);
    return success.intValue();
  }

  @Override
  public void register(CommandDispatcher<ServerCommandSource> commandDispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
    final KeywordArgsArgumentType transformKeywordArgs = KeywordArgsArgumentType.builder()
        .addOptionalArg("affect_only", BlockPredicateArgumentType.blockPredicate(commandRegistryAccess), null)
        .addOptionalArg("recursive", BoolArgumentType.bool(), false)
        .addOptionalArg("lenient", BoolArgumentType.bool(), false)
        .build();
    final KeywordArgsArgumentType substringKeywordArgs = KeywordArgsArgumentType.builder()
        .addOptionalArg("lenient", BoolArgumentType.bool(), false)
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
                .then(argument("path", nbtPath())
                    .then(argument("nbt_predicate", NbtPredicateArgumentType.element(commandRegistryAccess))
                        .then(argument("nbt_function", NbtFunctionArgumentType.element(commandRegistryAccess))
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
                    .then(argument("regex", RegexArgumentType.REGEX)
                        .then(argument("replacement", string())
                            .executes(context -> executeRegexReplace(context, transformKeywordArgs.defaultArgs()))
                            .then(argument("keyword_args", transformKeywordArgs)
                                .executes(context -> executeRegexReplace(context, getKeywordArgs(context, "keyword_args")))))))))
        .then(literal("substring")
            .then(argument("startIndex", integer())
                .executes(context -> executeSubstring(OptionalInt.empty(), context, substringKeywordArgs.defaultArgs()))
                .then(argument("endIndex", integer())
                    .executes(context -> executeSubstring(OptionalInt.of(getInteger(context, "end_index")), context, substringKeywordArgs.defaultArgs()))
                    .then(argument("keyword_args", substringKeywordArgs)
                        .executes(context -> executeSubstring(OptionalInt.of(getInteger(context, "end_index")), context, getKeywordArgs(context, "keyword_args")))))))
        .then(literal("remove")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("path", nbtPath())
                    .executes(context -> executeRemove(getNbtTarget(context, "target"), getNbtPath(context, "path"), context.getSource()))))));
  }
}
