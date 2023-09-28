package pers.solid.ecmd.command;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.NbtElementArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.ecmd.EnhancedCommands;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.HashSet;
import java.util.Set;

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
        .then(addPosProperties(literal("pos")))
        .then(addRegionProperties(literal("region"), registryAccess))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addBlockFunctionProperties(T argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("block_function", BlockFunctionArgumentType.blockFunction(registryAccess))
        .executes(context -> {
          final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block_function");
          CommandBridge.sendFeedback(context, () -> Text.literal(blockFunction.asString()), false);
          CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(blockFunction.createNbt()), false);
          return 1;
        })
        .then(literal("string")
            .executes(context -> {
              final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block_function");
              CommandBridge.sendFeedback(context, () -> Text.literal(blockFunction.asString()), false);
              return 1;
            }))
        .then(literal("nbt")
            .executes(context -> {
              final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block_function");
              CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(blockFunction.createNbt()), false);
              return 1;
            }))
        .then(literal("reparse")
            .executes(context -> {
              final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block_function");
              final String s = blockFunction.asString();
              CommandBridge.sendFeedback(context, () -> Text.literal(s), false);
              final BlockFunction reparse = BlockFunction.parse(registryAccess, s, context.getSource());
              final boolean b = blockFunction.equals(reparse);
              CommandBridge.sendFeedback(context, () -> TextUtil.wrapBoolean(b), false);
              return BooleanUtils.toInteger(b);
            }))
        .then(literal("redeserialize")
            .executes(context -> {
              final BlockFunction blockFunction = BlockFunctionArgumentType.getBlockFunction(context, "block_function");
              final NbtCompound nbt = blockFunction.createNbt();
              CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(nbt), false);
              try {
                final BlockFunction reDeserialize = BlockFunction.fromNbt(nbt);
                final boolean b = blockFunction.equals(reDeserialize);
                CommandBridge.sendFeedback(context, () -> TextUtil.wrapBoolean(b), false);
                return BooleanUtils.toInteger(b);
              } catch (Throwable e) {
                EnhancedCommands.LOGGER.error("Parsing block function from NBT:", e);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(e.toString());
              }
            }))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addBlockPredicateProperties(T argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("block_predicate", BlockPredicateArgumentType.blockPredicate(registryAccess))
        .executes(context -> {
          final BlockPredicate blockPredicate = BlockPredicateArgumentType.getBlockPredicate(context, "block_predicate");
          CommandBridge.sendFeedback(context, () -> Text.literal(blockPredicate.asString()), false);
          CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(blockPredicate.createNbt()), false);
          return 1;
        })
        .then(literal("string")
            .executes(context -> {
              final BlockPredicate blockPredicate = BlockPredicateArgumentType.getBlockPredicate(context, "block_predicate");
              CommandBridge.sendFeedback(context, () -> Text.literal(blockPredicate.asString()), false);
              return 1;
            }))
        .then(literal("nbt")
            .executes(context -> {
              final BlockPredicate blockPredicate = BlockPredicateArgumentType.getBlockPredicate(context, "block_predicate");
              CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(blockPredicate.createNbt()), false);
              return 1;
            }))
        .then(literal("reparse")
            .executes(context -> {
              final BlockPredicate blockPredicate = BlockPredicateArgumentType.getBlockPredicate(context, "block_predicate");
              final String s = blockPredicate.asString();
              CommandBridge.sendFeedback(context, () -> Text.literal(s), false);
              final BlockPredicate reparse = BlockPredicate.parse(registryAccess, s, context.getSource());
              final boolean b = blockPredicate.equals(reparse);
              CommandBridge.sendFeedback(context, () -> TextUtil.wrapBoolean(b), false);
              return BooleanUtils.toInteger(b);
            }))
        .then(literal("redeserialize")
            .executes(context -> {
              final BlockPredicate blockPredicate = BlockPredicateArgumentType.getBlockPredicate(context, "block_predicate");
              final NbtCompound nbt = blockPredicate.createNbt();
              CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(nbt), false);
              try {
                final BlockPredicate reDeserialize = BlockPredicate.fromNbt(nbt);
                final boolean b = blockPredicate.equals(reDeserialize);
                CommandBridge.sendFeedback(context, () -> TextUtil.wrapBoolean(b), false);
                return BooleanUtils.toInteger(b);
              } catch (Throwable e) {
                EnhancedCommands.LOGGER.error("Parsing block predicate from NBT:", e);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(e.toString());
              }
            }))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addNbtProperties(T argumentBuilder) {
    return argumentBuilder.then(argument("nbt", NbtElementArgumentType.nbtElement())
        .executes(context -> {
          CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(NbtElementArgumentType.getNbtElement(context, "nbt")), false);
          return 1;
        })
        .then(literal("plainstring")
            .executes(context -> {
              CommandBridge.sendFeedback(context, () -> Text.literal(TextUtil.toSpacedStringNbt(NbtElementArgumentType.getNbtElement(context, "nbt"))), false);
              return 1;
            }))
        .then(literal("prettyprinted")
            .executes(context -> {
              CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(NbtElementArgumentType.getNbtElement(context, "nbt")), false);
              return 1;
            }))
        .then(literal("indented")
            .executes(context -> {
              CommandBridge.sendFeedback(context, () -> new NbtTextFormatter("  ", 0).apply(NbtElementArgumentType.getNbtElement(context, "nbt")), false);
              return 1;
            }))
        .then(literal("test")
            .executes(context -> {
              final NbtElement nbtElement = NbtElementArgumentType.getNbtElement(context, "nbt");
              final String s = TextUtil.toSpacedStringNbt(nbtElement);
              CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.testarg.nbt.nbt_to_string", Text.literal(s).styled(TextUtil.STYLE_FOR_RESULT)), false);
              final NbtPredicate reparsedPredicate = new NbtPredicateSuggestedParser(new StringReader(s)).parsePredicate(false, false);
              CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.testarg.nbt.reparsed_predicate", Text.literal(reparsedPredicate.asString(false)).styled(TextUtil.STYLE_FOR_RESULT)), false);
              final NbtFunction reparsedFunction = new NbtFunctionSuggestedParser(new StringReader(s)).parseFunction(false, false);
              CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.testarg.nbt.reparsed_function", Text.literal(reparsedFunction.asString(false)).styled(TextUtil.STYLE_FOR_RESULT)), false);
              final boolean reparsedPredicateMatches = reparsedPredicate.test(nbtElement);
              CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.testarg.nbt.reparsed_predicate_matches", TextUtil.wrapBoolean(reparsedPredicateMatches)), false);
              final boolean reparsedFunctionEqual = reparsedFunction.apply(null).equals(nbtElement);
              CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.testarg.nbt.reparsed_function_equal", TextUtil.wrapBoolean(reparsedFunctionEqual)), false);
              return (reparsedPredicateMatches ? 2 : 0) + (reparsedFunctionEqual ? 1 : 0);
            }))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addNbtPredicateProperties(T argumentBuilder) {
    return argumentBuilder.then(argument("nbt_predicate", NbtPredicateArgumentType.ELEMENT)
        .executes(context -> {
          final NbtPredicate nbtPredicate = NbtPredicateArgumentType.getNbtPredicate(context, "nbt_predicate");
          CommandBridge.sendFeedback(context, () -> Text.literal(nbtPredicate.asString()), false);
          return 1;
        })
        .then(literal("match")
            .then(argument("nbt_to_test", NbtElementArgumentType.nbtElement())
                .executes(context -> {
                  final NbtElement nbtToTest = NbtElementArgumentType.getNbtElement(context, "nbt_to_test");
                  final NbtPredicate nbtPredicate = NbtPredicateArgumentType.getNbtPredicate(context, "nbt_predicate");
                  final boolean test = nbtPredicate.test(nbtToTest);
                  CommandBridge.sendFeedback(context, () -> Text.literal(Boolean.toString(test)), false);
                  return BooleanUtils.toInteger(test);
                })))
        .then(literal("tostring")
            .executes(context -> {
              final NbtPredicate nbtPredicate = NbtPredicateArgumentType.getNbtPredicate(context, "nbt_predicate");
              CommandBridge.sendFeedback(context, () -> Text.literal(nbtPredicate.asString()), false);
              return 1;
            }))
        .then(literal("reparse")
            .executes(context -> {
              final NbtPredicate nbtPredicate = NbtPredicateArgumentType.getNbtPredicate(context, "nbt_predicate");
              final String s = nbtPredicate.asString();
              CommandBridge.sendFeedback(context, () -> Text.literal(s), false);
              final NbtPredicate reparse = new NbtPredicateSuggestedParser(new StringReader(s)).parseCompound(false, false);
              final boolean b = nbtPredicate.equals(reparse);
              CommandBridge.sendFeedback(context, () -> TextUtil.wrapBoolean(b), false);
              return BooleanUtils.toInteger(b);
            }))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addNbtFunctionProperties(T argumentBuilder) {
    return argumentBuilder.then(argument("nbt_function", NbtFunctionArgumentType.ELEMENT)
        .executes(context -> {
          final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
          CommandBridge.sendFeedback(context, () -> Text.literal(nbtFunction.asString()), false);
          return 1;
        })
        .then(literal("apply")
            .executes(context -> {
              final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
              final NbtElement apply = nbtFunction.apply(null);
              CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(apply), false);
              return 1;
            })
            .then(argument("nbt_element", NbtElementArgumentType.nbtElement())
                .executes(context -> {
                  final NbtElement nbtElement = NbtElementArgumentType.getNbtElement(context, "nbt_element");
                  final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
                  final NbtElement apply = nbtFunction.apply(nbtElement);
                  CommandBridge.sendFeedback(context, () -> NbtHelper.toPrettyPrintedText(apply), false);
                  return 1;
                })))
        .then(literal("tostring")
            .executes(context -> {
              final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
              CommandBridge.sendFeedback(context, () -> Text.literal(nbtFunction.asString(false)), false);
              return 1;
            }))
        .then(literal("reparse")
            .executes(context -> {
              final NbtFunction nbtFunction = NbtFunctionArgumentType.getNbtFunction(context, "nbt_function");
              final String s = nbtFunction.asString(false);
              CommandBridge.sendFeedback(context, () -> Text.literal(s), false);
              final NbtFunction reparse = new NbtFunctionSuggestedParser(new StringReader(s)).parseFunction(false, false);
              CommandBridge.sendFeedback(context, () -> Text.literal(reparse.asString(false)).formatted(Formatting.GRAY), false);
              final boolean b = nbtFunction.equals(reparse);
              CommandBridge.sendFeedback(context, () -> TextUtil.wrapBoolean(b), false);
              return BooleanUtils.toInteger(b);
            }))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addPosProperties(T argumentBuilder) {
    final Command<ServerCommandSource> execution = context -> {
      final PosArgument pos = EnhancedPosArgumentType.getPosArgument(context, "pos");
      final Vec3d absolutePos = pos.toAbsolutePos(context.getSource());
      CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.testarg.pos.result"), true);
      CommandBridge.sendFeedback(context, () -> Text.literal(String.format(" x = %s\n y = %s\n z = %s", absolutePos.x, absolutePos.y, absolutePos.z)).formatted(Formatting.GRAY), true);
      return 1;
    };
    for (final var value : EnhancedPosArgumentType.Behavior.values()) {
      argumentBuilder.then(literal(value.name().toLowerCase())
          .then(argument("pos", new EnhancedPosArgumentType(value, false))
              .executes(execution)));
    }

    // 由于传入客户端的数据包并不会告知这个参数类型是强制使用了原版的，因此需要在这里手动指定 suggestionProvider
    argumentBuilder.then(literal("vanilla_vec3")
            .then(argument("pos", new VanillaWrappedArgumentType<>(new Vec3ArgumentType(true)))
                .executes(execution)))
        .then(literal("vanilla_vec3_accurate")
            .then(argument("pos", new VanillaWrappedArgumentType<>(new Vec3ArgumentType(false)))
                .executes(execution)))
        .then(literal("vanilla_block_pos")
            .then(argument("pos", new VanillaWrappedArgumentType<>(new BlockPosArgumentType()))
                .executes(execution)));

    return argumentBuilder;
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addRegionProperties(T argumentBuilder, CommandRegistryAccess commandRegistryAccess) {
    return argumentBuilder.then(argument("region", RegionArgumentType.region(commandRegistryAccess))
        .then(literal("string")
            .executes(context -> {
              final Region region = RegionArgumentType.getRegion(context, "region");
              CommandBridge.sendFeedback(context, () -> Text.literal(region.asString()), false);
              return 1;
            }))
        .then(literal("reparse")
            .executes(context -> {
              final Region region = RegionArgumentType.getRegion(context, "region");
              final String s = region.asString();
              CommandBridge.sendFeedback(context, () -> Text.literal(s), false);
              final Region reparse = RegionArgument.parse(commandRegistryAccess, new SuggestedParser(s), false).toAbsoluteRegion(context.getSource());
              final boolean b = reparse.equals(reparse);
              CommandBridge.sendFeedback(context, () -> TextUtil.wrapBoolean(b), false);
              return BooleanUtils.toInteger(b);
            }))
        .then(literal("illustrate")
            .executes(context -> {
              final Region region = RegionArgumentType.getRegion(context, "region");
              final ServerWorld world = context.getSource().getWorld();
              int numOfIteratedButNotMatch = 0;
              int numOfNotIteratedButMatch = 0;
              final ImmutableSet<BlockPos> collect = region.stream().map(BlockPos::toImmutable).collect(ImmutableSet.toImmutableSet());
              final Set<BlockPos> iteratedNearby = new HashSet<>();
              for (BlockPos blockPos : collect) {
                if (region.contains(blockPos)) {
                  world.setBlockState(blockPos, Blocks.GLASS.getDefaultState(), Block.NOTIFY_LISTENERS);
                } else {
                  numOfIteratedButNotMatch++;
                  world.setBlockState(blockPos, Blocks.RED_STAINED_GLASS.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
                for (Direction direction : Direction.values()) {
                  final BlockPos offset = blockPos.offset(direction);
                  if (!collect.contains(offset) && !iteratedNearby.contains(offset)) {
                    if (region.contains(offset)) {
                      numOfNotIteratedButMatch++;
                      world.setBlockState(offset, Blocks.ORANGE_STAINED_GLASS.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                    iteratedNearby.add(offset);
                  }
                }
              }
              final int finalNumOfIteratedButNotMatch = numOfIteratedButNotMatch;
              final int finalNumOfNotIteratedButMatch = numOfNotIteratedButMatch;
              CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.testarg.region.verify.result", Text.literal(region.asString()).formatted(Formatting.GRAY), Integer.toString(finalNumOfIteratedButNotMatch), Blocks.RED_STAINED_GLASS.getName(), Integer.toString(finalNumOfNotIteratedButMatch), Blocks.ORANGE_STAINED_GLASS.getName()), true);
              return numOfIteratedButNotMatch;
            }))
    );
  }
}
