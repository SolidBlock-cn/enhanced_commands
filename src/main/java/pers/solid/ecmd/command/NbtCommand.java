package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.argument.NbtFunctionArgumentType;
import pers.solid.ecmd.argument.NbtPredicateArgumentType;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.nbt.NbtSource;
import pers.solid.ecmd.nbt.NbtTarget;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static net.minecraft.command.argument.NbtPathArgumentType.getNbtPath;
import static net.minecraft.command.argument.NbtPathArgumentType.nbtPath;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.NbtFunctionArgumentType.getNbtFunction;
import static pers.solid.ecmd.argument.NbtPredicateArgumentType.getNbtPredicate;
import static pers.solid.ecmd.argument.NbtSourceArgumentType.getNbtSource;
import static pers.solid.ecmd.argument.NbtSourceArgumentType.nbtSource;
import static pers.solid.ecmd.argument.NbtTargetArgumentType.getNbtTarget;
import static pers.solid.ecmd.argument.NbtTargetArgumentType.nbtTarget;

public enum NbtCommand implements CommandRegistrationCallback {
  INSTANCE;

  @Override
  public void register(CommandDispatcher<ServerCommandSource> commandDispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
    commandDispatcher.register(literal("nbt")
        .then(literal("get")
            .then(argument("source", nbtSource(commandRegistryAccess))
                .executes(context -> executeGet(getNbtSource(context, "source"), context))
                .then(argument("path", nbtPath())
                    .executes(context -> executeGet(getNbtSource(context, "source"), getNbtPath(context, "path"), 1, context))
                    .then(argument("scale", doubleArg())
                        .executes(context -> executeGet(getNbtSource(context, "source"), getNbtPath(context, "path"), getDouble(context, "scale"), context))))))
        .then(literal("set")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("nbt_function", NbtFunctionArgumentType.ELEMENT)
                    .executes(context -> executeSet(getNbtTarget(context, "target"), getNbtFunction(context, "nbt_function"), context)))))
        .then(literal("replace")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("nbt_predicate", NbtPredicateArgumentType.ELEMENT)
                    .then(argument("nbt_function", NbtFunctionArgumentType.ELEMENT)
                        .executes(context -> executeReplace(getNbtTarget(context, "target"), getNbtPredicate(context, "nbt_predicate"), getNbtFunction(context, "nbt_function"), context)))))));
  }

  private static int executeGet(NbtSource<?> nbtSource, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    return nbtSource.executeQuery(source, null, 1);
  }

  private static int executeGet(NbtSource<?> nbtSource, NbtPathArgumentType.NbtPath nbtPath, double scale, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    return nbtSource.executeQuery(source, nbtPath, scale);
  }

  private static int executeSet(NbtTarget<?> target, NbtFunction nbtFunction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    target.transformNbt(nbtCompound -> nbtFunction.apply(nbtCompound) instanceof final NbtCompound newCompound ? newCompound : nbtCompound, source.getRegistryManager());
    source.sendFeedback$ecBridge(target::feedbackModify, true);
    return 1; // 应该修改为执行成功数量
  }

  private static int executeReplace(NbtTarget<?> target, NbtPredicate nbtPredicate, NbtFunction nbtFunction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    target.transformNbt(nbtCompound -> nbtFunction.recursivelyApply(nbtCompound, nbtPredicate) instanceof final NbtCompound newCompound ? newCompound : nbtCompound, source.getRegistryManager());
    source.sendFeedback$ecBridge(target::feedbackModify, true);
    return 1; // 应该修改为执行成功数量
  }
}
