package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.EntityDataObject;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import org.apache.commons.lang3.function.FailableFunction;
import pers.solid.ecmd.math.NbtConcentrationType;

public record EntityNbtData(EntityDataObject entityDataObject) implements NbtSource.Single, NbtTarget {
  @Override
  public Text feedbackQuery(NbtElement nbtElement, NbtConcentrationType nbtConcentrationType) {
    return entityDataObject.feedbackQuery(nbtElement);
  }

  @Override
  public NbtCompound getNbt() {
    return entityDataObject.getNbt();
  }

  @Override
  public void setNbt(NbtCompound nbt) throws CommandSyntaxException {
    entityDataObject.setNbt(nbt);
  }

  @Override
  public void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator) throws CommandSyntaxException {
    entityDataObject.setNbt(operator.apply(entityDataObject.getNbt()));
  }

  @Override
  public Text feedbackModify() {
    return entityDataObject.feedbackModify();
  }
}
