package pers.solid.ecmd.mixins.general;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pers.solid.ecmd.exception.CommandRuntimeException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;

@Mixin(ContextChain.class)
public abstract class ContextChainMixin {
  /**
   * 在调用 {@code run} 方法时，捕获本模组的 {@link CommandRuntimeException}，转化为 {@link CommandSyntaxException} 并抛出。
   *
   * @see ExecuteCommandMixin#catchCallingRunExecutable
   */
  @WrapOperation(method = "runExecutable", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/Command;run(Lcom/mojang/brigadier/context/CommandContext;)I"))
  private static <S> int catchCallingRun(Command<S> instance, CommandContext<S> context, Operation<Integer> original) throws CommandSyntaxException {
    try {
      return original.call(instance, context);
    } catch (CommandRuntimeException e) {
      if (e.getCause() instanceof CommandSyntaxException commandSyntaxException) {
        throw commandSyntaxException;
      } else {
        throw EnhancedCommandsCommandExceptionTypes.DIRECT.create(e.rawMessage);
      }
    }
  }
}
