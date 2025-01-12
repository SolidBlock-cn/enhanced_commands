package pers.solid.ecmd.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import pers.solid.ecmd.argument.BlockPredicateArgumentType;
import pers.solid.ecmd.argument.RegionArgumentType;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.StringUtil;
import pers.solid.ecmd.util.TextUtil;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public enum TestForBlocksCommand implements TestForCommands.Entry {
  INSTANCE;

  @Override
  public void addArguments(LiteralArgumentBuilder<ServerCommandSource> testForBuilder, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    // todo expand this
    testForBuilder.then(literal("blocks")
        .then(argument("region", RegionArgumentType.region(registryAccess))
            .then(argument("block_predicate", BlockPredicateArgumentType.blockPredicate(registryAccess))
                .executes(commandContext -> {
                  final BlockPredicate blockPredicate = BlockPredicateArgumentType.getBlockPredicate(commandContext, "block_predicate");
                  final Region region = RegionArgumentType.getRegion(commandContext, "region");

                  final ServerCommandSource source = commandContext.getSource();
                  for (BlockPos blockPos : region) {
                    if (!source.getWorld().isChunkLoaded(blockPos)) {
                      continue;
                    }

                    final CachedBlockPosition cachedBlockPosition = new CachedBlockPosition(source.getWorld(), blockPos, false);
                    if (blockPredicate.test(cachedBlockPosition)) {
                      source.sendFeedback$ecBridge(() -> TextUtil.wrapVector(blockPos).styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @s " + StringUtil.wrapPosition(blockPos.toBottomCenterPos())))).append(" ").append(cachedBlockPosition.getBlockState().getBlock().getName()), true);
                      return 1;
                    }
                  }

                  source.sendFeedback$ecBridge(() -> Text.literal("not found").formatted(Formatting.RED), true);
                  return 0;
                }))));
  }
}
