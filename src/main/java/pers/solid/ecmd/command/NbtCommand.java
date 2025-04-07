package pers.solid.ecmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.commons.lang3.function.FailableFunction;
import pers.solid.ecmd.argument.NbtFunctionArgumentType;
import pers.solid.ecmd.function.nbt.NbtFunction;
import pers.solid.ecmd.nbt.NbtSource;
import pers.solid.ecmd.nbt.NbtTarget;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static pers.solid.ecmd.argument.NbtFunctionArgumentType.getNbtFunction;
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
                .then(argument("path", NbtPathArgumentType.nbtPath())
                    .executes(context -> executeGet(getNbtSource(context, "source"), NbtPathArgumentType.getNbtPath(context, "path"), context)))))
        .then(literal("set")
            .then(argument("target", nbtTarget(commandRegistryAccess))
                .then(argument("nbt_function", NbtFunctionArgumentType.ELEMENT)
                    .executes(context -> executeSet(getNbtTarget(context, "target"), getNbtFunction(context, "nbt_function"), context))))));
  }

  private int executeGet(NbtSource nbtSource, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final Collection<NbtCompound> nbts = nbtSource.getNbts(source.getRegistryManager());
    final NbtElement concentratedNbts = nbtSource.getConcentratedNbts(FailableFunction.identity(), source.getWorld().getRegistryManager());
    source.sendFeedback$ecBridge(() -> nbtSource.feedbackQuery(concentratedNbts), false);
    return nbts.size();
  }

  private int executeGet(NbtSource nbtSource, NbtPathArgumentType.NbtPath nbtPath, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    final Collection<NbtCompound> nbts = nbtSource.getNbts(source.getRegistryManager());
    final NbtElement concentratedNbts = nbtSource.getConcentratedNbts(nbtPath, source.getWorld().getRegistryManager());
    source.sendFeedback$ecBridge(() -> nbtSource.feedbackQuery(concentratedNbts), false);
    return nbts.size();
  }

  private int executeSet(NbtTarget target, NbtFunction nbtFunction, CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    final ServerCommandSource source = context.getSource();
    target.changeNbt(nbtCompound -> nbtFunction.apply(nbtCompound) instanceof final NbtCompound newCompound ? newCompound : nbtCompound, source.getRegistryManager());
    source.sendFeedback$ecBridge(target::feedbackModify, true);
    return 1; // 应该修改为执行成功数量
  }
}
