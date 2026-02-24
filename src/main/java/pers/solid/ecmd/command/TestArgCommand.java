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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
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
import org.apache.commons.lang3.function.FailableFunction;
import pers.solid.ecmd.argument.*;
import pers.solid.ecmd.curve.Curve;
import pers.solid.ecmd.curve.CurveProvider;
import pers.solid.ecmd.extensions.HistoryHolder;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.history.BlockPlacementHistory;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.predicate.entity.EntityPredicate;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.region.RegionProvider;
import pers.solid.ecmd.util.*;
import pers.solid.ecmd.util.codec.CodecUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.CompoundTagArgument.getCompoundTag;
import static net.minecraft.commands.arguments.NbtTagArgument.getNbtTag;
import static pers.solid.ecmd.argument.BlockFunctionArgument.getBlockFunction;
import static pers.solid.ecmd.argument.BlockPredicateArgument.getBlockPredicate;
import static pers.solid.ecmd.argument.CurveArgument.getCurve;
import static pers.solid.ecmd.argument.EnhancedPosArgument.getPosArgument;
import static pers.solid.ecmd.argument.EntityPredicateArgument.entityPredicate;
import static pers.solid.ecmd.argument.EntityPredicateArgument.getEntityPredicate;
import static pers.solid.ecmd.argument.NbtFunctionArgument.getNbtFunction;
import static pers.solid.ecmd.argument.NbtPredicateArgument.getNbtPredicate;
import static pers.solid.ecmd.argument.RegionArgument.getRegion;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum TestArgCommand implements CommandRegistrationCallback {
  INSTANCE;

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBlockFunctionProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("block_function", BlockFunctionArgument.blockFunction(commandBuildContext))
        .executes(context -> executeStringShow(context, getBlockFunction(context, "block_function"), ExpressionConvertible::asString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getBlockFunction(context, "block_function"), ExpressionConvertible::asString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getBlockFunction(context, "block_function"), BlockFunction.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getBlockFunction(context, "block_function"), BlockFunction.CODEC, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getBlockFunction(context, "block_function"), ExpressionConvertible::asString, s -> BlockFunction.parse(new ParseContext<>(commandBuildContext, s, false, true)))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getBlockFunction(context, "block_function"), BlockFunction.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getBlockFunction(context, "block_function"), BlockFunction.CODEC, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addBlockPredicateProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("block_predicate", BlockPredicateArgument.blockPredicate(commandBuildContext))
        .executes(context -> executeStringShow(context, getBlockPredicate(context, "block_predicate"), ExpressionConvertible::asString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getBlockPredicate(context, "block_predicate"), ExpressionConvertible::asString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getBlockPredicate(context, "block_predicate"), BlockPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getBlockPredicate(context, "block_predicate"), BlockPredicate.CODEC, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getBlockPredicate(context, "block_predicate"), ExpressionConvertible::asString, s -> BlockPredicate.parse(new ParseContext<>(commandBuildContext, s, false, true)))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getBlockPredicate(context, "block_predicate"), BlockPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getBlockPredicate(context, "block_predicate"), BlockPredicate.CODEC, JsonOps.INSTANCE)))
    );
  }

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addEntityPredicateProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("entity_predicate", entityPredicate(commandBuildContext))
        .executes(context -> executeStringShow(context, getEntityPredicate(context, "entity_predicate"), ExpressionConvertible::asString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getEntityPredicate(context, "entity_predicate"), ExpressionConvertible::asString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getEntityPredicate(context, "entity_predicate"), EntityPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getEntityPredicate(context, "entity_predicate"), EntityPredicate.CODEC, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getEntityPredicate(context, "entity_predicate"), ExpressionConvertible::asString, s -> EntityPredicateArgument.entityPredicate(commandBuildContext).parse(new StringReader(s)))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getEntityPredicate(context, "entity_predicate"), EntityPredicate.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getEntityPredicate(context, "entity_predicate"), EntityPredicate.CODEC, JsonOps.INSTANCE)))
    );
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
            .then(literal("nbt_function")
                .executes(context -> executeConvertShow(context, NbtFunction.CODEC)))
            .then(literal("nbt_predicate")
                .executes(context -> executeConvertShow(context, NbtPredicate.CODEC)))
            .then(literal("pos_argument")
                .executes(context -> executeConvertShow(context, EnhancedCoordinates.CODEC)))
            .then(literal("region")
                .executes(context -> executeConvertShow(context, Region.CODEC)))
            .then(literal("region_argument")
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

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addNbtPredicateProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("nbt_predicate", NbtPredicateArgument.element(commandBuildContext))
        .executes(context -> executeStringShow(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate::asString))
        .then(literal("match")
            .then(argument("nbt_to_test", NbtTagArgument.nbtTag())
                .executes(context -> {
                  final Tag nbtToTest = getNbtTag(context, "nbt_to_test");
                  final NbtPredicate nbtPredicate = getNbtPredicate(context, "nbt_predicate");
                  final boolean test = nbtPredicate.test(nbtToTest);
                  context.getSource().sendFeedback$ecBridge(() -> Component.literal(Boolean.toString(test)), false);
                  return BooleanUtils.toInteger(test);
                })))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate::asString)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getNbtPredicate(context, "nbt_predicate"), NbtPredicate::asString, s -> NbtPredicate.parse(new ParseContext<>(commandBuildContext, s, false, true), false, false))))
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

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addNbtFunctionProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("nbt_function", NbtFunctionArgument.element(commandBuildContext))
        .executes(context -> executeStringShow(context, getNbtFunction(context, "nbt_function"), NbtFunction::asString))
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
        .then(literal("string")
            .executes(context -> executeStringShow(context, getNbtFunction(context, "nbt_function"), NbtFunction::asString)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getNbtFunction(context, "nbt_function"), NbtFunction::asString, s -> NbtFunction.parse(new ParseContext<>(commandBuildContext, s, false, true), false, true))))
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

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addRegionProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("region", RegionArgument.region(commandBuildContext))
        .executes(context -> executeStringShow(context, getRegion(context, "region"), Region::asString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getRegion(context, "region"), Region::asString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getRegion(context, "region"), Region.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getRegion(context, "region"), Region.CODEC, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getRegion(context, "region"), Region::asString, s -> RegionProvider.parse(new ParseContext<>(commandBuildContext, s, false, true)).toAbsoluteRegion(context.getSource()))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getRegion(context, "region"), Region.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getRegion(context, "region"), Region.CODEC, JsonOps.INSTANCE)))
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

  private static <T extends ArgumentBuilder<CommandSourceStack, T>> T addCurveProperties(T argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder.then(argument("curve", CurveArgument.curve(commandBuildContext))
        .executes(context -> executeStringShow(context, getCurve(context, "curve"), Curve::asString))
        .then(literal("string")
            .executes(context -> executeStringShow(context, getCurve(context, "curve"), Curve::asString)))
        .then(literal("nbt")
            .executes(context -> executeCodecShow(context, getCurve(context, "curve"), Curve.CODEC, NbtOps.INSTANCE)))
        .then(literal("json")
            .executes(context -> executeCodecShow(context, getCurve(context, "curve"), Curve.CODEC, JsonOps.INSTANCE)))
        .then(literal("string_test")
            .executes(context -> executeStringTest(context, getCurve(context, "curve"), Curve::asString, s -> CurveProvider.parse(new ParseContext<>(commandBuildContext, s, false, true)).toAbsoluteRegion(context.getSource()))))
        .then(literal("nbt_test")
            .executes(context -> executeCodecTest(context, getCurve(context, "curve"), Curve.CODEC, NbtOps.INSTANCE)))
        .then(literal("json_test")
            .executes(context -> executeCodecTest(context, getCurve(context, "curve"), Curve.CODEC, JsonOps.INSTANCE)))
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
    final T code = codec.encodeStart(ops, fetchedArg).getOrThrow(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()::create);
    if (code instanceof Tag nbt) {
      context.getSource().sendFeedback$ecBridge(() -> NbtUtils.toPrettyComponent(nbt), false);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.literal(code.toString()).withStyle(Styles.RESULT), false);
    }
    return 1;
  }

  private static <A, T> int executeCodecTest(CommandContext<CommandSourceStack> context, A fetchedArg, Codec<A> codec, DynamicOps<T> ops) throws CommandSyntaxException {
    ops = context.getSource().registryAccess().createSerializationContext(ops);
    final T code = codec.encodeStart(ops, fetchedArg).getOrThrow(CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()::create);
    if (code instanceof Tag nbt) {
      context.getSource().sendFeedback$ecBridge(() -> NbtUtils.toPrettyComponent(nbt), false);
    } else {
      context.getSource().sendFeedback$ecBridge(() -> Component.literal(code.toString()).withStyle(Styles.RESULT), false);
    }
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

  @Override
  public void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    dispatcher.register(literalR2("testarg")
        .then(addBlockFunctionProperties(literal("block_function"), registryAccess))
        .then(addBlockPredicateProperties(literal("block_predicate"), registryAccess))
        .then(addCurveProperties(literal("curve"), registryAccess))
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
