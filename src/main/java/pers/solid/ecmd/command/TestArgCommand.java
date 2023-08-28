package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NbtElementArgumentType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.util.TextUtil;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum TestArgCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literal("testarg")
        .then(addBlockFunctionProperties(literal("block_function"), registryAccess))
        .then(addBlockPredicateProperties(literal("block_predicate"), registryAccess))
        .then(addNbtProperties(literal("nbt")))
        .then(addNbtPredicateProperties(literal("nbt_predicate")))
        .then(addNbtFunctionProperties(literal("nbt_function")))
    );
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addBlockFunctionProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("block_function", BlockFunctionArgumentType.blockFunction(registryAccess))
        .executes(context -> {
          final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block_function");
          context.getSource().sendFeedback(Text.literal(blockFunction.asString()), false);
          context.getSource().sendFeedback(NbtHelper.toPrettyPrintedText(blockFunction.createNbt()), false);
          return 1;
        })
        .then(literal("string")
            .executes(context -> {
              final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block_function");
              context.getSource().sendFeedback(Text.literal(blockFunction.asString()), false);
              return 1;
            }))
        .then(literal("nbt")
            .executes(context -> {
              final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block_function");
              context.getSource().sendFeedback(NbtHelper.toPrettyPrintedText(blockFunction.createNbt()), false);
              return 1;
            })));
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addBlockPredicateProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("block_predicate", BlockPredicateArgumentType.blockPredicate(registryAccess))
        .executes(context -> {
          final BlockPredicate blockPredicate = BlockPredicateArgumentType.getBlockPredicate(context, "block_predicate");
          context.getSource().sendFeedback(Text.literal(blockPredicate.asString()), false);
          context.getSource().sendFeedback(NbtHelper.toPrettyPrintedText(blockPredicate.createNbt()), false);
          return 1;
        })
        .then(literal("string")
            .executes(context -> {
              final BlockPredicate blockPredicate = BlockPredicateArgumentType.getBlockPredicate(context, "block_predicate");
              context.getSource().sendFeedback(Text.literal(blockPredicate.asString()), false);
              return 1;
            }))
        .then(literal("nbt")
            .executes(context -> {
              final BlockPredicate blockPredicate = BlockPredicateArgumentType.getBlockPredicate(context, "block_predicate");
              context.getSource().sendFeedback(NbtHelper.toPrettyPrintedText(blockPredicate.createNbt()), false);
              return 1;
            })));
  }

  private LiteralArgumentBuilder<ServerCommandSource> addNbtProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder) {
    return argumentBuilder.then(argument("nbt", NbtElementArgumentType.nbtElement())
        .executes(context -> {
          context.getSource().sendFeedback(NbtHelper.toPrettyPrintedText(NbtElementArgumentType.getNbtElement(context, "nbt")), false);
          return 1;
        })
        .then(literal("plainstring")
            .executes(context -> {
              context.getSource().sendFeedback(Text.literal(TextUtil.toSpacedStringNbt(NbtElementArgumentType.getNbtElement(context, "nbt"))), false);
              return 1;
            }))
        .then(literal("prettyprinted")
            .executes(context -> {
              context.getSource().sendFeedback(NbtHelper.toPrettyPrintedText(NbtElementArgumentType.getNbtElement(context, "nbt")), false);
              return 1;
            }))
        .then(literal("indented")
            .executes(context -> {
              context.getSource().sendFeedback(new NbtTextFormatter("  ", 0).apply(NbtElementArgumentType.getNbtElement(context, "nbt")), false);
              return 1;
            }))
        .then(literal("test")
            .executes(context -> {
              final NbtElement nbtElement = NbtElementArgumentType.getNbtElement(context, "nbt");
              final String s = TextUtil.toSpacedStringNbt(nbtElement);
              context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.testarg.nbt.nbt_to_string", Text.literal(s).styled(TextUtil.STYLE_FOR_RESULT)), false);
              final NbtPredicate reparsedPredicate = new NbtPredicateSuggestedParser(new StringReader(s)).parsePredicate(false, false);
              context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.testarg.nbt.reparsed_predicate", Text.literal(reparsedPredicate.asString(false)).styled(TextUtil.STYLE_FOR_RESULT)), false);
              final NbtFunction reparsedFunction = new NbtFunctionSuggestedParser(new StringReader(s)).parseFunction(false, false);
              context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.testarg.nbt.reparsed_function", Text.literal(reparsedFunction.asString(false)).styled(TextUtil.STYLE_FOR_RESULT)), false);
              final boolean reparsedPredicateMatches = reparsedPredicate.test(nbtElement);
              context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.testarg.nbt.reparsed_predicate_matches", TextUtil.wrapBoolean(reparsedPredicateMatches)), false);
              final boolean reparsedFunctionEqual = reparsedFunction.apply(null).equals(nbtElement);
              context.getSource().sendFeedback(Text.translatable("enhancedCommands.commands.testarg.nbt.reparsed_function_equal", TextUtil.wrapBoolean(reparsedFunctionEqual)), false);
              return (reparsedPredicateMatches ? 2 : 0) + (reparsedFunctionEqual ? 1 : 0);
            }))
    );
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addNbtPredicateProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder) {
    return argumentBuilder.then(argument("nbt_predicate", NbtPredicateArgumentType.ELEMENT)
        .executes(context -> {
          final NbtPredicate nbtPredicate = NbtPredicateArgumentType.getNbtPredicate(context, "nbt_predicate");
          context.getSource().sendFeedback(Text.literal(nbtPredicate.asString()), false);
          return 1;
        })
        .then(literal("match")
            .then(argument("nbt_to_test", NbtElementArgumentType.nbtElement())
                .executes(context -> {
                  final NbtElement nbtToTest = NbtElementArgumentType.getNbtElement(context, "nbt_to_test");
                  final NbtPredicate nbtPredicate = NbtPredicateArgumentType.getNbtPredicate(context, "nbt_predicate");
                  final boolean test = nbtPredicate.test(nbtToTest);
                  context.getSource().sendFeedback(Text.literal(Boolean.toString(test)), false);
                  return BooleanUtils.toInteger(test);
                })))
        .then(literal("tostring")
            .executes(context -> {
              final NbtPredicate nbtPredicate = NbtPredicateArgumentType.getNbtPredicate(context, "nbt_predicate");
              context.getSource().sendFeedback(Text.literal(nbtPredicate.asString()), false);
              return 1;
            }))
        .then(literal("reparse")
            .executes(context -> {
              final NbtPredicate nbtPredicate = NbtPredicateArgumentType.getNbtPredicate(context, "nbt_predicate");
              final String s = nbtPredicate.asString();
              context.getSource().sendFeedback(Text.literal(s), false);
              final NbtPredicate reparse = new NbtPredicateSuggestedParser(new StringReader(s)).parseCompound(false, false);
              final boolean b = nbtPredicate.equals(reparse);
              context.getSource().sendFeedback(TextUtil.wrapBoolean(b), false);
              return BooleanUtils.toInteger(b);
            }))
    );
  }

  private static LiteralArgumentBuilder<ServerCommandSource> addNbtFunctionProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder) {
    return argumentBuilder.then(argument("nbt_function", NbtFunctionArgumentType.ELEMENT)
        .executes(context -> {
          final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
          context.getSource().sendFeedback(Text.literal(nbtFunction.asString()), false);
          return 1;
        })
        .then(literal("apply")
            .executes(context -> {
              final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
              final NbtElement apply = nbtFunction.apply(null);
              context.getSource().sendFeedback(NbtHelper.toPrettyPrintedText(apply), false);
              return 1;
            })
            .then(argument("nbt_element", NbtElementArgumentType.nbtElement())
                .executes(context -> {
                  final NbtElement nbtElement = NbtElementArgumentType.getNbtElement(context, "nbt_element");
                  final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
                  final NbtElement apply = nbtFunction.apply(nbtElement);
                  context.getSource().sendFeedback(NbtHelper.toPrettyPrintedText(apply), false);
                  return 1;
                })))
        .then(literal("tostring")
            .executes(context -> {
              final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
              context.getSource().sendFeedback(Text.literal(nbtFunction.asString(false)), false);
              return 1;
            }))
        .then(literal("reparse")
            .executes(context -> {
              final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
              final String s = nbtFunction.asString(false);
              context.getSource().sendFeedback(Text.literal(s), false);
              final NbtFunction reparse = new NbtFunctionSuggestedParser(new StringReader(s)).parseFunction(false, false);
              context.getSource().sendFeedback(Text.literal(reparse.asString(false)).formatted(Formatting.GRAY), false);
              final boolean b = nbtFunction.equals(reparse);
              context.getSource().sendFeedback(TextUtil.wrapBoolean(b), false);
              return BooleanUtils.toInteger(b);
            }))
    );
  }
}
