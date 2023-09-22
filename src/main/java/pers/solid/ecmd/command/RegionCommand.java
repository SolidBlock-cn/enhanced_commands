package pers.solid.ecmd.command;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import pers.solid.ecmd.argument.RegionArgumentType;
import pers.solid.ecmd.region.Region;
import pers.solid.ecmd.util.bridge.CommandBridge;

import java.util.HashSet;
import java.util.Set;

public enum RegionCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
    dispatcher.register(CommandManager.literal("region")
        .then(addVerifyArguments(CommandManager.literal("verify"), registryAccess)));
  }

  private static <T extends ArgumentBuilder<ServerCommandSource, T>> T addVerifyArguments(T builder, CommandRegistryAccess registryAccess) {
    return builder.then(CommandManager.argument("region", RegionArgumentType.region(registryAccess))
        .executes(context -> executeVerify(context, RegionArgumentType.getRegion(context, "region"))));
  }

  private static int executeVerify(CommandContext<ServerCommandSource> context, Region region) {
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
    CommandBridge.sendFeedback(context, () -> Text.translatable("enhancedCommands.commands.region.verify.result", Text.literal(region.asString()).formatted(Formatting.GRAY), Integer.toString(finalNumOfIteratedButNotMatch), Blocks.RED_STAINED_GLASS.getName(), Integer.toString(finalNumOfNotIteratedButMatch), Blocks.ORANGE_STAINED_GLASS.getName()), true);
    return numOfIteratedButNotMatch;
  }
}
