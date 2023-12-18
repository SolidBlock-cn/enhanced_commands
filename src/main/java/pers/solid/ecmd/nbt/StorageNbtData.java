package pers.solid.ecmd.nbt;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.DataCommandStorage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.function.FailableFunction;
import pers.solid.ecmd.math.NbtConcentrationType;

public record StorageNbtData(DataCommandStorage dataCommandStorage, Identifier id) implements NbtSource.Single, NbtTarget {
  @Override
  public Text feedbackQuery(NbtElement nbtElement, NbtConcentrationType nbtConcentrationType) {
    return Text.translatable("commands.data.storage.query", this.id, NbtHelper.toPrettyPrintedText(nbtElement));
  }

  @Override
  public NbtCompound getNbt() {
    return dataCommandStorage.get(id);
  }

  @Override
  public void setNbt(NbtCompound nbt) {
    dataCommandStorage.set(id, nbt);
  }

  @Override
  public void changeNbt(FailableFunction<NbtCompound, NbtCompound, CommandSyntaxException> operator) throws CommandSyntaxException {
    setNbt(operator.apply(getNbt()));
  }

  @Override
  public Text feedbackModify() {
    return Text.translatable("commands.data.storage.modified", this.id);
  }
}
