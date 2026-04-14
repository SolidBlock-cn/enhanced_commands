package pers.solid.ecmd.command;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.function.FailableBiFunction;
import org.apache.commons.lang3.function.FailableFunction;
import pers.solid.ecmd.api.CommandRegistrationCallbackBridge;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.block.predicate.BlockPredicate;
import pers.solid.ecmd.curve.Curve;
import pers.solid.ecmd.curve.CurveProvider;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.item.function.ItemFunction;
import pers.solid.ecmd.nbt.function.NbtFunction;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.entity.predicate.EntityPredicate;
import pers.solid.ecmd.item.predicate.ItemPredicate;
import pers.solid.ecmd.nbt.predicate.NbtPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionProvider;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.codec.CodecUtil;
import pers.solid.ecmd.util.extension.HistoryHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.CompoundTagArgument.getCompoundTag;
import static net.minecraft.commands.arguments.NbtTagArgument.getNbtTag;
import static pers.solid.ecmd.argument.EnhancedPosArgument.getPosArgument;
import static pers.solid.ecmd.argument.NbtFunctionArgument.getNbtFunction;
import static pers.solid.ecmd.argument.NbtPredicateArgument.getNbtPredicate;
import static pers.solid.ecmd.argument.RegionArgument.getRegion;
import static pers.solid.ecmd.command.EnhancedCommandsCommands.literalR2;

public enum TestArgCommand implements CommandRegistrationCallbackBridge {
  INSTANCE;

  private static <T extends ArgumentBuilder<CommandSourceStack, T>, E> T addValueProperties(T argumentBuilder, CommandBuildContext commandBuildContext, String argumentName, ArgumentType<?> argumentType, FailableBiFunction<ParseContext<?>, CommandContext<CommandSourceStack>, E, CommandSyntaxException> argumentParser, FailableBiFunction<CommandContext<CommandSourceStack>, String, E, CommandSyntaxException> argumentValueGetter, FailableFunction<E, String, CommandSyntaxException> toString, Codec<E> codec) {
    return argumentBuilder.then(argument(argumentName, argumentType)
        .executes(context -> executeStringShow(context, argumentValueGetter.apply(context, argumentName), toString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, argumentValueGetter.apply(context, argumentName), toString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, argumentValueGetter.apply(context, argumentName), codec, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, argumentValueGetter.apply(context, argumentName), codec, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, argumentValueGetter.apply(context, argumentName), toString, s -> argumentParser.apply(new ParseContext<>(commandBuildContext, s, false, true), context))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, argumentValueGetter.apply(context, argumentName), codec, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, argumentValueGetter.apply(context, argumentName), codec, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>, A extends ArgumentType<E>, E> T addValueProperties(T argumentBuilder, CommandBuildContext commandBuildContext, String argumentName, A argumentType, FailableFunction<ParseContext<?>, E, CommandSyntaxException> argumentParser, FailableBiFunction<CommandContext<CommandSourceStack>, String, E, CommandSyntaxException> argumentValueGetter, FailableFunction<E, String, CommandSyntaxException> toString, Codec<E> codec) {
    return addValueProperties(argumentBuilder, commandBuildContext, argumentName, argumentType, (parseContext, commandContext) -> argumentParser.apply(parseContext), argumentValueGetter, toString, codec);
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addNbtProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("nbt", NbtTagArgument.nbtTag())
        .executes(context -> {
          context.getSource().sendFeedback$ecBridge(() -> NbtUtils.toPrettyComponent(getNbtTag(context, "nbt")), false);
          return 1;
        })
        .then(literal("plainstring")
            .executes(context -> {
              context.getSource().sendFeedback$ecBridge(() -> Component.literal(TextUtil.toSpacedStringNbt(getNbtTag(context, "nbt"))), false);
              return 1;
            }))
        .then(literal("prettyprinted")
            .executes(context -> {
              context.getSource().sendFeedback$ecBridge(() -> NbtUtils.toPrettyComponent(getNbtTag(context, "nbt")), false);
              return 1;
            }))
        .then(literal("indented")
            .executes(context -> {
              context.getSource().sendFeedback$ecBridge(() -> new TextComponentTagVisitor("  ").visit(getNbtTag(context, "nbt")), false);
              return 1;
            }))
        .then(literal("test")
            .executes(context -> {
              final Tag nbtElement = getNbtTag(context, "nbt");
              final String s = TextUtil.toSpacedStringNbt(nbtElement);
              final CommandSourceStack source = context.getSource();
              source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testarg.nbt.nbt_to_string", Component.literal(s).withStyle(Styles.RESULT)), false);
              final NbtPredicate reparsedPredicate = NbtPredicate.parse(new ParseContext<>(commandBuildContext, s, false, true), false, false);
              source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testarg.nbt.reparsed_predicate", Component.literal(reparsedPredicate.asString(false)).withStyle(Styles.RESULT)), false);
              final NbtFunction reparsedFunction = NbtFunction.parse(new ParseContext<>(commandBuildContext, s, false, true), false, false);
              source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testarg.nbt.reparsed_function", Component.literal(reparsedFunction.asString()).withStyle(Styles.RESULT)), false);
              final boolean reparsedPredicateMatches = reparsedPredicate.test(nbtElement);
              source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testarg.nbt.reparsed_predicate_matches", TextUtil.wrapBoolean(reparsedPredicateMatches)), false);
              final boolean reparsedFunctionEqual = reparsedFunction.apply(null, new ExecutionContext(source)).equals(nbtElement);
              source.sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testarg.nbt.reparsed_function_equal", TextUtil.wrapBoolean(reparsedFunctionEqual)), false);
              return (reparsedPredicateMatches ? 2 : 0) + (reparsedFunctionEqual ? 1 : 0);
            }))
        .then(literal("convert")
            .then(literal("block_function")
                .executes(context -> executeConvertShow(context, BlockFunction.CODEC)))
            .then(literal("block_predicate")
                .executes(context -> executeConvertShow(context, BlockPredicate.CODEC)))
            .then(literal("entity_predicate")
                .executes(context -> executeConvertShow(context, EntityPredicate.CODEC)))
            .then(literal("item_function")
                .executes(context -> executeConvertShow(context, ItemFunction.CODEC)))
            .then(literal("item_predicate")
                .executes(context -> executeConvertShow(context, ItemPredicate.CODEC)))
            .then(literal("nbt_function")
                .executes(context -> executeConvertShow(context, NbtFunction.CODEC)))
            .then(literal("nbt_predicate")
                .executes(context -> executeConvertShow(context, NbtPredicate.CODEC)))
            .then(literal("coordinates")
                .executes(context -> executeConvertShow(context, EnhancedCoordinates.CODEC)))
            .then(literal("region")
                .executes(context -> executeConvertShow(context, Region.CODEC)))
            .then(literal("region_provider")
                .executes(context -> executeConvertShow(context, RegionProvider.CODEC)))
        )
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getNbtTag(context, "nbt"), CodecUtil.NBT_ELEMENT, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getNbtTag(context, "nbt"), CodecUtil.NBT_ELEMENT, JsonOps.INSTANCE)))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getNbtTag(context, "nbt"), CodecUtil.NBT_ELEMENT, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getNbtTag(context, "nbt"), CodecUtil.NBT_ELEMENT, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addNbtCompoundProperties(T argumentBuilder) {
    return argumentBuilder.then(argument("nbt_compound", CompoundTagArgument.compoundTag())
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getCompoundTag(context, "nbt_compound"), CompoundTag.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getCompoundTag(context, "nbt_compound"), CompoundTag.CODEC, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addExtraNbtPredicateProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("nbt_predicate", NbtPredicateArgument.element(commandBuildContext))
        .then(literal("match")
            .then(argument("nbt_to_test", NbtTagArgument.nbtTag())
                .executes(context -> {
                  final Tag nbtToTest = getNbtTag(context, "nbt_to_test");
                  final NbtPredicate nbtPredicate = getNbtPredicate(context, "nbt_predicate");
                  final boolean test = nbtPredicate.test(nbtToTest);
                  context.getSource().sendFeedback$ecBridge(() -> Component.literal(Boolean.toString(test)), false);
                  return BooleanUtils.toInteger(test);
                })))
    );
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addExtraNbtFunctionProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("nbt_function", NbtFunctionArgument.element(commandBuildContext))
        .then(literal("apply")
            .executes(context -> {
              final NbtFunction nbtFunction = getNbtFunction(context, "nbt_function");
              final CommandSourceStack source = context.getSource();
              final Tag apply = nbtFunction.apply(null, new ExecutionContext(source));
              source.sendFeedback$ecBridge(() -> NbtUtils.toPrettyComponent(apply), false);
              return 1;
            })
            .then(argument("nbt_element", NbtTagArgument.nbtTag())
                .executes(context -> {
                  final Tag nbtElement = getNbtTag(context, "nbt_element");
                  final NbtFunction nbtFunction = getNbtFunction(context, "nbt_function");
                  final CommandSourceStack source = context.getSource();
                  final Tag apply = nbtFunction.apply(nbtElement, new ExecutionContext(source));
                  source.sendFeedback$ecBridge(() -> NbtUtils.toPrettyComponent(apply), false);
                  return 1;
                })))
    );
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addPosProperties(T argumentBuilder) {
    final Command<CommandSourceStack> execution = context -> {
      final Coordinates pos = context.getArgument("pos", Coordinates.class);
      final Vec3 absolutePos = pos.getPosition(context.getSource());
      context.getSource().sendFeedback$ecBridge(() -> Component.literal(EnhancedCoordinates.asString(pos)), false);
      if (pos instanceof final EnhancedCoordinates enhanced) {
        context.getSource().sendFeedback$ecBridge(() -> NbtUtils.toPrettyComponent(EnhancedCoordinates.CODEC.encodeStart(NbtOps.INSTANCE, enhanced).getOrThrow()), false);
      }
      context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testarg.pos.result").append(CommonComponents.NEW_LINE).append(Component.literal(String.format(" x = %s\n y = %s\n z = %s", StringUtil.nf.format(absolutePos.x), StringUtil.nf.format(absolutePos.y), StringUtil.nf.format(absolutePos.z))).withStyle(ChatFormatting.GRAY)), false);
      return 1;
    };
    for (final EnhancedPosArgument.NumberType numberType : EnhancedPosArgument.NumberType.values()) {
      final LiteralArgumentBuilder<CommandSourceStack> node = literal(numberType.name().toLowerCase());
      for (EnhancedPosArgument.IntAlignType intAlignType : EnhancedPosArgument.IntAlignType.values()) {
        final EnhancedPosArgument type = new EnhancedPosArgument(numberType, intAlignType);
        node.then(literal(intAlignType.name().toLowerCase())
            .then(argument("pos", type)
                .executes(execution)
                .then(literal("string")
                    .executes(context -> executeStringShow(context, getPosArgument(context, "pos"), ExpressionConvertible::asString)))
                .then(literal("string_test")
                    .executes(context -> executeStringTest(context, getPosArgument(context, "pos"), ExpressionConvertible::asString, s -> type.parse(new StringReader(s)))))
                .then(literal("nbt")
                    .executes(context -> executeCodecShow(context, getPosArgument(context, "pos"), EnhancedCoordinates.CODEC, NbtOps.INSTANCE)))
                .then(literal("json")
                    .executes(context -> executeCodecShow(context, getPosArgument(context, "pos"), EnhancedCoordinates.CODEC, JsonOps.INSTANCE)))
                .then(literal("nbt_test")
                    .executes(context -> executeCodecTest(context, getPosArgument(context, "pos"), EnhancedCoordinates.CODEC, NbtOps.INSTANCE)))
                .then(literal("json_test")
                    .executes(context -> executeCodecTest(context, getPosArgument(context, "pos"), EnhancedCoordinates.CODEC, JsonOps.INSTANCE)))));
      }
      argumentBuilder.then(node);
    }

    // 由于传入客户端的数据包并不会告知这个参数类型是强制使用了原版的，因此需要在这里手动指定 suggestionProvider
    argumentBuilder.then(literal("vanilla_vec3")
            .then(argument("pos", new VanillaWrappedArgument<>(new Vec3Argument(true)))
                .executes(execution)))
        .then(literal("vanilla_vec3_accurate")
            .then(argument("pos", new VanillaWrappedArgument<>(new Vec3Argument(false)))
                .executes(execution)))
        .then(literal("vanilla_block_pos")
            .then(argument("pos", new VanillaWrappedArgument<>(new BlockPosArgument()))
                .executes(execution)));

    return argumentBuilder;
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addExtraRegionProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("region", RegionArgument.region(commandBuildContext))
        .then(literal("illustrate")
            .executes(context -> {
              final Region region = getRegion(context, "region");
              final ServerLevel world = context.getSource().getLevel();
              int numOfIteratedButNotMatch = 0;
              int numOfNotIteratedButMatch = 0;
              final ImmutableSet<BlockPos> collect = region.stream().map(BlockPos::immutable).collect(ImmutableSet.toImmutableSet());
              final Set<BlockPos> iteratedNearby = new HashSet<>();
              final BlockPlacementHistory history = new BlockPlacementHistory(Component.translatable("enhanced_commands.commands.testarg.region.illustrate.task_name", region.asString()), world, Block.UPDATE_CLIENTS, 0);
              for (BlockPos blockPos : collect) {
                if (region.contains(blockPos)) {
                  history.recordBlockAndEntity(world, blockPos, Blocks.GLASS.defaultBlockState());
                  world.removeBlockEntity(blockPos);
                  world.setBlock(blockPos, Blocks.GLASS.defaultBlockState(), Block.UPDATE_CLIENTS);
                } else {
                  numOfIteratedButNotMatch++;
                  history.recordBlockAndEntity(world, blockPos, Blocks.RED_STAINED_GLASS.defaultBlockState());
                  world.removeBlockEntity(blockPos);
                  world.setBlock(blockPos, Blocks.RED_STAINED_GLASS.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                for (Direction direction : Direction.values()) {
                  final BlockPos offset = blockPos.relative(direction);
                  if (!collect.contains(offset) && !iteratedNearby.contains(offset)) {
                    if (region.contains(offset)) {
                      numOfNotIteratedButMatch++;
                      history.recordBlockAndEntity(world, blockPos, Blocks.ORANGE_STAINED_GLASS.defaultBlockState());
                      world.removeBlockEntity(blockPos);
                      world.setBlock(offset, Blocks.ORANGE_STAINED_GLASS.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    iteratedNearby.add(offset);
                  }
                }
              }
              final int finalNumOfIteratedButNotMatch = numOfIteratedButNotMatch;
              final int finalNumOfNotIteratedButMatch = numOfNotIteratedButMatch;
              final HistoryHolder historyHolder = HistoryHolder.fromSource(context.getSource());
              if (historyHolder != null) {
                historyHolder.addUndoableHistory$ec(history);
              }
              context.getSource().sendFeedback$ecBridge(() -> Component.translatable("enhanced_commands.commands.testarg.region.illustrate.result", TextUtil.literal(region).withStyle(ChatFormatting.GRAY), Integer.toString(finalNumOfIteratedButNotMatch), Blocks.RED_STAINED_GLASS.getName(), Integer.toString(finalNumOfNotIteratedButMatch), Blocks.ORANGE_STAINED_GLASS.getName()), false);
              return numOfIteratedButNotMatch;
            }))
    );
  }

  private static <A> int executeConvertShow(Tag nbtElement, Codec<A> codec, Consumer<A> resultConsumer, HolderLookup.Provider registryLookup) throws CommandSyntaxException {
    final DataResult<Pair<A, Tag>> decode = codec.decode(registryLookup.createSerializationContext(NbtOps.INSTANCE), nbtElement);
    final A result = decode.getOrThrow(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()::create).getFirst();
    resultConsumer.accept(result);
    return 1;
  }

  private static <A extends ExpressionConvertible> int executeConvertShow(CommandContext<CommandSourceStack> context, Codec<A> codec) throws CommandSyntaxException {
    return executeConvertShow(getNbtTag(context, "nbt"), codec, a -> context.getSource().sendFeedback$ecBridge(() -> Component.literal(a.asString()).withStyle(Styles.RESULT), false), context.getSource().registryAccess());
  }

  private static <A> int executeStringShow(CommandContext<CommandSourceStack> context, A fetchedArg, FailableFunction<A, String, CommandSyntaxException> toString) throws CommandSyntaxException {
    final String s = toString.apply(fetchedArg);
    context.getSource().sendFeedback$ecBridge(() -> Component.literal(s), false);
    return 1;
  }

  private static <A> int executeStringTest(CommandContext<CommandSourceStack> context, A fetchedArg, FailableFunction<A, String, CommandSyntaxException> toString, FailableFunction<String, A, CommandSyntaxException> fromString) throws CommandSyntaxException {
    final String s = toString.apply(fetchedArg);
    context.getSource().sendFeedback$ecBridge(() -> Component.literal(s), false);
    final A second = fromString.apply(s);
    final boolean b = second.equals(fetchedArg);
    context.getSource().sendFeedback$ecBridge(() -> TextUtil.wrapBoolean(b), false);
    return BooleanUtils.toInteger(b);
  }

  private static <A, T> int executeCodecShow(CommandContext<CommandSourceStack> context, A fetchedArg, Codec<A> codec, DynamicOps<T> ops) throws CommandSyntaxException {
    ops = context.getSource().registryAccess().createSerializationContext(ops);
    executeCodecShowInternal(context, fetchedArg, codec, ops);
    return 1;
  }

  private static <A, T> int executeCodecTest(CommandContext<CommandSourceStack> context, A fetchedArg, Codec<A> codec, DynamicOps<T> ops) throws CommandSyntaxException {
    ops = context.getSource().registryAccess().createSerializationContext(ops);
    final T code = executeCodecShowInternal(context, fetchedArg, codec, ops);
    try {
      final A second = codec.decode(ops, code).getOrThrow(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()::create).getFirst();
      if (second instanceof Tag nbt) {
        context.getSource().sendFeedback$ecBridge(() -> NbtUtils.toPrettyComponent(nbt), false);
      }
      final boolean b = second.equals(fetchedArg);
      context.getSource().sendFeedback$ecBridge(() -> TextUtil.wrapBoolean(b), false);
      return BooleanUtils.toInteger(b);
    } catch (Throwable e) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create(e.toString());
    }
  }

  private static <A, T> T executeCodecShowInternal(CommandContext<CommandSourceStack> context, A fetchedArg, Codec<A> codec, DynamicOps<T> ops) throws CommandSyntaxException {
    final T code = codec.encodeStart(ops, fetchedArg).getOrThrow(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()::create);
    if (code instanceof Tag nbt) {
      context.getSource().sendFeedback$ecBridge(() -> NbtUtils.toPrettyComponent(nbt), false);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.literal(code.toString()).withStyle(Styles.RESULT), false);
    }
    return code;
  }

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(literalR2("testarg")
        .then(addValueProperties(
            literal("block_function"),
            commandBuildContext,
            "block_function",
            BlockFunctionArgument.blockFunction(commandBuildContext),
            BlockFunction::parse,
            BlockFunctionArgument::getBlockFunction,
            ExpressionConvertible::asString,
            BlockFunction.CODEC))
        .then(addValueProperties(
            literal("block_predicate"),
            commandBuildContext,
            "block_predicate",
            BlockPredicateArgument.blockPredicate(commandBuildContext),
            BlockPredicate::parse,
            BlockPredicateArgument::getBlockPredicate,
            ExpressionConvertible::asString,
            BlockPredicate.CODEC))
        .then(addValueProperties(
            literal("curve"),
            commandBuildContext,
            "curve",
            CurveArgument.curve(commandBuildContext),
            (parseContext, commandContext) -> CurveProvider.parse(parseContext).toAbsoluteRegion(commandContext.getSource()),
            CurveArgument::getCurve,
            ExpressionConvertible::asString,
            Curve.CODEC))
        .then(addValueProperties(
            literal("entity_predicate"),
            commandBuildContext,
            "entity_predicate",
            EntityPredicateArgument.entityPredicate(commandBuildContext),
            parseContext -> EntityPredicate.parse(new EntitySelectorParser(parseContext.reader(), true)),
            EntityPredicateArgument::getEntityPredicate,
            ExpressionConvertible::asString,
            EntityPredicate.CODEC))
        .then(addValueProperties(
            literal("item_predicate"),
            commandBuildContext,
            "item_predicate",
            ItemPredicateArgument.itemPredicate(commandBuildContext),
            parseContext -> ItemPredicateArgument.itemPredicate(commandBuildContext).parse(parseContext.reader()),
            ItemPredicateArgument::getItemPredicate,
            ExpressionConvertible::asString,
            ItemPredicate.CODEC))
        .then(addNbtProperties(literal("nbt"), commandBuildContext))
        .then(addNbtCompoundProperties(literal("nbt_compound")))
        .then(addValueProperties(
            addExtraNbtPredicateProperties(literal("nbt_predicate"), commandBuildContext),
            commandBuildContext,
            "nbt_predicate",
            NbtPredicateArgument.element(commandBuildContext),
            parseContext -> NbtPredicate.parse(parseContext, false, false),
            NbtPredicateArgument::getNbtPredicate,
            NbtPredicate::asString,
            NbtPredicate.CODEC))
        .then(addValueProperties(
            addExtraNbtFunctionProperties(literal("nbt_function"), commandBuildContext),
            commandBuildContext,
            "nbt_function",
            NbtFunctionArgument.element(commandBuildContext),
            parseContext -> NbtFunction.parse(parseContext, false, false),
            NbtFunctionArgument::getNbtFunction,
            NbtFunction::asString,
            NbtFunction.CODEC))
        .then(addPosProperties(literal("pos")))
        .then(addValueProperties(
            addExtraRegionProperties(literal("region"), commandBuildContext),
            commandBuildContext,
            "region",
            RegionArgument.region(commandBuildContext),
            (parseContext, commandContext) -> RegionProvider.parse(parseContext).toAbsoluteRegion(commandContext.getSource()),
            RegionArgument::getRegion,
            Region::asString,
            Region.CODEC))
    );
  }
}
