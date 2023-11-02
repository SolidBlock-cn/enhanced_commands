package pers.solid.ecmd.command;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.mutable.MutableInt;
import pers.solid.ecmd.argument.BlockFunctionArgumentType;
import pers.solid.ecmd.argument.CurveArgumentType;
import pers.solid.ecmd.argument.KeywordArgs;
import pers.solid.ecmd.argument.KeywordArgsArgumentType;
import pers.solid.ecmd.curve.Curve;
import pers.solid.ecmd.extensions.ThreadExecutorExtension;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.region.SphereRegion;
import pers.solid.ecmd.util.TextUtil;
import pers.solid.ecmd.util.bridge.CommandBridge;
import pers.solid.ecmd.util.iterator.IterateUtils;

import java.util.Iterator;
import java.util.stream.Stream;

import static net.minecraft.server.command.CommandManager.argument;
import static pers.solid.ecmd.command.ModCommands.literalR2;

public enum DrawCommand implements CommandRegistrationCallback {
  INSTANCE;
  public static final KeywordArgsArgumentType KEYWORD_ARGS = KeywordArgsArgumentType.builder()
      .addAll(FillReplaceCommand.KEYWORD_ARGS)
      .addOptionalArg("interval", DoubleArgumentType.doubleArg(0d), 0d)
      .addOptionalArg("thickness", DoubleArgumentType.doubleArg(0d, 64d), 0d)
      .build();

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(literalR2("draw")
        .then(argument("curve", CurveArgumentType.curve(registryAccess))
            .then(argument("block", BlockFunctionArgumentType.blockFunction(registryAccess))
                .executes(context -> execute(context, false, false, 0, Block.NOTIFY_ALL, 0, 0))
                .then(argument("kwargs", KEYWORD_ARGS)
                    .executes(context -> {
                      final KeywordArgs kwargs = KeywordArgsArgumentType.getKeywordArgs(context, "kwargs");
                      return execute(context, kwargs.getBoolean("immediately"), kwargs.getBoolean("bypass_limit"), kwargs.getDouble("interval"), FillReplaceCommand.getFlags(kwargs), FillReplaceCommand.getModFlags(kwargs), kwargs.getDouble("thickness"));
                    })))));
  }

  private static int execute(CommandContext<ServerCommandSource> context, boolean immediately, boolean bypassLimit, double interval, int flags, int modFlags, double thickness) throws CommandSyntaxException {
    final Curve curve = CurveArgumentType.getCurve(context, "curve");
    if (interval > 0 && interval < 0.05) interval = 0.05;
    final double estimatedIterationAmount = curve.length() / (interval == 0 ? 1 : interval) * (thickness == 0 ? 1 : Math.max(1d, MathHelper.square(thickness) * Math.PI));
    if (!Double.isFinite(estimatedIterationAmount)) {
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
    }
    if (!bypassLimit && estimatedIterationAmount > FillReplaceCommand.REGION_SIZE_LIMIT) {
      throw FillReplaceCommand.REGION_TOO_LARGE.create(estimatedIterationAmount, FillReplaceCommand.REGION_SIZE_LIMIT);
    }
    final BlockFunction block = BlockFunctionArgumentType.getBlockFunction(context, "block");
    final ServerCommandSource source = context.getSource();
    final ServerWorld world = source.getWorld();

    final Iterator<?> mainIterator;
    final MutableInt numbersAffected = new MutableInt();
    Stream<BlockPos> stream = interval == 0 ? curve.streamBlockPos() : curve.streamPoints(interval)
        .map(BlockPos::ofFloored)
        .distinct();

    if (thickness > 0) {
      stream = stream.flatMap(pos -> new SphereRegion(thickness, pos.toCenterPos()).stream()).distinct();
    }

    mainIterator = stream
        .peek(blockPos -> {
          if (block.setBlock(world, blockPos, flags, modFlags))
            numbersAffected.increment();
        })
        .map(blockPos -> null)
        .iterator();
    final Iterator<?> iterator = Iterators.concat(mainIterator, IterateUtils.singletonPeekingIterator(() -> source.sendFeedback(TextUtil.enhancedTranslatable("enhanced_commands.commands.fill.complete", numbersAffected.getValue()), true)));
    if (!immediately && estimatedIterationAmount > 16384) {
      // The region is too large. Send a server task.
      ((ThreadExecutorExtension) source.getServer()).ec_addIteratorTask(Text.translatable("enhanced_commands.commands.draw.task_name", curve.asString()), IterateUtils.batchAndSkip(iterator, 32768, 15));
      CommandBridge.sendFeedback(source, () -> Text.translatable("enhanced_commands.commands.fill.large_region", estimatedIterationAmount).formatted(Formatting.YELLOW), true);
      return 1;
    } else {
      IterateUtils.exhaust(iterator);
      return numbersAffected.intValue();
    }
  }
}
