package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;

public interface NbtTarget {
  void setNbt(NbtCompound nbt) throws CommandSyntaxException;

  void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator) throws CommandSyntaxException;

  default void modifyNbt(FailableConsumer<NbtCompound, CommandSyntaxException> consumer) throws CommandSyntaxException {
    changeNbt(nbtCompound -> {
      consumer.accept(nbtCompound);
      return nbtCompound;
    });
  }

  default void modifyNbt(NbtPathArgumentType.NbtPath nbtPath, NbtElement element) throws CommandSyntaxException {
    modifyNbt(nbt -> nbtPath.put(nbt, element));
  }

  Text feedbackModify();
}
