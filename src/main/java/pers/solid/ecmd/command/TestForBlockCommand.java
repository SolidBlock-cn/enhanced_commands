package pers.solid.ecmd.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.argument.BlockPredicateArgumentType;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.util.*;

import java.util.Collection;

public enum TestForBlockCommand implements TestForCommands.Entry {
  INSTANCE;

  public static final KeywordArgsArgumentType BLOCK_KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("force_load", BoolArgumentType.bool(), false)
      .addOptionalArg("seed", LongArgumentType.longArg(), null)
      .build();
  public static final DynamicCommandExceptionType TEST_FOR_BLOCK_NOT_LOADED = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.commands.testfor.block.not_loaded", o));
  public static final DynamicCommandExceptionType TEST_FOR_BLOCK_PREDICATE_NOT_LOADED = new DynamicCommandExceptionType(o -> Component.translatable("enhanced_commands.commands.testfor.block.not_loaded_for_predicate", o));

  private static LiteralArgumentBuilder<CommandSourceStack> addBlockCommandProperties(LiteralArgumentBuilder<CommandSourceStack> argumentBuilder, CommandBuildContext commandBuildContext) {
    return argumentBuilder
        .then(Commands.argument("pos", EnhancedPosArgumentType.blockPos())
            .executes(TestForBlockCommand::executeTestForBlock)
            .then(Commands.argument("predicate", new BlockPredicateArgumentType(commandBuildContext))
                .executes(context -> executeTestForBlockPredicate(context, false, null))
                .then(Commands.argument("keyword_args", BLOCK_KEYWORD_ARGS)
                    .executes(context -> executeTestForBlockPredicate(context, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args"))))));
  }

  private static int executeTestForBlock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    final BlockPos blockPos = EnhancedPosArgumentType.getBlockPos(context, "pos");
    // 会检查区块已加载，不过不是在这里，而是在下面。
    final CommandSourceStack source = context.getSource();
    final ServerLevel world = source.getLevel();
    @SuppressWarnings("deprecation") final boolean hasChunk = world.hasChunkAt(blockPos);
    if (!hasChunk) {
      throw TEST_FOR_BLOCK_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final BlockState blockState = world.getBlockState(blockPos);
    final Collection<Property<?>> properties = blockState.getProperties();
    source.sendFeedback$ecBridge(() -> {
      final MutableComponent posText = TextUtil.wrapVector(blockPos);
      final MutableComponent resultText = blockState.getBlock().getName().withStyle(Styles.RESULT);
      final MutableComponent idText = TextUtil.literal(BuiltInRegistries.BLOCK.getKey(blockState.getBlock())).withStyle(Styles.RESULT);
      if (properties.isEmpty()) {
        return Component.translatable("enhanced_commands.commands.testfor.block.info", posText, resultText, idText);
      } else {
        return Component.translatable("enhanced_commands.commands.testfor.block.info_with_properties", posText, resultText, idText);
      }
    }, false);
    for (Property<?> property : properties) {
      source.sendFeedback$ecBridge(() -> expressPropertyValue(blockState, property), false);
    }
    return 1;
  }

  private static int executeTestForBlockPredicate(CommandContext<CommandSourceStack> context, KeywordArgs keywordArgs) throws CommandSyntaxException {
    return executeTestForBlockPredicate(context, keywordArgs.getBoolean("force_load"), keywordArgs.getArg("seed"));
  }

  private static int executeTestForBlockPredicate(CommandContext<CommandSourceStack> context, boolean forceLoad, @Nullable Long seed) throws CommandSyntaxException {
    final CommandSourceStack source = context.getSource();
    final BlockPos blockPos = EnhancedPosArgumentType.getBlockPos(context, "pos");

    // 检查方块的代码在后面
    final BlockInWorld blockInWorld = new BlockInWorld(source.getLevel(), blockPos, forceLoad);
    if (blockInWorld.getState() == null) {
      throw TEST_FOR_BLOCK_PREDICATE_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final TestResult testResult = BlockPredicateArgumentType.getBlockPredicate(context, "predicate").testAndDescribe(blockInWorld, new ExecutionContext(source.getLevel().getRandom(), PositionProvider.of(blockPos.getCenter(), Vec2.ZERO, null, EntityAnchorArgument.Anchor.FEET), seed));
    testResult.sendMessage(source);
    return BooleanUtils.toInteger(testResult.successes());
  }

  private static <T extends Comparable<T>> MutableComponent expressPropertyValue(BlockState blockState, Property<T> property) {
    final MutableComponent text = Component.literal("  ").append(property.getName()).append(" = ");
    final T value = blockState.getValue(property);
    return text.append(value instanceof Boolean bool ? Component.literal(property.getName(value)).withStyle(bool ? ChatFormatting.GREEN : ChatFormatting.RED) : Component.literal(property.getName(value)));
  }

  @Override
  public void addArguments(LiteralArgumentBuilder<CommandSourceStack> testForBuilder, CommandBuildContext commandBuildContext, Commands.CommandSelection environment) {
    testForBuilder.then(addBlockCommandProperties(Commands.literal("block"), commandBuildContext));
  }
}
