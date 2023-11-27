package pers.solid.ecmd.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.BooleanUtils;
import pers.solid.ecmd.argument.BlockPredicateArgumentType;
import pers.solid.ecmd.argument.EnhancedPosArgumentType;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.Collection;

public enum TestForBlockCommand implements TestForCommands.Entry {
  INSTANCE;

  public static final KeywordArgsArgumentType BLOCK_KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addOptionalArg("force_load", BoolArgumentType.bool(), false)
      .build();
  public static final DynamicCommandExceptionType TEST_FOR_BLOCK_NOT_LOADED = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.testfor.block.not_loaded", o));
  public static final DynamicCommandExceptionType TEST_FOR_BLOCK_PREDICATE_NOT_LOADED = new DynamicCommandExceptionType(o -> Text.translatable("enhanced_commands.commands.testfor.block.not_loaded_for_predicate", o));

  private static LiteralArgumentBuilder<ServerCommandSource> addBlockCommandProperties(LiteralArgumentBuilder<ServerCommandSource> argumentBuilder, CommandRegistryAccess registryAccess) {
    return argumentBuilder
        .then(CommandManager.argument("pos", EnhancedPosArgumentType.blockPos())
            .executes(TestForBlockCommand::executeTestForBlock)
            .then(CommandManager.argument("predicate", new BlockPredicateArgumentType(registryAccess))
                .executes(context -> executeTestForBlockPredicate(context, false))
                .then(CommandManager.argument("keyword_args", BLOCK_KEYWORD_ARGS)
                    .executes(context -> executeTestForBlockPredicate(context, KeywordArgsArgumentType.getKeywordArgs(context, "keyword_args").getBoolean("force_load"))))));
  }

  private static int executeTestForBlock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final BlockPos blockPos = EnhancedPosArgumentType.getBlockPos(context, "pos");
    // 会检查区块已加载，不过不是在这里，而是在下面。
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();
    if (!world.isChunkLoaded(blockPos)) {
      throw TEST_FOR_BLOCK_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final BlockState blockState = world.getBlockState(blockPos);
    final Collection<Property<?>> properties = blockState.getProperties();
    CommandBridge.sendFeedback(source, () -> Text.translatable(properties.isEmpty() ? "enhanced_commands.commands.testfor.block.info" : "enhanced_commands.commands.testfor.block.info_with_properties", TextUtil.wrapVector(blockPos), blockState.getBlock().getName().styled(TextUtil.STYLE_FOR_RESULT), TextUtil.literal(Registries.BLOCK.getId(blockState.getBlock())).styled(TextUtil.STYLE_FOR_RESULT)), false);
    for (Property<?> property : properties) {
      CommandBridge.sendFeedback(source, () -> expressPropertyValue(blockState, property), false);
    }
    return 1;
  }

  private static int executeTestForBlockPredicate(CommandContext<ServerCommandSource> context, boolean forceLoad) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final BlockPos blockPos = EnhancedPosArgumentType.getBlockPos(context, "pos");
    // 检查方块的代码在后面
    final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(source.getWorld(), blockPos, forceLoad);
    if (cachedBlockPosition.getBlockState() == null) {
      throw TEST_FOR_BLOCK_PREDICATE_NOT_LOADED.create(TextUtil.wrapVector(blockPos));
    }
    final TestResult testResult = BlockPredicateArgumentType.getBlockPredicate(context, "predicate").testAndDescribe(cachedBlockPosition);
    testResult.sendMessage(source);
    return BooleanUtils.toInteger(testResult.successes());
  }

  private static <T extends Comparable<T>> MutableText expressPropertyValue(BlockState blockState, Property<T> property) {
    final MutableText text = Text.literal("  ").append(property.getName()).append(" = ");
    final T value = blockState.get(property);
    return text.append(value instanceof Boolean bool ? Text.literal(property.name(value)).formatted(bool ? Formatting.GREEN : Formatting.RED) : Text.literal(property.name(value)));
  }

  @Override
  public void addArguments(LiteralArgumentBuilder<ServerCommandSource> testForBuilder, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    testForBuilder.then(addBlockCommandProperties(CommandManager.literal("block"), registryAccess));
  }
}
