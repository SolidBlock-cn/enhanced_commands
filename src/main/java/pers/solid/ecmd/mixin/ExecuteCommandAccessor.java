package pers.solid.ecmd.mixin;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.DataCommandObject;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.entity.boss.CommandBossBar;
import net.minecraft.nbt.NbtElement;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.ExecuteCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Collection;
import java.util.function.IntFunction;

@Mixin(ExecuteCommand.class)
public interface ExecuteCommandAccessor {
  @Accessor
  static Dynamic2CommandExceptionType getBLOCKS_TOOBIG_EXCEPTION() {
    throw new AssertionError();
  }

  @Accessor
  static SimpleCommandExceptionType getCONDITIONAL_FAIL_EXCEPTION() {
    throw new AssertionError();
  }

  @Accessor
  static DynamicCommandExceptionType getCONDITIONAL_FAIL_COUNT_EXCEPTION() {
    throw new UnsupportedOperationException();
  }

  @Accessor
  static Dynamic2CommandExceptionType getINSTANTIATION_FAILURE_EXCEPTION() {
    throw new UnsupportedOperationException();
  }

  @Accessor
  static SuggestionProvider<ServerCommandSource> getLOOT_CONDITIONS() {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static ServerCommandSource callExecuteStoreScore(
      ServerCommandSource source, Collection<ScoreHolder> targets, ScoreboardObjective objective, boolean requestResult
  ) {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static ServerCommandSource callExecuteStoreBossbar(ServerCommandSource source, CommandBossBar bossBar, boolean storeInValue, boolean requestResult) {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static ServerCommandSource callExecuteStoreData(
      ServerCommandSource source, DataCommandObject object, NbtPathArgumentType.NbtPath path, IntFunction<NbtElement> nbtSetter, boolean requestResult
  ) {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static boolean callIsLoaded(ServerWorld world, BlockPos pos) {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static ArgumentBuilder<ServerCommandSource, ?> callAddBlocksConditionLogic(
      CommandNode<ServerCommandSource> root, ArgumentBuilder<ServerCommandSource, ?> builder, boolean positive, boolean masked
  ) {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static int callExecutePositiveBlockCondition(CommandContext<ServerCommandSource> context, boolean masked) throws CommandSyntaxException {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static int callExecuteNegativeBlockCondition(CommandContext<ServerCommandSource> context, boolean masked) throws CommandSyntaxException {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static LiteralArgumentBuilder<ServerCommandSource> callAddOnArguments(
      CommandNode<ServerCommandSource> node, LiteralArgumentBuilder<ServerCommandSource> builder
  ) {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static ArgumentBuilder<ServerCommandSource, ?> callAddConditionArguments(
      CommandNode<ServerCommandSource> root,
      LiteralArgumentBuilder<ServerCommandSource> argumentBuilder,
      boolean positive,
      CommandRegistryAccess commandRegistryAccess
  ) {
    throw new UnsupportedOperationException();
  }

  @Invoker
  static Collection<ServerCommandSource> callGetSourceOrEmptyForConditionFork(CommandContext<ServerCommandSource> context, boolean positive, boolean value) {
    throw new UnsupportedOperationException();
  }
}
