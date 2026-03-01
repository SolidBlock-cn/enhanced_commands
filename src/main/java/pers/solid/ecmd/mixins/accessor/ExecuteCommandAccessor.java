package pers.solid.ecmd.mixins.accessor;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.Tag;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Collection;
import java.util.function.IntFunction;

@Mixin(ExecuteCommand.class)
public interface ExecuteCommandAccessor {
  @Accessor
  static SimpleCommandExceptionType getERROR_CONDITIONAL_FAILED() {
    throw new AssertionError();
  }

  @Invoker("storeValue")
  static CommandSourceStack callStoreScoreValue(CommandSourceStack source, Collection<ScoreHolder> targets, Objective objective, boolean requestResult) {
    throw new AssertionError();
  }


  @Invoker
  static CommandSourceStack callStoreData(CommandSourceStack source, DataAccessor object, NbtPathArgument.NbtPath path, IntFunction<Tag> nbtSetter, boolean requestResult) {
    throw new AssertionError();
  }

  @Invoker
  static LiteralArgumentBuilder<CommandSourceStack> callCreateRelationOperations(CommandNode<CommandSourceStack> node, LiteralArgumentBuilder<CommandSourceStack> builder) {
    throw new AssertionError();
  }

  @Invoker
  static ArgumentBuilder<CommandSourceStack, ?> callAddConditionals(CommandNode<CommandSourceStack> root, LiteralArgumentBuilder<CommandSourceStack> argumentBuilder, boolean positive, CommandBuildContext commandBuildContext) {
    throw new AssertionError();
  }

  @Invoker
  static Collection<CommandSourceStack> callExpect(CommandContext<CommandSourceStack> context, boolean positive, boolean value) {
    throw new AssertionError();
  }
}
