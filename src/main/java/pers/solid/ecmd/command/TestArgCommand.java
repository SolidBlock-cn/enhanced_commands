package pers.solid.ecmd.command;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.NbtElementArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.function.FailableFunction;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.mixins.accessor.ServerCommandSourceAccessor;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.entity.EntityPredicate;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionArgument;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.parse.ParseContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.command.argument.NbtCompoundArgumentType.getNbtCompound;
import static net.minecraft.command.argument.NbtElementArgumentType.getNbtElement;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.BlockFunctionArgumentType.getBlockFunction;
import static pers.solid.ecmd.argument.BlockPredicateArgumentType.getBlockPredicate;
import static pers.solid.ecmd.argument.EnhancedPosArgumentType.getPosArgument;
import static pers.solid.ecmd.argument.EntityPredicateArgumentType.entityPredicate;
import static pers.solid.ecmd.argument.EntityPredicateArgumentType.getEntityPredicate;
import static pers.solid.ecmd.argument.NbtFunctionArgumentType.getNbtFunction;
import static pers.solid.ecmd.argument.NbtPredicateArgumentType.getNbtPredicate;
import static pers.solid.ecmd.argument.RegionArgumentType.getRegion;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum TestArgCommand implements CommandRegistrationCallback {
  INSTANCE;

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addBlockFunctionProperties(T argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("block_function", BlockFunctionArgumentType.blockFunction(registryAccess))
        .executes(context -> executeStringShow(context, getBlockFunction(context, "block_function"), ExpressionConvertible::asString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getBlockFunction(context, "block_function"), ExpressionConvertible::asString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getBlockFunction(context, "block_function"), BlockFunction.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getBlockFunction(context, "block_function"), BlockFunction.CODEC, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getBlockFunction(context, "block_function"), ExpressionConvertible::asString, s -> BlockFunction.parse(new ParseContext<>(registryAccess, s, false, true)))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getBlockFunction(context, "block_function"), BlockFunction.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getBlockFunction(context, "block_function"), BlockFunction.CODEC, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addBlockPredicateProperties(T argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("block_predicate", BlockPredicateArgumentType.blockPredicate(registryAccess))
        .executes(context -> executeStringShow(context, getBlockPredicate(context, "block_predicate"), ExpressionConvertible::asString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getBlockPredicate(context, "block_predicate"), ExpressionConvertible::asString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getBlockPredicate(context, "block_predicate"), BlockPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getBlockPredicate(context, "block_predicate"), BlockPredicate.CODEC, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getBlockPredicate(context, "block_predicate"), ExpressionConvertible::asString, s -> BlockPredicate.parse(new ParseContext<>(registryAccess, s, false, true)))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getBlockPredicate(context, "block_predicate"), BlockPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getBlockPredicate(context, "block_predicate"), BlockPredicate.CODEC, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addEntityPredicateProperties(T argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("entity_predicate", entityPredicate(registryAccess))
        .executes(context -> executeStringShow(context, getEntityPredicate(context, "entity_predicate"), ExpressionConvertible::asString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getEntityPredicate(context, "entity_predicate"), ExpressionConvertible::asString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getEntityPredicate(context, "entity_predicate"), EntityPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getEntityPredicate(context, "entity_predicate"), EntityPredicate.CODEC, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getEntityPredicate(context, "entity_predicate"), ExpressionConvertible::asString, s -> EntityPredicateArgumentType.entityPredicate(registryAccess).parse(new StringReader(s)))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getEntityPredicate(context, "entity_predicate"), EntityPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getEntityPredicate(context, "entity_predicate"), EntityPredicate.CODEC, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addNbtProperties(T argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("nbt", NbtElementArgumentType.nbtElement())
        .executes(context -> {
          context.getSource().sendFeedback$ecBridge(() -> NbtHelper.toPrettyPrintedText(getNbtElement(context, "nbt")), false);
          return 1;
        })
        .then(literal("plainstring")
            .executes(context -> {
              context.getSource().sendFeedback$ecBridge(() -> Text.literal(TextUtil.toSpacedStringNbt(getNbtElement(context, "nbt"))), false);
              return 1;
            }))
        .then(literal("prettyprinted")
            .executes(context -> {
              context.getSource().sendFeedback$ecBridge(() -> NbtHelper.toPrettyPrintedText(getNbtElement(context, "nbt")), false);
              return 1;
            }))
        .then(literal("indented")
            .executes(context -> {
              context.getSource().sendFeedback$ecBridge(() -> new NbtTextFormatter("  ").apply(getNbtElement(context, "nbt")), false);
              return 1;
            }))
        .then(literal("test")
            .executes(context -> {
              final NbtElement nbtElement = getNbtElement(context, "nbt");
              final String s = TextUtil.toSpacedStringNbt(nbtElement);
              final ServerCommandSource source = context.getSource();
              source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testarg.nbt.nbt_to_string", Text.literal(s).styled(Styles.RESULT)), false);
              final NbtPredicate reparsedPredicate = NbtPredicate.parse(new ParseContext<>(registryAccess, s, false, true), false, false);
              source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testarg.nbt.reparsed_predicate", Text.literal(reparsedPredicate.asString(false)).styled(Styles.RESULT)), false);
              final NbtFunction reparsedFunction = NbtFunction.parse(new ParseContext<>(registryAccess, s, false, true), false, false);
              source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testarg.nbt.reparsed_function", Text.literal(reparsedFunction.asString()).styled(Styles.RESULT)), false);
              final boolean reparsedPredicateMatches = reparsedPredicate.test(nbtElement);
              source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testarg.nbt.reparsed_predicate_matches", TextUtil.wrapBoolean(reparsedPredicateMatches)), false);
              final boolean reparsedFunctionEqual = reparsedFunction.apply(null, new ExecutionContext(source)).equals(nbtElement);
              source.sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testarg.nbt.reparsed_function_equal", TextUtil.wrapBoolean(reparsedFunctionEqual)), false);
              return (reparsedPredicateMatches ? 2 : 0) + (reparsedFunctionEqual ? 1 : 0);
            }))
        .then(literal("convert")
            .then(literal("block_function")
                .executes(context -> executeConvertShow(context, BlockFunction.CODEC)))
            .then(literal("block_predicate")
                .executes(context -> executeConvertShow(context, BlockPredicate.CODEC)))
            .then(literal("nbt_function")
                .executes(context -> executeConvertShow(context, NbtFunction.CODEC)))
            .then(literal("nbt_predicate")
                .executes(context -> executeConvertShow(context, NbtPredicate.CODEC)))
            .then(literal("pos_argument")
                .executes(context -> executeConvertShow(context, EnhancedPosArgument.CODEC)))
            .then(literal("region")
                .executes(context -> executeConvertShow(context, Region.CODEC)))
            .then(literal("region_argument")
                .executes(context -> executeConvertShow(context, RegionArgument.CODEC)))
        )
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getNbtElement(context, "nbt"), CodecUtil.NBT_ELEMENT, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getNbtElement(context, "nbt"), CodecUtil.NBT_ELEMENT, JsonOps.INSTANCE)))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getNbtElement(context, "nbt"), CodecUtil.NBT_ELEMENT, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getNbtElement(context, "nbt"), CodecUtil.NBT_ELEMENT, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addNbtCompoundProperties(T argumentBuilder) {
    return argumentBuilder.then(argument("nbt_compound", NbtCompoundArgumentType.nbtCompound())
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getNbtCompound(context, "nbt_compound"), NbtCompound.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getNbtCompound(context, "nbt_compound"), NbtCompound.CODEC, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addNbtPredicateProperties(T argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("nbt_predicate", NbtPredicateArgumentType.element(registryAccess))
        .executes(context -> executeStringShow(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate::asString))
        .then(literal("match")
            .then(argument("nbt_to_test", NbtElementArgumentType.nbtElement())
                .executes(context -> {
                  final NbtElement nbtToTest = getNbtElement(context, "nbt_to_test");
                  final NbtPredicate nbtPredicate = getNbtPredicate(context, "nbt_predicate");
                  final boolean test = nbtPredicate.test(nbtToTest);
                  context.getSource().sendFeedback$ecBridge(() -> Text.literal(Boolean.toString(test)), false);
                  return BooleanUtils.toInteger(test);
                })))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate::asString)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate::asString, s -> NbtPredicate.parse(new ParseContext<>(registryAccess, s, false, true), false, false))))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate.CODEC, JsonOps.INSTANCE)))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate.CODEC, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addNbtFunctionProperties(T argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("nbt_function", NbtFunctionArgumentType.element(registryAccess))
        .executes(context -> executeStringShow(context, getNbtFunction(context, "nbt_function"), NbtFunction::asString))
        .then(literal("apply")
            .executes(context -> {
              final NbtFunction nbtFunction = getNbtFunction(context, "nbt_function");
              final ServerCommandSource source = context.getSource();
              final NbtElement apply = nbtFunction.apply(null, new ExecutionContext(source));
              source.sendFeedback$ecBridge(() -> NbtHelper.toPrettyPrintedText(apply), false);
              return 1;
            })
            .then(argument("nbt_element", NbtElementArgumentType.nbtElement())
                .executes(context -> {
                  final NbtElement nbtElement = getNbtElement(context, "nbt_element");
                  final NbtFunction nbtFunction = getNbtFunction(context, "nbt_function");
                  final ServerCommandSource source = context.getSource();
                  final NbtElement apply = nbtFunction.apply(nbtElement, new ExecutionContext(source));
                  source.sendFeedback$ecBridge(() -> NbtHelper.toPrettyPrintedText(apply), false);
                  return 1;
                })))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getNbtFunction(context, "nbt_function"), NbtFunction::asString)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getNbtFunction(context, "nbt_function"), NbtFunction::asString, s -> NbtFunction.parse(new ParseContext<>(registryAccess, s, false, true), false, true))))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getNbtFunction(context, "nbt_function"), NbtFunction.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getNbtFunction(context, "nbt_function"), NbtFunction.CODEC, JsonOps.INSTANCE)))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getNbtFunction(context, "nbt_function"), NbtFunction.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getNbtFunction(context, "nbt_function"), NbtFunction.CODEC, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addPosProperties(T argumentBuilder) {
    final Command<ServerCommandSource> execution = context -> {
      final EnhancedPosArgument pos = getPosArgument(context, "pos");
      final Vec3d absolutePos = pos.toAbsolutePos(context.getSource());
      context.getSource().sendFeedback$ecBridge(() -> Text.literal(EnhancedPosArgument.asString(pos)), false);
      context.getSource().sendFeedback$ecBridge(() -> NbtHelper.toPrettyPrintedText(EnhancedPosArgument.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow()), false);
      context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testarg.pos.result").append(ScreenTexts.LINE_BREAK).append(Text.literal(String.format(" x = %s\n y = %s\n z = %s", absolutePos.x, absolutePos.y, absolutePos.z)).formatted(Formatting.GRAY)), false);
      return 1;
    };
    for (final EnhancedPosArgumentType.NumberType numberType : EnhancedPosArgumentType.NumberType.values()) {
      final LiteralArgumentBuilder<ServerCommandSource> node = literal(numberType.name().toLowerCase());
      for (EnhancedPosArgumentType.IntAlignType intAlignType : EnhancedPosArgumentType.IntAlignType.values()) {
        node.then(literal(intAlignType.name().toLowerCase())
            .then(argument("pos", new EnhancedPosArgumentType(numberType, intAlignType))
                .executes(execution)));
      }
      argumentBuilder.then(node);
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

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addRegionProperties(T argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder.then(argument("region", RegionArgumentType.region(registryAccess))
        .executes(context -> executeStringShow(context, getRegion(context, "region"), Region::asString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getRegion(context, "region"), Region::asString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getRegion(context, "region"), Region.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getRegion(context, "region"), Region.CODEC, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getRegion(context, "region"), Region::asString, s -> RegionArgument.parse(new ParseContext<>(registryAccess, s, false, true)).toAbsoluteRegion((PositionProvider) context.getSource()))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getRegion(context, "region"), Region.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getRegion(context, "region"), Region.CODEC, JsonOps.INSTANCE)))
        .then(literal("illustrate")
            .executes(context -> {
              final Region region = getRegion(context, "region");
              final ServerWorld world = context.getSource().getWorld();
              int numOfIteratedButNotMatch = 0;
              int numOfNotIteratedButMatch = 0;
              final ImmutableSet<BlockPos> collect = region.stream().map(BlockPos::toImmutable).collect(ImmutableSet.toImmutableSet());
              final Set<BlockPos> iteratedNearby = new HashSet<>();
              final BlockPlacementHistory history = new BlockPlacementHistory(Text.translatable("enhanced_commands.commands.testarg.region.illustrate.task_name", region.asString()), world, Block.NOTIFY_LISTENERS, 0);
              for (BlockPos blockPos : collect) {
                if (region.contains(blockPos)) {
                  history.recordBlockAndEntity(world, blockPos, Blocks.GLASS.getDefaultState());
                  world.setBlockState(blockPos, Blocks.GLASS.getDefaultState(), Block.NOTIFY_LISTENERS);
                } else {
                  numOfIteratedButNotMatch++;
                  history.recordBlockAndEntity(world, blockPos, Blocks.RED_STAINED_GLASS.getDefaultState());
                  world.setBlockState(blockPos, Blocks.RED_STAINED_GLASS.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
                for (Direction direction : Direction.values()) {
                  final BlockPos offset = blockPos.offset(direction);
                  if (!collect.contains(offset) && !iteratedNearby.contains(offset)) {
                    if (region.contains(offset)) {
                      numOfNotIteratedButMatch++;
                      history.recordBlockAndEntity(world, blockPos, Blocks.ORANGE_STAINED_GLASS.getDefaultState());
                      world.setBlockState(offset, Blocks.ORANGE_STAINED_GLASS.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                    iteratedNearby.add(offset);
                  }
                }
              }
              final int finalNumOfIteratedButNotMatch = numOfIteratedButNotMatch;
              final int finalNumOfNotIteratedButMatch = numOfNotIteratedButMatch;
              if (((ServerCommandSourceAccessor) context.getSource()).getOutput() instanceof final HistoryHolder holder) {
                holder.addUndoableHistory$ec(history);
              }
              context.getSource().sendFeedback$ecBridge(() -> Text.translatable("enhanced_commands.commands.testarg.region.illustrate.result", TextUtil.literal(region).formatted(Formatting.GRAY), Integer.toString(finalNumOfIteratedButNotMatch), Blocks.RED_STAINED_GLASS.getName(), Integer.toString(finalNumOfNotIteratedButMatch), Blocks.ORANGE_STAINED_GLASS.getName()), false);
              return numOfIteratedButNotMatch;
            }))
    );
  }

  private static <A> int executeConvertShow(NbtElement nbtElement, Codec<A> codec, Consumer<A> resultConsumer, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    final DataResult<Pair<A, NbtElement>> decode = codec.decode(registryLookup.getOps(NbtOps.INSTANCE), nbtElement);
    final A result = decode.getOrThrow(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()::create).getFirst();
    resultConsumer.accept(result);
    return 1;
  }

  private static <A extends ExpressionConvertible> int executeConvertShow(CommandContext<ServerCommandSource> context, Codec<A> codec) throws CommandSyntaxException {
    return executeConvertShow(getNbtElement(context, "nbt"), codec, a -> context.getSource().sendFeedback$ecBridge(() -> Text.literal(a.asString()).styled(Styles.RESULT), false), context.getSource().getRegistryManager());
  }

  private static <A> int executeStringShow(CommandContext<ServerCommandSource> context, A fetchedArg, FailableFunction<A, String, CommandSyntaxException> toString) throws CommandSyntaxException {
    final String s = toString.apply(fetchedArg);
    context.getSource().sendFeedback$ecBridge(() -> Text.literal(s), false);
    return 1;
  }

  private static <A> int executeStringTest(CommandContext<ServerCommandSource> context, A fetchedArg, FailableFunction<A, String, CommandSyntaxException> toString, FailableFunction<String, A, CommandSyntaxException> fromString) throws CommandSyntaxException {
    final String s = toString.apply(fetchedArg);
    context.getSource().sendFeedback$ecBridge(() -> Text.literal(s), false);
    final A second = fromString.apply(s);
    final boolean b = second.equals(fetchedArg);
    context.getSource().sendFeedback$ecBridge(() -> TextUtil.wrapBoolean(b), false);
    return BooleanUtils.toInteger(b);
  }

  private static <A, T> int executeCodecShow(CommandContext<ServerCommandSource> context, A fetchedArg, Codec<A> codec, DynamicOps<T> ops) throws CommandSyntaxException {
    ops = context.getSource().getRegistryManager().getOps(ops);
    final T code = codec.encodeStart(ops, fetchedArg).getOrThrow(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()::create);
    if (code instanceof NbtElement nbt) {
      context.getSource().sendFeedback$ecBridge(() -> NbtHelper.toPrettyPrintedText(nbt), false);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Text.literal(code.toString()).styled(Styles.RESULT), false);
    }
    return 1;
  }

  private static <A, T> int executeCodecTest(CommandContext<ServerCommandSource> context, A fetchedArg, Codec<A> codec, DynamicOps<T> ops) throws CommandSyntaxException {
    ops = context.getSource().getRegistryManager().getOps(ops);
    final T code = codec.encodeStart(ops, fetchedArg).getOrThrow(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()::create);
    if (code instanceof NbtElement nbt) {
      context.getSource().sendFeedback$ecBridge(() -> NbtHelper.toPrettyPrintedText(nbt), false);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Text.literal(code.toString()).styled(Styles.RESULT), false);
    }
    try {
      final A second = codec.decode(ops, code).getOrThrow(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()::create).getFirst();
      if (second instanceof NbtElement nbt) {
        context.getSource().sendFeedback$ecBridge(() -> NbtHelper.toPrettyPrintedText(nbt), false);
      }
      final boolean b = second.equals(fetchedArg);
      context.getSource().sendFeedback$ecBridge(() -> TextUtil.wrapBoolean(b), false);
      return BooleanUtils.toInteger(b);
    } catch (Throwable e) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(e.toString());
    }
  }

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literalR2("testarg")
        .then(addBlockFunctionProperties(literal("block_function"), registryAccess))
        .then(addBlockPredicateProperties(literal("block_predicate"), registryAccess))
        .then(addEntityPredicateProperties(literal("entity_predicate"), registryAccess))
        .then(addNbtProperties(literal("nbt"), registryAccess))
        .then(addNbtCompoundProperties(literal("nbt_compound")))
        .then(addNbtPredicateProperties(literal("nbt_predicate"), registryAccess))
        .then(addNbtFunctionProperties(literal("nbt_function"), registryAccess))
        .then(addPosProperties(literal("pos")))
        .then(addRegionProperties(literal("region"), registryAccess))
    );
  }
}
