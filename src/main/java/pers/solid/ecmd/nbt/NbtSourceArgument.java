package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;

public interface NbtSourceArgument {
  NbtSource getNbtSource(ServerCommandSource source) throws CommandSyntaxException;
}
