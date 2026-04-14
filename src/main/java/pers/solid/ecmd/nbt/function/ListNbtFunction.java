package pers.solid.ecmd.nbt.function;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.ExecutionContext;

public interface ListNbtFunction extends NbtFunction {
  @Override
  default Tag apply(@Nullable Tag nbtElement, ExecutionContext context) throws CommandSyntaxException {
    final ListTag targetList = nbtElement instanceof final ListTag nbtList ? nbtList : new ListTag();
    return applyOnList(targetList, context);
  }

  ListTag applyOnList(ListTag listTag, ExecutionContext context) throws CommandSyntaxException;
}
