package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.BlockDataObject;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableFunction;

public record BlockNbtData(BlockDataObject blockDataObject) implements NbtSource.Single, NbtTarget {
  @Override
  public Text feedbackQuery(NbtElement element) {
    return blockDataObject.feedbackQuery(element);
  }

  @Override
  public NbtCompound getNbt() {
    return blockDataObject.getNbt();
  }

  @Override
  public void setNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
    blockDataObject.setNbt(nbt);
  }

  @Override
  public void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator, RegistryWrapper.WrapperLookup registryLookup) throws CommandSyntaxException {
    blockDataObject.setNbt(operator.apply(blockDataObject.getNbt()));
  }

  @Override
  public Text feedbackModify() {
    return blockDataObject.feedbackModify();
  }
}
