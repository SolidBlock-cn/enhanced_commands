package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.execution.tasks.ExecuteCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;

@Mixin(ExecuteCommand.class)
public abstract class ExecuteCommandMixin {
  /**
   * 在执行方法时，捕获本模组中的 {@link CommandRuntimeException}，并将其转化为 {@link CommandSyntaxException} 抛出。
   *
   * @see ContextChainMixin#catchCallingRun
   */
  @WrapOperation(method = "execute(Lnet/minecraft/commands/ExecutionCommandSource;Lnet/minecraft/commands/execution/ExecutionContext;Lnet/minecraft/commands/execution/Frame;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/context/ContextChain;runExecutable(Lcom/mojang/brigadier/context/CommandContext;Ljava/lang/Object;Lcom/mojang/brigadier/ResultConsumer;Z)I"))
  private static <S> int catchCallingRunExecutable(CommandContext<S> executable, S source, ResultConsumer<S> resultConsumer, boolean forkedMode, Operation<Integer> original) throws CommandSyntaxException {
    try {
      return original.call(executable, source, resultConsumer, forkedMode);
    } catch (CommandRuntimeException e) {
      if (e.getCause() instanceof CommandSyntaxException commandSyntaxException) {
        throw commandSyntaxException;
      } else {
        throw EnhancedCommandsCommandExceptionTypes.DIRECT.create(e.rawMessage);
      }
    }
  }
}
