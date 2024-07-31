package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;

public interface NbtTarget {
  void setNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException;

  void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException;

  default void modifyNbt(FailableConsumer<NbtCompound, CommandSyntaxException> consumer, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    changeNbt(nbtCompound -> {
      consumer.accept(nbtCompound);
      return nbtCompound;
    }, registryLookup);
  }

  default void modifyNbt(NbtPathArgumentType.NbtPath nbtPath, NbtElement element, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    modifyNbt(nbt -> nbtPath.put(nbt, element), registryLookup);
  }

  Text feedbackModify();
}
