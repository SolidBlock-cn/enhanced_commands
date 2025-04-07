package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;

public interface NbtTargetArgument<T> {
  NbtTarget<T> getNbtTarget(ServerCommandSource source) throws CommandSyntaxException;
}
